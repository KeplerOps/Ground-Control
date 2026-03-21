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
| PUT | `/requirements/{id}` | UpdateRequirementRequest | 200 | Update requirement (partial) |
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

### Audit History

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| GET | `/requirements/{id}/history` | — | 200 | Requirement revision history |
| GET | `/requirements/{id}/relations/{relationId}/history` | — | 200 | Relation revision history |
| GET | `/requirements/{id}/traceability/{linkId}/history` | — | 200 | Traceability link revision history |
| GET | `/requirements/{id}/timeline` | — | 200 | Unified audit timeline |

`GET /requirements/{id}/timeline` accepts query parameters:

| Parameter | Type | Description |
|-----------|------|-------------|
| `changeCategory` | enum | REQUIREMENT, RELATION, TRACEABILITY_LINK |
| `from` | ISO-8601 instant | Start of date range |
| `to` | ISO-8601 instant | End of date range |
| `limit` | integer | Max entries to return (default 100) |
| `offset` | integer | Number of entries to skip (default 0) |

**TimelineEntryResponse:**

```json
{
  "revisionNumber": 3,
  "revisionType": "MOD",
  "timestamp": "2026-03-21T04:00:00Z",
  "actor": "user@example.com",
  "changeCategory": "REQUIREMENT",
  "entityId": "uuid",
  "snapshot": { "title": "New Title", "status": "ACTIVE", "..." : "..." },
  "changes": { "title": { "oldValue": "Old Title", "newValue": "New Title" } }
}
```

### Analysis

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| GET | `/analysis/cycles` | — | 200 | Detect dependency cycles |
| GET | `/analysis/orphans` | — | 200 | Find orphan requirements |
| GET | `/analysis/coverage-gaps?linkType=X` | — | 200 | Find coverage gaps by link type |
| GET | `/analysis/impact/{id}` | — | 200 | Transitive impact analysis |
| GET | `/analysis/cross-wave` | — | 200 | Cross-wave dependency violations |

**CycleResponse** (`GET /analysis/cycles`):

```json
[
  {
    "members": ["REQ-A", "REQ-B", "REQ-C", "REQ-A"],
    "edges": [
      { "sourceUid": "REQ-A", "targetUid": "REQ-B", "relationType": "DEPENDS_ON" },
      { "sourceUid": "REQ-B", "targetUid": "REQ-C", "relationType": "DEPENDS_ON" },
      { "sourceUid": "REQ-C", "targetUid": "REQ-A", "relationType": "PARENT" }
    ]
  }
]
```

Each cycle lists the member UIDs (closing back to the start) and the edges that
form it, including the relation type between each consecutive pair.

### Baselines

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/baselines?project=` | BaselineRequest | 201 | Create baseline |
| GET | `/baselines?project=` | — | 200 | List baselines |
| GET | `/baselines/{id}` | — | 200 | Get baseline |
| GET | `/baselines/{id}/snapshot` | — | 200 | Requirement snapshot at baseline |
| GET | `/baselines/{id}/compare/{otherId}` | — | 200 | Compare two baselines |
| DELETE | `/baselines/{id}` | — | 204 | Delete baseline |

**BaselineRequest:**

```json
{
  "name": "v1.0",
  "description": "First release baseline"
}
```

**BaselineComparisonResponse** (`GET /baselines/{id}/compare/{otherId}`):

```json
{
  "baselineId": "uuid",
  "baselineName": "v1.0",
  "otherBaselineId": "uuid",
  "otherBaselineName": "v2.0",
  "addedCount": 2,
  "removedCount": 0,
  "modifiedCount": 1,
  "added": [...],
  "removed": [...],
  "modified": [{ "requirementId": "uuid", "uid": "REQ-001", "before": {...}, "after": {...} }]
}
```

### Graph

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/admin/graph/materialize` | — | 200 | Materialize graph (AGE) |
| GET | `/graph/ancestors/{uid}?depth=N` | — | 200 | Ancestor UIDs |
| GET | `/graph/descendants/{uid}?depth=N` | — | 200 | Descendant UIDs |
| GET | `/graph/paths?source=X&target=Y` | — | 200 | All paths between two UIDs (with edges) |

**Path response shape:**

```json
[
  {
    "nodes": ["REQ-A", "REQ-B", "REQ-C"],
    "edges": [
      { "sourceUid": "REQ-A", "targetUid": "REQ-B", "relationType": "DEPENDS_ON" },
      { "sourceUid": "REQ-B", "targetUid": "REQ-C", "relationType": "PARENT" }
    ]
  }
]
```

### GitHub Issues

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/admin/github/issues` | GitHubIssueRequest | 201 | Create issue from requirement |

**GitHubIssueRequest:**

```json
{
  "requirementUid": "GC-A001",
  "repo": "owner/repo",
  "extraBody": "Additional markdown (optional)",
  "labels": ["enhancement"]
}
```

**GitHubIssueResponse:**

```json
{
  "issueUrl": "https://github.com/owner/repo/issues/42",
  "issueNumber": 42,
  "traceabilityLinkId": "uuid",
  "warning": null
}
```

### Import / Sync

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/admin/import/strictdoc` | multipart/form-data | 200 | Import .sdoc file |
| POST | `/admin/import/reqif` | multipart/form-data | 200 | Import .reqif file |
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
