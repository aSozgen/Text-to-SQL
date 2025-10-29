class ServerConfig:
    """Server configuration settings"""
    
    # Server settings
    HOST = "0.0.0.0"
    PORT = 8000
    MAX_WORKERS = 1
    LOG_LEVEL = "INFO"
    
    # Model settings
    MODEL_PATH = "./text2sql_model"
    DEVICE = None  # Auto-detected
    
    # Request limits
    REQUEST_TIMEOUT = 30
    MAX_BATCH_SIZE = 10
    MAX_QUESTION_LENGTH = 500
    MAX_SCHEMA_LENGTH = 2000
    
    # Model inference settings
    MAX_SOURCE_LENGTH = 768  # For T5-Large
    MAX_TARGET_LENGTH = 256
    DEFAULT_NUM_BEAMS = 5
    
    # CORS settings
    ALLOWED_ORIGINS = ["*"]
    
    # Caching (optional)
    ENABLE_CACHE = True
    CACHE_SIZE = 100