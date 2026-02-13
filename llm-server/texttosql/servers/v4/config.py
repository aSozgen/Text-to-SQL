import os
from pathlib import Path

class ServerConfig:

    # Server settings
    HOST = "0.0.0.0"
    PORT = 8000
    MAX_WORKERS = 1
    LOG_LEVEL = "INFO"
    
    # Model settings
    MODEL_PATH = "../../models/v4"
    DEVICE = None  # Auto-detect
    
    # Request limits
    REQUEST_TIMEOUT = 45  # Increased for dialog processing
    MAX_BATCH_SIZE = 8
    MAX_CONVERSATION_HISTORY = 2
    
    MAX_SOURCE_LENGTH = 768
    MAX_TARGET_LENGTH = 192
    DEFAULT_NUM_BEAMS = 5
    
    # Schema settings
    MAX_SCHEMA_LENGTH = 500
    
    # CORS settings
    ALLOWED_ORIGINS = ["*"]