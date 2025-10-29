from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager
import time
import logging

from config import ServerConfig
from model_manager import ModelManager
from routes import health, prediction

# Setup logging
logging.basicConfig(
    level=getattr(logging, ServerConfig.LOG_LEVEL),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler('server.log')
    ]
)
logger = logging.getLogger(__name__)

# Global model manager
model_manager = ModelManager()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup and shutdown events"""
    # Startup
    logger.info("=" * 80)
    logger.info("Starting Text-to-SQL FastAPI Server")
    logger.info("=" * 80)
    
    try:
        model_manager.load_model()
        
        # Inject model manager into routes
        health.set_model_manager(model_manager)
        prediction.set_model_manager(model_manager)
        
        logger.info(" Server ready to accept requests")
        logger.info(f" Device: {ServerConfig.DEVICE}")
        logger.info(f" Listening on {ServerConfig.HOST}:{ServerConfig.PORT}")
        logger.info("=" * 80)
        
    except Exception as e:
        logger.error(f"Failed to start server: {e}", exc_info=True)
        raise
    
    yield
    
    # Shutdown
    logger.info("Shutting down server...")
    model_manager.clear_cache()
    logger.info(" Server stopped")


# Create FastAPI app
app = FastAPI(
    title="Text-to-SQL API",
    description="""
    Advanced Text-to-SQL API powered by T5-Large model.
    
    ## Features
    - Convert natural language to SQL queries
    - Optional schema support for better accuracy
    - Schema-agnostic mode for flexibility
    - Batch processing
    - SQL syntax validation
    - Confidence scores
    
    ## Models
    - T5-Large (770M parameters)
    - Trained on Spider dataset
    - Supports SQLite dialect
    """,
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc"
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
    
    # Log request
    logger.info(f" {request.method} {request.url.path}")
    
    try:
        response = await call_next(request)
        process_time = (time.time() - start_time) * 1000
        
        # Log response
        logger.info(
            f" {request.method} {request.url.path} - "
            f"Status: {response.status_code} - "
            f"Time: {process_time:.2f}ms"
        )
        
        # Add timing header
        response.headers["X-Process-Time"] = f"{process_time:.2f}ms"
        
        return response
        
    except Exception as e:
        process_time = (time.time() - start_time) * 1000
        logger.error(
            f"✗ {request.method} {request.url.path} - "
            f"Error: {str(e)} - "
            f"Time: {process_time:.2f}ms",
            exc_info=True
        )
        raise


# Exception handlers
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled exception: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={
            "status": "error",
            "message": "Internal server error",
            "detail": str(exc)
        }
    )


# Include routers
app.include_router(health.router)
app.include_router(prediction.router)


@app.get("/", tags=["Root"])
async def root():
    """Root endpoint with API information"""
    return {
        "name": "Text-to-SQL API",
        "version": "1.0.0",
        "status": "running",
        "model": "T5-Large",
        "endpoints": {
            "predict": "/predict",
            "batch_predict": "/batch-predict",
            "health": "/health",
            "docs": "/docs",
            "redoc": "/redoc"
        },
        "example_request": {
            "with_schema": {
                "question": "List all users",
                "schema": {
                    "schema_info": "Users: user_id (number) [PK], name (text)"
                }
            },
            "without_schema": {
                "question": "Show students with high GPA"
            }
        }
    }
