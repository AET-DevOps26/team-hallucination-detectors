from app.chains import render_builder_guidance
from app.retrieval import NO_CONTEXT
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


def _fake_chain_returning(content: str, captured: dict | None = None):
    class FakeResult:
        pass

    class FakeChain:
        async def ainvoke(self, inputs):
            if captured is not None:
                captured.update(inputs)
            result = FakeResult()
            result.content = content
            return result

    return FakeChain()


# A prompt that passes every guardrail in app.guardrails: it references the
# affected target, ends with a verification step, has no markdown fences, and
# doesn't contain any of the dangerous phrases.
CLEAN_PROMPT = (
    "Add the Content-Security-Policy header to https://example.com/. "
    "Then verify the header appears in the response."
)


def test_fix_prompt_returns_generated_prompt(client, monkeypatch):
    captured = {}
    monkeypatch.setattr(
        "app.main._fix_prompt_chain",
        lambda provider: _fake_chain_returning(f"  {CLEAN_PROMPT}  ", captured),
    )

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
    assert response.json() == {"prompt": CLEAN_PROMPT}
    assert captured == {
        "title": "Missing security headers",
        "severity": "High",
        "check": "headers",
        "affected": "https://example.com/",
        "summary": "No CSP header found.",
        "impact": "Increases XSS blast radius.",
        "builder": "Cursor",
        "builder_guidance": render_builder_guidance("Cursor"),
        "retrieved_context": NO_CONTEXT,
    }


def test_fix_prompt_fills_defaults_for_optional_fields(client, monkeypatch):
    captured = {}
    monkeypatch.setattr(
        "app.main._fix_prompt_chain",
        lambda provider: _fake_chain_returning(CLEAN_PROMPT, captured),
    )

    response = client.post(
        "/fix-prompt",
        json={"title": "Exposed .git directory", "severity": "critical"},
    )

    assert response.status_code == 200
    assert captured == {
        "title": "Exposed .git directory",
        "severity": "Critical",
        "check": "n/a",
        "affected": "n/a",
        "summary": "n/a",
        "impact": "n/a",
        "builder": "Generic",
        "builder_guidance": render_builder_guidance("Generic"),
        "retrieved_context": NO_CONTEXT,
    }


def test_fix_prompt_requires_title_and_severity(client):
    response = client.post("/fix-prompt", json={"title": "", "severity": ""})

    assert response.status_code == 422
    assert response.json()["code"] == "VALIDATION_ERROR"


def test_fix_prompt_rejects_unknown_severity(client):
    response = client.post(
        "/fix-prompt", json={"title": "Missing headers", "severity": "super-urgent"}
    )

    assert response.status_code == 422
    body = response.json()
    assert body["code"] == "VALIDATION_ERROR"
    # A custom field_validator's ValueError ends up under ctx.error in FastAPI's
    # error dicts; it must be stringified; jsonable_encoder can't serialize a raw
    # exception instance, and a bug here would surface as a 500, not a 422.
    assert isinstance(body["details"][0]["ctx"]["error"], str)


def test_fix_prompt_normalizes_unknown_builder_to_generic(client, monkeypatch):
    captured = {}
    monkeypatch.setattr(
        "app.main._fix_prompt_chain",
        lambda provider: _fake_chain_returning(CLEAN_PROMPT, captured),
    )

    response = client.post(
        "/fix-prompt",
        json={
            "title": "Missing headers",
            "severity": "high",
            "builder": "SomeToolThatDoesNotExist",
        },
    )

    assert response.status_code == 200
    assert captured["builder"] == "Generic"


def test_fix_prompt_passes_retrieved_context_to_the_chain(client, monkeypatch):
    captured = {}
    monkeypatch.setattr(
        "app.main._fix_prompt_chain",
        lambda provider: _fake_chain_returning(CLEAN_PROMPT, captured),
    )

    async def fake_retrieve_context(check, title, summary):
        return "- [OWASP A05:2021] Some grounding snippet."

    monkeypatch.setattr("app.main.retrieve_context", fake_retrieve_context)

    response = client.post(
        "/fix-prompt", json={"title": "Missing headers", "severity": "high"}
    )

    assert response.status_code == 200
    assert captured["retrieved_context"] == "- [OWASP A05:2021] Some grounding snippet."


def test_fix_prompt_defaults_retrieved_context_when_rag_is_unconfigured(client, monkeypatch):
    captured = {}
    monkeypatch.setattr(
        "app.main._fix_prompt_chain",
        lambda provider: _fake_chain_returning(CLEAN_PROMPT, captured),
    )

    response = client.post(
        "/fix-prompt", json={"title": "Missing headers", "severity": "high"}
    )

    assert response.status_code == 200
    assert captured["retrieved_context"] == NO_CONTEXT


def test_fix_prompt_regenerates_once_when_output_fails_validation(client, monkeypatch):
    attempts = {"count": 0}

    class FlakyChain:
        async def ainvoke(self, inputs):
            attempts["count"] += 1
            result = type("Result", (), {})()
            result.content = "no verification step here" if attempts["count"] == 1 else CLEAN_PROMPT
            return result

    monkeypatch.setattr("app.main._fix_prompt_chain", lambda provider: FlakyChain())

    response = client.post(
        "/fix-prompt", json={"title": "Missing headers", "severity": "high"}
    )

    assert response.status_code == 200
    assert response.json() == {"prompt": CLEAN_PROMPT}
    assert attempts["count"] == 2


def test_fix_prompt_returns_502_when_output_fails_validation_twice(client, monkeypatch):
    monkeypatch.setattr(
        "app.main._fix_prompt_chain",
        lambda provider: _fake_chain_returning("no verification step here"),
    )

    response = client.post(
        "/fix-prompt", json={"title": "Missing headers", "severity": "high"}
    )

    assert response.status_code == 502
    body = response.json()
    assert body["code"] == "FIX_PROMPT_GENERATION_FAILED"
    assert "reasons" in body["details"]


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
