from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import time
import logging

from config import ServerConfig
from model_manager import ModelManager
from routes import health, prediction

# Setup logging
logging.basicConfig(
    level=getattr(logging, ServerConfig.LOG_LEVEL),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Global model manager
model_manager = ModelManager()

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup and shutdown events"""
    # Startup
    logger.info("Starting Dialog-Aware Text2SQL Server...")
    model_manager.load_model()
    
    # Inject model manager into routes
    health.set_model_manager(model_manager)
    prediction.set_model_manager(model_manager)
    
    logger.info("Server ready to accept requests")
    yield
    # Shutdown
    logger.info("Shutting down server...")

# Create FastAPI app
app = FastAPI(
    title="Dialog-Aware Text-to-SQL Server",
    description="FastAPI server for converting natural language to SQL with conversation context",
    version="1.0.0",
    lifespan=lifespan
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=ServerConfig.ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Request logging middleware
@app.middleware("http")
async def log_requests(request: Request, call_next):
    start_time = time.time()
    response = await call_next(request)
    process_time = (time.time() - start_time) * 1000
    logger.info(
        f"{request.method} {request.url.path} - "
        f"Status: {response.status_code} - "
        f"Time: {process_time:.2f}ms"
    )
    return response

# Include routers
app.include_router(health.router)
app.include_router(prediction.router)

@app.get("/", tags=["Root"])
async def root():
    """Root endpoint"""
    return {
        "message": "Dialog-Aware Text-to-SQL Server",
        "version": "1.0.0",
        "model_type": "dialog-aware",
        "features": [
            "Single-turn queries",
            "Multi-turn dialog support",
            "Conversation history tracking",
            "Schema-aware and agnostic modes"
        ],
        "endpoints": {
            "predict": "/predict",
            "batch_predict": "/batch-predict",
            "health": "/health",
            "docs": "/docs"
        }
    }

