from pydantic import BaseModel, Field, validator, field_validator
from typing import Optional, List, Dict, Any
import re

class SchemaInfo(BaseModel):
    """Simple schema format - user provides pre-formatted string"""
    schema_info: str = Field(
        ..., 
        description="Schema in format: 'Table: col1 (type) [PK], col2 (type) | Foreign Keys: ...'",
        max_length=2000
    )
    
    @field_validator('schema_info')
    @classmethod
    def validate_schema_info(cls, v: str) -> str:
        v = v.strip()
        if not v:
            raise ValueError("schema_info cannot be empty")
        if len(v) > 2000:
            raise ValueError("schema_info too long (max 2000 chars)")
        return v


class PredictRequest(BaseModel):
    """Optimized prediction request - schema is optional"""
    question: str = Field(
        ..., 
        description="Natural language query",
        min_length=1,
        max_length=500
    )
    schema: Optional[SchemaInfo] = Field(
        None,
        description="Optional schema information"
    )
    
    # Generation parameters
    num_beams: int = Field(
        1, 
        description="Number of beams (1=greedy, >1=beam search)",
        ge=1,
        le=10
    )
    temperature: float = Field(
        1.0,
        description="Sampling temperature",
        ge=0.1,
        le=2.0
    )
    
    @field_validator('question')
    @classmethod
    def validate_question(cls, v: str) -> str:
        v = v.strip()
        if not v:
            raise ValueError("Question cannot be empty")
        if len(v) > 500:
            raise ValueError("Question too long (max 500 chars)")
        return v
    
    @property
    def include_schema(self) -> bool:
        """Helper to check if schema is provided"""
        return self.schema is not None


class PredictResponse(BaseModel):
    """Optimized prediction response"""
    status: str = Field(..., description="'success' or 'error'")
    sql: Optional[str] = Field(None, description="Generated SQL query")
    is_valid: bool = Field(False, description="SQL syntax validation result")
    validation_error: Optional[str] = Field(None, description="Validation error if invalid")
    
    # Optional fields
    confidence: Optional[float] = Field(None, description="Model confidence score")
    schema_used: bool = Field(False, description="Whether schema was used")
    processing_time_ms: int = Field(..., description="Processing time in milliseconds")
    
    # Error handling
    error: Optional[str] = Field(None, description="Error message if failed")
    
    # Metadata
    metadata: Dict[str, Any] = Field(
        default_factory=dict,
        description="Additional metadata"
    )


class BatchQuery(BaseModel):
    """Single query in batch request"""
    question: str = Field(..., min_length=1, max_length=500)
    schema: Optional[SchemaInfo] = None
    num_beams: int = Field(1, ge=1, le=10)


class BatchPredictRequest(BaseModel):
    """Batch prediction request"""
    queries: List[BatchQuery] = Field(
        ..., 
        min_items=1,
        max_items=10,
        description="List of queries to process"
    )


class BatchPredictResponse(BaseModel):
    """Batch prediction response"""
    status: str
    results: List[Dict[str, Any]]
    total_queries: int
    successful: int
    failed: int
    total_processing_time_ms: int


class HealthResponse(BaseModel):
    """Health check response"""
    status: str
    model_loaded: bool
    model_name: str
    device: str
    uptime_seconds: float
    total_requests: int
    avg_response_time_ms: float
    memory_usage_mb: Optional[float] = None

