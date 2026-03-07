---
title: "Implement configuration management with pydantic-settings"
labels: [foundation, backend, cross-cutting]
phase: 0
priority: P0
---

## Description

Create a centralized, typed configuration system using `pydantic-settings` that loads from environment variables (with `.env` fallback). All configuration is validated at startup — fail fast on misconfiguration.

## References

- Deployment: Section 2.5 (Environment Variables)
- Architecture: Section 7 (Pydantic for validation)

## Acceptance Criteria

- [ ] `backend/src/ground_control/config.py` with `Settings` class:
  ```python
  class Settings(BaseSettings):
      # Core
      secret_key: SecretStr
      allowed_origins: list[str]
      log_level: Literal["debug", "info", "warning", "error"] = "info"
      environment: Literal["development", "staging", "production"] = "development"

      # Database
      database_url: PostgresDsn
      db_pool_size: int = 20
      db_pool_overflow: int = 10

      # Redis
      redis_url: RedisDsn

      # Object Storage
      s3_endpoint: AnyHttpUrl
      s3_bucket: str = "gc-artifacts"
      s3_access_key: SecretStr
      s3_secret_key: SecretStr

      # Search
      search_url: AnyHttpUrl
      search_key: SecretStr

      # Auth
      jwt_algorithm: str = "HS256"
      access_token_expire_minutes: int = 60
      refresh_token_expire_days: int = 30

      # Multi-tenancy
      multi_tenancy_mode: Literal["shared_schema", "schema_per_tenant", "database_per_tenant"] = "shared_schema"

      model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")
  ```
- [ ] Settings are a singleton, loaded once at startup
- [ ] Validation errors produce clear messages indicating which env vars are missing/invalid
- [ ] `SecretStr` used for all sensitive values (prevents accidental logging)
- [ ] Settings are injectable via FastAPI dependency injection
- [ ] Unit tests with mock environment variables

## Technical Notes

- Use `pydantic-settings` v2 (Pydantic v2 compatible)
- Nested settings use env prefix: `SSO__PROVIDER`, `SMTP__HOST`, etc.
- Never log settings values — only log which settings were loaded
- Consider adding a `settings.display_safe()` method that redacts secrets for debug output
