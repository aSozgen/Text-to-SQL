from pydantic import BaseModel, Field, validator
from typing import Optional, List, Dict, Any, Union
import json

class ConversationTurn(BaseModel):
    """Single turn in conversation history"""
    utterance: str = Field(..., description="User's question", alias="question")
    query: str = Field(..., description="Generated SQL query", alias="sql")

    class Config:
        populate_by_name = True

    @validator('utterance', 'query')
    def strip_whitespace(cls, v):
        return v.strip() if v else v

class PredictRequest(BaseModel):
    """Request model matching your dialog format"""
    question: str = Field(..., description="Current natural language query", min_length=1)
    database_id: Optional[str] = Field(None, description="Database ID (if schema lookup needed)")

    # DİKKAT: Hem String hem de Dictionary(JSON) kabul etmesi için Union eklendi
    db_schema: Optional[Union[str, Dict[str, Any]]] = Field(None, description="Database schema string or object", alias="schema")

    conversation_history: Optional[List[ConversationTurn]] = Field(
        None,
        description="Previous conversation turns (max 2 for optimal performance)",
        max_items=2
    )
    include_schema: bool = Field(True, description="Include schema in prompt")
    num_beams: int = Field(5, description="Number of beams (5 recommended)", ge=1, le=10)

    class Config:
        populate_by_name = True

    @validator('question')
    def validate_question(cls, v):
        if not v or len(v.strip()) == 0:
            raise ValueError("Question cannot be empty")
        return v.strip()

    @validator('db_schema')
    def parse_schema(cls, v):
        # Eğer Java tarafından schema JSON/Dictionary olarak gelirse, onu String'e çevirir
        if isinstance(v, dict):
            return json.dumps(v, ensure_ascii=False)
        return v

    @validator('conversation_history')
    def limit_history(cls, v):
        if v and len(v) > 2:
            return v[-2:]  # Keep only last 2 turns
        return v

class PredictResponse(BaseModel):
    """Response model"""
    status: str
    sql: Optional[str] = None
    is_valid: bool = False
    validation_error: Optional[str] = None
    confidence: Optional[float] = None
    schema_used: bool = False
    context_used: bool = False
    context_turns: int = 0
    processing_time_ms: int
    error: Optional[str] = None
    metadata: Dict[str, Any] = Field(default_factory=dict)

class BatchQuery(BaseModel):
    """Single query in batch"""
    question: str
    database_id: Optional[str] = None
    db_schema: Optional[Union[str, Dict[str, Any]]] = Field(None, alias="schema")
    conversation_history: Optional[List[ConversationTurn]] = None
    include_schema: bool = True

    class Config:
        populate_by_name = True

    @validator('db_schema')
    def parse_schema(cls, v):
        if isinstance(v, dict):
            return json.dumps(v, ensure_ascii=False)
        return v

class BatchPredictRequest(BaseModel):
    """Batch request"""
    queries: List[BatchQuery] = Field(..., max_items=8)

class BatchPredictResponse(BaseModel):
    """Batch response"""
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
    model_type: str
    model_version: str
    device: str
    uptime_seconds: float
    total_requests: int
    dialog_requests: int
    single_turn_requests: int
    avg_response_time_ms: float
    config: Dict[str, Any]