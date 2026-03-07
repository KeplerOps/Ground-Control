---
title: "Scaffold Python backend project with pyproject.toml"
labels: [foundation, backend, devex]
phase: 0
priority: P0
---

## Description

Create the Python backend project structure with `pyproject.toml` as the single source of truth for dependencies, build config, and tool settings. Set up the virtual environment tooling, dependency management, and basic project metadata.

## References

- Architecture: Section 7 (Python 3.12+, FastAPI, SQLAlchemy 2.0, Pydantic)
- Issue #001 (Repository Structure)

## Acceptance Criteria

- [ ] `backend/pyproject.toml` created with:
  - Project metadata (name=`ground-control`, version, description, license=Apache-2.0)
  - Python `requires-python = ">=3.12"`
  - Core dependencies: `fastapi`, `uvicorn[standard]`, `pydantic>=2.0`, `pydantic-settings`, `sqlalchemy[asyncio]>=2.0`, `asyncpg`, `alembic`, `redis`, `boto3`, `meilisearch`, `python-jose[cryptography]`, `passlib[argon2]`, `structlog`, `httpx`, `icontract`
  - Dev dependencies: `pytest`, `pytest-asyncio`, `pytest-cov`, `hypothesis`, `crosshair-tool`, `mypy`, `ruff`, `pre-commit`, `factory-boy`, `respx`, `deal`
  - Ruff config section
  - Mypy config section
  - Pytest config section (asyncio_mode = "auto", testpaths = ["tests"])
- [ ] `backend/src/ground_control/__init__.py` with `__version__`
- [ ] `backend/src/ground_control/py.typed` marker file (PEP 561)
- [ ] `backend/tests/__init__.py` and `backend/tests/conftest.py`
- [ ] `Makefile` or `justfile` at repo root with common commands:
  - `make install` — create venv, install deps
  - `make lint` — run ruff check + mypy
  - `make format` — run ruff format
  - `make test` — run pytest
  - `make dev` — start development server
- [ ] Can run `pip install -e ".[dev]"` and import `ground_control`

## Technical Notes

- Use `uv` as the package installer for speed (document as recommended, `pip` as fallback)
- Pin major versions of critical dependencies; use compatible release (`~=`) for minors
- Separate `[project.optional-dependencies]` groups: `dev`, `test`, `docs`
