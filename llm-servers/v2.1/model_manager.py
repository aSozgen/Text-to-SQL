import torch
from transformers import T5ForConditionalGeneration, T5Tokenizer
import time
import re
from datetime import datetime
from typing import Dict, Any, Optional, List
import logging

from config import ServerConfig
from utils import validate_sql, normalize_sql

logger = logging.getLogger(__name__)

class ModelManager:
    """Model manager matching your Phase 2 dialog training"""
    
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
        
    def load_model(self):
        """Load your Phase 2 dialog model"""
        try:
            logger.info(f"Loading dialog model from {ServerConfig.MODEL_PATH}")
            logger.info(f"Using device: {self.device}")
            
            # Load model and tokenizer
            self.tokenizer = T5Tokenizer.from_pretrained(ServerConfig.MODEL_PATH)
            self.model = T5ForConditionalGeneration.from_pretrained(ServerConfig.MODEL_PATH)
            self.model.to(self.device)
            self.model.eval()
            
            # Enable optimizations (only for GPU)
            if torch.cuda.is_available():
                self.model.half()  # FP16 for faster inference
            
            self.model_loaded = True
            logger.info("✓ Dialog model loaded successfully")
            logger.info(f"  Model parameters: {sum(p.numel() for p in self.model.parameters()) / 1e6:.2f}M")
            logger.info(f"  Max source length: {ServerConfig.MAX_SOURCE_LENGTH}")
            logger.info(f"  Max target length: {ServerConfig.MAX_TARGET_LENGTH}")
            logger.info(f"  Max context turns: {ServerConfig.MAX_CONVERSATION_HISTORY}")
            
        except Exception as e:
            logger.error(f"Failed to load model: {str(e)}")
            raise
    
    def create_dialog_input(
        self,
        question: str,
        schema: Optional[str] = None,
        conversation_history: Optional[List[Dict[str, str]]] = None
    ) -> str:
        """
        Create input text matching your training format:
        
        translate natural language to SQL with context:
        [PREVIOUS] question: ... sql: ...
        [CURRENT] question: ...
        schema: ...
        """
        parts = []
        
        # Check context
        has_context = conversation_history and len(conversation_history) > 0
        
        # Task prefix (matches your training)
        if has_context:
            parts.append("translate natural language to SQL with context:")
        else:
            parts.append("translate natural language to SQL:")
        
        # Add conversation history (last 2 turns - matches your training)
        if has_context:
            max_turns = min(len(conversation_history), ServerConfig.MAX_CONVERSATION_HISTORY)
            
            for turn in conversation_history[-max_turns:]:
                turn_q = turn.get('utterance', turn.get('question', ''))
                turn_sql = turn.get('query', turn.get('sql', ''))
                turn_sql = normalize_sql(turn_sql)
                
                parts.append(f"[PREVIOUS] question: {turn_q} sql: {turn_sql}")
        
        # Current question
        parts.append(f"[CURRENT] question: {question}")
        
        # Schema (with truncation like in training)
        if schema:
            if len(schema) > ServerConfig.MAX_SCHEMA_LENGTH:
                schema = schema[:ServerConfig.MAX_SCHEMA_LENGTH] + "..."
            parts.append(f"schema: {schema}")
        
        return " ".join(parts)
    
    def predict(
        self,
        question: str,
        schema: Optional[str] = None,
        conversation_history: Optional[List[Dict[str, str]]] = None,
        include_schema: bool = True,
        num_beams: int = 5
    ) -> Dict[str, Any]:
        """Generate SQL with dialog support"""
        
        if not self.model_loaded:
            raise RuntimeError("Model not loaded")
        
        start_time = time.time()
        # ✅ FIX: Ensure has_context is always bool
        has_context = bool(conversation_history and len(conversation_history) > 0)
        
        try:
            # Create input (matches training format)
            input_text = self.create_dialog_input(
                question=question,
                schema=schema if include_schema else None,
                conversation_history=conversation_history
            )
            
            logger.debug(f"Input length: {len(input_text)} chars")
            logger.debug(f"Input preview: {input_text[:200]}...")
            
            # Tokenize
            inputs = self.tokenizer(
                input_text,
                return_tensors="pt",
                max_length=ServerConfig.MAX_SOURCE_LENGTH,
                truncation=True,
                padding=False
            )
            inputs = {k: v.to(self.device) for k, v in inputs.items()}
            
            # Generate (matches training params)
            with torch.no_grad():
                outputs = self.model.generate(
                    **inputs,
                    max_length=ServerConfig.MAX_TARGET_LENGTH,
                    num_beams=num_beams,
                    early_stopping=True,
                    return_dict_in_generate=True,
                    output_scores=True
                )
            
            # Decode
            predicted_sql = self.tokenizer.decode(
                outputs.sequences[0],
                skip_special_tokens=True
            )
            predicted_sql = normalize_sql(predicted_sql)
            
            # Validate
            is_valid, validation_error = validate_sql(predicted_sql)
            
            # Calculate confidence
            confidence = None
            if hasattr(outputs, 'scores') and outputs.scores:
                try:
                    probs = [torch.softmax(score, dim=-1).max().item() 
                            for score in outputs.scores]
                    confidence = sum(probs) / len(probs) if probs else None
                except Exception as e:
                    logger.warning(f"Confidence calculation failed: {e}")
            
            # Update stats
            processing_time = (time.time() - start_time) * 1000
            self.total_requests += 1
            if has_context:
                self.dialog_requests += 1
            else:
                self.single_turn_requests += 1
            self.total_processing_time += processing_time
            
            # ✅ FIX: Ensure all bools are explicitly set
            return {
                "status": "success",
                "sql": predicted_sql,
                "is_valid": bool(is_valid),
                "validation_error": validation_error,
                "confidence": round(confidence, 4) if confidence else None,
                "schema_used": bool(include_schema and schema is not None),
                "context_used": bool(has_context),  # Explicitly bool
                "context_turns": len(conversation_history) if has_context else 0,
                "processing_time_ms": int(processing_time),
                "metadata": {
                    "model_version": "text2sql-dialog-v2.0-improved",
                    "timestamp": datetime.utcnow().isoformat() + "Z",
                    "device": str(self.device),
                    "num_beams": num_beams,
                    "input_length": len(input_text),
                    "max_context_turns": ServerConfig.MAX_CONVERSATION_HISTORY
                }
            }
            
        except Exception as e:
            logger.error(f"Prediction error: {str(e)}", exc_info=True)
            processing_time = (time.time() - start_time) * 1000
            
            # ✅ FIX: Explicitly set all bools to False in error case
            return {
                "status": "error",
                "sql": None,
                "is_valid": False,  # Explicitly False
                "validation_error": None,
                "confidence": None,
                "schema_used": False,  # Explicitly False
                "context_used": False,  # Explicitly False (not None!)
                "context_turns": 0,
                "processing_time_ms": int(processing_time),
                "error": str(e)
            }
    
    def get_health_info(self) -> Dict[str, Any]:
        """Get health information"""
        uptime = time.time() - self.start_time
        avg_time = (
            self.total_processing_time / self.total_requests 
            if self.total_requests > 0 
            else 0
        )
        
        return {
            "status": "healthy" if self.model_loaded else "unhealthy",
            "model_loaded": self.model_loaded,
            "model_name": "text2sql-dialog-improved",
            "model_type": "dialog-aware-2turn",
            "model_version": "v2.0",
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