from fastapi import APIRouter
from models import HealthResponse

router = APIRouter(tags=["Health"])

# Global model manager will be injected
model_manager = None

def set_model_manager(manager):
    global model_manager
    model_manager = manager

@router.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint with dialog stats"""
    return model_manager.get_health_info()