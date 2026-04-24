from fastapi import APIRouter, HTTPException
from models import *
from config import ServerConfig
import time
import logging

logger = logging.getLogger(__name__)
router = APIRouter(tags=["Prediction"])

model_manager = None

def set_model_manager(manager):
    global model_manager
    model_manager = manager

@router.post("/predict", response_model=PredictResponse)
async def predict(request: PredictRequest):
    """
    Text-to-SQL with dialog support (2-turn context)

    Examples:

    1. Single-turn:
    {
        "question": "Show all students",
        "schema": "Student: id (number) [PK], name (text), age (number)",
        "include_schema": true
    }

    2. Dialog Turn 2:
    {
        "question": "Filter by age greater than 20",
        "schema": "Student: id (number) [PK], name (text), age (number)",
        "conversation_history": [
            {
                "question": "Show all students",
                "sql": "SELECT * FROM Student"
            }
        ]
    }

    3. Dialog Turn 3 (uses last 2 turns):
    {
        "question": "Order by name",
        "conversation_history": [
            {
                "question": "Show all students",
                "sql": "SELECT * FROM Student"
            },
            {
                "question": "Filter by age greater than 20",
                "sql": "SELECT * FROM Student WHERE age > 20"
            }
        ]
    }
    """
    logger.info(f"Request: {request.question[:50]}...")

    # Convert history
    history = None
    if request.conversation_history:
        history = [
            {
                "utterance": turn.utterance,
                "query": turn.query
            }
            for turn in request.conversation_history
        ]
        logger.info(f"Dialog with {len(history)} turn(s) (using last 2)")

    result = model_manager.predict(
        question=request.question,
        schema=request.db_schema,
        conversation_history=history,
        include_schema=request.include_schema,
        num_beams=request.num_beams
    )

    return result

@router.post("/batch-predict", response_model=BatchPredictResponse)
async def batch_predict(request: BatchPredictRequest):
    """Batch processing"""

    if len(request.queries) > ServerConfig.MAX_BATCH_SIZE:
        raise HTTPException(
            status_code=400,
            detail=f"Max batch size: {ServerConfig.MAX_BATCH_SIZE}"
        )

    logger.info(f"Batch: {len(request.queries)} queries")

    start_time = time.time()
    results = []
    successful = 0
    failed = 0

    for query in request.queries:
        history = None
        if query.conversation_history:
            history = [
                {"utterance": t.utterance, "query": t.query}
                for t in query.conversation_history
            ]

        result = model_manager.predict(
            question=query.question,
            schema=query.db_schema,
            conversation_history=history,
            include_schema=query.include_schema
        )

        results.append({
            "question": query.question,
            "sql": result["sql"],
            "is_valid": result["is_valid"],
            "validation_error": result["validation_error"],
            "schema_used": result["schema_used"],
            "context_used": result["context_used"],
            "context_turns": result["context_turns"]
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