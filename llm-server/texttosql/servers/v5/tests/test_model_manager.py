import pytest
from unittest.mock import patch, MagicMock
from model_manager import ModelManager


@patch("model_manager.requests.post")
def test_predict_success(mock_post):
    # Ollama'nın döneceği sahte JSON yanıtını hazırlıyoruz
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.json.return_value = {
        "choices": [
            {
                "message": {
                    "content": "Aşağıda istediğiniz SQL sorgusu bulunmaktadır:\n```sql\nSELECT name FROM employees WHERE salary > 5000;\n```"
                },
                "logprobs": None  # Logprobs dönmediği senaryo
            }
        ]
    }
    mock_post.return_value = mock_response

    # Fonksiyonu mock'lu ortamda çalıştırıyoruz
    manager = ModelManager()

    # Utils modülündeki fonksiyonların gerçek halini bilmediğimiz için
    # geçici olarak test amaçlı monkeypatch ile basitleştiriyoruz
    with patch("model_manager.validate_sql", return_value=(True, None)), \
            patch("model_manager.normalize_sql", lambda x: x):
        result = manager.predict(question="Maaşı 5000'den büyük olanlar?")

    # 1. İstek doğru atılmış mı?
    assert mock_post.called

    # 2. regex (_extract_sql) gereksiz metinleri atıp sadece SQL'i yakalayabilmiş mi?
    assert result["status"] == "success"
    assert result["sql"] == "SELECT name FROM employees WHERE salary > 5000;"
    assert result["confidence"] == 0.95  # Logprobs yoksa default 0.95 atanmalı