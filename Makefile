.PHONY: rapid build test test-cov format lint check integration verify dev clean up down docker-build cloud-db-env dev-cloud cloud-db-ip

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

# Cloud DB config
AWS_PROFILE  ?= catalyst-dev
AWS_REGION   ?= us-east-2
TF_DIR       := deploy/terraform/environments/dev

cloud-db-env: ## Print export statements for cloud DB credentials (usage: eval $$(make cloud-db-env))
	@DB_HOST=$$(aws ssm get-parameter --name /gc/dev/db_host --with-decryption \
		--query 'Parameter.Value' --output text --region $(AWS_REGION) --profile $(AWS_PROFILE)) && \
	DB_USER=$$(aws ssm get-parameter --name /gc/dev/db_username --with-decryption \
		--query 'Parameter.Value' --output text --region $(AWS_REGION) --profile $(AWS_PROFILE)) && \
	DB_PASS=$$(aws ssm get-parameter --name /gc/dev/db_password --with-decryption \
		--query 'Parameter.Value' --output text --region $(AWS_REGION) --profile $(AWS_PROFILE)) && \
	echo "export GC_DATABASE_URL=jdbc:postgresql://$$DB_HOST:5432/ground_control" && \
	echo "export GC_DATABASE_USER=$$DB_USER" && \
	echo "export GC_DATABASE_PASSWORD=$$DB_PASS" && \
	echo "export GC_AGE_ENABLED=false"

dev-cloud: ## Start app against cloud RDS
	@eval $$($(MAKE) cloud-db-env) && cd backend && ./gradlew bootRun

cloud-db-ip: ## Authorize your current IP on the dev database security group
	@MY_IP=$$(curl -s ifconfig.me) && \
	SG_ID=$$(terraform -chdir=$(TF_DIR) output -raw security_group_id) && \
	echo "Revoking existing port 5432 ingress rules..." && \
	EXISTING=$$(aws ec2 describe-security-groups --group-ids $$SG_ID \
		--query 'SecurityGroups[0].IpPermissions[?FromPort==`5432`].IpRanges[].CidrIp' \
		--output text --region $(AWS_REGION) --profile $(AWS_PROFILE)) && \
	for cidr in $$EXISTING; do \
		aws ec2 revoke-security-group-ingress --group-id $$SG_ID \
			--protocol tcp --port 5432 --cidr $$cidr \
			--region $(AWS_REGION) --profile $(AWS_PROFILE) > /dev/null; \
	done && \
	echo "Authorizing $$MY_IP/32 on port 5432..." && \
	aws ec2 authorize-security-group-ingress --group-id $$SG_ID \
		--protocol tcp --port 5432 --cidr $$MY_IP/32 \
		--region $(AWS_REGION) --profile $(AWS_PROFILE) > /dev/null && \
	echo "Authorized $$MY_IP/32 on security group $$SG_ID"

clean: ## Remove build artifacts
	cd backend && ./gradlew clean

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help
