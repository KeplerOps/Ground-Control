"""Tests for the shared exception hierarchy and API exception handler."""

import json
from typing import Any

import pytest
from django.test import RequestFactory

from ground_control.api import handle_ground_control_error
from ground_control.exceptions import (
    AuthenticationError,
    AuthorizationError,
    ConflictError,
    DomainValidationError,
    GroundControlError,
    NotFoundError,
)


class TestExceptionHierarchy:
    """All domain exceptions inherit from GroundControlError."""

    @pytest.mark.parametrize(
        "exc_class",
        [
            NotFoundError,
            DomainValidationError,
            AuthenticationError,
            AuthorizationError,
            ConflictError,
        ],
    )
    def test_subclass_of_ground_control_error(self, exc_class: type[GroundControlError]) -> None:
        assert issubclass(exc_class, GroundControlError)

    @pytest.mark.parametrize(
        "exc_class",
        [
            GroundControlError,
            NotFoundError,
            DomainValidationError,
            AuthenticationError,
            AuthorizationError,
            ConflictError,
        ],
    )
    def test_subclass_of_exception(self, exc_class: type[Exception]) -> None:
        assert issubclass(exc_class, Exception)


class TestExceptionAttributes:
    """Default and custom attributes are set correctly."""

    def test_ground_control_error_defaults(self) -> None:
        exc = GroundControlError()
        assert exc.error_code == "ground_control_error"
        assert exc.message == "An unexpected error occurred"
        assert exc.detail is None
        assert str(exc) == "An unexpected error occurred"

    @pytest.mark.parametrize(
        ("exc_class", "expected_code"),
        [
            (NotFoundError, "not_found"),
            (DomainValidationError, "validation_error"),
            (AuthenticationError, "authentication_error"),
            (AuthorizationError, "authorization_error"),
            (ConflictError, "conflict"),
        ],
    )
    def test_default_error_codes(
        self, exc_class: type[GroundControlError], expected_code: str
    ) -> None:
        exc = exc_class()
        assert exc.error_code == expected_code

    def test_custom_error_code(self) -> None:
        exc = NotFoundError(error_code="user_not_found")
        assert exc.error_code == "user_not_found"

    def test_message_in_str(self) -> None:
        exc = NotFoundError("Widget not found")
        assert str(exc) == "Widget not found"
        assert exc.message == "Widget not found"

    def test_detail_preserved(self) -> None:
        detail: dict[str, Any] = {"field": "email", "reason": "invalid format"}
        exc = DomainValidationError("Bad input", detail=detail)
        assert exc.detail == detail


class TestExceptionImports:
    """All exception classes are importable from ground_control.exceptions."""

    def test_all_exports(self) -> None:
        from ground_control import exceptions

        assert hasattr(exceptions, "GroundControlError")
        assert hasattr(exceptions, "NotFoundError")
        assert hasattr(exceptions, "DomainValidationError")
        assert hasattr(exceptions, "AuthenticationError")
        assert hasattr(exceptions, "AuthorizationError")
        assert hasattr(exceptions, "ConflictError")


class TestExceptionHandler:
    """The django-ninja exception handler maps exceptions to correct HTTP responses."""

    @pytest.fixture()
    def rf(self) -> RequestFactory:
        return RequestFactory()

    @pytest.mark.parametrize(
        ("exc", "expected_status"),
        [
            (NotFoundError("gone"), 404),
            (DomainValidationError("bad"), 422),
            (AuthenticationError("who"), 401),
            (AuthorizationError("denied"), 403),
            (ConflictError("duplicate"), 409),
        ],
    )
    def test_status_code_mapping(
        self, rf: RequestFactory, exc: GroundControlError, expected_status: int
    ) -> None:
        request = rf.get("/")
        response = handle_ground_control_error(request, exc)
        assert response.status_code == expected_status

    def test_fallback_500_for_base_error(self, rf: RequestFactory) -> None:
        request = rf.get("/")
        response = handle_ground_control_error(request, GroundControlError("boom"))
        assert response.status_code == 500

    def test_subclass_inherits_parent_status(self, rf: RequestFactory) -> None:
        """A subclass of NotFoundError should still map to 404, not fall through to 500."""

        class UserNotFoundError(NotFoundError):
            pass

        request = rf.get("/")
        response = handle_ground_control_error(request, UserNotFoundError("no user"))
        assert response.status_code == 404

    def test_response_body_structure(self, rf: RequestFactory) -> None:
        request = rf.get("/")
        exc = NotFoundError("User 42 not found")
        response = handle_ground_control_error(request, exc)
        body = json.loads(response.content)
        assert body["error"]["code"] == "not_found"
        assert body["error"]["message"] == "User 42 not found"
        assert body["error"]["detail"] is None

    def test_response_body_with_detail(self, rf: RequestFactory) -> None:
        request = rf.get("/")
        detail = {"field": "name", "constraint": "max_length"}
        exc = DomainValidationError("Invalid input", detail=detail)
        response = handle_ground_control_error(request, exc)
        body = json.loads(response.content)
        assert body["error"]["detail"] == detail
