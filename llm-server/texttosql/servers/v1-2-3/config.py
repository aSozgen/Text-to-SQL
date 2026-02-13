import os
from pathlib import Path

class ServerConfig:

    # Server settings
    HOST = "0.0.0.0"
    PORT = 8000
    MAX_WORKERS = 1
    LOG_LEVEL = "INFO"
    
    # Model settings
    MODEL_PATH = "../../models/v3"
    DEVICE = None  # Auto-detect
    
    # Request limits
    REQUEST_TIMEOUT = 45  # Increased for dialog processing
    MAX_BATCH_SIZE = 8
    MAX_CONVERSATION_HISTORY = 2  # Matches your training (2-turn context)
    
    # Model inference settings - MATCHES your training config
    MAX_SOURCE_LENGTH = 768   # Your training: 768
    MAX_TARGET_LENGTH = 192   # Your training: 192
    DEFAULT_NUM_BEAMS = 5     # Your training: 5
    
    # Schema settings
    MAX_SCHEMA_LENGTH = 500  # Schema truncation limit
    
    # CORS settings
    ALLOWED_ORIGINS = ["*"]