import uvicorn
import logging
from config import ServerConfig

logger = logging.getLogger(__name__)

if __name__ == "__main__":
    logger.info(f"Starting server on {ServerConfig.HOST}:{ServerConfig.PORT}")
    logger.info(f"Model path: {ServerConfig.MODEL_NAME}")

    uvicorn.run(
        "main:app",
        host=ServerConfig.HOST,
        port=ServerConfig.PORT,
        workers=1,
        log_level=ServerConfig.LOG_LEVEL.lower(),
        reload=False
    )
