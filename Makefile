.PHONY: rapid build test test-cov format lint check integration verify dev clean up down docker-build smoke \
       frontend-install frontend-dev frontend-build frontend-lint frontend-format

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

# --- Frontend ---

frontend-install: ## Install frontend dependencies
	cd frontend && npm install

frontend-dev: ## Start frontend dev server (Vite)
	cd frontend && npm run dev

frontend-build: ## Build frontend for production
	cd frontend && npm run build

frontend-lint: ## Lint frontend code (Biome)
	cd frontend && npm run lint

frontend-format: ## Format frontend code (Biome)
	cd frontend && npm run format

# --- Infrastructure ---

dev: ## Start development server (loads .env)
	set -a && [ -f .env ] && . ./.env && set +a && cd backend && ./gradlew bootRun

up: ## Start Docker Compose services (PostgreSQL, Redis)
	docker compose up -d

down: ## Stop Docker Compose services
	docker compose down

docker-build: ## Build Docker image (frontend + backend)
	docker build -f backend/Dockerfile -t ghcr.io/keplerops/ground-control:latest .

smoke: docker-build ## Build Docker image and verify Flyway + health
	@echo "Starting smoke test..."
	@docker rm -f gc-smoke-db gc-smoke 2>/dev/null || true
	@docker run -d --name gc-smoke-db \
		-e POSTGRES_DB=ground_control \
		-e POSTGRES_USER=gc \
		-e POSTGRES_PASSWORD=gc \
		-p 5433:5432 \
		--health-cmd "pg_isready -U gc -d ground_control" \
		--health-interval 2s --health-timeout 5s --health-retries 10 \
		postgres:16
	@echo "Waiting for database..."
	@for i in $$(seq 1 30); do \
		docker inspect --format='{{.State.Health.Status}}' gc-smoke-db 2>/dev/null | grep -q healthy && break; \
		sleep 1; \
	done
	@docker run -d --name gc-smoke \
		--network host \
		-e GC_DATABASE_URL=jdbc:postgresql://localhost:5433/ground_control \
		-e GC_DATABASE_USER=gc \
		-e GC_DATABASE_PASSWORD=gc \
		ghcr.io/keplerops/ground-control:latest
	@echo "Waiting for application startup..."
	@PASS=false; for i in $$(seq 1 60); do \
		HEALTH=$$(curl -sf http://localhost:8000/actuator/health 2>/dev/null) && { \
			echo "Smoke test passed: $$HEALTH"; PASS=true; break; \
		}; \
		sleep 2; \
	done; \
	if [ "$$PASS" != "true" ]; then \
		echo "Smoke test failed after 120s"; \
		docker logs gc-smoke; \
	fi; \
	docker rm -f gc-smoke-db gc-smoke 2>/dev/null || true; \
	[ "$$PASS" = "true" ]

clean: ## Remove build artifacts
	cd backend && ./gradlew clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help
