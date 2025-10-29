from fastapi import APIRouter, HTTPException
from models import (
    PredictRequest,
    PredictResponse,
    BatchPredictRequest,
    BatchPredictResponse
)
from config import ServerConfig
import time
import logging

logger = logging.getLogger(__name__)
router = APIRouter(tags=["Prediction"])

# Global model manager will be injected
model_manager = None

def set_model_manager(manager):
    global model_manager
    model_manager = manager

@router.post("/predict", response_model=PredictResponse)
async def predict(request: PredictRequest):
    """
    Convert natural language question to SQL query with dialog support
    
    Example Single-Turn:
    {
        "question": "Show all students",
        "schema": "Student: id (number) [PK], name (text), age (number)",
        "include_schema": true
    }
    
    Example Dialog (Turn 2):
    {
        "question": "Filter by age greater than 20",
        "schema": "Student: id (number) [PK], name (text), age (number)",
        "conversation_history": [
            {
                "question": "Show all students",
                "sql": "SELECT * FROM Student"
            }
        ],
        "include_schema": true
    }
    """
    logger.info(f"Received request: {request.question[:50]}...")
    
    # Convert conversation_history to dict format
    history = None
    if request.conversation_history:
        history = [
            {"question": turn.question, "sql": turn.sql}
            for turn in request.conversation_history
        ]
        logger.info(f"Dialog request with {len(history)} previous turn(s)")
    
    result = model_manager.predict(
        question=request.question,
        schema=request.schema,
        conversation_history=history,
        include_schema=request.include_schema,
        num_beams=request.num_beams
    )
    
    return result

@router.post("/batch-predict", response_model=BatchPredictResponse)
async def batch_predict(request: BatchPredictRequest):
    """Process multiple queries at once (supports dialog)"""
    
    if len(request.queries) > ServerConfig.MAX_BATCH_SIZE:
        raise HTTPException(
            status_code=400,
            detail=f"Batch size exceeds maximum of {ServerConfig.MAX_BATCH_SIZE}"
        )
    
    logger.info(f"Batch request with {len(request.queries)} queries")
    
    start_time = time.time()
    results = []
    successful = 0
    failed = 0
    
    for query in request.queries:
        # Convert conversation history
        history = None
        if query.conversation_history:
            history = [
                {"question": turn.question, "sql": turn.sql}
                for turn in query.conversation_history
            ]
        
        result = model_manager.predict(
            question=query.question,
            schema=query.schema,
            conversation_history=history,
            include_schema=query.include_schema
        )
        
        results.append({
            "question": query.question,
            "sql": result["sql"],
            "is_valid": result["is_valid"],
            "validation_error": result["validation_error"],
            "schema_used": result["schema_used"],
            "context_used": result["context_used"]
        })
        
        if result["status"] == "success":
            successful += 1
        else:
            failed += 1
    
    total_time = int((time.time() - start_time) * 1000)
    
    return {
        "status": "success",
        "results": results,
        "total_queries": len(request.queries),
        "successful": successful,
        "failed": failed,
        "total_processing_time_ms": total_time
    }