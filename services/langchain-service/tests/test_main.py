from app.settings import settings


def test_health_returns_ok(client):
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok", "service": settings.app_name}


def test_chat_returns_model_response(client, monkeypatch):
    class FakeResult:
        content = "hello there"

    class FakeChain:
        async def ainvoke(self, inputs):
            assert inputs == {"message": "hi"}
            return FakeResult()

    monkeypatch.setattr("app.main._chat_chain", lambda provider: FakeChain())

    response = client.post("/chat", json={"message": "hi", "provider": "openai"})

    assert response.status_code == 200
    assert response.json() == {"response": "hello there"}


def test_chat_defaults_provider_to_openai(client, monkeypatch):
    seen = {}

    class FakeResult:
        content = "ok"

    class FakeChain:
        async def ainvoke(self, inputs):
            return FakeResult()

    def fake_chat_chain(provider):
        seen["provider"] = provider
        return FakeChain()

    monkeypatch.setattr("app.main._chat_chain", fake_chat_chain)

    response = client.post("/chat", json={"message": "hi"})

    assert response.status_code == 200
    assert seen["provider"] == "openai"


def test_chat_rejects_empty_message(client):
    response = client.post("/chat", json={"message": ""})

    assert response.status_code == 422
    body = response.json()
    assert body["code"] == "VALIDATION_ERROR"
    assert body["message"] == "Request validation failed."
    assert isinstance(body["details"], list)


def test_chat_rejects_unknown_provider(client):
    response = client.post("/chat", json={"message": "hi", "provider": "bogus"})

    assert response.status_code == 422
    assert response.json()["code"] == "VALIDATION_ERROR"


def test_chat_returns_503_when_provider_not_configured(client):
    """No monkeypatch of the chain here: openai/logos keys are cleared by the
    autouse fixture, so building the real chain raises ProviderNotConfigured
    before any network call is attempted."""
    response = client.post("/chat", json={"message": "hi", "provider": "logos"})

    assert response.status_code == 503
    body = response.json()
    assert body["code"] == "PROVIDER_NOT_CONFIGURED"
    assert body["details"] == {"provider": "logos"}


def test_chat_returns_500_on_unexpected_error(client, monkeypatch):
    class FakeChain:
        async def ainvoke(self, inputs):
            raise RuntimeError("boom")

    monkeypatch.setattr("app.main._chat_chain", lambda provider: FakeChain())

    response = client.post("/chat", json={"message": "hi"})

    assert response.status_code == 500
    body = response.json()
    assert body["code"] == "INTERNAL_ERROR"
    assert body["details"] is None


def test_fix_prompt_returns_generated_prompt(client, monkeypatch):
    captured = {}

    class FakeResult:
        content = "  Paste this into your builder.  "

    class FakeChain:
        async def ainvoke(self, inputs):
            captured.update(inputs)
            return FakeResult()

    monkeypatch.setattr("app.main._fix_prompt_chain", lambda provider: FakeChain())

    response = client.post(
        "/fix-prompt",
        json={
            "title": "Missing security headers",
            "severity": "high",
            "check": "headers",
            "affected": "https://example.com/",
            "summary": "No CSP header found.",
            "impact": "Increases XSS blast radius.",
            "builder": "Cursor",
        },
    )

    assert response.status_code == 200
    assert response.json() == {"prompt": "Paste this into your builder."}
    assert captured == {
        "title": "Missing security headers",
        "severity": "high",
        "check": "headers",
        "affected": "https://example.com/",
        "summary": "No CSP header found.",
        "impact": "Increases XSS blast radius.",
        "builder": "Cursor",
    }


def test_fix_prompt_fills_defaults_for_optional_fields(client, monkeypatch):
    captured = {}

    class FakeResult:
        content = "prompt"

    class FakeChain:
        async def ainvoke(self, inputs):
            captured.update(inputs)
            return FakeResult()

    monkeypatch.setattr("app.main._fix_prompt_chain", lambda provider: FakeChain())

    response = client.post(
        "/fix-prompt",
        json={"title": "Exposed .git directory", "severity": "critical"},
    )

    assert response.status_code == 200
    assert captured == {
        "title": "Exposed .git directory",
        "severity": "critical",
        "check": "n/a",
        "affected": "n/a",
        "summary": "n/a",
        "impact": "n/a",
        "builder": "Generic",
    }


def test_fix_prompt_requires_title_and_severity(client):
    response = client.post("/fix-prompt", json={"title": "", "severity": ""})

    assert response.status_code == 422
    assert response.json()["code"] == "VALIDATION_ERROR"


def test_fix_prompt_returns_503_when_provider_not_configured(client):
    response = client.post(
        "/fix-prompt",
        json={"title": "Finding", "severity": "low", "provider": "logos"},
    )

    assert response.status_code == 503
    assert response.json()["code"] == "PROVIDER_NOT_CONFIGURED"


def test_unknown_route_returns_unified_404(client):
    response = client.get("/does-not-exist")

    assert response.status_code == 404
    body = response.json()
    assert body["code"] == "HTTP_ERROR"
    assert "message" in body
