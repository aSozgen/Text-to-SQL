from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager
import time
import logging

from config import ServerConfig
from model_manager import ModelManager
from routes import health, prediction

logging.basicConfig(
    level=getattr(logging, ServerConfig.LOG_LEVEL),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

model_manager = ModelManager()

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup and shutdown"""
    logger.info(" Starting Dialog-Aware Text2SQL Server (Phase 2 Improved)")
    model_manager.load_model()

    health.set_model_manager(model_manager)
    prediction.set_model_manager(model_manager)

    logger.info(" Server ready")
    yield
    logger.info("Shutting down...")

app = FastAPI(
    title="Dialog-Aware Text2SQL Server",
    description="Phase 2 improved model with 2-turn context support",
    version="2.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=ServerConfig.ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.middleware("http")
async def log_requests(request: Request, call_next):
    start = time.time()
    response = await call_next(request)
    duration = (time.time() - start) * 1000
    logger.info(
        f"{request.method} {request.url.path} - "
        f"{response.status_code} - {duration:.2f}ms"
    )
    return response

@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled error: {str(exc)}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={"detail": "Internal server error", "error": str(exc)}
    )

app.include_router(health.router)
app.include_router(prediction.router)

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "name": "Dialog-Aware Text2SQL Server",
        "version": "2.0.0",
        "model": "Phase 2 Improved (2-turn context)",
        "features": [
            "Single-turn queries",
            "Multi-turn dialog (2-turn context)",
            "Schema-aware and agnostic",
            "Batch processing",
            "SQL validation"
        ],
        "config": {
            "max_source_length": ServerConfig.MAX_SOURCE_LENGTH,
            "max_target_length": ServerConfig.MAX_TARGET_LENGTH,
            "max_context_turns": ServerConfig.MAX_CONVERSATION_HISTORY,
            "default_beams": ServerConfig.DEFAULT_NUM_BEAMS
        },
        "endpoints": {
            "predict": "/predict",
            "batch": "/batch-predict",
            "health": "/health",
            "docs": "/docs"
        }
    }
