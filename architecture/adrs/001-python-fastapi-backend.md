# ADR-001: Python 3.12+ with FastAPI for Backend

## Status

Accepted

## Date

2026-03-07

## Context

Ground Control needs a backend framework that supports async I/O for high-concurrency multi-tenant workloads, has strong typing support, generates OpenAPI documentation automatically, and is accessible to the GRC/security professional community where Python is the dominant language.

We evaluated: FastAPI (Python), Django REST Framework (Python), Express/Fastify (Node.js), and Go (stdlib/Gin).

## Decision

Use Python 3.12+ with FastAPI as the backend framework.

- FastAPI provides automatic OpenAPI 3.1 schema generation from Pydantic models
- Native async/await support via Starlette and uvicorn
- Pydantic v2 for request/response validation with high performance
- Python's ecosystem for data processing, AI/ML integration, and security tooling
- Strong typing via mypy strict mode catches bugs at development time
- Dependency injection built into the framework

## Consequences

### Positive

- Automatic API documentation reduces maintenance burden
- Pydantic models serve as both validation and documentation
- Large talent pool familiar with Python
- Rich ecosystem for GRC-adjacent tasks (data analysis, reporting, AI agents)
- async/await enables high concurrency without threads

### Negative

- Python is slower than Go/Rust for CPU-bound work (mitigated: ITRM is I/O-bound)
- GIL limits true parallelism (mitigated: async I/O, and Python 3.12+ has improved GIL)
- Requires discipline to maintain type safety (enforced via mypy strict + CI)

### Risks

- FastAPI is maintained primarily by one developer (mitigated: built on Starlette/Pydantic which have broader maintainership)
