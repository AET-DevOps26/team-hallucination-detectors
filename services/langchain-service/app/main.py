import logging
from typing import Any, Literal, Optional

from fastapi import FastAPI, Request
from fastapi.encoders import jsonable_encoder
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.chains import build_chat_chain, build_fix_prompt_chain
from app.remediation import (
    SUPPORTED_BUILDERS,
    SUPPORTED_MODES,
    builder_guidance,
    guidance_for,
    mode_guidance,
    rescan_guidance,
)
from app.settings import settings


logger = logging.getLogger(__name__)

app = FastAPI(title=settings.app_name)

chat_chain = build_chat_chain()
fix_prompt_chain = build_fix_prompt_chain()


class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1)


class ChatResponse(BaseModel):
    response: str


class FixPromptRequest(BaseModel):
    """One security finding, in the plain-language shape the dashboard already holds."""

    builder: Literal["Generic", "Cursor", "Lovable", "v0", "Bolt", "Replit"] = "Generic"
    mode: Literal[
        "Quick fix",
        "Detailed implementation",
        "Explain and fix",
        "Verification only",
    ] = "Quick fix"
    title: str = Field(..., min_length=1)
    severity: str = Field(..., min_length=1)
    check: str = ""
    affected: str = ""
    summary: str = ""
    impact: str = ""
    changeStatus: Optional[str] = None


class FixPromptResponse(BaseModel):
    """A fix package the user can copy into their AI builder."""

    prompt: str
    verificationPrompt: str
    expectedResult: str
    riskNote: str
    rollbackNote: str
    likelyTargets: str
    issueType: str
    builder: str
    mode: str


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


@app.post("/fix-prompt", response_model=FixPromptResponse)
async def fix_prompt(request: FixPromptRequest):
    """Generate a ready-to-paste fix prompt for a single finding (core GenAI feature)."""
    guidance = guidance_for(request.title, request.check, request.summary, request.impact)
    builder_note = builder_guidance(request.builder)
    mode_note = mode_guidance(request.mode)
    scan_context = rescan_guidance(request.changeStatus)

    result = await fix_prompt_chain.ainvoke(
        {
            "builder": request.builder,
            "mode": request.mode,
            "title": request.title,
            "severity": request.severity,
            "check": request.check or "n/a",
            "affected": request.affected or "n/a",
            "summary": request.summary or "n/a",
            "impact": request.impact or "n/a",
            "issue_type": guidance.issue_type,
            "secure_behavior": guidance.secure_behavior,
            "likely_targets": guidance.likely_targets,
            "implementation_notes": guidance.implementation_notes,
            "verification_steps": guidance.verification_steps,
            "rollback_note": guidance.rollback_note,
            "risk_note": guidance.risk_note,
            "builder_guidance": builder_note,
            "mode_guidance": mode_note,
            "rescan_guidance": scan_context,
        }
    )

    verification_prompt = (
        f"Verify this VibeShield finding for {request.affected or 'the affected target'}.\n\n"
        f"Issue type: {guidance.issue_type}\n"
        f"Required secure behavior: {guidance.secure_behavior}\n"
        f"Verification steps: {guidance.verification_steps}\n"
        "Do not make unrelated changes. Report whether the issue is fixed and what evidence confirms it."
    )

    return FixPromptResponse(
        prompt=result.content.strip(),
        verificationPrompt=verification_prompt,
        expectedResult=guidance.secure_behavior,
        riskNote=guidance.risk_note,
        rollbackNote=guidance.rollback_note,
        likelyTargets=guidance.likely_targets,
        issueType=guidance.issue_type,
        builder=request.builder if request.builder in SUPPORTED_BUILDERS else "Generic",
        mode=request.mode if request.mode in SUPPORTED_MODES else "Quick fix",
    )
