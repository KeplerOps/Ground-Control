.PHONY: install lint format test dev clean

# Use uv if available, fall back to pip
UV := $(shell command -v uv 2>/dev/null)
ifdef UV
    PIP_INSTALL = uv pip install
    VENV_CREATE = uv venv
else
    PIP_INSTALL = pip install
    VENV_CREATE = python3 -m venv
endif

install: ## Create venv and install dependencies
	cd backend && $(VENV_CREATE) .venv && \
	. .venv/bin/activate && \
	$(PIP_INSTALL) -e ".[dev]"

lint: ## Run ruff check + mypy
	cd backend && ruff check src/ tests/
	cd backend && mypy src/

format: ## Run ruff format
	cd backend && ruff format src/ tests/

test: ## Run pytest
	cd backend && pytest

test-cov: ## Run pytest with coverage
	cd backend && pytest --cov=ground_control --cov-report=term-missing

dev: ## Start development server
	cd backend && python manage.py runserver 0.0.0.0:8000

clean: ## Remove build artifacts and caches
	find backend -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
	find backend -type d -name .mypy_cache -exec rm -rf {} + 2>/dev/null || true
	find backend -type d -name .ruff_cache -exec rm -rf {} + 2>/dev/null || true
	find backend -type d -name .pytest_cache -exec rm -rf {} + 2>/dev/null || true
	find backend -type d -name "*.egg-info" -exec rm -rf {} + 2>/dev/null || true
	rm -rf backend/.venv

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help
