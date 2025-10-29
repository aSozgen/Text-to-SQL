class ServerConfig:
    """Server configuration settings"""
    
    # Server settings
    HOST = "0.0.0.0"
    PORT = 8000
    MAX_WORKERS = 1  # Use 1 for GPU
    LOG_LEVEL = "INFO"
    
    # Model settings
    MODEL_PATH = "./text2sql_model"  # Phase 2 dialog model
    DEVICE = None  # Will be set at runtime (cuda/cpu)
    
    # Request limits
    REQUEST_TIMEOUT = 30
    MAX_BATCH_SIZE = 10
    MAX_CONVERSATION_HISTORY = 5  # Max previous turns to keep
    
    # Model inference settings
    MAX_SOURCE_LENGTH = 768  # Longer for dialog context
    MAX_TARGET_LENGTH = 192
    DEFAULT_NUM_BEAMS = 6  # Higher for dialog accuracy
    
    # CORS settings (adjust for production)
    ALLOWED_ORIGINS = ["*"]  # In production: ["http://your-backend:8080"]

