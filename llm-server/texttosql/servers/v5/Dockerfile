FROM python:3.11-slim

WORKDIR /app

# Sağlık kontrolü için curl kalsın
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

COPY servers/v5/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY servers/v5/ .

EXPOSE 8000

ENV HOST=0.0.0.0
ENV PORT=8000
# Ollama'nın varsayılan adresi (Docker'dan host makineye çıkış)
ENV OLLAMA_BASE_URL=http://ollama:11434
# Qwen modelini varsayılan yaptık
ENV MODEL_NAME="hf.co/abdlkdr/QueryGen_Qwen2.5_Coder"

CMD ["python", "server.py"]