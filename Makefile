.PHONY: rapid build test test-cov format lint check integration verify dev clean up down docker-build

# --- Rapid dev loop (< 5s) ---

rapid: ## Format + compile, no tests or static analysis
	cd backend && ./gradlew spotlessApply compileJava -Pquick

# --- Standard ---

build: ## Build the project (no tests)
	cd backend && ./gradlew build -x test -Pquick

test: ## Run unit tests (no static analysis)
	cd backend && ./gradlew test -Pquick

test-cov: ## Run tests with coverage report
	cd backend && ./gradlew test jacocoTestReport

format: ## Format code with Spotless
	cd backend && ./gradlew spotlessApply

lint: ## Check formatting
	cd backend && ./gradlew spotlessCheck

# --- Full verification (CI-equivalent) ---

check: ## Full build + tests + static analysis + coverage
	cd backend && ./gradlew check

integration: ## Integration tests (Testcontainers)
	cd backend && ./gradlew integrationTest

verify: ## Full CI-equivalent verification
	cd backend && ./gradlew check integrationTest openjmlEsc

# --- Infrastructure ---

dev: ## Start development server
	cd backend && ./gradlew bootRun

up: ## Start Docker Compose services (PostgreSQL, Redis)
	docker compose up -d

down: ## Stop Docker Compose services
	docker compose down

docker-build: ## Build backend Docker image
	docker build -t ghcr.io/keplerops/ground-control:latest backend/

clean: ## Remove build artifacts
	cd backend && ./gradlew clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help
