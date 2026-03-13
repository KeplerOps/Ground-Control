.PHONY: rapid build test test-cov format lint check integration verify dev clean up down docker-build deploy-dev

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

deploy-dev: ## Run Docker image against dev RDS
	@echo "Fetching credentials from SSM..."
	@GC_DB_PASS=$$(aws ssm get-parameter --name /gc/dev/db_password --with-decryption \
		--query 'Parameter.Value' --output text --region us-east-2 --profile catalyst-dev) && \
	docker run --rm -p 8000:8000 \
		-e GC_DATABASE_URL=jdbc:postgresql://groundcontrol-dev.cjmae0omm2br.us-east-2.rds.amazonaws.com:5432/ground_control \
		-e GC_DATABASE_USER=gcadmin \
		-e "GC_DATABASE_PASSWORD=$$GC_DB_PASS" \
		ghcr.io/keplerops/ground-control:dev

clean: ## Remove build artifacts
	cd backend && ./gradlew clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help
