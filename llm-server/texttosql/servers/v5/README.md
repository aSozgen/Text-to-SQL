# Dialog-Aware Text-to-SQL Server

FastAPI server for Phase 2 improved dialog-aware Text-to-SQL model with 2-turn context support.

## Model Performance

Training results on Spider dataset:

|           | Exact Match | Execution Accuracy | SQL Validity |
|-----------|-------------|--------------------|--------------|
| Zero-Shot | %39.2175    | %65.6264           | %87.0983     |
| Few-Shot  | %41.3600    | %69.6320           | %94.6903     |



## Features

- ✅ Single-turn Text-to-SQL
- ✅ Multi-turn dialog with 2-turn context
- ✅ Schema-aware and schema-agnostic modes
- ✅ Batch processing
- ✅ SQL validation with sqlglot
- ✅ Confidence scores

## Project Structure

```
llm_server/
├── config.py              # Configuration
├── models.py              # Pydantic models
├── model_manager.py       # Model inference
├── utils.py               # SQL validation
├── routes/
│   ├── health.py          # Health endpoints
│   └── prediction.py      # Prediction endpoints
├── main.py                # FastAPI app
├── server.py              # Startup script
├── test_client.py         # Example usage
└── requirements.txt

```
## Prerequest

- **Please install *NVIDIA Container Toolkit* for better performance.**

## Installation

```bash
pip install -r requirements.txt
```

## Configuration

Edit `config.py`:

```python
MODEL_PATH = "./text2sql_dialog_final_v2"
MAX_SOURCE_LENGTH = 768        # Matches training
MAX_TARGET_LENGTH = 192        # Matches training
MAX_CONVERSATION_HISTORY = 2   # 2-turn context
DEFAULT_NUM_BEAMS = 5          # Matches training
```

## Run Server

```bash
python server.py
```

Server will start on `http://localhost:8000`

## API Examples

### Single-Turn Query

```bash
curl -X POST http://localhost:8000/predict \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Show all students",
    "schema": "Student: id (number) [PK], name (text), age (number)",
    "include_schema": true
  }'
```

### Dialog Query (Turn 2)

```bash
curl -X POST http://localhost:8000/predict \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Filter by age greater than 20",
    "schema": "Student: id (number) [PK], name (text), age (number)",
    "conversation_history": [
      {
        "question": "Show all students",
        "sql": "SELECT * FROM Student"
      }
    ]
  }'
```

### Dialog Query (Turn 3 - uses last 2 turns)

```bash
curl -X POST http://localhost:8000/predict \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Order by name",
    "conversation_history": [
      {
        "question": "Show all students",
        "sql": "SELECT * FROM Student"
      },
      {
        "question": "Filter by age > 20",
        "sql": "SELECT * FROM Student WHERE age > 20"
      }
    ]
  }'
```

### Batch Processing

```bash
curl -X POST http://localhost:8000/batch-predict \
  -H "Content-Type: application/json" \
  -d '{
    "queries": [
      {
        "question": "Show all students",
        "schema": "Student: id (number) [PK], name (text)"
      },
      {
        "question": "Count students",
        "schema": "Student: id (number) [PK], name (text)"
      }
    ]
  }'
```

## API Documentation

Interactive API docs available at:
- Swagger UI: `http://localhost:8000/docs`
- ReDoc: `http://localhost:8000/redoc`

## Endpoints

| Endpoint         | Method | Description                    |
| ---------------- | ------ | ------------------------------ |
| `/`              | GET    | Root info                      |
| `/health`        | GET    | Health check with stats        |
| `/ready`         | GET    | Readiness probe                |
| `/predict`       | POST   | Single prediction with dialog  |
| `/batch-predict` | POST   | Batch predictions              |

## Response Format

```json
{
  "status": "success",
  "sql": "SELECT * FROM Student WHERE age > 20",
  "is_valid": true,
  "validation_error": null,
  "confidence": 0.9234,
  "schema_used": true,
  "context_used": true,
  "context_turns": 1,
  "processing_time_ms": 145,
  "metadata": {
    "model_version": "text2sql-dialog-v2.0-improved",
    "timestamp": "2025-10-29T12:34:56.789Z",
    "device": "cuda",
    "num_beams": 5
  }
}
```

## Model Requirements

Your model folder must contain:
- `config.json`
- `pytorch_model.bin` (or `model.safetensors`)
- `tokenizer_config.json`
- `special_tokens_map.json`
- `tokenizer.json`

## Performance

- Single-turn: ~100-150ms (GPU)
- Dialog (2-turn): ~150-250ms (GPU)
- Batch: Parallel execution

## Docker Deployment (Optional)

```dockerfile
FROM python:3.10-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt

COPY ../.. .

EXPOSE 8000
CMD ["python", "server.py"]
```

Build and run:
```bash
docker build -t text2sql-server .
docker run -p 8000:8000 -v /path/to/model:/app/text2sql_dialog_final_v2 text2sql-server
```

## Troubleshooting

**Model not loading?**
- Check `MODEL_PATH` in `config.py`
- Verify model files exist
- Check GPU memory (requires ~2GB)

**Low accuracy?**
- Model achieves 18.47% on CoSQL (expected for dialog)
- Enable schema with `include_schema: true`
- Provide conversation history for dialog turns

**Out of memory?**
- Reduce `per_device_train_batch_size` to 1
- Disable FP16: `model.half()` in `model_manager.py`
- Use CPU: Set `CUDA_VISIBLE_DEVICES=""`

## License

MIT