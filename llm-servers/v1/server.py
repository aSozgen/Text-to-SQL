import uvicorn
import logging
from config import ServerConfig

logger = logging.getLogger(__name__)

if __name__ == "__main__":
    logger.info("="*80)
    logger.info("Text-to-SQL Server Startup")
    logger.info("="*80)
    logger.info(f"Host: {ServerConfig.HOST}")
    logger.info(f"Port: {ServerConfig.PORT}")
    logger.info(f"Model: {ServerConfig.MODEL_PATH}")
    logger.info("="*80)
    
    uvicorn.run(
        "main:app",
        host=ServerConfig.HOST,
        port=ServerConfig.PORT,
        workers=ServerConfig.MAX_WORKERS,
        log_level=ServerConfig.LOG_LEVEL.lower(),
        reload=False,
        access_log=True
    )
