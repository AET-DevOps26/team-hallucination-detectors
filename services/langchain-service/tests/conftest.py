import jwt
import pytest
from fastapi.testclient import TestClient

from app.main import _chat_chain, _fix_prompt_chain, app
from app.settings import settings


@pytest.fixture(autouse=True)
def _reset_provider_keys(monkeypatch):
    """Baseline: no provider configured, regardless of a local .env file.

    Individual tests opt in to a configured provider via monkeypatch so
    behavior never depends on what happens to be in the environment. The
    chain builders are cached per-provider across the process, so the cache
    is cleared too or a test could observe a chain built with a previous
    test's settings.
    """
    monkeypatch.setattr(settings, "openai_api_key", "")
    monkeypatch.setattr(settings, "logos_api_key", "")
    _chat_chain.cache_clear()
    _fix_prompt_chain.cache_clear()


@pytest.fixture
def auth_token():
    """A valid JWT signed with the same shared secret the service validates against."""
    return jwt.encode(
        {"sub": "user@example.com", "userId": 1},
        settings.app_jwt_secret,
        algorithm="HS256",
    )


@pytest.fixture
def client(auth_token):
    # The app registers a catch-all exception handler that turns unexpected
    # errors into a 500 response, matching production behavior. TestClient
    # re-raises server exceptions by default (raise_server_exceptions=True),
    # which would bypass that handler in tests, so it's disabled here.
    #
    # The GenAI endpoints require a valid bearer token, so the default client
    # carries one — endpoint tests exercise behavior, not auth. Auth itself is
    # covered explicitly in test_auth.py via unauth_client.
    test_client = TestClient(app, raise_server_exceptions=False)
    test_client.headers.update({"Authorization": f"Bearer {auth_token}"})
    return test_client


@pytest.fixture
def unauth_client():
    """A client with no Authorization header, for exercising the 401 paths."""
    return TestClient(app, raise_server_exceptions=False)
