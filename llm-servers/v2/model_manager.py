import torch
from transformers import T5ForConditionalGeneration, T5Tokenizer
import time
from datetime import datetime
from typing import Dict, Any, Optional, List
import logging

from config import ServerConfig
from utils import validate_sql, normalize_sql

logger = logging.getLogger(__name__)

class ModelManager:
    """Manages model loading and inference with dialog support"""
    
    def __init__(self):
        self.model = None
        self.tokenizer = None
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        ServerConfig.DEVICE = str(self.device)
        
        self.model_loaded = False
        self.total_requests = 0
        self.dialog_requests = 0  # Track dialog requests
        self.total_processing_time = 0.0
        self.start_time = time.time()
        
    def load_model(self):
        """Load the trained dialog model and tokenizer"""
        try:
            logger.info(f"Loading dialog model from {ServerConfig.MODEL_PATH}")
            logger.info(f"Using device: {self.device}")
            
            self.tokenizer = T5Tokenizer.from_pretrained(ServerConfig.MODEL_PATH)
            self.model = T5ForConditionalGeneration.from_pretrained(ServerConfig.MODEL_PATH)
            self.model.to(self.device)
            self.model.eval()
            
            self.model_loaded = True
            logger.info(" Dialog model loaded successfully")
            
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
        Create model input with dialog context
        
        Format:
        translate natural language to SQL with context:
        [PREVIOUS] question: Show all singers sql: SELECT * FROM singer
        [CURRENT] question: Filter by age > 25
        schema: singer: id (number) [PK], name (text), age (number)
        """
        parts = []
        
        # Check if we have dialog context
        has_context = conversation_history and len(conversation_history) > 0
        
        # Task prefix
        if has_context:
            parts.append("translate natural language to SQL with context:")
        else:
            parts.append("translate natural language to SQL:")
        
        # Add conversation history (last N turns)
        if has_context:
            max_turns = min(
                len(conversation_history), 
                ServerConfig.MAX_CONVERSATION_HISTORY
            )
            
            for turn in conversation_history[-max_turns:]:
                turn_q = turn.get('question', '')
                turn_sql = normalize_sql(turn.get('sql', ''))
                parts.append(f"[PREVIOUS] question: {turn_q} sql: {turn_sql}")
        
        # Add current question
        parts.append(f"[CURRENT] question: {question}")
        
        # Add schema if provided
        if schema:
            parts.append(f"schema: {schema}")
        
        return " ".join(parts)
    
    def predict(
        self,
        question: str,
        schema: Optional[str] = None,
        conversation_history: Optional[List[Dict[str, str]]] = None,
        include_schema: bool = True,
        num_beams: int = 6
    ) -> Dict[str, Any]:
        """
        Generate SQL from natural language question with dialog support
        
        Args:
            question: Current user question
            schema: Schema string (e.g., "Student: id (number) [PK], name (text)")
            conversation_history: List of previous turns [{"question": "...", "sql": "..."}]
            include_schema: Whether to use schema
            num_beams: Number of beams for generation
            
        Returns:
            Dictionary with prediction results
        """
        
        if not self.model_loaded:
            raise RuntimeError("Model not loaded")
        
        start_time = time.time()
        has_context = bool(conversation_history and len(conversation_history) > 0)        
        try:
            # Create input text with dialog context
            input_text = self.create_dialog_input(
                question=question,
                schema=schema if include_schema else None,
                conversation_history=conversation_history
            )
            
            logger.debug(f"Input text: {input_text[:300]}...")
            
            # Tokenize
            inputs = self.tokenizer(
                input_text,
                return_tensors="pt",
                max_length=ServerConfig.MAX_SOURCE_LENGTH,
                truncation=True
            )
            inputs = {k: v.to(self.device) for k, v in inputs.items()}
            
            # Generate with beam search (better for dialog)
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
            
            # Validate SQL
            is_valid, validation_error = validate_sql(predicted_sql)
            
            # Calculate confidence from token probabilities
            confidence = None
            if hasattr(outputs, 'scores') and outputs.scores:
                try:
                    # Average max probability across all generated tokens
                    probs = [torch.softmax(score, dim=-1).max().item() 
                            for score in outputs.scores]
                    confidence = sum(probs) / len(probs) if probs else None
                except Exception as e:
                    logger.warning(f"Failed to calculate confidence: {e}")
            
            # Update stats
            processing_time = (time.time() - start_time) * 1000
            self.total_requests += 1
            if has_context:
                self.dialog_requests += 1
            self.total_processing_time += processing_time
            
            return {
                "status": "success",
                "sql": predicted_sql,
                "is_valid": is_valid,
                "validation_error": validation_error,
                "confidence": confidence,
                "schema_used": include_schema and schema is not None,
                "context_used": has_context,
                "processing_time_ms": int(processing_time),
                "metadata": {
                    "model_version": "text2sql-dialog-v1.0",
                    "timestamp": datetime.utcnow().isoformat() + "Z",
                    "device": str(self.device),
                    "num_beams": num_beams,
                    "context_turns": len(conversation_history) if has_context else 0
                }
            }
            
        except Exception as e:
            logger.error(f"Prediction error: {str(e)}", exc_info=True)
            processing_time = (time.time() - start_time) * 1000
            
            return {
                "status": "error",
                "sql": None,
                "is_valid": False,
                "validation_error": None,
                "confidence": None,
                "schema_used": False,
                "context_used": False,
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
            "model_name": "text2sql-dialog-v1.0",
            "model_type": "dialog-aware",
            "device": str(self.device),
            "uptime_seconds": uptime,
            "total_requests": self.total_requests,
            "dialog_requests": self.dialog_requests,
            "avg_response_time_ms": avg_time
        }
