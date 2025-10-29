# Text-to-SQL LLM Server

## Project Structure

```
llm_server/
├── config.py              # Configuration
├── models.py              # Pydantic models
├── model_manager.py       # Model inference
├── utils.py               # Utilities
├── routes/
│   ├── __init__.py
│   ├── health.py          # Health endpoints
│   └── prediction.py      # Prediction endpoints
├── main.py                # FastAPI app
├── server.py              # Startup script
├── requirements.txt       # Dependencies
└── text2sql_final_model/  # Your trained model
```

## Installation

```bash
pip install -r requirements.txt
```

## Run Server

```bash
python server.py
```

## API Documentation

Visit: http://localhost:8000/docs

## Example Request

```bash
curl -X POST http://localhost:8000/predict \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Show all students",
    "schema": {
      "db_name": "school",
      "schema_string": "Student: id (number) [PK], name (text)"
    },
    "include_schema": true
  }'
```
