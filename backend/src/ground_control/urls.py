"""Root URL configuration for Ground Control."""

from django.contrib import admin
from django.urls import path
from ninja import NinjaAPI

api = NinjaAPI(
    title="Ground Control",
    version="0.1.0",
    description="Open IT Risk Management Platform API",
    urls_namespace="api",
)

urlpatterns = [
    path("admin/", admin.site.urls),
    path("api/v1/", api.urls),
]
