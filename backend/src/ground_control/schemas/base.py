"""Base schema for all Ground Control API schemas."""

from ninja import Schema


class BaseSchema(Schema):
    """Project-wide base schema.

    All project schemas should inherit from this class. Provides a single
    point for future shared configuration (custom serializers, alias
    generators, etc.). Inherits ``from_attributes=True`` from ninja.Schema.
    """

    pass
