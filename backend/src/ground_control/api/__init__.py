"""Ground Control API configuration and exception handlers."""

from typing import Any

from django.http import HttpRequest, HttpResponse
from ninja import NinjaAPI

from ground_control.exceptions import (
    AuthenticationError,
    AuthorizationError,
    ConflictError,
    DomainValidationError,
    GroundControlError,
    NotFoundError,
)

api = NinjaAPI(
    title="Ground Control",
    version="0.1.0",
    description="Open IT Risk Management Platform API",
    urls_namespace="api",
)

_STATUS_MAP: dict[type[GroundControlError], int] = {
    NotFoundError: 404,
    DomainValidationError: 422,
    AuthenticationError: 401,
    AuthorizationError: 403,
    ConflictError: 409,
}


@api.exception_handler(GroundControlError)
def handle_ground_control_error(request: HttpRequest, exc: GroundControlError) -> HttpResponse:
    """Map domain exceptions to HTTP responses with consistent JSON bodies."""
    status = 500
    for cls in type(exc).__mro__:
        if cls in _STATUS_MAP:
            status = _STATUS_MAP[cls]
            break
    body: dict[str, Any] = {
        "error": {
            "code": exc.error_code,
            "message": exc.message,
            "detail": exc.detail,
        },
    }
    return api.create_response(request, body, status=status)
