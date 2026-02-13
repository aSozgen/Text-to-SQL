from fastapi import APIRouter
from models import HealthResponse

router = APIRouter(tags=["Health"])

model_manager = None

def set_model_manager(manager):
    global model_manager
    model_manager = manager

@router.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check with dialog statistics"""
    return model_manager.get_health_info()

@router.get("/ready")
async def readiness_check():
    """Kubernetes readiness probe"""
    if model_manager and model_manager.model_loaded:
        return {"status": "ready"}
    return {"status": "not_ready"}, 503