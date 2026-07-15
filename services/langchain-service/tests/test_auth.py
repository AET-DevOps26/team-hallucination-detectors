"""JWT enforcement on the GenAI endpoints.

/chat and /fix-prompt drive real LLM calls, so they must reject unauthenticated
callers; /health and /metrics stay open for probes and Prometheus scraping.
"""

import jwt
import pytest

from app.settings import settings


PROTECTED_ENDPOINTS = [
    ("/chat", {"message": "hi", "provider": "openai"}),
    ("/fix-prompt", {
        "title": "Missing HTTPS",
        "severity": "high",
        "check": "https",
        "affected": "http://example.org",
        "summary": "s",
        "impact": "i",
        "provider": "openai",
    }),
]


@pytest.mark.parametrize("path,body", PROTECTED_ENDPOINTS)
def test_missing_authorization_header_is_401(unauth_client, path, body):
    response = unauth_client.post(path, json=body)

    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"


@pytest.mark.parametrize("path,body", PROTECTED_ENDPOINTS)
def test_non_bearer_scheme_is_401(unauth_client, path, body):
    response = unauth_client.post(
        path, json=body, headers={"Authorization": "Basic abc123"}
    )

    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"


@pytest.mark.parametrize("path,body", PROTECTED_ENDPOINTS)
def test_token_signed_with_wrong_secret_is_401(unauth_client, path, body):
    forged = jwt.encode({"sub": "attacker"}, "a-different-secret", algorithm="HS256")

    response = unauth_client.post(
        path, json=body, headers={"Authorization": f"Bearer {forged}"}
    )

    assert response.status_code == 401
    assert response.json()["code"] == "INVALID_TOKEN"


@pytest.mark.parametrize("path,body", PROTECTED_ENDPOINTS)
def test_expired_token_is_401(unauth_client, path, body):
    expired = jwt.encode(
        {"sub": "user@example.com", "exp": 1},  # 1970 — long past
        settings.app_jwt_secret,
        algorithm="HS256",
    )

    response = unauth_client.post(
        path, json=body, headers={"Authorization": f"Bearer {expired}"}
    )

    assert response.status_code == 401
    assert response.json()["code"] == "INVALID_TOKEN"


def test_health_is_public(unauth_client):
    """Liveness/readiness probes must reach /health without a token."""
    assert unauth_client.get("/health").status_code == 200


def test_metrics_is_public(unauth_client):
    """Prometheus scrapes /metrics without a token."""
    assert unauth_client.get("/metrics").status_code == 200


def test_valid_token_reaches_the_handler(client, monkeypatch):
    """With a valid token, /chat runs the normal flow (proving auth doesn't block it)."""
    class FakeResult:
        content = "authenticated response"

    class FakeChain:
        async def ainvoke(self, inputs):
            return FakeResult()

    monkeypatch.setattr("app.main._chat_chain", lambda provider: FakeChain())

    response = client.post("/chat", json={"message": "hi", "provider": "openai"})

    assert response.status_code == 200
    assert response.json() == {"response": "authenticated response"}