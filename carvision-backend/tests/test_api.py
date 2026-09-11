from fastapi.testclient import TestClient

from app.main import app


def test_health_and_security_headers():
    response = TestClient(app).get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}
    assert response.headers["x-content-type-options"] == "nosniff"


def test_protected_endpoint_requires_token():
    response = TestClient(app).get("/api/v1/users/me")
    assert response.status_code == 401
    assert response.json()["error"]["code"] == "authentication_required"
