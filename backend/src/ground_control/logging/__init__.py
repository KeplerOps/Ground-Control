"""Structured logging setup for Ground Control.

Configures structlog with Django integration via ``django-structlog``.
Call ``configure()`` at Django startup (from settings) to wire everything.

**Production** (``DEBUG=False``): JSON lines to stdout — ready for log
aggregators (ELK, Datadog, CloudWatch).

**Development** (``DEBUG=True``): Colored, human-readable console output.

Usage::

    import structlog

    logger = structlog.get_logger()

    # Simple event
    logger.info("server_started", port=8000)

    # With tenant/actor context (bind once, carry everywhere)
    logger = logger.bind(tenant_id=tenant.id, actor_id=user.id)
    logger.info("risk_created", risk_id=risk.id)

``django-structlog`` middleware automatically binds ``request_id``, ``ip``,
and ``user_id`` to every log entry within a request.
"""

from __future__ import annotations

import logging
import sys
from collections.abc import Mapping, MutableMapping
from typing import Any

import structlog


def _make_service_identity_processor(
    service_name: str,
    service_version: str,
) -> structlog.types.Processor:
    """Return a processor that stamps service identity onto every log entry."""

    def _add_service_identity(
        logger: Any,  # noqa: ANN401
        method_name: str,
        event_dict: MutableMapping[str, Any],
    ) -> Mapping[str, Any]:
        event_dict["service.name"] = service_name
        event_dict["service.version"] = service_version
        return event_dict

    return _add_service_identity


def configure(
    *,
    debug: bool,
    service_name: str = "ground-control",
    service_version: str | None = None,
) -> None:
    """Configure structlog and stdlib logging for the application.

    Args:
        debug: When True, use colored console renderer. When False, use JSON.
        service_name: Value for ``service.name`` in every log entry.
        service_version: Value for ``service.version``. Falls back to
            ``ground_control.__version__`` if not supplied.
    """
    if service_version is None:
        from ground_control import __version__

        service_version = __version__

    shared_processors: list[structlog.types.Processor] = [
        structlog.contextvars.merge_contextvars,
        structlog.stdlib.add_log_level,
        structlog.stdlib.ExtraAdder(),
        structlog.stdlib.PositionalArgumentsFormatter(),
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.UnicodeDecoder(),
        _make_service_identity_processor(service_name, service_version),
    ]

    structlog.configure(
        processors=[
            *shared_processors,
            structlog.stdlib.ProcessorFormatter.wrap_for_formatter,
        ],
        wrapper_class=structlog.stdlib.BoundLogger,
        context_class=dict,
        logger_factory=structlog.stdlib.LoggerFactory(),
        cache_logger_on_first_use=True,
    )

    if debug:
        renderer: structlog.types.Processor = structlog.dev.ConsoleRenderer()
    else:
        renderer = structlog.processors.JSONRenderer()

    formatter = structlog.stdlib.ProcessorFormatter(
        processors=[
            structlog.stdlib.ProcessorFormatter.remove_processors_meta,
            renderer,
        ],
        foreign_pre_chain=shared_processors,
    )

    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(formatter)

    root = logging.getLogger()
    root.handlers.clear()
    root.addHandler(handler)
    root.setLevel(logging.INFO)

    # Quiet noisy Django loggers
    logging.getLogger("django.utils.autoreload").setLevel(logging.WARNING)
