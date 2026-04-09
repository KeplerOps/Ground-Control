# Ground Control REST API

REST API for direct HTTP usage. Pre-alpha, localhost only, no authentication.

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
| GET | `/analysis/consistency-violations` | — | 200 | Detect consistency violations |
| GET | `/analysis/completeness` | — | 200 | Analyze completeness |
| GET | `/analysis/work-order` | — | 200 | Topological work order |
| GET | `/analysis/dashboard-stats` | — | 200 | Aggregate project health stats |
| GET | `/analysis/semantic-similarity` | — | 200 | Find semantically similar requirement pairs |
| POST | `/analysis/sweep` | — | 200 | Run analysis sweep on one project |
| POST | `/analysis/sweep/all` | — | 200 | Run analysis sweep on all projects |

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

`GET /analysis/semantic-similarity` accepts query parameters:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `project` | string | auto-resolved | Project identifier |
| `threshold` | double | 0.85 | Minimum similarity score (0–1) |

**SimilarityResultResponse:**

```json
{
  "totalRequirements": 50,
  "embeddedCount": 48,
  "pairsAnalyzed": 1128,
  "threshold": 0.85,
  "pairs": [
    {
      "uid1": "REQ-012",
      "title1": "User authentication via SSO",
      "uid2": "REQ-037",
      "title2": "Single sign-on login support",
      "score": 0.93
    }
  ]
}
```

### Embeddings

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/embeddings/{requirementId}` | — | 200 | Embed a single requirement |
| GET | `/embeddings/{requirementId}/status` | — | 200 | Get embedding status |
| POST | `/embeddings/batch?project=&force=false` | — | 200 | Batch embed all requirements in a project |
| DELETE | `/embeddings/{requirementId}` | — | 204 | Delete embedding |

Requires `GC_EMBEDDING_PROVIDER=openai` and `GC_EMBEDDING_API_KEY` to be set.
When no provider is configured, endpoints return `provider_unavailable` status
(graceful degradation).

**EmbeddingStatusResponse** (`GET /embeddings/{id}/status`):

```json
{
  "requirementId": "uuid",
  "hasEmbedding": true,
  "isStale": false,
  "modelMismatch": false,
  "currentModelId": "text-embedding-3-small",
  "embeddingModelId": "text-embedding-3-small",
  "embeddedAt": "2026-03-22T03:00:00Z"
}
```

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

### Document Grammar

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| PUT | `/documents/{id}/grammar` | Grammar JSON | 200 | Set/replace grammar |
| GET | `/documents/{id}/grammar` | — | 200 | Get grammar |
| DELETE | `/documents/{id}/grammar` | — | 204 | Remove grammar |

**Grammar JSON:**

```json
{
  "fields": [
    {"name": "acceptance_criteria", "type": "STRING", "required": false},
    {"name": "risk_level", "type": "ENUM", "required": true, "enumValues": ["LOW", "MEDIUM", "HIGH"]}
  ],
  "allowedRequirementTypes": ["FUNCTIONAL", "NON_FUNCTIONAL"],
  "allowedRelationTypes": ["PARENT", "DEPENDS_ON", "REFINES"]
}
```

Field types: `STRING`, `INTEGER`, `BOOLEAN`, `ENUM`. Declarative metadata — no runtime enforcement.

### Document Reading Order

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| GET | `/documents/{id}/reading-order` | — | 200 | Full document in reading order |

Returns the document with all sections nested, each containing its content items
(requirement references and text blocks) in authored sequence.

### Section Content

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/sections/{sectionId}/content` | SectionContentRequest | 201 | Add content item |
| GET | `/sections/{sectionId}/content` | — | 200 | List content in order |
| PUT | `/sections/content/{id}` | UpdateSectionContentRequest | 200 | Update content item |
| DELETE | `/sections/content/{id}` | — | 204 | Delete content item |

**SectionContentRequest:**

```json
{
  "contentType": "REQUIREMENT",
  "requirementId": "uuid",
  "sortOrder": 0
}
```

or for text blocks:

```json
{
  "contentType": "TEXT_BLOCK",
  "textContent": "This section describes...",
  "sortOrder": 1
}
```

### Sections

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/documents/{documentId}/sections` | SectionRequest | 201 | Create section |
| GET | `/documents/{documentId}/sections` | — | 200 | List sections (flat) |
| GET | `/documents/{documentId}/sections/tree` | — | 200 | Get section tree (nested) |
| GET | `/sections/{id}` | — | 200 | Get section |
| PUT | `/sections/{id}` | UpdateSectionRequest | 200 | Update section |
| DELETE | `/sections/{id}` | — | 204 | Delete section (cascades children) |

**SectionRequest:**

```json
{
  "parentId": null,
  "title": "Chapter 1: Introduction",
  "description": "Overview section",
  "sortOrder": 0
}
```

Sections support arbitrary nesting — set `parentId` to a section UUID to create a child.
The tree endpoint returns a nested JSON structure with `children` arrays.

### Documents

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/documents?project=` | DocumentRequest | 201 | Create document |
| GET | `/documents?project=` | — | 200 | List documents |
| GET | `/documents/{id}` | — | 200 | Get document |
| PUT | `/documents/{id}` | UpdateDocumentRequest | 200 | Update document |
| DELETE | `/documents/{id}` | — | 204 | Delete document |

**DocumentRequest:**

```json
{
  "title": "System Requirements Specification",
  "version": "1.0.0",
  "description": "Top-level SRS document"
}
```

### Architecture Decision Records

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/adrs?project=` | AdrRequest | 201 | Create ADR |
| GET | `/adrs?project=` | — | 200 | List ADRs |
| GET | `/adrs/{id}` | — | 200 | Get ADR by UUID |
| GET | `/adrs/uid/{uid}?project=` | — | 200 | Get ADR by UID |
| PUT | `/adrs/{id}` | UpdateAdrRequest | 200 | Update ADR (partial) |
| DELETE | `/adrs/{id}` | — | 204 | Delete ADR |
| PUT | `/adrs/{id}/status` | `{ "status": "ACCEPTED" }` | 200 | Transition status |
| GET | `/adrs/{id}/requirements` | — | 200 | Get linked requirements (reverse traceability) |

**AdrRequest:**

```json
{
  "uid": "ADR-018",
  "title": "AWS EC2 Deployment",
  "decisionDate": "2026-03-15",
  "context": "We need a deployment target for the application",
  "decision": "Deploy to AWS EC2 with Docker",
  "consequences": "Simple, cost-effective, but single-instance"
}
```

**Status transitions:** PROPOSED → ACCEPTED → DEPRECATED | SUPERSEDED

### Operational Assets

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/assets?project=` | AssetRequest | 201 | Create asset |
| GET | `/assets?project=&type=` | — | 200 | List assets (optional type filter) |
| GET | `/assets/{id}` | — | 200 | Get asset by UUID |
| GET | `/assets/uid/{uid}?project=` | — | 200 | Get asset by UID |
| PUT | `/assets/{id}` | UpdateAssetRequest | 200 | Update asset (partial) |
| DELETE | `/assets/{id}` | — | 204 | Delete asset (cascade deletes relations) |
| POST | `/assets/{id}/archive` | — | 200 | Archive (soft-delete) asset |

**AssetRequest:**

```json
{
  "uid": "ASSET-001",
  "name": "Production Database",
  "description": "Primary PostgreSQL instance",
  "assetType": "DATABASE"
}
```

Asset types: `APPLICATION`, `SERVICE`, `SYSTEM`, `DATABASE`, `NETWORK`, `HOST`, `CONTAINER`, `IDENTITY`, `DATA_STORE`, `ENDPOINT`, `INTEGRATION`, `WORKLOAD`, `THIRD_PARTY`, `BOUNDARY`, `OTHER`

### Asset Relations

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/assets/{id}/relations` | AssetRelationRequest | 201 | Create typed relation |
| PUT | `/assets/{id}/relations/{relationId}` | UpdateAssetRelationRequest | 200 | Update relation metadata |
| GET | `/assets/{id}/relations` | — | 200 | List relations (incoming + outgoing) |
| DELETE | `/assets/{id}/relations/{relationId}` | — | 204 | Delete relation |

**AssetRelationRequest:**

```json
{
  "targetId": "uuid",
  "relationType": "DEPENDS_ON",
  "description": "Observed runtime dependency",
  "sourceSystem": "AWS_CONFIG",
  "externalSourceId": "cfg-123",
  "collectedAt": "2026-04-01T12:00:00Z",
  "confidence": "0.80"
}
```

**UpdateAssetRelationRequest:**

```json
{
  "description": "Refined runtime dependency",
  "sourceSystem": "CMDB",
  "externalSourceId": "cmdb-789",
  "collectedAt": "2026-04-02T12:00:00Z",
  "confidence": "0.95"
}
```

**AssetRelationResponse fields:** `id`, `sourceId`, `sourceUid`, `targetId`, `targetUid`, `relationType`, `description`, `sourceSystem`, `externalSourceId`, `collectedAt`, `confidence`, `createdAt`, `updatedAt`

Relation types: `CONTAINS`, `DEPENDS_ON`, `COMMUNICATES_WITH`, `TRUST_BOUNDARY`, `SUPPORTS`, `ACCESSES`, `DATA_FLOW`

### Asset Links (Cross-Entity Linking)

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/assets/{id}/links` | AssetLinkRequest | 201 | Link asset to a requirement, control, or other entity |
| GET | `/assets/{id}/links?target_type=` | — | 200 | List links (optional target type filter) |
| DELETE | `/assets/{id}/links/{linkId}` | — | 204 | Delete link |
| GET | `/assets/links/by-target?target_type=&target_identifier=&project=` | — | 200 | Reverse lookup: find assets linked to a target |

**AssetLinkRequest:**

```json
{
  "targetType": "REQUIREMENT",
  "targetIdentifier": "GC-M010",
  "linkType": "IMPLEMENTS",
  "targetUrl": "https://example.com/req/GC-M010",
  "targetTitle": "Operational Asset Entity"
}
```

Target types: `REQUIREMENT`, `CONTROL`, `RISK_SCENARIO`, `THREAT_MODEL_ENTRY`, `FINDING`, `EVIDENCE`, `AUDIT`, `EXTERNAL`

Link types: `IMPLEMENTS`, `MITIGATES`, `SUBJECT_OF`, `EVIDENCED_BY`, `GOVERNED_BY`, `DEPENDS_ON`, `ASSOCIATED`

### Asset Topology

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| GET | `/assets/topology/cycles?project=` | — | 200 | Detect cycles in asset graph |
| GET | `/assets/{id}/topology/impact` | — | 200 | Multi-hop impact analysis |
| POST | `/assets/topology/subgraph?project=` | SubgraphRequest | 200 | Extract connected subgraph |

**SubgraphRequest:**

```json
{
  "rootUids": ["ASSET-001", "ASSET-002"]
}
```

**AssetSubgraphResponse:**

```json
{
  "assets": [{ "id": "uuid", "uid": "ASSET-001", "name": "...", "..." : "..." }],
  "relations": [{ "id": "uuid", "sourceUid": "ASSET-001", "targetUid": "ASSET-002", "relationType": "DEPENDS_ON" }]
}
```

### Quality Gates

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/quality-gates?project=` | QualityGateRequest | 201 | Create quality gate |
| GET | `/quality-gates?project=` | — | 200 | List quality gates |
| GET | `/quality-gates/{id}` | — | 200 | Get quality gate |
| PUT | `/quality-gates/{id}` | UpdateQualityGateRequest | 200 | Update quality gate |
| DELETE | `/quality-gates/{id}` | — | 204 | Delete quality gate |
| POST | `/quality-gates/evaluate?project=` | — | 200 | Evaluate all enabled gates (CI/CD) |

**QualityGateRequest:**

```json
{
  "name": "Test Coverage Gate",
  "description": "Minimum 80% of ACTIVE requirements must have TESTS link",
  "metricType": "COVERAGE",
  "metricParam": "TESTS",
  "scopeStatus": "ACTIVE",
  "operator": "GTE",
  "threshold": 80.0
}
```

- `metricType`: `COVERAGE` (% with link type), `ORPHAN_COUNT`, `COMPLETENESS` (issue count)
- `metricParam`: Required for `COVERAGE` — a LinkType (`IMPLEMENTS`, `TESTS`, `DOCUMENTS`, `CONSTRAINS`, `VERIFIES`)
- `scopeStatus`: Filter requirements by status. Omit to check all non-archived
- `operator`: `GTE` (>=), `LTE` (<=), `EQ` (==), `GT` (>), `LT` (<)

**QualityGateEvaluationResponse** (`POST /quality-gates/evaluate`):

```json
{
  "projectIdentifier": "ground-control",
  "timestamp": "2026-03-24T06:00:00Z",
  "passed": false,
  "totalGates": 2,
  "passedCount": 1,
  "failedCount": 1,
  "gates": [
    {
      "gateId": "uuid",
      "gateName": "Test Coverage Gate",
      "metricType": "COVERAGE",
      "metricParam": "TESTS",
      "scopeStatus": "ACTIVE",
      "operator": "GTE",
      "threshold": 80.0,
      "actualValue": 65.0,
      "passed": false
    }
  ]
}
```

### Graph

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/admin/graph/materialize` | — | 200 | Materialize graph (AGE) |
| GET | `/graph/ancestors/{uid}?depth=N` | — | 200 | Ancestor UIDs |
| GET | `/graph/descendants/{uid}?depth=N` | — | 200 | Descendant UIDs |
| GET | `/graph/visualization?entityTypes=X,Y` | — | 200 | Full graph (filterable by entity type) |
| GET | `/graph/subgraph?roots=X&entityTypes=Y` | — | 200 | Subgraph (filterable by entity type) |
| GET | `/graph/paths?source=X&target=Y` | — | 200 | All paths between two UIDs (with edges) |

`entityTypes` is an optional comma-separated list (e.g. `REQUIREMENT`). When omitted, all entity types are returned. Each node includes an `entityType` field.

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

### Export

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| GET | `/export/requirements?project=&format=csv` | — | 200 | Export requirements as CSV, Excel, or PDF |
| POST | `/export/sweep?project=&format=csv` | — | 200 | Run sweep and export as CSV, Excel, or PDF |
| GET | `/export/document/{documentId}?format=sdoc` | — | 200 | Export document (sdoc, html, pdf, or reqif) |

The `format` query parameter accepts `csv` (default), `xlsx`, or `pdf`. Responses include
`Content-Disposition: attachment` headers with a generated filename.

Content types by format:
- `csv` — `text/csv`
- `xlsx` — `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- `pdf` — `application/pdf`

**Requirements export** includes: UID, title, statement, rationale, type, priority,
status, wave, traceability links, timestamps. Excel format adds a second "Traceability"
sheet with the full link matrix.

**Sweep export** includes: summary, cycles, orphans, coverage gaps, cross-wave violations,
consistency violations, completeness, and quality gate results. Excel format uses one
sheet per analysis category.

### Import / Sync

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/admin/import/strictdoc` | multipart/form-data | 200 | Import .sdoc file |
| POST | `/admin/import/reqif` | multipart/form-data | 200 | Import .reqif file |
| POST | `/admin/sync/github?owner=X&repo=Y` | — | 200 | Sync GitHub issues |
| POST | `/admin/sync/github/prs?owner=X&repo=Y` | — | 200 | Sync GitHub pull requests |

StrictDoc import creates requirements, relations, traceability links, and preserves the
document structure (document, sections, text blocks). The response includes all counters:
`requirementsParsed`, `requirementsCreated`, `requirementsUpdated`, `relationsCreated`,
`relationsSkipped`, `traceabilityLinksCreated`, `traceabilityLinksSkipped`,
`documentsCreated`, `sectionsCreated`, `sectionContentsCreated`, `errors`.

### Verification Results

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/verification-results` | VerificationResultRequest | 201 | Create verification result |
| GET | `/verification-results` | — | 200 | List verification results |
| GET | `/verification-results/{id}` | — | 200 | Get verification result by UUID |
| PUT | `/verification-results/{id}` | UpdateVerificationResultRequest | 200 | Update verification result |
| DELETE | `/verification-results/{id}` | — | 204 | Delete verification result |

All endpoints accept an optional `project` query parameter.

**Filters on GET list:**
- `requirement_id` (UUID) — filter by requirement
- `prover` (string) — filter by verifier tool identifier
- `result` (enum) — PROVEN, REFUTED, TIMEOUT, UNKNOWN, ERROR

**VerificationResultRequest fields:** `prover` (required), `result` (required),
`assuranceLevel` (required, L0-L3), `verifiedAt` (required, ISO 8601), `targetId`
(optional, traceability link UUID), `requirementId` (optional), `property` (optional),
`evidence` (optional, JSON object), `expiresAt` (optional, ISO 8601).

### Plugins

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| GET | `/plugins` | — | 200 | List all registered plugins |
| GET | `/plugins/{name}` | — | 200 | Get plugin by name |
| POST | `/plugins` | RegisterPluginRequest | 201 | Register a dynamic plugin |
| DELETE | `/plugins/{name}` | — | 204 | Unregister a dynamic plugin |

All endpoints accept an optional `project` query parameter.

**Filters on GET list:**
- `type` (enum) — PACK_HANDLER, REGISTRY_BACKEND, VALIDATOR, POLICY_HOOK, VERIFIER, EMBEDDING_PROVIDER, GRAPH_CONTRIBUTOR, CUSTOM
- `capability` (string) — filter by capability tag
- `project` (string) — filter dynamic plugins by project

**RegisterPluginRequest fields:** `name` (required, max 100), `version` (required, max 50),
`type` (required, PluginType enum), `description` (optional), `capabilities` (optional, string set),
`metadata` (optional, JSON object).

### Control Packs

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/control-packs/install` | InstallControlPackRequest | 201 | Install a control pack (idempotent) |
| POST | `/control-packs/upgrade` | UpgradeControlPackRequest | 200 | Upgrade to a new version |
| GET | `/control-packs` | — | 200 | List installed packs |
| GET | `/control-packs/{packId}` | — | 200 | Get pack by identifier |
| PUT | `/control-packs/{packId}/deprecate` | — | 200 | Deprecate a pack |
| DELETE | `/control-packs/{packId}` | — | 204 | Remove a pack |
| GET | `/control-packs/{packId}/entries` | — | 200 | List pack entries |
| GET | `/control-packs/{packId}/entries/{entryUid}` | — | 200 | Get a pack entry |
| POST | `/control-packs/{packId}/entries/{entryUid}/overrides` | CreateControlPackOverrideRequest | 201 | Create field override |
| GET | `/control-packs/{packId}/entries/{entryUid}/overrides` | — | 200 | List overrides |
| DELETE | `/control-packs/{packId}/entries/{entryUid}/overrides/{id}` | — | 204 | Delete override |

All endpoints accept an optional `project` query parameter.

**InstallControlPackRequest fields:** `packId` (required, max 200), `version` (required, max 50),
`publisher` (optional), `description` (optional), `sourceUrl` (optional), `checksum` (optional),
`compatibility` (optional, JSON object), `packMetadata` (optional, JSON object),
`entries` (required, array of control definitions with `uid`, `title`, `controlFunction`, plus optional
`description`, `objective`, `owner`, `implementationScope`, `methodologyFactors`, `effectiveness`,
`category`, `source`, `implementationGuidance`, `expectedEvidence`, `frameworkMappings`).

**UpgradeControlPackRequest fields:** Same as install, but uses `newVersion` instead of `version`.

**CreateControlPackOverrideRequest fields:** `fieldName` (required — title, description, objective,
controlFunction, owner, implementationScope, or category), `overrideValue` (optional; title
must be non-blank), `reason` (optional, max 500).

**Lifecycle states:** INSTALLED → UPGRADED → DEPRECATED → REMOVED.

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

### Pack Registry

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/pack-registry` | RegisterPackRequest | 201 | Register pack version in catalog |
| GET | `/pack-registry` | — | 200 | List registry entries (optional `packType` filter) |
| GET | `/pack-registry/{packId}` | — | 200 | List versions of a pack |
| GET | `/pack-registry/{packId}/{version}` | — | 200 | Get specific pack version |
| PUT | `/pack-registry/{packId}/{version}` | UpdatePackRegistryEntryRequest | 200 | Update pack metadata |
| PUT | `/pack-registry/{packId}/{version}/withdraw` | — | 200 | Withdraw pack version |
| DELETE | `/pack-registry/{packId}/{version}` | — | 204 | Delete pack version |
| POST | `/pack-registry/resolve` | ResolvePackRequest | 200 | Resolve version from registry |
| POST | `/pack-registry/check-compatibility` | ResolvePackRequest | 200 | Check pack compatibility |

### Trust Policies

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/trust-policies` | CreateTrustPolicyRequest | 201 | Create trust policy |
| GET | `/trust-policies` | — | 200 | List trust policies |
| GET | `/trust-policies/{id}` | — | 200 | Get trust policy |
| PUT | `/trust-policies/{id}` | UpdateTrustPolicyRequest | 200 | Update trust policy |
| DELETE | `/trust-policies/{id}` | — | 204 | Delete trust policy |

### Pack Install Records

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/pack-install-records/install` | InstallPackRequest | 201 | Install pack via registry with trust evaluation |
| POST | `/pack-install-records/upgrade` | InstallPackRequest | 200 | Upgrade pack via registry with trust evaluation |
| GET | `/pack-install-records` | — | 200 | List install records (optional `packId` filter) |
| GET | `/pack-install-records/{id}` | — | 200 | Get install record |

## Interactive Docs

- Swagger UI: `http://localhost:8000/api/docs`
- OpenAPI JSON: `http://localhost:8000/api/openapi.json`
