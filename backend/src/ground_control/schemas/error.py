"""Error response schema for structured API error responses."""

from typing import Any

from ninja import Schema


class ErrorResponse(Schema):
    """Standard error response body returned by exception handlers."""

    error_code: str
    message: str
    detail: dict[str, Any] | None = None
