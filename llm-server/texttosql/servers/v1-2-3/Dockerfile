FROM python:3.11-slim

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential curl \
    && rm -rf /var/lib/apt/lists/*

COPY servers/v1-2-3/requirements.txt .

RUN pip install --no-cache-dir torch==2.6.0 --index-url https://download.pytorch.org/whl/cpu

RUN pip install --no-cache-dir -r requirements.txt

COPY servers/v1-2-3/ .

EXPOSE 8000

ENV HOST=0.0.0.0
ENV PORT=8000
ENV MODEL_PATH=/app/models/v3

CMD ["python", "server.py"]