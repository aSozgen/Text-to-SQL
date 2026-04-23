import os
import time
import math
import re
import requests
import logging
from datetime import datetime
from typing import Dict, Any, Optional, List

from config import ServerConfig
from utils import validate_sql, normalize_sql

logger = logging.getLogger(__name__)

class ModelManager:
    """
    Ollama tabanlı Model Manager (v5).
    Ağır PyTorch kütüphaneleri yerine host makinedeki Ollama API'sini kullanır.
    Java Backend için v4 ile %100 uyumlu yanıtlar üretir.
    """

    def __init__(self):
        self.ollama_url = f"{ServerConfig.OLLAMA_BASE_URL}/v1/chat/completions"
        self.model_name = ServerConfig.MODEL_NAME

        self.model_loaded = False
        self.total_requests = 0
        self.dialog_requests = 0
        self.single_turn_requests = 0
        self.total_processing_time = 0.0
        self.start_time = time.time()

    def load_model(self):
        """
        Gerçekte RAM'e model yüklemez, sadece Ollama'nın ayakta olup olmadığını
        ve modelin Ollama içinde yüklü olup olmadığını kontrol eder.
        """
        try:
            logger.info(f"Ollama bağlantısı kontrol ediliyor: {ServerConfig.OLLAMA_BASE_URL}")
            logger.info(f"Kullanılacak Model: {self.model_name}")

            # Ollama'nın tags endpoint'ine basit bir istek atarak ayakta mı bakıyoruz
            health_check_url = f"{ServerConfig.OLLAMA_BASE_URL}/api/tags"
            response = requests.get(health_check_url, timeout=5)

            if response.status_code == 200:
                self.model_loaded = True
                logger.info("✓ Ollama servisi ayakta ve erişilebilir durumda.")
            else:
                logger.warning(f"Ollama yanıt verdi ama statü kodu: {response.status_code}")
                # Yine de true yapıyoruz çünkü /v1/chat/completions açık olabilir
                self.model_loaded = True

        except Exception as e:
            logger.error(f"Ollama servisine ulaşılamadı! Docker extra_hosts ayarlarını kontrol et. Hata: {str(e)}")
            # Sunucunun çökmemesi için hatayı yutuyoruz, istek geldiğinde hata dönecek
            self.model_loaded = False

    def predict(
        self,
        question: str,
        schema: Optional[str] = None,
        conversation_history: Optional[List[Dict[str, str]]] = None,
        include_schema: bool = True,
        num_beams: int = 5 # Ollama'da direkt kullanılmaz ama interface uyumluluğu için var
    ) -> Dict[str, Any]:
        """SQL üretimi için Ollama'ya istek atar ve V4 formatında yanıt döner."""

        start_time = time.time()
        has_context = bool(conversation_history and len(conversation_history) > 0)

        try:
            # 1. Ollama (OpenAI formatı) için mesajları hazırla
            messages = []

            # Sistem prompt'u eklenebilir, Qwen instruction için faydalıdır
            system_prompt = "You are an expert SQL assistant. Convert the given natural language questions to SQL queries."
            messages.append({"role": "system", "content": system_prompt})

            # Geçmiş konuşmalar
            if has_context:
                max_turns = min(len(conversation_history), ServerConfig.MAX_CONVERSATION_HISTORY)
                for turn in conversation_history[-max_turns:]:
                    turn_q = turn.get("utterance", turn.get("question", "")).strip()
                    turn_sql = turn.get("query", turn.get("sql", "")).strip()

                    if turn_q and turn_sql:
                        messages.append({"role": "user", "content": turn_q})
                        messages.append({"role": "assistant", "content": turn_sql})

            # Güncel Soru ve Şema
            full_prompt = question.strip()
            if include_schema and schema:
                # Şemayı çok uzunsa kes
                if len(schema) > ServerConfig.MAX_SCHEMA_LENGTH:
                    schema = schema[:ServerConfig.MAX_SCHEMA_LENGTH] + "..."
                full_prompt += f"\n\nDatabase Schema:\n{schema}"

            messages.append({"role": "user", "content": full_prompt})

            # 2. Ollama API Payload'u
            payload = {
                "model": self.model_name,
                "messages": messages,
                "stream": False,
                "logprobs": True,
                "top_logprobs": 1,
                "temperature": 0.0,  # SQL için her zaman 0 idealdir
                "top_p": 0.1
            }

            logger.debug(f"Ollama'ya gönderiliyor. Model: {self.model_name}, Bağlam(Turn): {len(messages)//2}")

            # 3. İsteği At
            resp = requests.post(self.ollama_url, json=payload, timeout=ServerConfig.OLLAMA_TIMEOUT)
            resp.raise_for_status() # HTTP hatası varsa exception fırlatır

            response_data = resp.json()
            choice = response_data["choices"][0]
            raw_content = choice["message"]["content"]

            #Confidence Hesaplaması
            calculated_confidence = 0.0

            if "logprobs" in choice and choice["logprobs"] is not None:
                # Token olasılık listesini al
                tokens = choice["logprobs"].get("content", [])

                if tokens:
                    # Bütün kelimelerin logaritmik olasılıklarını topla
                    sum_logprob = sum(t.get("logprob", 0.0) for t in tokens)
                    # Ortalama logaritmik olasılığı bul
                    avg_logprob = sum_logprob / len(tokens)
                    # Logaritmadan çıkarıp (e^x) 0 ile 1 arası gerçek yüzdeye çevir
                    calculated_confidence = round(math.exp(avg_logprob), 4)
                else:
                    # Eğer model logprobs desteklemiyorsa veya hata verdiyse 0.95 ata
                    calculated_confidence = 0.95

            # 4. Çıktıyı İşle ve Doğrula
            predicted_sql = self._extract_sql(raw_content)
            predicted_sql = normalize_sql(predicted_sql)
            is_valid, validation_error = validate_sql(predicted_sql)

            # İstatistikleri Güncelle
            processing_time = (time.time() - start_time) * 1000
            self.total_requests += 1
            if has_context:
                self.dialog_requests += 1
            else:
                self.single_turn_requests += 1
            self.total_processing_time += processing_time

            # 5. V4 ile %100 Uyumlu Yanıt Formatı
            return {
                "status": "success",
                "sql": predicted_sql,
                "is_valid": bool(is_valid),
                "validation_error": validation_error,
                "confidence": calculated_confidence, # Ollama standart OpenAI dönüştürücüsünde logprobs eksik olabilir, default dönüyoruz
                "schema_used": bool(include_schema and schema is not None),
                "context_used": bool(has_context),
                "context_turns": len(conversation_history) if has_context else 0,
                "processing_time_ms": int(processing_time),
                "metadata": {
                    "model_version": self.model_name,
                    "timestamp": datetime.utcnow().isoformat() + "Z",
                    "device": "ollama-host",
                    "num_beams": 1, # Ollama greedy çalışır
                    "max_context_turns": ServerConfig.MAX_CONVERSATION_HISTORY
                }
            }

        except Exception as e:
            logger.error(f"Ollama Prediction Hatası: {str(e)}", exc_info=True)
            processing_time = (time.time() - start_time) * 1000

            return {
                "status": "error",
                "sql": None,
                "is_valid": False,
                "validation_error": None,
                "confidence": None,
                "schema_used": False,
                "context_used": False,
                "context_turns": 0,
                "processing_time_ms": int(processing_time),
                "error": f"Ollama API Hatası: {str(e)}"
            }

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