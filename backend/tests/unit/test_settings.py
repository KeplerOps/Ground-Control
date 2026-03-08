"""Verify Django settings load correctly."""

from django.conf import settings


def test_debug_is_false() -> None:
    assert settings.DEBUG is False


def test_default_auto_field() -> None:
    assert settings.DEFAULT_AUTO_FIELD == "django.db.models.BigAutoField"


def test_database_engine_is_sqlite_in_tests() -> None:
    assert settings.DATABASES["default"]["ENGINE"] == "django.db.backends.sqlite3"


def test_installed_apps_contains_core() -> None:
    assert "django.contrib.auth" in settings.INSTALLED_APPS
    assert "django.contrib.contenttypes" in settings.INSTALLED_APPS
    assert "ninja" in settings.INSTALLED_APPS
    assert "auditlog" in settings.INSTALLED_APPS


def test_no_tenant_middleware() -> None:
    for mw in settings.MIDDLEWARE:
        assert "tenant" not in mw.lower()


def test_root_urlconf() -> None:
    assert settings.ROOT_URLCONF == "ground_control.urls"
