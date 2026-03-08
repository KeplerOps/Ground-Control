# syntax=docker/dockerfile:1

# ---- Builder stage ----
FROM python:3.12-slim AS builder

COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

WORKDIR /app

ENV UV_COMPILE_BYTECODE=1
ENV UV_LINK_MODE=copy

# Install dependencies first (layer caching)
COPY pyproject.toml uv.lock README.md ./
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --frozen --no-install-project --no-dev

# Copy source and install project
COPY src/ src/
COPY manage.py .
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --frozen --no-dev

# Collect static files with dummy env vars
RUN GC_DATABASE_URL=postgres://x:x@localhost/x \
    GC_SECRET_KEY=build-only \
    .venv/bin/python manage.py collectstatic --noinput

# ---- Runtime stage ----
FROM python:3.12-slim

RUN groupadd --gid 1000 gc && \
    useradd --uid 1000 --gid gc --create-home gc

WORKDIR /app

COPY --from=builder /app/.venv /app/.venv
COPY --from=builder /app/staticfiles /app/staticfiles
COPY --from=builder /app/src /app/src
COPY --from=builder /app/manage.py /app/manage.py

ENV PATH="/app/.venv/bin:$PATH"

USER gc
EXPOSE 8000

CMD ["gunicorn", "ground_control.wsgi:application", \
     "--bind", "0.0.0.0:8000", \
     "--workers", "4", \
     "--access-logfile", "-", \
     "--error-logfile", "-"]
