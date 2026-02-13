# ============================================================================
# FILE: model_manager.py  — Llama 3 + PEFT/LoRA versiyonu  (v4 - Server Compatible)
# ============================================================================
#
# v4 Düzeltmeleri (V3 Server Uyumluluğu):
#   1. get_health_info() metodu eklendi (V3'teki gibi)
#   2. predict() dönüş formatı V3 ile %100 uyumlu hale getirildi
#   3. include_schema parametresi eklendi
#   4. Tüm response field'ları V3 formatında (status, confidence, vb.)
#   5. Error handling V3 ile aynı formatta
# ============================================================================

import os
import torch
import time
import re
import logging
from datetime import datetime
from typing import Dict, Any, Optional, List

from transformers import AutoTokenizer, AutoModelForCausalLM

from config import ServerConfig
from utils import validate_sql, normalize_sql

logger = logging.getLogger(__name__)

# PEFT kurulu mu kontrol et (opsiyonel bağımlılık)
try:
    from peft import PeftModel
    PEFT_AVAILABLE = True
except ImportError:
    PEFT_AVAILABLE = False
    logger.warning("peft kütüphanesi bulunamadı. LoRA adapter desteği devre dışı.")

# Llama 3 base model — tokenizer bozuksa buradan çekilir
LLAMA3_BASE_MODEL = "meta-llama/Meta-Llama-3-8B-Instruct"
HF_TOKEN = "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"


class ModelManager:
    """
    Model manager — PEFT/LoRA ile fine-tune edilmiş Llama 3 Text-to-SQL modeli için.
    V3 Server endpoint'leri ile tam uyumlu.

    Model klasörü iki senaryoyu otomatik algılar:
    A) Merged model:
       v4/ ── config.json, model-*.safetensors, tokenizer.json, ...
    B) LoRA adapter (birleştirilmemiş):
       v4/ ── adapter_config.json, adapter_model.safetensors, tokenizer.json, ...
       Bu durumda base model HF Hub'dan indirilir.
    """

    def __init__(self):
        self.model = None
        self.tokenizer = None
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        ServerConfig.DEVICE = str(self.device)

        self.model_loaded = False
        self.total_requests = 0
        self.dialog_requests = 0
        self.single_turn_requests = 0
        self.total_processing_time = 0.0
        self.start_time = time.time()

    # ------------------------------------------------------------------
    # MODEL YÜKLEME
    # ------------------------------------------------------------------

    def load_model(self):
        """Llama 3 modelini ve tokenizer'ı yükler (PEFT desteğiyle)."""
        try:
            model_path = ServerConfig.MODEL_PATH
            logger.info(f"Loading model from {model_path}")
            logger.info(f"Using device: {self.device}")

            # ── TOKENIZER ──────────────────────────────────────────────
            self.tokenizer = self._load_tokenizer(model_path)

            if self.tokenizer.pad_token is None:
                self.tokenizer.pad_token = self.tokenizer.eos_token
                self.tokenizer.pad_token_id = self.tokenizer.eos_token_id

            # ── MODEL ──────────────────────────────────────────────────
            is_lora_adapter = os.path.exists(
                os.path.join(model_path, "adapter_config.json")
            )

            if is_lora_adapter:
                self.model = self._load_peft_model(model_path)
            else:
                self.model = self._load_full_model(model_path)

            self.model.eval()
            self.model_loaded = True

            param_count = sum(p.numel() for p in self.model.parameters()) / 1e6
            logger.info("✓ Model yüklendi")
            logger.info(f"  Tür          : {'LoRA adapter' if is_lora_adapter else 'Full/Merged model'}")
            logger.info(f"  Parametreler : {param_count:.0f}M")
            logger.info(f"  Max src len  : {ServerConfig.MAX_SOURCE_LENGTH}")
            logger.info(f"  Max tgt len  : {ServerConfig.MAX_TARGET_LENGTH}")
            logger.info(f"  Max context  : {ServerConfig.MAX_CONVERSATION_HISTORY}")

        except Exception as e:
            logger.error(f"Model yüklenemedi: {str(e)}")
            raise

    def _load_tokenizer(self, model_path: str):
        """
        Tokenizer yükleme — 3 aşamalı fallback:
          1. Yerel fast tokenizer (tokenizer.json)
          2. Yerel slow tokenizer (use_fast=False)
          3. HF Hub'dan Llama 3 tokenizer (tokenizer.json bozuksa)
        """
        # Deneme 1: fast tokenizer (yerel)
        try:
            tok = AutoTokenizer.from_pretrained(model_path, use_fast=True)
            logger.info("✓ Fast tokenizer yüklendi (yerel)")
            return tok
        except Exception as e1:
            logger.warning(f"Fast tokenizer başarısız: {e1}")

        # Deneme 2: slow tokenizer (yerel)
        try:
            tok = AutoTokenizer.from_pretrained(model_path, use_fast=False)
            logger.info("✓ Slow tokenizer yüklendi (yerel)")
            return tok
        except Exception as e2:
            logger.warning(f"Slow tokenizer da başarısız: {e2}")

        # Deneme 3: HF Hub fallback
        logger.info(f"Tokenizer HF Hub'dan çekiliyor: {LLAMA3_BASE_MODEL}")
        tok = AutoTokenizer.from_pretrained(LLAMA3_BASE_MODEL, use_fast=True, token=HF_TOKEN)
        logger.info("✓ Tokenizer HF Hub'dan yüklendi")
        return tok

    def _load_full_model(self, model_path: str) -> AutoModelForCausalLM:
        """Merged / full safetensors modelini yükler."""

        if torch.cuda.is_available():
            # GPU var → 4-bit quantization ile yükle (GTX 1650 için şart)
            try:
                from transformers import BitsAndBytesConfig
                bnb_config = BitsAndBytesConfig(
                    load_in_4bit=True,
                    bnb_4bit_compute_dtype=torch.float16,
                    bnb_4bit_use_double_quant=True,
                    bnb_4bit_quant_type="nf4",
                )
                model = AutoModelForCausalLM.from_pretrained(
                    model_path,
                    quantization_config=bnb_config,
                    device_map="auto",
                    low_cpu_mem_usage=True,
                )
                logger.info("✓ Model 4-bit quantization ile GPU'ya yüklendi")
            except Exception as e:
                logger.warning(f"4-bit yükleme başarısız, float16 deneniyor: {e}")
                model = AutoModelForCausalLM.from_pretrained(
                    model_path,
                    torch_dtype=torch.float16,
                    device_map="auto",
                    low_cpu_mem_usage=True,
                )
        else:
            # CPU fallback
            model = AutoModelForCausalLM.from_pretrained(
                model_path,
                torch_dtype=torch.float32,
                low_cpu_mem_usage=True,
            )
            model = model.to(self.device)

        logger.info("✓ Full/Merged model yüklendi")
        return model

    def _load_peft_model(self, adapter_path: str):
        """LoRA adapter modelini yükler (base model ayrıca Hub'dan indirilir)."""
        if not PEFT_AVAILABLE:
            raise ImportError(
                "PEFT modeli için 'peft' kütüphanesi gerekli: pip install peft"
            )

        import json
        with open(os.path.join(adapter_path, "adapter_config.json")) as f:
            adapter_cfg = json.load(f)

        base_model_name = adapter_cfg.get("base_model_name_or_path", LLAMA3_BASE_MODEL)
        logger.info(f"LoRA adapter bulundu. Base model: {base_model_name}")

        load_kwargs: Dict[str, Any] = {
            "torch_dtype": torch.float16 if torch.cuda.is_available() else torch.float32,
            "low_cpu_mem_usage": True,
        }
        if torch.cuda.is_available():
            load_kwargs["device_map"] = "auto"

        logger.info("Base model yükleniyor (bu birkaç dakika sürebilir)...")
        base_model = AutoModelForCausalLM.from_pretrained(base_model_name, **load_kwargs)

        logger.info("LoRA adapter uygulanıyor...")
        model = PeftModel.from_pretrained(base_model, adapter_path)

        if not torch.cuda.is_available():
            model = model.to(self.device)

        logger.info("✓ LoRA adapter yüklendi ve uygulandı")
        return model

    # ------------------------------------------------------------------
    # PROMPT OLUŞTURMA — EĞİTİMLE AYNI FORMAT
    # ------------------------------------------------------------------

    def create_dialog_input(
        self,
        question: str,
        schema: Optional[str] = None,
        conversation_history: Optional[List[Dict[str, str]]] = None,
    ) -> str:
        """
        Eğitim notebook'undaki formatla birebir aynı Llama 3 chat prompt'u üretir.

        Eğitim kodu (notebook'tan):
            text = (
                "<|begin_of_text|>"
                "<|start_header_id|>user<|end_header_id|>\\n\\n"
                "{instruction}"
                "<|eot_id|>"
                "<|start_header_id|>assistant<|end_header_id|>\\n\\n"
                "{sql}"
                "<|eot_id|>"
            )

        Inference'ta sql kısmını çıkarıp modelin tamamlamasını bekliyoruz.
        """
        # Şema bölümü
        schema_section = ""
        if schema:
            # V3'teki gibi şema truncation
            if len(schema) > ServerConfig.MAX_SCHEMA_LENGTH:
                schema = schema[:ServerConfig.MAX_SCHEMA_LENGTH] + "..."
            schema_section = f"\n\nDatabase Schema:\n{schema.strip()}"

        # Konuşma geçmişi bölümü
        history_section = ""
        has_context = bool(conversation_history and len(conversation_history) > 0)
        if has_context:
            max_turns = min(len(conversation_history), ServerConfig.MAX_CONVERSATION_HISTORY)
            recent = conversation_history[-max_turns:]
            lines = []
            for turn in recent:
                q = (turn.get("utterance") or turn.get("question", "")).strip()
                s = (turn.get("query") or turn.get("sql", "")).strip()
                s = normalize_sql(s)  # V3'teki gibi normalize et
                lines.append(f"Previous Question: {q}")
                lines.append(f"Previous SQL: {s}")
            history_section = "\n\n" + "\n".join(lines)

        instruction = (
            "You are a helpful assistant that converts natural language questions to SQL queries.\n"
            "Given a database schema and a question, generate the corresponding SQL query."
            f"{schema_section}"
            f"{history_section}"
            f"\n\nQuestion: {question.strip()}"
            "\n\nSQL Query:"
        )

        # Llama 3 chat sarmalayıcısı — eğitimle birebir aynı
        return (
            "<|begin_of_text|>"
            "<|start_header_id|>user<|end_header_id|>\n\n"
            f"{instruction}"
            "<|eot_id|>"
            "<|start_header_id|>assistant<|end_header_id|>\n\n"
        )

    # ------------------------------------------------------------------
    # INFERENCE — V3 SERVER UYUMLU
    # ------------------------------------------------------------------

    def predict(
        self,
        question: str,
        schema: Optional[str] = None,
        conversation_history: Optional[List[Dict[str, str]]] = None,
        include_schema: bool = True,  # ✅ V3 parametresi eklendi
        num_beams: int = 5  # ✅ V3 default değeri
    ) -> Dict[str, Any]:
        """
        Verilen sorudan SQL üretir.
        V3 server endpoint'leri ile %100 uyumlu dönüş formatı.
        """
        if not self.model_loaded:
            raise RuntimeError("Model not loaded")

        start_time = time.time()

        # ✅ V3'teki gibi context kontrolü
        has_context = bool(conversation_history and len(conversation_history) > 0)

        try:
            # ✅ include_schema parametresini kullan (V3'teki gibi)
            prompt = self.create_dialog_input(
                question=question,
                schema=schema if include_schema else None,
                conversation_history=conversation_history
            )

            logger.debug(f"Input length: {len(prompt)} chars")
            logger.debug(f"Input preview: {prompt[:200]}...")

            inputs = self.tokenizer(
                prompt,
                return_tensors="pt",
                max_length=ServerConfig.MAX_SOURCE_LENGTH,
                truncation=True,
                padding=False,
            )

            # Tensor'ları modelin cihazına taşı
            try:
                input_device = next(self.model.parameters()).device
            except StopIteration:
                input_device = self.device

            inputs = {k: v.to(input_device) for k, v in inputs.items()}
            prompt_len = inputs["input_ids"].shape[1]

            # EOS listesi: eos_token_id + <|eot_id|> (128009)
            eot_id = self.tokenizer.convert_tokens_to_ids("<|eot_id|>")
            eos_ids = list({self.tokenizer.eos_token_id, eot_id} - {None, -1})

            with torch.no_grad():
                output_ids = self.model.generate(
                    **inputs,
                    max_new_tokens=ServerConfig.MAX_TARGET_LENGTH,
                    num_beams=num_beams,
                    do_sample=False,
                    temperature=None,
                    top_p=None,
                    pad_token_id=self.tokenizer.pad_token_id,
                    eos_token_id=eos_ids,
                    early_stopping=(num_beams > 1),
                )

            # Sadece yeni üretilen token'ları decode et
            generated_ids = output_ids[0][prompt_len:]
            raw_output = self.tokenizer.decode(generated_ids, skip_special_tokens=True)

            predicted_sql = self._extract_sql(raw_output)
            predicted_sql = normalize_sql(predicted_sql)

            # Validate
            is_valid, validation_error = validate_sql(predicted_sql)

            # ✅ V3'teki gibi confidence hesaplama (Llama'da scores yok, None döndür)
            confidence = None  # Causal LM'de sequence scores yok

            # Update stats
            processing_time = (time.time() - start_time) * 1000
            self.total_requests += 1
            if has_context:
                self.dialog_requests += 1
            else:
                self.single_turn_requests += 1
            self.total_processing_time += processing_time

            # ✅ V3 ile %100 uyumlu dönüş formatı
            return {
                "status": "success",  # ✅ V3'teki gibi status field
                "sql": predicted_sql,
                "is_valid": bool(is_valid),
                "validation_error": validation_error,
                "confidence": confidence,  # None (Llama'da yok)
                "schema_used": bool(include_schema and schema is not None),  # ✅ V3 formatı
                "context_used": bool(has_context),
                "context_turns": len(conversation_history) if has_context else 0,
                "processing_time_ms": int(processing_time),
                "metadata": {
                    "model_version": "llama3-text2sql-lora-v1.0",
                    "timestamp": datetime.utcnow().isoformat() + "Z",
                    "device": str(self.device),
                    "num_beams": num_beams,
                    "input_length": len(prompt),
                    "max_context_turns": ServerConfig.MAX_CONVERSATION_HISTORY
                }
            }

        except Exception as e:
            logger.error(f"Prediction error: {str(e)}", exc_info=True)
            processing_time = (time.time() - start_time) * 1000

            # ✅ V3 ile %100 uyumlu error formatı
            return {
                "status": "error",  # ✅ V3'teki gibi
                "sql": None,
                "is_valid": False,
                "validation_error": None,
                "confidence": None,
                "schema_used": False,
                "context_used": False,
                "context_turns": 0,
                "processing_time_ms": int(processing_time),
                "error": str(e)
            }

    # ------------------------------------------------------------------
    # YARDIMCI METODLAR
    # ------------------------------------------------------------------

    @staticmethod
    def _extract_sql(raw: str) -> str:
        """
        Model çıktısından temiz SQL çıkarır.
        Öncelik: ```sql blok > SQL ifadesiyle başlayan satır > ham çıktı
        """
        raw = raw.strip()

        # ```sql``` blokları ara
        code_block = re.search(r"```(?:sql)?\s*(.*?)```", raw, re.DOTALL | re.IGNORECASE)
        if code_block:
            return code_block.group(1).strip()

        # SQL keyword ile başlayan ilk satırı bul
        for line in raw.split("\n"):
            line = line.strip()
            if line.upper().startswith(("SELECT", "INSERT", "UPDATE", "DELETE", "WITH")):
                return line

        # Hiçbiri yoksa ham çıktıyı döndür
        return raw

    # ------------------------------------------------------------------
    # HEALTH & STATS — V3 SERVER UYUMLU
    # ------------------------------------------------------------------

    def get_health_info(self) -> Dict[str, Any]:
        """
        ✅ V3'teki get_health_info() metodu — server endpoint'i için gerekli
        """
        uptime = time.time() - self.start_time
        avg_time = (
            self.total_processing_time / self.total_requests
            if self.total_requests > 0
            else 0
        )

        return {
            "status": "healthy" if self.model_loaded else "unhealthy",
            "model_loaded": self.model_loaded,
            "model_name": "llama3-text2sql-lora",
            "model_type": "dialog-aware-llama3-lora",
            "model_version": "v1.0",
            "device": str(self.device),
            "uptime_seconds": round(uptime, 2),
            "total_requests": self.total_requests,
            "dialog_requests": self.dialog_requests,
            "single_turn_requests": self.single_turn_requests,
            "avg_response_time_ms": round(avg_time, 2),
            "config": {
                "max_source_length": ServerConfig.MAX_SOURCE_LENGTH,
                "max_target_length": ServerConfig.MAX_TARGET_LENGTH,
                "max_context_turns": ServerConfig.MAX_CONVERSATION_HISTORY,
                "default_num_beams": ServerConfig.DEFAULT_NUM_BEAMS
            }
        }

    def get_stats(self) -> Dict[str, Any]:
        """
        ✅ Ek stats metodu (varsa kullanılır)
        """
        uptime = time.time() - self.start_time
        avg_time = (
            self.total_processing_time / self.total_requests
            if self.total_requests > 0
            else 0.0
        )
        return {
            "model_loaded": self.model_loaded,
            "model_name": ServerConfig.MODEL_PATH,
            "model_type": "LlamaForCausalLM (PEFT/LoRA)",
            "model_version": "llama3-text2sql-lora-v1.0",
            "device": str(self.device),
            "uptime_seconds": round(uptime, 2),
            "total_requests": self.total_requests,
            "dialog_requests": self.dialog_requests,
            "single_turn_requests": self.single_turn_requests,
            "avg_response_time_ms": round(avg_time, 2),
        }