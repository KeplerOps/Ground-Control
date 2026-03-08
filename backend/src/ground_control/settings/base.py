"""Base Django settings for Ground Control."""

from pathlib import Path

from pydantic_settings import BaseSettings


class GroundControlSettings(BaseSettings):  # type: ignore[misc]
    """Application settings loaded from environment variables."""

    secret_key: str = "insecure-change-me-in-production"  # noqa: S105
    debug: bool = False
    database_url: str = "postgres://localhost:5432/ground_control"
    redis_url: str = "redis://localhost:6379/0"
    aws_storage_bucket_name: str = ""

    model_config = {"env_prefix": "GC_"}


env = GroundControlSettings()

BASE_DIR = Path(__file__).resolve().parent.parent.parent.parent

SECRET_KEY = env.secret_key

DEBUG = env.debug

ALLOWED_HOSTS: list[str] = ["*"] if DEBUG else []

INSTALLED_APPS: list[str] = [
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    "auditlog",
    "ninja",
    "storages",
    "django_q",
]

MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
    "auditlog.middleware.AuditlogMiddleware",
]

ROOT_URLCONF = "ground_control.urls"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [],
        "APP_DIRS": True,
        "OPTIONS": {
            "context_processors": [
                "django.template.context_processors.debug",
                "django.template.context_processors.request",
                "django.contrib.auth.context_processors.auth",
                "django.contrib.messages.context_processors.messages",
            ],
        },
    },
]

WSGI_APPLICATION = "ground_control.wsgi.application"

DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "NAME": "ground_control",
        "HOST": "localhost",
        "PORT": "5432",
    },
}

AUTH_PASSWORD_VALIDATORS = [
    {"NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator"},
    {"NAME": "django.contrib.auth.password_validation.MinimumLengthValidator"},
    {"NAME": "django.contrib.auth.password_validation.CommonPasswordValidator"},
    {"NAME": "django.contrib.auth.password_validation.NumericPasswordValidator"},
]

LANGUAGE_CODE = "en-us"
TIME_ZONE = "UTC"
USE_I18N = True
USE_TZ = True

STATIC_URL = "static/"

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

# django-q2
Q_CLUSTER = {
    "name": "ground-control",
    "workers": 4,
    "recycle": 500,
    "timeout": 60,
    "redis": env.redis_url,
}

# structlog
LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
        },
    },
    "root": {
        "handlers": ["console"],
        "level": "INFO",
    },
}
