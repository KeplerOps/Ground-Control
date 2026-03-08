"""Verify URL configuration loads."""

from ground_control.urls import api


def test_api_instance_exists() -> None:
    assert api.title == "Ground Control"
    assert api.version == "0.1.0"
