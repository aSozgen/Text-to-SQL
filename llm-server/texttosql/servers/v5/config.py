import os

class ServerConfig:
    # Server settings
    HOST = os.environ.get("HOST", "0.0.0.0")
    PORT = int(os.environ.get("PORT", 8000))
    MAX_WORKERS = 1
    LOG_LEVEL = os.environ.get("LOG_LEVEL", "INFO")

    # Ollama settings
    OLLAMA_BASE_URL = os.environ.get("OLLAMA_BASE_URL", "http://ollama:11434")
    MODEL_NAME = os.environ.get("MODEL_NAME", "hf.co/abdlkdr/QueryGen_Qwen2.5_Coder")

    # Request limits
    MAX_SOURCE_LENGTH = 768
    MAX_TARGET_LENGTH = 192
    DEFAULT_NUM_BEAMS = 5
    MAX_BATCH_SIZE = 8
    MAX_CONVERSATION_HISTORY = 2
    OLLAMA_TIMEOUT = 120  # Ollama'nın cevap vermesi için beklenecek maksimum süre (saniye)

    # Schema settings
    MAX_SCHEMA_LENGTH = 500
    ALLOWED_ORIGINS = ["*"]