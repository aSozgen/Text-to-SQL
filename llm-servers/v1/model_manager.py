import torch
from transformers import T5ForConditionalGeneration, T5Tokenizer
import time
from datetime import datetime
from typing import Dict, Any, Optional
import logging
from functools import lru_cache
import psutil
import gc

from config import ServerConfig
from utils import validate_sql, normalize_sql, sanitize_input
from models import SchemaInfo


logger = logging.getLogger(__name__)


class ModelManager:
    """Optimized model manager with better resource handling"""
    
    def __init__(self):
        self.model = None
        self.tokenizer = None
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        ServerConfig.DEVICE = str(self.device)
        
        self.model_loaded = False
        self.total_requests = 0
        self.total_processing_time = 0.0
        self.start_time = time.time()
        
        # Simple cache for repeated queries
        self._prediction_cache = {}
        
        logger.info(f"Initialized ModelManager on device: {self.device}")
    
    def load_model(self):
        """Load the trained model and tokenizer"""
        try:
            logger.info(f"Loading model from {ServerConfig.MODEL_PATH}")
            
            # Load tokenizer
            self.tokenizer = T5Tokenizer.from_pretrained(
                ServerConfig.MODEL_PATH,
                model_max_length=ServerConfig.MAX_SOURCE_LENGTH
            )
            
            # Load model with optimizations
            self.model = T5ForConditionalGeneration.from_pretrained(
                ServerConfig.MODEL_PATH,
                torch_dtype=torch.float16 if self.device.type == "cuda" else torch.float32
            )
            
            # Move to device and set to eval mode
            self.model.to(self.device)
            self.model.eval()
            
            # Enable optimizations for inference
            if self.device.type == "cuda":
                # Use torch.compile for faster inference (PyTorch 2.0+)
                try:
                    self.model = torch.compile(self.model, mode="reduce-overhead")
                    logger.info(" Model compiled with torch.compile")
                except Exception as e:
                    logger.warning(f"Could not compile model: {e}")
            
            self.model_loaded = True
            logger.info(" Model loaded successfully")
            
            # Log memory usage
            self._log_memory_usage()
            
        except Exception as e:
            logger.error(f"Failed to load model: {str(e)}", exc_info=True)
            raise
    
    def _log_memory_usage(self):
        """Log current memory usage"""
        if self.device.type == "cuda":
            allocated = torch.cuda.memory_allocated() / 1e9
            reserved = torch.cuda.memory_reserved() / 1e9
            logger.info(f"GPU Memory: {allocated:.2f}GB allocated, {reserved:.2f}GB reserved")
        
        process = psutil.Process()
        ram_mb = process.memory_info().rss / 1024 / 1024
        logger.info(f"RAM Usage: {ram_mb:.2f}MB")
    
    def _get_cache_key(self, question: str, schema_str: Optional[str]) -> str:
        """Generate cache key for a query"""
        return f"{question}|{schema_str or 'no_schema'}"
    
    def _create_input_text(
        self, 
        question: str, 
        schema_str: Optional[str] = None
    ) -> str:
        """Create optimized model input text"""
        # Start with task prefix
        if schema_str:
            prefix = "translate natural language to SQL with schema:"
        else:
            prefix = "translate natural language to SQL:"
        
        # Add question
        parts = [prefix, f"question: {question}"]
        
        # Add schema only if provided
        if schema_str:
            parts.append(f"schema: {schema_str}")
        
        return " ".join(parts)
    
    @torch.no_grad()
    def predict(
        self, 
        question: str,
        schema: Optional[SchemaInfo] = None,
        num_beams: int = 1,
        temperature: float = 1.0
    ) -> Dict[str, Any]:
        """
        Generate SQL from natural language question
        
        Args:
            question: Natural language query
            schema: Optional schema information
            num_beams: Number of beams for beam search (1=greedy)
            temperature: Sampling temperature
            
        Returns:
            Dictionary with prediction results
        """
        
        if not self.model_loaded:
            raise RuntimeError("Model not loaded")
        
        start_time = time.time()
        
        try:
            # Sanitize inputs
            question = sanitize_input(question, ServerConfig.MAX_QUESTION_LENGTH)
            
            # Extract schema string if provided
            schema_str = None
            if schema is not None:
                schema_str = sanitize_input(
                    schema.schema_info, 
                    ServerConfig.MAX_SCHEMA_LENGTH
                )
            
            # Check cache
            if ServerConfig.ENABLE_CACHE:
                cache_key = self._get_cache_key(question, schema_str)
                if cache_key in self._prediction_cache:
                    logger.debug("Cache hit!")
                    cached_result = self._prediction_cache[cache_key].copy()
                    cached_result["processing_time_ms"] = int((time.time() - start_time) * 1000)
                    cached_result["metadata"]["cached"] = True
                    return cached_result
            
            # Create input text
            input_text = self._create_input_text(question, schema_str)
            logger.debug(f"Input length: {len(input_text)} chars")
            
            # Tokenize
            inputs = self.tokenizer(
                input_text,
                return_tensors="pt",
                max_length=ServerConfig.MAX_SOURCE_LENGTH,
                truncation=True,
                padding=False  # No padding needed for single input
            )
            inputs = {k: v.to(self.device) for k, v in inputs.items()}
            
            # Generate SQL
            generation_kwargs = {
                "max_length": ServerConfig.MAX_TARGET_LENGTH,
                "return_dict_in_generate": True,
                "output_scores": True,
            }
            
            if num_beams > 1:
                # Beam search
                generation_kwargs.update({
                    "num_beams": num_beams,
                    "early_stopping": True,
                })
            else:
                # Greedy or sampling
                if temperature != 1.0:
                    generation_kwargs.update({
                        "do_sample": True,
                        "temperature": temperature,
                    })
                else:
                    generation_kwargs["do_sample"] = False
            
            outputs = self.model.generate(**inputs, **generation_kwargs)
            
            # Decode
            predicted_sql = self.tokenizer.decode(
                outputs.sequences[0], 
                skip_special_tokens=True
            )
            
            # Normalize SQL
            predicted_sql = normalize_sql(predicted_sql)
            
            # Validate SQL
            is_valid, validation_error = validate_sql(predicted_sql)
            
            # Calculate confidence (if scores available)
            confidence = None
            if hasattr(outputs, 'scores') and outputs.scores:
                try:
                    # Average max probability across all generated tokens
                    probs = [torch.softmax(score, dim=-1).max().item() for score in outputs.scores]
                    confidence = sum(probs) / len(probs) if probs else None
                except Exception as e:
                    logger.debug(f"Could not calculate confidence: {e}")
            
            # Calculate processing time
            processing_time = (time.time() - start_time) * 1000
            
            # Update stats
            self.total_requests += 1
            self.total_processing_time += processing_time
            
            # Prepare result
            result = {
                "status": "success",
                "sql": predicted_sql,
                "is_valid": is_valid,
                "validation_error": validation_error,
                "confidence": round(confidence, 4) if confidence else None,
                "schema_used": schema is not None,
                "processing_time_ms": int(processing_time),
                "metadata": {
                    "model_version": "text2sql-t5-large-v1.0",
                    "timestamp": datetime.utcnow().isoformat() + "Z",
                    "device": str(self.device),
                    "num_beams": num_beams,
                    "temperature": temperature,
                    "cached": False,
                    "input_tokens": len(inputs["input_ids"][0]),
                    "output_tokens": len(outputs.sequences[0])
                }
            }
            
            # Cache result
            if ServerConfig.ENABLE_CACHE and is_valid:
                cache_key = self._get_cache_key(question, schema_str)
                if len(self._prediction_cache) >= ServerConfig.CACHE_SIZE:
                    # Remove oldest entry
                    self._prediction_cache.pop(next(iter(self._prediction_cache)))
                self._prediction_cache[cache_key] = result.copy()
            
            return result
            
        except Exception as e:
            logger.error(f"Prediction error: {str(e)}", exc_info=True)
            processing_time = (time.time() - start_time) * 1000
            
            return {
                "status": "error",
                "sql": None,
                "is_valid": False,
                "validation_error": None,
                "confidence": None,
                "schema_used": schema is not None,
                "processing_time_ms": int(processing_time),
                "error": str(e),
                "metadata": {
                    "timestamp": datetime.utcnow().isoformat() + "Z"
                }
            }
    
    def get_health_info(self) -> Dict[str, Any]:
        """Get detailed health information"""
        uptime = time.time() - self.start_time
        avg_time = (
            self.total_processing_time / self.total_requests 
            if self.total_requests > 0 
            else 0
        )
        
        # Memory usage
        memory_mb = None
        if self.device.type == "cuda":
            memory_mb = torch.cuda.memory_allocated() / 1024 / 1024
        else:
            process = psutil.Process()
            memory_mb = process.memory_info().rss / 1024 / 1024
        
        return {
            "status": "healthy" if self.model_loaded else "unhealthy",
            "model_loaded": self.model_loaded,
            "model_name": "text2sql-t5-large-v1.0",
            "device": str(self.device),
            "uptime_seconds": round(uptime, 2),
            "total_requests": self.total_requests,
            "avg_response_time_ms": round(avg_time, 2),
            "memory_usage_mb": round(memory_mb, 2) if memory_mb else None
        }
    
    def clear_cache(self):
        """Clear prediction cache"""
        self._prediction_cache.clear()
        if self.device.type == "cuda":
            torch.cuda.empty_cache()
        gc.collect()
        logger.info("Cache cleared")