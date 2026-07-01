import logging
from functools import lru_cache
from typing import Any, Optional

from fastapi import FastAPI, Request
from fastapi.encoders import jsonable_encoder
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.chains import (
    Provider,
    ProviderNotConfigured,
    build_chat_chain,
    build_fix_prompt_chain,
)
from app.settings import settings


logger = logging.getLogger(__name__)

app = FastAPI(title=settings.app_name)


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

    title: str = Field(..., min_length=1)
    severity: str = Field(..., min_length=1)
    check: str = ""
    affected: str = ""
    summary: str = ""
    impact: str = ""
    builder: str = "Generic"
    provider: Provider = "openai"


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


@app.exception_handler(RequestValidationError)
async def handle_validation_error(_: Request, exc: RequestValidationError) -> JSONResponse:
    """Maps FastAPI's default ``{ detail: [...] }`` validation body to the unified schema."""
    return _error(422, "VALIDATION_ERROR", "Request validation failed.", exc.errors())


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
async def chat(request: ChatRequest):
    result = await _chat_chain(request.provider).ainvoke({"message": request.message})

    return ChatResponse(response=result.content)


@app.post("/fix-prompt", response_model=FixPromptResponse)
async def fix_prompt(request: FixPromptRequest):
    """Generate a ready-to-paste fix prompt for a single finding (core GenAI feature)."""
    result = await _fix_prompt_chain(request.provider).ainvoke(
        {
            "title": request.title,
            "severity": request.severity,
            "check": request.check or "n/a",
            "affected": request.affected or "n/a",
            "summary": request.summary or "n/a",
            "impact": request.impact or "n/a",
            "builder": request.builder or "Generic",
        }
    )

    return FixPromptResponse(prompt=result.content.strip())
