# Dialog-Aware Text-to-SQL Server

## Features
- ✅ Single-turn Text-to-SQL
- ✅ Multi-turn dialog support with conversation history
- ✅ Schema-aware and schema-agnostic modes
- ✅ Batch processing
- ✅ SQL validation
- ✅ Confidence scores

## Project Structure

```
llm_server/
├── config.py              # Configuration
├── models.py              # Pydantic models (with dialog support)
├── model_manager.py       # Model inference (dialog-aware)
├── utils.py               # Utilities
├── routes/
│   ├── __init__.py
│   ├── health.py          # Health endpoints
│   └── prediction.py      # Prediction endpoints (dialog support)
├── main.py                # FastAPI app
├── server.py              # Startup script
├── test_client.py         # Example usage
├── requirements.txt       # Dependencies
└── text2sql_dialog_final/ # Your Phase 2 trained model
```

## Installation

```bash
pip install -r requirements.txt
```

## Configuration

Edit `config.py`:
```python
MODEL_PATH = "./text2sql_dialog_final"  # Your Phase 2 model
MAX_CONVERSATION_HISTORY = 5            # Max previous turns
DEFAULT_NUM_BEAMS = 6                   # Higher for dialog accuracy
```

## Run Server

```bash
python server.py
```

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
    ],
    "include_schema": true
  }'
```

## API Documentation

Visit: http://localhost:8000/docs

## Model Path

Make sure `text2sql_dialog_final` folder contains:
- config.json
- pytorch_model.bin (or model.safetensors)
- tokenizer_config.json
- special_tokens_map.json
- tokenizer.json

## Performance

- Single-turn: ~100-200ms
- Dialog (with context): ~150-300ms
- Batch processing: Parallel execution