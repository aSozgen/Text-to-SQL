from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from contextlib import asynccontextmanager
import time
import logging
import json

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

    # --- GELEN ISTEK BODY'SINI YAKALA VE LOGLA ---
    req_body = b""
    try:
        req_body = await request.body()
        if req_body:
            logger.debug(f"\n{'='*50}\n[INCOMING REQUEST] {request.method} {request.url.path}\n{req_body.decode('utf-8', 'ignore')}\n{'='*50}")

        # FastAPI'nin endpoint'lerde body'yi tekrar okuyabilmesi için sıfırlıyoruz
        async def receive():
            return {"type": "http.request", "body": req_body}
        request._receive = receive
    except Exception as e:
        logger.error(f"Request body okunamadı: {e}")

    # Isteği Route'a gönder (İşlem başlasın)
    response = await call_next(request)

    duration = (time.time() - start) * 1000
    logger.info(
        f"{request.method} {request.url.path} - "
        f"{response.status_code} - {duration:.2f}ms"
    )
    return response

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    body = await request.body()
    logger.error(f"VALIDATION ERROR (422) at {request.method} {request.url.path}")
    logger.error(f"Hatalı Alanlar: {exc.errors()}")

    return JSONResponse(
        status_code=422,
        content={
            "detail": "Data Validation Failed",
            "errors": exc.errors(),
            "body": body.decode('utf-8', errors='ignore')
        }
    )

@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled SERVER ERROR: {str(exc)}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={"detail": "Internal server error", "error": str(exc)}
    )

app.include_router(health.router)
app.include_router(prediction.router)

@app.get("/")
async def root():
    return {
        "name": "Dialog-Aware Text2SQL Server",
        "version": "2.0.0",
        "model": "Phase 2 Improved (2-turn context)",
        "features": [
            "Single-turn queries",
            "Multi-turn dialog",
            "Schema-aware and agnostic",
            "Batch processing",
            "SQL validation"
        ]
    }