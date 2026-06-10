import logging
from typing import Any, Optional

from fastapi import FastAPI, Request
from fastapi.encoders import jsonable_encoder
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.chains import build_chat_chain
from app.settings import settings


logger = logging.getLogger(__name__)

app = FastAPI(title=settings.app_name)

chat_chain = build_chat_chain()


class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1)


class ChatResponse(BaseModel):
    response: str


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
    result = await chat_chain.ainvoke({"message": request.message})

    return ChatResponse(response=result.content)
