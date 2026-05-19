import pytest
from fastapi.testclient import TestClient
from unittest.mock import MagicMock, patch
import sys
import os

# Add the server directory to sys.path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

# Mock model_manager before importing main to prevent it from starting Ollama check
mock_mm_instance = MagicMock()
mock_mm_instance.model_loaded = True

with patch("main.ModelManager", return_value=mock_mm_instance):
    from main import app
    import main
    import routes.health
    import routes.prediction

    # Manually set the mock instance to the modules
    main.model_manager = mock_mm_instance
    routes.health.model_manager = mock_mm_instance
    routes.prediction.model_manager = mock_mm_instance

def test_root():
    with TestClient(app) as client:
        response = client.get("/")
        assert response.status_code == 200
        assert response.json()["name"] == "Dialog-Aware Text2SQL Server"

def test_health():
    mock_info = {
        "status": "healthy",
        "model_loaded": True,
        "model_name": "test-model",
        "model_type": "ollama-endpoint",
        "model_version": "v1.0",
        "device": "cpu",
        "uptime_seconds": 100,
        "total_requests": 10,
        "dialog_requests": 2,
        "single_turn_requests": 8,
        "avg_response_time_ms": 150.5,
        "config": {
            "max_source_length": 512,
            "max_target_length": 512,
            "max_context_turns": 2,
            "default_num_beams": 5
        }
    }
    
    mock_mm_instance.get_health_info.return_value = mock_info
    with TestClient(app) as client:
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json()["status"] == "healthy"
        assert response.json()["model_name"] == "test-model"

def test_ready():
    mock_mm_instance.model_loaded = True
    with TestClient(app) as client:
        response = client.get("/ready")
        assert response.status_code == 200
        assert response.json()["status"] == "ready"

def test_predict_single_turn():
    mock_prediction = {
        "status": "success",
        "sql": "SELECT * FROM students",
        "is_valid": True,
        "validation_error": None,
        "confidence": 0.95,
        "schema_used": True,
        "context_used": False,
        "context_turns": 0,
        "processing_time_ms": 100,
        "metadata": {
            "model_version": "test",
            "timestamp": "2024-05-20T12:00:00Z",
            "device": "cpu",
            "num_beams": 1,
            "max_context_turns": 2
        }
    }
    
    payload = {
        "question": "Show all students",
        "db_schema": "Student: id, name",
        "include_schema": True
    }
    
    mock_mm_instance.predict.return_value = mock_prediction
    with TestClient(app) as client:
        response = client.post("/predict", json=payload)
        assert response.status_code == 200
        assert response.json()["sql"] == "SELECT * FROM students"
        assert response.json()["status"] == "success"

def test_predict_multi_turn():
    mock_prediction = {
        "status": "success",
        "sql": "SELECT * FROM students WHERE age > 20",
        "is_valid": True,
        "validation_error": None,
        "confidence": 0.92,
        "schema_used": True,
        "context_used": True,
        "context_turns": 1,
        "processing_time_ms": 120,
        "metadata": {
            "model_version": "test",
            "timestamp": "2024-05-20T12:00:00Z",
            "device": "cpu",
            "num_beams": 1,
            "max_context_turns": 2
        }
    }
    
    payload = {
        "question": "Filter by age greater than 20",
        "db_schema": "Student: id, name, age",
        "conversation_history": [
            {
                "utterance": "Show all students",
                "query": "SELECT * FROM students"
            }
        ]
    }
    
    mock_mm_instance.predict.return_value = mock_prediction
    with TestClient(app) as client:
        response = client.post("/predict", json=payload)
        assert response.status_code == 200
        assert response.json()["context_used"] is True
        assert response.json()["context_turns"] == 1

def test_batch_predict():
    mock_prediction = {
        "status": "success",
        "sql": "SELECT * FROM students",
        "is_valid": True,
        "validation_error": None,
        "schema_used": True,
        "context_used": False,
        "context_turns": 0,
        "status": "success" # added for batch logic
    }
    
    payload = {
        "queries": [
            {
                "question": "Show all students",
                "db_schema": "Student: id, name"
            },
            {
                "question": "Show all courses",
                "db_schema": "Course: id, name"
            }
        ]
    }
    
    mock_mm_instance.predict.return_value = mock_prediction
    with TestClient(app) as client:
        response = client.post("/batch-predict", json=payload)
        assert response.status_code == 200
        assert response.json()["total_queries"] == 2
        assert len(response.json()["results"]) == 2

def test_validation_error():
    # Missing required field 'question'
    payload = {
        "db_schema": "Student: id, name"
    }
    with TestClient(app) as client:
        response = client.post("/predict", json=payload)
        assert response.status_code == 422
        assert "detail" in response.json()
