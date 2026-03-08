"""Tests for structured logging configuration."""

import json
import logging
from io import StringIO

import structlog

from ground_control.logging import configure


def _capture_buffer() -> StringIO:
    """Replace the root logger's stream handler with a StringIO buffer."""
    root = logging.getLogger()
    buf = StringIO()
    handler = root.handlers[0]
    assert isinstance(handler, logging.StreamHandler)
    handler.stream = buf
    return buf


class TestStructlogConfiguration:
    """Verify structlog is configured correctly."""

    def test_structlog_configured_with_bound_logger(self) -> None:
        configure(debug=False)
        config = structlog.get_config()
        assert config["wrapper_class"] is structlog.stdlib.BoundLogger

    def test_json_output_in_production_mode(self) -> None:
        configure(debug=False)
        buf = _capture_buffer()

        logger = structlog.get_logger("test.json")
        logger.info("test_event", key="value")

        line = buf.getvalue().strip().split("\n")[-1]
        parsed = json.loads(line)
        assert parsed["event"] == "test_event"
        assert parsed["key"] == "value"
        assert "timestamp" in parsed

    def test_console_output_in_debug_mode(self) -> None:
        configure(debug=True)
        buf = _capture_buffer()

        logger = structlog.get_logger("test.console")
        logger.info("hello_dev")

        output = buf.getvalue()
        assert "hello_dev" in output
        # Console output should NOT be valid JSON
        try:
            json.loads(output.strip().split("\n")[-1])
            raise AssertionError("Expected non-JSON output in debug mode")
        except json.JSONDecodeError:
            pass

    def test_service_identity_in_log_output(self) -> None:
        configure(debug=False, service_name="test-svc", service_version="1.2.3")
        buf = _capture_buffer()

        logger = structlog.get_logger("test.identity")
        logger.info("identity_check")

        line = buf.getvalue().strip().split("\n")[-1]
        parsed = json.loads(line)
        assert parsed["service.name"] == "test-svc"
        assert parsed["service.version"] == "1.2.3"

    def test_stdlib_logging_routed_through_structlog(self) -> None:
        configure(debug=False)
        root = logging.getLogger()
        assert len(root.handlers) >= 1
        formatter = root.handlers[0].formatter
        assert isinstance(formatter, structlog.stdlib.ProcessorFormatter)


class TestDjangoStructlogIntegration:
    """Verify django-structlog is wired into Django settings."""

    def test_django_structlog_middleware_in_middleware_stack(self) -> None:
        from django.conf import settings

        assert "django_structlog.middlewares.RequestMiddleware" in settings.MIDDLEWARE

    def test_django_structlog_in_installed_apps(self) -> None:
        from django.conf import settings

        assert "django_structlog" in settings.INSTALLED_APPS
