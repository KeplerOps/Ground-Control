"""Error response schemas for structured API error responses."""

from typing import Any

from ground_control.schemas.base import BaseSchema


class ErrorDetail(BaseSchema):
    """Inner error payload with machine-readable code and human-readable message."""

    code: str
    message: str
    detail: dict[str, Any] | None = None


class ErrorEnvelope(BaseSchema):
    """Top-level error response: ``{"error": {"code": ..., "message": ..., "detail": ...}}``."""

    error: ErrorDetail
