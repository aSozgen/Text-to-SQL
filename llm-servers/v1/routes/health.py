from fastapi import APIRouter
from models import HealthResponse

router = APIRouter(tags=["Health"])

model_manager = None

def set_model_manager(manager):
    global model_manager
    model_manager = manager


@router.get("/health", response_model=HealthResponse)
async def health_check():
    """
    Health check endpoint
    
    Returns model status, uptime, and performance metrics
    """
    return model_manager.get_health_info()


@router.post("/admin/clear-cache")
async def clear_cache():
    """Clear prediction cache (admin only)"""
    model_manager.clear_cache()
    return {"status": "success", "message": "Cache cleared"}
