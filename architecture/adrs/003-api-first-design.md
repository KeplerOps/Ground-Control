# ADR-003: API-First Design (REST)

## Status

Accepted

## Date

2026-03-07

## Context

Ground Control must support multiple client types: web UI, AI agents, CLI tools, and external system integrations. The API must be the primary interface — the UI is one consumer among many. We need a well-documented, stable API contract.

We evaluated: REST, GraphQL, gRPC, and hybrid approaches.

## Decision

Adopt a REST-first API design with OpenAPI 3.1 specification.

- All endpoints follow REST conventions under `/api/v1/`
- OpenAPI schema is auto-generated from FastAPI/Pydantic models
- Flat JSON responses (no deep nesting) with optional `include` parameter for related entities
- PATCH uses RFC 7396 (JSON Merge Patch) for partial updates
- Pagination via cursor-based tokens for stable iteration
- Versioned at the URL path level (`/api/v1/`, `/api/v2/`)

## Consequences

### Positive

- REST is universally understood — lowest barrier for integrations
- OpenAPI spec enables code generation for client SDKs
- Auto-generated docs from Pydantic models stay in sync with implementation
- Flat responses are simple to parse and cache

### Negative

- REST can over-fetch or under-fetch data (mitigated: field selection and `include` parameter)
- No subscription/streaming support natively (mitigated: webhooks + SSE for real-time needs)
- URL-based versioning requires careful migration planning

### Risks

- API surface could grow large — discipline required to keep endpoints focused
