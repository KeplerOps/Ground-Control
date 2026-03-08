"""Tests for base schemas, error schemas, and pagination schemas."""

from typing import Any
from unittest.mock import MagicMock

from django.http import HttpRequest

from ground_control.schemas.base import BaseSchema
from ground_control.schemas.error import ErrorDetail, ErrorEnvelope
from ground_control.schemas.pagination import (
    GroundControlPagination,
    PageMeta,
    PaginatedResponse,
)


class TestBaseSchema:
    """BaseSchema provides a shared foundation for all project schemas."""

    def test_subclassing_works(self) -> None:
        class MySchema(BaseSchema):
            name: str

        instance = MySchema(name="test")
        assert instance.name == "test"

    def test_from_attributes_enabled(self) -> None:
        assert BaseSchema.model_config.get("from_attributes") is True


class TestErrorSchemas:
    """ErrorDetail and ErrorEnvelope serialize the nested error format."""

    def test_error_detail_serializes(self) -> None:
        detail = ErrorDetail(code="not_found", message="User 42 not found")
        data = detail.model_dump()
        assert data == {"code": "not_found", "message": "User 42 not found", "detail": None}

    def test_error_detail_with_detail(self) -> None:
        extra: dict[str, Any] = {"field": "email"}
        detail = ErrorDetail(code="validation_error", message="Bad input", detail=extra)
        data = detail.model_dump()
        assert data["detail"] == {"field": "email"}

    def test_error_detail_defaults_to_none(self) -> None:
        detail = ErrorDetail(code="x", message="y")
        assert detail.detail is None

    def test_error_envelope_serializes(self) -> None:
        envelope = ErrorEnvelope(
            error=ErrorDetail(code="conflict", message="Already exists"),
        )
        data = envelope.model_dump()
        assert data == {
            "error": {
                "code": "conflict",
                "message": "Already exists",
                "detail": None,
            },
        }


class TestPaginationSchemas:
    """PageMeta and PaginatedResponse have the expected shape."""

    def test_page_meta_fields(self) -> None:
        meta = PageMeta(total_count=100, page=2, per_page=20, total_pages=5)
        data = meta.model_dump()
        assert data == {"total_count": 100, "page": 2, "per_page": 20, "total_pages": 5}

    def test_paginated_response_shape(self) -> None:
        resp = PaginatedResponse(
            items=[{"id": 1}],
            meta=PageMeta(total_count=1, page=1, per_page=20, total_pages=1),
        )
        data = resp.model_dump()
        assert "items" in data
        assert "meta" in data
        assert data["items"] == [{"id": 1}]
        assert data["meta"]["total_count"] == 1


class TestGroundControlPagination:
    """GroundControlPagination produces the correct output shape."""

    def _make_queryset(self, items: list[Any]) -> MagicMock:
        """Create a mock queryset that supports slicing and count."""
        qs = MagicMock()
        qs.all.return_value.count.return_value = len(items)
        qs.__getitem__ = lambda self, s: items[s]
        return qs

    def test_paginate_queryset_first_page(self) -> None:
        paginator = GroundControlPagination()
        items = list(range(50))
        qs = self._make_queryset(items)
        pagination_input = GroundControlPagination.Input(page=1, per_page=20)
        request = MagicMock(spec=HttpRequest)

        result = paginator.paginate_queryset(qs, pagination_input, request)

        assert result["items"] == list(range(20))
        assert result["meta"]["total_count"] == 50
        assert result["meta"]["page"] == 1
        assert result["meta"]["per_page"] == 20
        assert result["meta"]["total_pages"] == 3

    def test_paginate_queryset_last_page(self) -> None:
        paginator = GroundControlPagination()
        items = list(range(50))
        qs = self._make_queryset(items)
        pagination_input = GroundControlPagination.Input(page=3, per_page=20)
        request = MagicMock(spec=HttpRequest)

        result = paginator.paginate_queryset(qs, pagination_input, request)

        assert result["items"] == list(range(40, 50))
        assert result["meta"]["total_pages"] == 3

    def test_paginate_empty_queryset(self) -> None:
        paginator = GroundControlPagination()
        qs = self._make_queryset([])
        pagination_input = GroundControlPagination.Input(page=1, per_page=20)
        request = MagicMock(spec=HttpRequest)

        result = paginator.paginate_queryset(qs, pagination_input, request)

        assert result["items"] == []
        assert result["meta"]["total_count"] == 0
        assert result["meta"]["total_pages"] == 1
