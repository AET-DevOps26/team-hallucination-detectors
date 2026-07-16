import logging
from functools import lru_cache
from typing import Any, Optional

from fastapi import Depends, FastAPI, Request
from fastapi.encoders import jsonable_encoder
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field, field_validator
from starlette.exceptions import HTTPException as StarletteHTTPException

from prometheus_client import Counter, Histogram
from prometheus_fastapi_instrumentator import Instrumentator

from app.chains import (
    Provider,
    ProviderNotConfigured,
    build_chat_chain,
    build_fix_prompt_chain,
    render_builder_guidance,
)
from app.guardrails import (
    MAX_LONG_FIELD_LENGTH,
    MAX_SHORT_FIELD_LENGTH,
    MAX_TITLE_LENGTH,
    FixPromptGenerationFailed,
    normalize_builder,
    normalize_severity,
    validate_fix_prompt,
    validate_title,
)
from app.auth import Unauthorized, require_auth, unauthorized_response
from app.settings import settings


logger = logging.getLogger(__name__)

app = FastAPI(title=settings.app_name)

Instrumentator().instrument(app).expose(app, endpoint="/metrics")

# Business metrics: which LLM provider is actually driving the core "generate a fix
# prompt" workflow, how often it succeeds/fails, and how long the provider call takes.
# The generic HTTP metrics from Instrumentator can't tell provider selection or
# provider-specific latency apart from that.
FIX_PROMPT_REQUESTS = Counter(
    "vibeshield_fix_prompt_requests_total",
    "Fix-prompt generation requests by provider and outcome",
    ["provider", "status"],
)
FIX_PROMPT_LATENCY = Histogram(
    "vibeshield_fix_prompt_latency_seconds",
    "Fix-prompt generation latency by provider",
    ["provider"],
)
CHAT_REQUESTS = Counter(
    "vibeshield_chat_requests_total",
    "Chat requests by provider and outcome",
    ["provider", "status"],
)

# Chains are cheap but hold a provider-specific model client, so build one per
# provider on first use and reuse it. A build raises ProviderNotConfigured when
# the provider's key is missing; lru_cache never caches that, so it retries once
# the key is configured.
@lru_cache(maxsize=None)
def _chat_chain(provider: Provider):
    return build_chat_chain(provider)


@lru_cache(maxsize=None)
def _fix_prompt_chain(provider: Provider):
    return build_fix_prompt_chain(provider)


class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1)
    provider: Provider = "openai"


class ChatResponse(BaseModel):
    response: str


class FixPromptRequest(BaseModel):
    """One security finding, in the plain-language shape the dashboard already holds."""

    title: str = Field(..., min_length=1, max_length=MAX_TITLE_LENGTH)
    severity: str = Field(..., min_length=1, max_length=40)
    check: str = Field("", max_length=MAX_SHORT_FIELD_LENGTH)
    affected: str = Field("", max_length=MAX_LONG_FIELD_LENGTH)
    summary: str = Field("", max_length=MAX_LONG_FIELD_LENGTH)
    impact: str = Field("", max_length=MAX_LONG_FIELD_LENGTH)
    builder: str = "Generic"
    provider: Provider = "openai"

    @field_validator("title")
    @classmethod
    def _validate_title(cls, value: str) -> str:
        return validate_title(value)

    @field_validator("severity")
    @classmethod
    def _validate_severity(cls, value: str) -> str:
        return normalize_severity(value)

    @field_validator("builder")
    @classmethod
    def _normalize_builder(cls, value: str) -> str:
        return normalize_builder(value)


class FixPromptResponse(BaseModel):
    """A single prompt the user pastes straight into their AI builder."""

    prompt: str


class ErrorResponse(BaseModel):
    """Unified error body shared across all VibeShield services: {code, message, details}.

    ``code`` is machine-readable, ``message`` is human-readable, and ``details`` carries
    optional structured context (e.g. field-level validation errors) or ``null``.
    """

    code: str
    message: str
    details: Optional[Any] = None


def _error(status_code: int, code: str, message: str, details: Any = None) -> JSONResponse:
    body = ErrorResponse(code=code, message=message, details=details)
    return JSONResponse(status_code=status_code, content=jsonable_encoder(body))


def _sanitize_validation_error(error: dict[str, Any]) -> dict[str, Any]:
    """Custom field_validator failures put the raw ValueError under ctx.error,
    which jsonable_encoder can't serialize; stringify it so the handler below
    never itself 500s on a validation failure."""
    ctx = error.get("ctx")
    if isinstance(ctx, dict) and "error" in ctx:
        error = {**error, "ctx": {**ctx, "error": str(ctx["error"])}}
    return error


@app.exception_handler(RequestValidationError)
async def handle_validation_error(_: Request, exc: RequestValidationError) -> JSONResponse:
    """Maps FastAPI's default ``{ detail: [...] }`` validation body to the unified schema."""
    errors = [_sanitize_validation_error(error) for error in exc.errors()]
    return _error(422, "VALIDATION_ERROR", "Request validation failed.", errors)


@app.exception_handler(Unauthorized)
async def handle_unauthorized(_: Request, exc: Unauthorized) -> JSONResponse:
    """Renders a failed JWT check in the unified schema with its specific code."""
    return unauthorized_response(exc)


@app.exception_handler(StarletteHTTPException)
async def handle_http_exception(_: Request, exc: StarletteHTTPException) -> JSONResponse:
    """Wraps explicit HTTP errors (e.g. 404) in the unified schema."""
    return _error(exc.status_code, "HTTP_ERROR", str(exc.detail))


@app.exception_handler(ProviderNotConfigured)
async def handle_provider_not_configured(_: Request, exc: ProviderNotConfigured) -> JSONResponse:
    """The caller picked a provider (e.g. Logos) whose API key isn't set on this deployment."""
    return _error(
        503,
        "PROVIDER_NOT_CONFIGURED",
        f"The '{exc.provider}' AI provider is not configured on the server.",
        {"provider": exc.provider},
    )


@app.exception_handler(FixPromptGenerationFailed)
async def handle_fix_prompt_generation_failed(
    _: Request, exc: FixPromptGenerationFailed
) -> JSONResponse:
    """The LLM's output still failed our guardrails after one regeneration attempt."""
    return _error(
        502,
        "FIX_PROMPT_GENERATION_FAILED",
        "Could not generate a safe fix prompt for this finding.",
        {"reasons": exc.reasons},
    )


@app.exception_handler(Exception)
async def handle_unexpected(_: Request, exc: Exception) -> JSONResponse:
    """Catch-all that hides internal details behind a generic 500, matching the Java services."""
    logger.exception("Unhandled exception")
    return _error(500, "INTERNAL_ERROR", "An unexpected error occurred.")


@app.get("/health")
def health():
    return {
        "status": "ok",
        "service": settings.app_name,
    }


@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest, _: None = Depends(require_auth)):
    try:
        result = await _chat_chain(request.provider).ainvoke({"message": request.message})
    except Exception:
        CHAT_REQUESTS.labels(provider=request.provider, status="error").inc()
        raise
    CHAT_REQUESTS.labels(provider=request.provider, status="success").inc()

    return ChatResponse(response=result.content)


@app.post("/fix-prompt", response_model=FixPromptResponse)
async def fix_prompt(request: FixPromptRequest, _: None = Depends(require_auth)):
    """Generate a ready-to-paste fix prompt for a single finding (core GenAI feature).

    The output is validated against app.guardrails before it's returned; a
    prompt that fails (missing verification step, dangerous instruction,
    markdown fences, ...) is regenerated once, and only surfaced as an error
    if it fails validation twice in a row.
    """
    inputs = {
        "title": request.title,
        "severity": request.severity,
        "check": request.check or "n/a",
        "affected": request.affected or "n/a",
        "summary": request.summary or "n/a",
        "impact": request.impact or "n/a",
        "builder": request.builder,
        "builder_guidance": render_builder_guidance(request.builder),
    }
    finding_context = " ".join(
        [request.title, request.check, request.summary, request.impact]
    ).lower()

    chain = _fix_prompt_chain(request.provider)

    # Metrics (#79) wrap the guardrailed generation (#84): the latency timer spans
    # the retry, and a guardrail failure after regeneration counts as an error.
    try:
        with FIX_PROMPT_LATENCY.labels(provider=request.provider).time():
            result = await chain.ainvoke(inputs)
            prompt = result.content.strip()
            problems = validate_fix_prompt(prompt, inputs["affected"], finding_context)

            if problems:
                logger.warning("Fix prompt failed validation (%s); regenerating once.", problems)
                result = await chain.ainvoke(inputs)
                prompt = result.content.strip()
                problems = validate_fix_prompt(prompt, inputs["affected"], finding_context)
                if problems:
                    raise FixPromptGenerationFailed(problems)
    except Exception:
        FIX_PROMPT_REQUESTS.labels(provider=request.provider, status="error").inc()
        raise
    FIX_PROMPT_REQUESTS.labels(provider=request.provider, status="success").inc()

    return FixPromptResponse(prompt=prompt)
