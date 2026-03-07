# Ground Control — Coding Standards & Development Practices

**Version:** 1.0.0

---

## 1. Project Structure

```
Ground-Control/
├── backend/
│   └── src/ground_control/
│       ├── api/              # Route handlers only. No business logic.
│       ├── domain/           # Entities, value objects, service interfaces, use cases
│       ├── infrastructure/   # SQLAlchemy repos, S3 client, Redis, external APIs
│       ├── schemas/          # Pydantic request/response models (shared across API layer)
│       ├── middleware/       # Tenant resolution, auth, request-id, logging context
│       ├── events/           # Domain event bus, event types, handlers
│       ├── exceptions/       # Exception hierarchy (shared across all layers)
│       ├── logging/          # Structured logging setup (shared across all layers)
│       ├── config.py         # pydantic-settings, fail-fast validation
│       └── main.py           # FastAPI app composition
│   ├── tests/
│   └── migrations/           # Alembic
├── frontend/
├── sdks/
├── plugins/
├── proofs/                   # Coq/Rocq formal proofs
├── deploy/
└── docs/
```

## 2. Dependency Rule (Clean Architecture)

The most important rule in the codebase:

```
api/ → domain/ ← infrastructure/
         ↑
     schemas/ (shared DTOs)
     exceptions/ (shared error types)
     logging/ (shared logging)
     events/ (shared event types)
```

- `domain/` has ZERO imports from `api/`, `infrastructure/`, FastAPI, SQLAlchemy, or any framework.
- `api/` depends on `domain/` (use cases) and `schemas/` (request/response models). Never imports from `infrastructure/`.
- `infrastructure/` implements interfaces defined in `domain/`. Depends on `domain/` and external libraries (SQLAlchemy, boto3, etc.).
- `schemas/`, `exceptions/`, `logging/`, `events/` are cross-cutting — importable by any layer.
- `config.py` is importable by any layer. It's the single source of truth for all configuration.

This is enforced by `import-linter` in CI. Violations fail the build.

## 3. Cross-Cutting Concerns

These must be used from day one, in every module. Not "added later."

### 3.1 Exceptions

All exceptions inherit from a base hierarchy in `exceptions/`:

```python
# exceptions/base.py
class GroundControlError(Exception):
    """Base for all application exceptions."""
    def __init__(self, message: str, code: str | None = None):
        self.message = message
        self.code = code
        super().__init__(message)

class NotFoundError(GroundControlError):
    """Entity not found."""

class ValidationError(GroundControlError):
    """Business rule validation failure."""

class AuthorizationError(GroundControlError):
    """Insufficient permissions."""

class ConflictError(GroundControlError):
    """Duplicate or optimistic lock failure."""

class ExternalServiceError(GroundControlError):
    """Failure in an external dependency (S3, Redis, IdP)."""
```

Rules:
- Domain layer raises `GroundControlError` subclasses. Never `HTTPException`.
- API layer maps exceptions to HTTP responses via a single exception handler middleware.
- Never catch `Exception` broadly. Catch specific types.
- Never swallow exceptions silently. Log and re-raise, or handle explicitly.
- External library exceptions (SQLAlchemy, boto3) are caught in `infrastructure/` and wrapped in `GroundControlError` subclasses.

### 3.2 Structured Logging

All logging uses `structlog` with JSON output. Every log line includes context.

```python
# In any module:
import structlog
logger = structlog.get_logger()

logger.info("risk_created", risk_id=risk.id, tenant_id=tenant_id, actor_id=user.id)
```

Rules:
- Use semantic event names, not sentences: `"risk_created"` not `"A new risk was created"`.
- Always include `tenant_id` and `actor_id` when available (bound via middleware context).
- Never log secrets, tokens, passwords, or PII. Scrub before logging.
- Never use `print()`. Ever.
- Log at appropriate levels: `debug` for developer detail, `info` for business events, `warning` for recoverable issues, `error` for failures requiring attention.

### 3.3 Audit Logging

Every state change to a domain entity MUST be audit-logged. This is non-negotiable.

```python
# Called from use cases, not from API routes or repositories
await audit_log.record(
    tenant_id=tenant_id,
    actor_id=actor_id,
    actor_type="user",  # or "agent", "system"
    action="update",
    resource_type="risk",
    resource_id=risk.id,
    changes={"status": {"old": "open", "new": "mitigated"}},
)
```

Rules:
- Audit logging is part of the use case, not an afterthought.
- The audit log is append-only. No updates. No deletes.
- Every audit entry includes the hash of the previous entry (tamper detection chain).
- Audit log writes happen in the same transaction as the state change.

### 3.4 Schemas (Pydantic)

All request/response models live in `schemas/`. They are the contract between API and domain.

```python
# schemas/risk.py
class RiskCreate(BaseModel):
    title: str
    description: str | None = None
    category: str
    inherent_likelihood: int = Field(ge=1, le=5)
    inherent_impact: int = Field(ge=1, le=5)
    owner_id: UUID | None = None

class RiskRead(BaseModel):
    id: UUID
    ref_id: str
    title: str
    # ... all fields
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)
```

Rules:
- One file per entity in `schemas/`: `risk.py`, `control.py`, `assessment.py`, etc.
- Every API endpoint uses explicit `Create`, `Read`, `Update` schema variants. No reuse of the same model for input and output.
- Validation (field ranges, patterns, enums) lives in the schema, not in the use case.
- Use `Field()` for constraints. Use `Annotated` types for reusable patterns.
- Never expose SQLAlchemy models directly in API responses.

### 3.5 Tenant Context

Every request carries a tenant context, set by middleware:

```python
# Accessible anywhere via context var
from ground_control.middleware.tenant import get_current_tenant_id

tenant_id = get_current_tenant_id()
```

Rules:
- Every database query filters by `tenant_id`. No exceptions.
- PostgreSQL Row-Level Security (RLS) is the safety net, not the primary mechanism. Application code must also filter.
- Tests must verify that cross-tenant data leakage is impossible.

### 3.6 Request Context

Every request gets a unique `request_id` (set by middleware). It propagates through:
- All log entries (`structlog` context binding)
- All audit log entries
- Error responses (returned to client for support correlation)

## 4. Domain Layer Rules

- Domain entities are plain Python classes or dataclasses. Not SQLAlchemy models.
- Use cases are single-purpose functions or classes. One use case per business operation.
- Use case signature: takes primitive types or domain entities, returns domain entities or DTOs. Never takes a `Request` object or returns a `Response`.
- Repository interfaces are abstract classes defined in `domain/`. Implementations live in `infrastructure/`.
- Domain services contain business logic that spans multiple entities.
- The domain layer is independently testable with no database, no HTTP, no external services.

```python
# domain/services/risk_service.py
class RiskService:
    def __init__(self, risk_repo: RiskRepository, audit_log: AuditLog):
        self.risk_repo = risk_repo
        self.audit_log = audit_log

    async def create_risk(self, tenant_id: UUID, actor_id: UUID, data: RiskCreate) -> Risk:
        risk = Risk(tenant_id=tenant_id, **data.model_dump())
        risk = await self.risk_repo.save(risk)
        await self.audit_log.record(...)
        await self.event_bus.publish(RiskCreated(risk_id=risk.id, tenant_id=tenant_id))
        return risk
```

## 5. API Layer Rules

- Route handlers are thin. They parse the request, call a use case, and format the response.
- No business logic in route handlers. If you're writing an `if` that isn't about HTTP concerns, it belongs in the domain layer.
- Use dependency injection (FastAPI's `Depends()`) for services, repos, and current user.
- All routes return Pydantic schemas, never dicts or SQLAlchemy models.

```python
# api/v1/risks.py
@router.post("/risks", response_model=RiskRead, status_code=201)
async def create_risk(
    data: RiskCreate,
    risk_service: RiskService = Depends(get_risk_service),
    current_user: User = Depends(get_current_user),
):
    risk = await risk_service.create_risk(
        tenant_id=current_user.tenant_id,
        actor_id=current_user.id,
        data=data,
    )
    return risk
```

## 6. Infrastructure Layer Rules

- Repository implementations use SQLAlchemy 2.0 async style.
- Every repository method takes `tenant_id` as a parameter (defense in depth alongside RLS).
- External service clients (S3, Redis, SMTP) are wrapped in thin adapter classes.
- Adapter classes implement interfaces from `domain/`. They are swappable in tests.
- Never import infrastructure modules from `domain/` or `api/`.

## 7. Testing

### Test Structure
```
tests/
├── unit/            # Domain logic only. No DB, no HTTP. Fast.
│   ├── domain/
│   └── schemas/
├── integration/     # With DB and real services. Use test containers.
│   ├── api/
│   └── infrastructure/
└── e2e/             # Playwright. Full stack via Docker Compose.
```

### Rules
- Unit tests cover domain services and entities. They use fakes/stubs for repositories.
- Integration tests cover API endpoints and repository implementations. They use a real PostgreSQL instance (testcontainers).
- Every API endpoint has at least one integration test for success and one for the primary error case.
- Every domain service method has unit tests for its business rules.
- Tests are independent. No shared mutable state. Each test sets up its own data.
- Use `pytest` fixtures for dependency setup. Use `factory_boy` or simple factory functions for test data.
- Test names describe behavior: `test_create_risk_fails_when_likelihood_out_of_range`.

### Coverage
- Minimum 80% line coverage for `domain/`. No exceptions.
- Minimum 70% line coverage for `api/` and `infrastructure/`.
- Coverage is measured in CI and reported. Coverage drops fail the build.

## 8. Error Handling Strategy

```
Layer           | Catches                        | Raises
----------------|-------------------------------|---------------------------
infrastructure/ | SQLAlchemy, boto3, redis-py    | GroundControlError subtypes
domain/         | Nothing (or domain errors)     | GroundControlError subtypes
api/            | GroundControlError subtypes    | HTTPException (via handler)
middleware      | All unhandled exceptions        | 500 with request_id
```

The exception handler middleware maps exceptions to HTTP:
```python
NotFoundError      → 404
ValidationError    → 422
AuthorizationError → 403
ConflictError      → 409
ExternalServiceError → 502
GroundControlError → 500 (catch-all)
```

Every error response includes `request_id` for correlation.

## 9. Code Style

### Python
- Formatter: `ruff format`
- Linter: `ruff check`
- Type checker: `mypy --strict`
- All functions have type annotations. No `Any` unless unavoidable (and commented why).
- Line length: 100 characters.
- Imports: sorted by `ruff` (isort-compatible).
- Docstrings: only on public API boundaries (use case functions, service classes). Not on every method. Code should be self-explanatory.

### TypeScript
- Formatter/Linter: `biome`
- Type checker: `tsc --strict`
- No `any` types. Use `unknown` and narrow.

## 10. Git & CI

- All code goes through PR. No direct push to `main` or `dev`.
- PRs require: passing CI (lint + typecheck + tests), no coverage regression.
- Commit messages: imperative mood, concise. `Add risk scoring engine` not `Added risk scoring engine` or `This commit adds...`
- CI pipeline runs: `ruff check` → `ruff format --check` → `mypy` → `pytest` → `import-linter` → coverage report.
- `import-linter` enforces the dependency rule. If `domain/` imports from `infrastructure/`, CI fails.

## 11. Formal Methods

Coq/Rocq proofs live in `proofs/` and verify critical invariants that testing alone cannot guarantee:

### What gets proved
- **Audit log integrity**: The hash chain is append-only and tamper-evident.
- **RBAC/ABAC policy evaluation**: Permission checks are correct and complete — no privilege escalation paths exist.
- **State machine transitions**: Entity lifecycle states (finding, assessment, remediation) can only reach valid configurations. No illegal state transitions are possible.
- **Tenant isolation**: Query construction guarantees no cross-tenant data leakage.

### What does NOT get proved
- CRUD operations, API routing, UI components, serialization — standard testing is sufficient.

### Structure
```
proofs/
├── audit_log/          # Hash chain integrity, append-only guarantees
├── authorization/      # RBAC/ABAC policy correctness
├── state_machines/     # Lifecycle transition validity
├── tenant_isolation/   # Query isolation guarantees
└── README.md           # How to build and verify proofs
```

### Integration
- Proofs are checked in CI (Coq compiler verifies them).
- When the corresponding domain logic changes, the proof must be updated to match.
- Proofs reference the domain types they verify (via comments/documentation, not code extraction — the proofs model the logic, they don't compile to Python).
- Development uses rocq-mcp (Model Context Protocol server for Rocq) for interactive proof writing, type checking, and tactic feedback. See issue #006b for setup.
