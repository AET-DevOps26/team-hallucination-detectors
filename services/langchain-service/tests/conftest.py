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
def client():
    # The app registers a catch-all exception handler that turns unexpected
    # errors into a 500 response, matching production behavior. TestClient
    # re-raises server exceptions by default (raise_server_exceptions=True),
    # which would bypass that handler in tests, so it's disabled here.
    return TestClient(app, raise_server_exceptions=False)
