# Ground Control REST API

HTTP API for direct REST usage. Pre-alpha, localhost only, no authentication.

## Base URL

```
http://localhost:8000/api/v1/
```

## Endpoints

### Requirements

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/requirements` | RequirementRequest | 201 | Create requirement |
| GET | `/requirements` | — | 200 | List requirements (paginated, filterable) |
| GET | `/requirements/{id}` | — | 200 | Get requirement by UUID |
| GET | `/requirements/uid/{uid}` | — | 200 | Get requirement by UID |
| PUT | `/requirements/{id}` | RequirementRequest | 200 | Update requirement |
| POST | `/requirements/{id}/transition` | `{ "status": "ACTIVE" }` | 200 | Transition status |
| POST | `/requirements/bulk/transition` | BulkStatusTransitionRequest | 200 | Bulk transition status |
| POST | `/requirements/{id}/clone` | CloneRequirementRequest | 201 | Clone requirement |
| POST | `/requirements/{id}/archive` | — | 200 | Archive requirement |

### Relations

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/requirements/{id}/relations` | RelationRequest | 201 | Create relation |
| GET | `/requirements/{id}/relations` | — | 200 | List relations |
| DELETE | `/requirements/{id}/relations/{relationId}` | — | 204 | Delete relation |

### Traceability

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/requirements/{id}/traceability` | TraceabilityLinkRequest | 201 | Create traceability link |
| GET | `/requirements/{id}/traceability` | — | 200 | List traceability links |
| DELETE | `/requirements/{id}/traceability/{linkId}` | — | 204 | Delete traceability link |

### Analysis

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| GET | `/analysis/cycles` | — | 200 | Detect dependency cycles |
| GET | `/analysis/orphans` | — | 200 | Find orphan requirements |
| GET | `/analysis/coverage-gaps?linkType=X` | — | 200 | Find coverage gaps by link type |
| GET | `/analysis/impact/{id}` | — | 200 | Transitive impact analysis |
| GET | `/analysis/cross-wave` | — | 200 | Cross-wave dependency violations |

### Graph

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/admin/graph/materialize` | — | 200 | Materialize graph (AGE) |
| GET | `/graph/ancestors/{uid}?depth=N` | — | 200 | Ancestor UIDs |
| GET | `/graph/descendants/{uid}?depth=N` | — | 200 | Descendant UIDs |
| GET | `/graph/paths?source=X&target=Y` | — | 200 | All paths between two UIDs |

### Import / Sync

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/admin/import/strictdoc` | multipart/form-data | 200 | Import .sdoc file |
| POST | `/admin/sync/github?owner=X&repo=Y` | — | 200 | Sync GitHub issues |

## Request / Response Format

JSON. Error responses use a nested envelope:

```json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "Requirement not found",
    "detail": {}
  }
}
```

HTTP status codes: 201 (created), 200 (ok), 204 (deleted), 404 (not found),
409 (conflict), 422 (validation error).

## Filtering

`GET /api/v1/requirements` accepts query parameters:

| Parameter | Type | Description |
|-----------|------|-------------|
| `status` | enum | DRAFT, ACTIVE, DEPRECATED, ARCHIVED |
| `type` | enum | FUNCTIONAL, NON_FUNCTIONAL, CONSTRAINT, INTERFACE |
| `wave` | integer | Wave number |
| `search` | string | Free-text search in title and statement |

Archived requirements are excluded by default. Filter by `status=ARCHIVED`
to include them.

## Pagination

Standard Spring Page parameters:

| Parameter | Default | Description |
|-----------|---------|-------------|
| `page` | 0 | Page number (0-based) |
| `size` | 20 | Page size |
| `sort` | — | Sort field and direction (e.g. `sort=uid,asc`) |

Response wraps results in a Spring Page object with `content`, `totalElements`,
`totalPages`, `number`, `size`.

## Interactive Docs

- Swagger UI: `http://localhost:8000/api/docs`
- OpenAPI JSON: `http://localhost:8000/api/openapi.json`
