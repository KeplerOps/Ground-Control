"""Pagination schemas and custom paginator for Ground Control API."""

import math
from typing import Any

from django.db.models import QuerySet
from django.http import HttpRequest
from ninja import Field, Query, Schema
from ninja.pagination import PaginationBase

from ground_control.schemas.base import BaseSchema


class PageMeta(BaseSchema):
    """Metadata for paginated responses."""

    total_count: int
    page: int
    per_page: int
    total_pages: int


class PaginatedResponse(BaseSchema):
    """Output shape for paginated list endpoints."""

    items: list[Any]  # Any: django-ninja replaces this with the concrete item type
    meta: PageMeta


class GroundControlPagination(PaginationBase):
    """Custom paginator producing ``{"items": [...], "meta": {...}}``."""

    class Input(Schema):
        page: int = Field(1, ge=1)
        per_page: int = Field(20, ge=1, le=100)

    class Output(Schema):
        items: list[Any]  # Any: replaced by django-ninja with the concrete item type
        meta: PageMeta

    InputSource = Query(...)  # type: ignore[type-arg]
    items_attribute: str = "items"

    def paginate_queryset(  # noqa: D102
        self,
        queryset: QuerySet,  # type: ignore[type-arg]
        pagination: Input,
        request: HttpRequest,
        **params: Any,  # noqa: ANN401
    ) -> dict[str, Any]:
        total_count = self._items_count(queryset)
        per_page = pagination.per_page
        page = pagination.page
        total_pages = max(1, math.ceil(total_count / per_page))

        offset = (page - 1) * per_page
        items = list(queryset[offset : offset + per_page])

        return {
            "items": items,
            "meta": {
                "total_count": total_count,
                "page": page,
                "per_page": per_page,
                "total_pages": total_pages,
            },
        }
