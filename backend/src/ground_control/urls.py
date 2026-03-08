"""Root URL configuration for Ground Control."""

from django.contrib import admin
from django.urls import path

from ground_control.api import api

urlpatterns = [
    path("admin/", admin.site.urls),
    path("api/v1/", api.urls),
]
