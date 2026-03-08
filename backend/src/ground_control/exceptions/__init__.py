"""Shared exception hierarchy for Ground Control.

Domain-pure exceptions with no HTTP concepts. The API layer maps these
to appropriate HTTP status codes via exception handlers.
"""

from typing import Any

__all__ = [
    "AuthenticationError",
    "AuthorizationError",
    "ConflictError",
    "DomainValidationError",
    "GroundControlError",
    "NotFoundError",
]


class GroundControlError(Exception):
    """Base exception for all Ground Control domain errors.

    Args:
        message: Human-readable error description.
        error_code: Machine-readable error identifier.
        detail: Optional structured data about the error.
    """

    def __init__(  # noqa: D107
        self,
        message: str = "An unexpected error occurred",
        *,
        error_code: str = "ground_control_error",
        detail: dict[str, Any] | None = None,
    ) -> None:
        super().__init__(message)
        self.message = message
        self.error_code = error_code
        self.detail = detail


class NotFoundError(GroundControlError):
    """Raised when a requested resource does not exist."""

    def __init__(  # noqa: D107
        self,
        message: str = "Resource not found",
        *,
        error_code: str = "not_found",
        detail: dict[str, Any] | None = None,
    ) -> None:
        super().__init__(message, error_code=error_code, detail=detail)


class DomainValidationError(GroundControlError):
    """Raised when input data fails domain validation rules.

    Named to avoid collision with ``django.core.exceptions.ValidationError``.
    """

    def __init__(  # noqa: D107
        self,
        message: str = "Validation error",
        *,
        error_code: str = "validation_error",
        detail: dict[str, Any] | None = None,
    ) -> None:
        super().__init__(message, error_code=error_code, detail=detail)


class AuthenticationError(GroundControlError):
    """Raised when the caller's identity cannot be verified."""

    def __init__(  # noqa: D107
        self,
        message: str = "Authentication required",
        *,
        error_code: str = "authentication_error",
        detail: dict[str, Any] | None = None,
    ) -> None:
        super().__init__(message, error_code=error_code, detail=detail)


class AuthorizationError(GroundControlError):
    """Raised when the caller lacks permission for the requested action."""

    def __init__(  # noqa: D107
        self,
        message: str = "Authorization denied",
        *,
        error_code: str = "authorization_error",
        detail: dict[str, Any] | None = None,
    ) -> None:
        super().__init__(message, error_code=error_code, detail=detail)


class ConflictError(GroundControlError):
    """Raised when an action conflicts with the current state of a resource."""

    def __init__(  # noqa: D107
        self,
        message: str = "Resource conflict",
        *,
        error_code: str = "conflict",
        detail: dict[str, Any] | None = None,
    ) -> None:
        super().__init__(message, error_code=error_code, detail=detail)
