import json
import pytest
from pydantic import ValidationError
from models import PredictRequest


def test_predict_request_valid_schema_conversion():
    # Java backend'den schema'nın JSON (dict) olarak geldiği senaryo
    payload = {
        "question": "Maaşı 5000'den büyük olan çalışanlar kimler?",
        "schema": {"table": "employees", "columns": ["name", "salary"]}
    }

    request = PredictRequest(**payload)

    # Dict olarak gelen schema'nın otomatik olarak String'e çevrildiğini doğruluyoruz
    assert isinstance(request.db_schema, str)
    assert "employees" in request.db_schema


def test_predict_request_empty_question():
    # Boş soru atıldığında 422 hatası (ValidationError) fırlatılmalı
    with pytest.raises(ValidationError):
        PredictRequest(question="   ", schema="employees(id)")


def test_conversation_history_limit():
    # 3 tur geçmiş gönderildiğinde, validator'ın sadece son 2 turu tuttuğunu test etme
    history = [
        {"question": "Soru 1", "sql": "SQL 1"},
        {"question": "Soru 2", "sql": "SQL 2"},
        {"question": "Soru 3", "sql": "SQL 3"}
    ]
    request = PredictRequest(question="Yeni soru", conversation_history=history)

    assert len(request.conversation_history) == 2
    assert request.conversation_history[0].utterance == "Soru 2"