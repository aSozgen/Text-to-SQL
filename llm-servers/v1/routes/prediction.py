from fastapi import APIRouter, HTTPException, status
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

model_manager = None

def set_model_manager(manager):
    global model_manager
    model_manager = manager


@router.post("/predict", response_model=PredictResponse, status_code=status.HTTP_200_OK)
async def predict(request: PredictRequest):
    """
    Convert natural language question to SQL query
    
    **Examples:**
    
    **With Schema:**
    ```json
    {
        "question": "List orders placed by user with id 10 in the last month",
        "schema": {
            "schema_info": "Users: user_id (number) [PK], name (text) | Orders: order_id (number) [PK], user_id (number), order_date (date), total (number) | Foreign Keys: Orders.user_id = Users.user_id"
        },
        "num_beams": 5
    }
    ```
    
    **Without Schema (Schema-Agnostic):**
    ```json
    {
        "question": "Show all students with GPA above 3.5",
        "num_beams": 1
    }
    ```
    """
    logger.info(f"Prediction request: '{request.question[:50]}...' (schema: {request.include_schema})")
    
    try:
        result = model_manager.predict(
            question=request.question,
            schema=request.schema,
            num_beams=request.num_beams,
            temperature=request.temperature
        )
        
        return result
        
    except Exception as e:
        logger.error(f"Prediction failed: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Prediction failed: {str(e)}"
        )


@router.post("/batch-predict", response_model=BatchPredictResponse)
async def batch_predict(request: BatchPredictRequest):
    """
    Process multiple queries in batch
    
    **Example:**
    ```json
    {
        "queries": [
            {
                "question": "Show all users",
                "schema": {"schema_info": "Users: id (number) [PK], name (text)"}
            },
            {
                "question": "Count total orders",
                "num_beams": 5
            }
        ]
    }
    ```
    """
    
    if len(request.queries) > ServerConfig.MAX_BATCH_SIZE:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Batch size exceeds maximum of {ServerConfig.MAX_BATCH_SIZE}"
        )
    
    logger.info(f"Batch request with {len(request.queries)} queries")
    
    start_time = time.time()
    results = []
    successful = 0
    failed = 0
    
    for idx, query in enumerate(request.queries):
        try:
            result = model_manager.predict(
                question=query.question,
                schema=query.schema,
                num_beams=query.num_beams
            )
            
            results.append({
                "index": idx,
                "question": query.question,
                "sql": result["sql"],
                "is_valid": result["is_valid"],
                "validation_error": result["validation_error"],
                "confidence": result["confidence"],
                "schema_used": result["schema_used"],
                "processing_time_ms": result["processing_time_ms"]
            })
            
            if result["status"] == "success":
                successful += 1
            else:
                failed += 1
                
        except Exception as e:
            logger.error(f"Batch query {idx} failed: {e}")
            results.append({
                "index": idx,
                "question": query.question,
                "sql": None,
                "is_valid": False,
                "error": str(e)
            })
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
