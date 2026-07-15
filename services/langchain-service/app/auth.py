"""JWT authentication for the GenAI endpoints.

The GenAI service is reachable through the gateway at ``/langchain/*`` and drives
real LLM calls, so its user-facing endpoints must not be open to the internet.
Following the same shared-secret scheme the Java services use (see docs/auth.md
and api-service's JwtService), this module verifies the HMAC-signed JWT the
auth-service issues, against the shared ``APP_JWT_SECRET``.

Validation-only: this service never issues tokens. The allowed algorithm set
covers the HMAC family (HS256/384/512) because jjwt on the issuing side selects
the algorithm from the secret's key length, which varies per deployment.
"""

import logging

import jwt
from fastapi import Header
from fastapi.encoders import jsonable_encoder
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.settings import settings


logger = logging.getLogger(__name__)

_ALLOWED_ALGORITHMS = ["HS256", "HS384", "HS512"]


class Unauthorized(StarletteHTTPException):
    """401 raised when the bearer token is missing, malformed, expired, or forged."""

    def __init__(self, code: str, message: str) -> None:
        super().__init__(status_code=401, detail=message)
        self.code = code
        self.message = message


def unauthorized_response(exc: Unauthorized) -> JSONResponse:
    """Renders an Unauthorized into the unified { code, message, details } schema."""
    body = {"code": exc.code, "message": exc.message, "details": None}
    return JSONResponse(status_code=401, content=jsonable_encoder(body))


async def require_auth(authorization: str = Header(default="")) -> None:
    """FastAPI dependency: require a valid ``Authorization: Bearer <jwt>`` header.

    Raises :class:`Unauthorized` (401) when the header is absent or does not carry
    a bearer token, and when the token fails signature or expiry verification.
    """
    scheme, _, token = authorization.partition(" ")
    if scheme.lower() != "bearer" or not token:
        raise Unauthorized("UNAUTHORIZED", "Missing or malformed Authorization header.")

    if not settings.app_jwt_secret:
        # Fail closed rather than silently accept every caller if the deployment
        # forgot to inject the secret — a misconfigured auth is worse than a 401.
        logger.error("APP_JWT_SECRET is not configured; rejecting all GenAI requests.")
        raise Unauthorized("UNAUTHORIZED", "Authentication is not available.")

    try:
        jwt.decode(token, settings.app_jwt_secret, algorithms=_ALLOWED_ALGORITHMS)
    except jwt.ExpiredSignatureError:
        raise Unauthorized("INVALID_TOKEN", "The authentication token has expired.")
    except jwt.InvalidTokenError:
        raise Unauthorized("INVALID_TOKEN", "The authentication token is invalid.")