from pydantic import BaseModel, Field, validator
from typing import Optional, List, Dict, Any

class ConversationTurn(BaseModel):
    """Single turn in conversation history"""
    question: str = Field(..., description="User's question")
    sql: str = Field(..., description="Generated SQL query")

class PredictRequest(BaseModel):
    """Request model for /predict endpoint with dialog support"""
    question: str = Field(..., description="Natural language query", min_length=1)
    schema: Optional[str] = Field(None, description="Schema string (simple format)")
    conversation_history: Optional[List[ConversationTurn]] = Field(
        None, 
        description="Previous conversation turns for context",
        max_items=5
    )
    include_schema: bool = Field(True, description="Whether to use schema")
    num_beams: int = Field(6, description="Number of beams for generation", ge=1, le=10)
    
    @validator('question')
    def validate_question(cls, v):
        if len(v.strip()) == 0:
            raise ValueError("Question cannot be empty")
        return v.strip()

class PredictResponse(BaseModel):
    """Response model for /predict endpoint"""
    status: str
    sql: Optional[str] = None
    is_valid: bool = False
    validation_error: Optional[str] = None
    confidence: Optional[float] = None
    schema_used: bool = False
    context_used: bool = False  # New: indicates if conversation history was used
    processing_time_ms: int
    error: Optional[str] = None
    metadata: Dict[str, Any] = Field(default_factory=dict)

class BatchQuery(BaseModel):
    """Single query in batch request"""
    question: str
    schema: Optional[str] = None
    conversation_history: Optional[List[ConversationTurn]] = None
    include_schema: bool = True
    
class BatchPredictRequest(BaseModel):
    """Request model for /batch-predict endpoint"""
    queries: List[BatchQuery] = Field(..., max_items=10)

class BatchPredictResponse(BaseModel):
    """Response model for /batch-predict endpoint"""
    status: str
    results: List[Dict[str, Any]]
    total_queries: int
    successful: int
    failed: int
    total_processing_time_ms: int

class HealthResponse(BaseModel):
    """Response model for /health endpoint"""
    status: str
    model_loaded: bool
    model_name: str
    model_type: str  # New: "dialog" or "single-turn"
    device: str
    uptime_seconds: float
    total_requests: int
    dialog_requests: int  # New: count of requests with context
    avg_response_time_ms: float