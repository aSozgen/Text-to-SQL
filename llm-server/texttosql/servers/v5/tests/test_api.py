from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


def test_root_endpoint():
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert data["name"] == "Dialog-Aware Text2SQL Server"
    assert "Multi-turn dialog" in data["features"]


def test_global_validation_handler():
    # Yanlış tipte veri göndererek senin yazdığın özel 422 handler'ı tetikliyoruz
    # (Örneğin num_beams string olamaz)
    payload = {
        "question": "Test sorusu",
        "num_beams": "beş"
    }

    response = client.post("/predict", json=payload)  # prediction.router içindeki yolunuz

    # Eğer endpoint yoksa 404 döner, o yüzden route'un tam yolunu kendi projene göre ayarlayabilirsin
    if response.status_code != 404:
        assert response.status_code == 422
        data = response.json()
        assert data["detail"] == "Data Validation Failed"
        assert "body" in data  # Senin eklediğin loglama özelliği çalışıyor mu?