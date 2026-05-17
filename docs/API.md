# Ground Control REST API

REST API for direct HTTP usage. Pre-alpha. The `dev` profile disables
authentication for local work; **production deployments require it**
(see [ADR-026](../architecture/adrs/026-rest-api-access-control.md) and
[`docs/deployment/DEPLOYMENT.md`](deployment/DEPLOYMENT.md)).

## Authentication

When `groundcontrol.security.enabled=true`:

- Send `Authorization: Bearer <token>` on every `/api/v1/**` request.
- `/api/v1/admin/**`, `/api/v1/embeddings/**`, `/api/v1/analysis/sweep/**`,
  and `/api/v1/pack-registry/**` require a token whose configured `role`
  is `ADMIN`. Other `/api/v1/**` paths accept any authenticated token.
- `/actuator/health` and `/actuator/info` are anonymous; the OpenAPI
  schema is gated by `groundcontrol.security.openapi-public`.
- An optional CIDR allowlist (`groundcontrol.security.ip-allowlist`)
  rejects out-of-range source addresses with 403 `access_denied` before
  the token check runs.

Errors use the standard envelope:

```
401 → {"error": {"code": "authentication_required", "message": "..."}}
403 → {"error": {"code": "access_denied", "message": "..."}}
```

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
| GET | `/requirements/traceability/by-artifact` | — | 200 | Reverse lookup: find links by artifact |
| DELETE | `/requirements/{id}/traceability/{linkId}` | — | 204 / 404 | Delete traceability link. Returns 404 if `linkId` does not belong to `id`. |

`GET /requirements/traceability/by-artifact` accepts query parameters:

| Parameter | Type | Description |
|-----------|------|-------------|
| `artifactType` | enum | GITHUB_ISSUE, PULL_REQUEST, CODE_FILE, ADR, CONFIG, POLICY, TEST, SPEC, PROOF, DOCUMENTATION, RISK_SCENARIO, CONTROL |
| `artifactIdentifier` | string | Artifact identifier (e.g. repo-relative path, issue number, ADR UID) |

### Audit History

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| GET | `/requirements/{id}/history` | — | 200 | Requirement revision history |
| GET | `/requirements/{id}/relations/{relationId}/history` | — | 200 / 404 | Relation revision history. Returns 404 if `relationId` does not belong to `id` (the requirement is neither the source nor the target of the relation). |
| GET | `/requirements/{id}/traceability/{linkId}/history` | — | 200 / 404 | Traceability link revision history. Returns 404 if `linkId` does not belong to `id`. |
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
| GET | `/analysis/status-drift` | — | 200 | Flag DRAFT requirements that have implementation evidence |
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

`GET /analysis/status-drift` flags `DRAFT` requirements that carry independent
evidence of implementation or design completion (read-only — it never transitions
requirements or creates links). Query parameters:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `project` | string | auto-resolved | Project identifier |
| `minimumConfidence` | enum (`HIGH` \| `MEDIUM` \| `LOW`) | `MEDIUM` | Lowest confidence band to report (default reports `HIGH` and `MEDIUM`; `LOW` is opt-in) |

**StatusDriftResponse** (`GET /analysis/status-drift`):

```json
{
  "draftRequirementsScanned": 14,
  "minimumConfidence": "MEDIUM",
  "findings": [
    {
      "uid": "GC-T010",
      "title": "Risk Assessment Result Entity",
      "confidence": "HIGH",
      "strongestSignal": "IMPLEMENTS_LINK_ON_DRAFT",
      "evidence": [
        {
          "signal": "IMPLEMENTS_LINK_ON_DRAFT",
          "confidence": "HIGH",
          "artifactType": "GITHUB_ISSUE",
          "artifactIdentifier": "826",
          "artifactTitle": "GC-T010: Risk Assessment Result Entity",
          "artifactUrl": "https://github.com/KeplerOps/Ground-Control/issues/826",
          "detail": "IMPLEMENTS link on a DRAFT requirement"
        }
      ]
    }
  ]
}
```

Evidence signals, strongest first: `IMPLEMENTS_LINK_ON_DRAFT` (`HIGH`);
`ACCEPTED_ADR_DOCUMENTS_LINK`, `LINKED_GITHUB_ISSUE`, `LINKED_PULL_REQUEST`
(`MEDIUM`); `LINKED_CODE_ARTIFACT`, `LINKED_DOC_ARTIFACT` (`LOW`). All signals are
derived from the requirement's own project — its canonical traceability links and
accepted ADR records — so the endpoint never reads the project-unscoped GitHub
issue/PR sync tables or the filesystem. A finding's `confidence` is the strongest
band across its `evidence`. Status drift is also surfaced inside
`POST /analysis/sweep` as a new problem class (`statusDrift` array, counted in
`totalProblems`).

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
| GET | `/assets?project=&type=&owner=&steward=&environment=&criticality=&scope=` | — | 200 | List assets (any combination of filters is optional) |
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
  "assetType": "DATABASE",
  "owner": "alice@example.com",
  "steward": "platform-sre",
  "environment": "PRODUCTION",
  "criticality": "CRITICAL",
  "businessContext": "Primary system of record for billing; PCI-DSS in scope.",
  "scopeDesignation": "IN_SCOPE"
}
```

`owner`, `steward`, and `businessContext` are free-text labels (≤ 200 chars on `owner`/`steward`; `businessContext` is `TEXT`). All six GC-M012 metadata fields are optional on `AssetRequest` and on `UpdateAssetRequest`. On the update path, `null` / absent means "leave field unchanged" (mirrors the existing `name`/`description`/`assetType` null-means-unchanged semantics). To reset a previously-designated metadata field back to NULL ("not designated"), send the paired clear flag — `clearOwner`, `clearSteward`, `clearEnvironment`, `clearCriticality`, `clearBusinessContext`, or `clearScopeDesignation` — as `true`. The clear flag wins over a same-payload assignment so the wire semantics stay unambiguous (the assign loses). This mirrors the `clearRootCauseAnalysis` / `clearOwner` / `clearDueDate` pattern on `UpdateFindingRequest`.

Asset types: `APPLICATION`, `SERVICE`, `SYSTEM`, `DATABASE`, `NETWORK`, `HOST`, `CONTAINER`, `IDENTITY`, `DATA_STORE`, `ENDPOINT`, `INTEGRATION`, `WORKLOAD`, `THIRD_PARTY`, `BOUNDARY`, `OTHER`

Asset criticality (GC-M012): `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`. Distinct from finding severity, risk level, control effectiveness, and assurance confidence per ADR-012 / `docs/CODING_STANDARDS.md`.

Asset environment (GC-M012): `PRODUCTION`, `STAGING`, `DEVELOPMENT`, `TEST`, `NON_PRODUCTION`, `OTHER`. `NON_PRODUCTION` is the umbrella value for assets that pre-date the more specific environment vocabulary.

Asset scope designation (GC-M012): `IN_SCOPE`, `OUT_OF_SCOPE`. Two-state explicit; the absence of either (NULL) means "not yet designated" — distinct from `archivedAt` (lifecycle), `quality_gate.scopeStatus`, control `implementationScope`, and risk `assetScopeSummary`.

List filters route through `OperationalAssetRepository.findByProjectIdAndArchivedAtIsNullAndFilters` so any combination of `type` / `owner` / `steward` / `environment` / `criticality` / `scope` query parameters is honored in a single JPQL pass; risk, control, audit, and reporting workflows consume this same surface rather than inventing per-workflow lookups.

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

Control-pack installation and upgrade are registry-backed operations only. Register
or import a `CONTROL_PACK` in `/pack-registry`, then use `/pack-install-records/install`
or `/pack-install-records/upgrade` so resolution, trust evaluation, and audit recording
cannot be bypassed.

**CreateControlPackOverrideRequest fields:** `fieldName` (required — title, description, objective,
controlFunction, owner, implementationScope, or category), `overrideValue` (optional; title
must be non-blank), `reason` (optional, max 500).

**Lifecycle states:** INSTALLED → UPGRADED → DEPRECATED → REMOVED.

### Threat Models

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/threat-models` | ThreatModelRequest | 201 | Create threat model entry |
| GET | `/threat-models` | — | 200 | List threat models for a project |
| GET | `/threat-models/{id}` | — | 200 | Get threat model by UUID |
| GET | `/threat-models/uid/{uid}` | — | 200 | Get threat model by UID |
| PUT | `/threat-models/{id}` | UpdateThreatModelRequest | 200 | Update mutable fields |
| DELETE | `/threat-models/{id}` | — | 204 | Delete threat model (cascades to links) |
| PUT | `/threat-models/{id}/status` | `{"status": "ACTIVE"}` | 200 | Transition lifecycle status |
| POST | `/threat-models/{id}/links` | ThreatModelLinkRequest | 201 | Create threat-model link |
| GET | `/threat-models/{id}/links` | — | 200 | List links for a threat model |
| DELETE | `/threat-models/{id}/links/{linkId}` | — | 204 | Delete threat-model link |

All endpoints accept an optional `project` query parameter. When omitted, the request
auto-resolves to the single project in single-project deployments. In multi-project
deployments the parameter is required and the request returns 422 `project_required`
if absent.

Threat models are a separate aggregate from risk scenarios per ADR-024. They capture
upstream security analysis (source, event, effect) and do not carry quantified risk,
treatment, or governance state.

`DELETE /threat-models/{id}` is rejected with 409 `threat_model_referenced` while any
`AssetLink` (`THREAT_MODEL_ENTRY` target) or `RiskScenarioLink` (`THREAT_MODEL` target)
still references the threat model. The conflict envelope's `detail` block lists the
referencing asset and scenario UIDs so callers can clean them up before retrying.

**ThreatModelRequest fields:** `uid` (required, max 30), `title` (required, max 200),
`threatSource` (required), `threatEvent` (required), `effect` (required), `stride`
(optional, STRIDE enum: SPOOFING, TAMPERING, REPUDIATION, INFORMATION_DISCLOSURE,
DENIAL_OF_SERVICE, ELEVATION_OF_PRIVILEGE), `narrative` (optional analyst context,
non-authoritative).

**UpdateThreatModelRequest fields:** `title`, `threatSource`, `threatEvent`, `effect`,
`stride`, `narrative`, `clearStride` (boolean), `clearNarrative` (boolean). Only fields
present in the request body are updated. Required fields (`title`, `threatSource`,
`threatEvent`, `effect`) reject blank strings server-side with 422 `validation_error`
when present. Optional fields (`stride`, `narrative`) cannot be cleared by sending
`null` (which means "no change") — set `clearStride` or `clearNarrative` to `true` to
explicitly null them. When a `clear*` flag is true, any value supplied in the
corresponding field is ignored.

**ThreatModelLinkRequest fields:** `targetType` (required, ThreatModelLinkTargetType
enum), `targetEntityId` (UUID, for internal first-class targets), `targetIdentifier`
(string max 500, for external / not-yet-modeled targets), `linkType` (required,
ThreatModelLinkType enum), `targetUrl` (optional, max 2000), `targetTitle` (optional,
max 255).

**Internal target types (require `targetEntityId`, resolved project-scoped):** ASSET
(includes boundaries via `AssetType.BOUNDARY`), REQUIREMENT, CONTROL, RISK_SCENARIO,
OBSERVATION, RISK_ASSESSMENT_RESULT, VERIFICATION_RESULT, FINDING (per GC-H009 —
governed vulnerability/scan/pentest finding records).

**External target types (require `targetIdentifier`):** ARCHITECTURE_MODEL (e.g. C4
source or Structurizr DSL, per ADR-011), CODE (repo-relative path), ISSUE (GitHub
issue or PR number), EVIDENCE (external evidence reference), EXTERNAL (catch-all —
also covers CVE identifiers, scanner finding IDs, and pentest report IDs that have
not been ingested as first-class `Finding` records).

**Link types:** AFFECTS (threat affects an asset or boundary), EXPLOITS (threat
exploits a requirement or condition), MITIGATED_BY (threat is mitigated by a control),
ASSESSED_IN (threat feeds a risk scenario or assessment), OBSERVED_IN (threat
evidenced by an observation, verification, or vulnerability finding), DOCUMENTED_IN
(threat documented in an architecture model, code, or issue), ASSOCIATED (generic
association).

**Lifecycle states:** DRAFT → ACTIVE → ARCHIVED (and DRAFT → ARCHIVED directly).

### Findings

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/findings` | FindingRequest | 201 | Create finding |
| GET | `/findings` | — | 200 | List findings for a project |
| GET | `/findings/{id}` | — | 200 | Get finding by UUID |
| GET | `/findings/uid/{uid}` | — | 200 | Get finding by UID |
| PUT | `/findings/{id}` | UpdateFindingRequest | 200 | Update mutable fields |
| DELETE | `/findings/{id}` | — | 204 | Delete finding (cascades to links) |
| PUT | `/findings/{id}/status` | `{"status": "REMEDIATION_IN_PROGRESS"}` | 200 | Transition lifecycle status |
| POST | `/findings/{id}/links` | FindingLinkRequest | 201 | Create finding link |
| GET | `/findings/{id}/links` | — | 200 | List links for a finding |
| DELETE | `/findings/{id}/links/{linkId}` | — | 204 | Delete finding link |

All endpoints accept an optional `project` query parameter (same semantics as the
Threat Model endpoints above).

Findings are a separate aggregate from observations, controls, and the risk-management
cluster per ADR-038. They capture governed GRC issues (audit findings, control
deficiencies, policy violations, vulnerabilities, exception escalations) and own the
remediation lifecycle. Affected controls, risks, assets, observations, evidence,
audits, and remediation plans are represented as outbound `FindingLink` edges.

`DELETE /findings/{id}` is rejected with 409 `finding_referenced` while any
`AssetLink` (`FINDING` target), `ControlLink` (`FINDING` target), or `RiskScenarioLink`
(`FINDING` target) still references the finding by `targetEntityId`. The conflict
envelope's `detail` block lists the referencing asset, control, and scenario UIDs so
callers can clean them up before retrying.

**FindingRequest fields:** `uid` (required, max 30), `title` (required, max 200),
`findingType` (required, enum: AUDIT_FINDING, CONTROL_DEFICIENCY, POLICY_VIOLATION,
VULNERABILITY, EXCEPTION_ESCALATION), `severity` (required, enum: CRITICAL, HIGH,
MEDIUM, LOW, INFORMATIONAL), `description` (required), `rootCauseAnalysis` (optional),
`owner` (optional, max 100), `dueDate` (optional, ISO-8601 date).

**UpdateFindingRequest fields:** `title`, `findingType`, `severity`, `description`,
`rootCauseAnalysis`, `owner`, `dueDate`, `clearRootCauseAnalysis` (boolean),
`clearOwner` (boolean), `clearDueDate` (boolean). Only fields present in the request
body are updated. Required fields (`title`, `description`) reject blank strings
server-side with 422 `validation_error` when present. Optional fields cannot be
cleared by sending `null` (which means "no change") — set the corresponding `clear*`
flag to `true` to explicitly null them. When a `clear*` flag is true, any value
supplied in the corresponding field is ignored.

**FindingLinkRequest fields:** `targetType` (required, FindingLinkTargetType enum),
`targetEntityId` (UUID, for internal first-class targets), `targetIdentifier` (string
max 500, for external / not-yet-modeled targets), `linkType` (required,
FindingLinkType enum), `targetUrl` (optional, max 2000), `targetTitle` (optional, max
255).

**Internal target types (require `targetEntityId`, resolved project-scoped):**
CONTROL, RISK_SCENARIO, ASSET, OBSERVATION.

**External target types (require `targetIdentifier`):** OPERATIONAL_ARTIFACT (generic
artifact reference, per ADR-011), EVIDENCE (external evidence reference, e.g.
`s3://evidence/...`), AUDIT (audit identifier), REMEDIATION_PLAN (remediation plan
identifier), EXTERNAL (catch-all).

**Link types:** AFFECTS (finding affects an entity), CAUSED_BY (finding is caused by
the linked entity), MITIGATED_BY (finding is mitigated by a control or plan),
EVIDENCED_BY (finding is evidenced by a referenced artifact), OBSERVED_IN (finding
was observed in an audit, observation, or evidence record), REMEDIATED_BY (finding is
remediated by a plan or control), ASSOCIATED (generic association).

**Lifecycle states:** OPEN → REMEDIATION_IN_PROGRESS → REMEDIATION_COMPLETE →
VERIFIED_CLOSED. `REMEDIATION_COMPLETE` can transition back to
`REMEDIATION_IN_PROGRESS` when verification rejects the claimed remediation.
`VERIFIED_CLOSED` is terminal — reopening a verified-closed finding creates a new
finding rather than reanimating the closed record.

### Control Tests (GC-I012)

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/control-tests` | ControlTestRequest | 201 | Create a control test evidence row |
| GET | `/control-tests` | — | 200 | List control tests for a project (optional `controlId` filter) |
| GET | `/control-tests/{id}` | — | 200 | Get control test by UUID |
| PUT | `/control-tests/{id}` | UpdateControlTestRequest | 200 | Update mutable fields |
| DELETE | `/control-tests/{id}` | — | 204 | Delete the control test row |

All endpoints accept the same optional `project` query parameter as the rest of `/api/v1/**`.
The control test is the durable, audited evidence record for one execution of a test plan
against a {@link Control}; it is not the same thing as a `ControlEffectivenessAssessment`
(which is a rating, not an execution). See ADR-039.

**ControlTestRequest fields:** `controlId` (required UUID, must belong to the same project),
`uid` (required, max 50), `methodology` (required, ControlTestMethodology enum: INQUIRY,
OBSERVATION, INSPECTION, RE_PERFORMANCE — PCAOB AS 2201 vocabulary), `testSteps` (required
TEXT), `expectedResults` (required TEXT), `actualResults` (required TEXT), `conclusion`
(required, ControlTestConclusion enum: EFFECTIVE, INEFFECTIVE, NOT_TESTED), `testerIdentity`
(required, max 200 — domain provenance; does **not** replace the authenticated audit actor),
`testDate` (required LocalDate, `@PastOrPresent`), `notes` (optional TEXT).

**UpdateControlTestRequest fields:** `methodology`, `testSteps`, `expectedResults`,
`actualResults`, `conclusion`, `testerIdentity`, `testDate`, `notes` — all optional; only
fields present in the request body are updated. `controlId` and `uid` are create-only
(updates ignore them).

### Control Effectiveness Assessments (GC-I013)

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/control-effectiveness-assessments` | ControlEffectivenessAssessmentRequest | 201 | Create an effectiveness rating row |
| GET | `/control-effectiveness-assessments` | — | 200 | List assessments for a project (optional `controlId` filter) |
| GET | `/control-effectiveness-assessments/{id}` | — | 200 | Get assessment by UUID |
| PUT | `/control-effectiveness-assessments/{id}` | UpdateControlEffectivenessAssessmentRequest | 200 | Update mutable fields |
| DELETE | `/control-effectiveness-assessments/{id}` | — | 204 | Delete the assessment row |

The assessment is the durable rating record. Design and operating effectiveness are stored
as separate fields because a control can be well-designed but poorly operated, or vice versa
(SOC 2 Type II / SOX testing convention). `operatingEffectiveness` is the stable, audited
read target that future GC-T003 risk-scoring code consumes; this PR does not perform the
residual-risk computation itself. See ADR-039.

**ControlEffectivenessAssessmentRequest fields:** `controlId` (required UUID, same project),
`uid` (required, max 50), `designEffectiveness` (required, ControlEffectivenessRating enum:
EFFECTIVE, PARTIALLY_EFFECTIVE, INEFFECTIVE), `operatingEffectiveness` (required, same enum),
`assessedAt` (required LocalDate, `@PastOrPresent`), `assessor` (required, max 200 — domain
provenance), `rationale` (optional TEXT), `notes` (optional TEXT), `supportingTestIds` (optional
list of `ControlTest` UUIDs that support this assessment's operating-effectiveness judgment;
every ID must resolve to a `ControlTest` belonging to the same control as the assessment;
duplicates are de-duplicated; null elements rejected with 422).

**UpdateControlEffectivenessAssessmentRequest fields:** `designEffectiveness`,
`operatingEffectiveness`, `assessedAt`, `assessor`, `rationale`, `notes`, `supportingTestIds`
— all optional; `controlId` and `uid` are create-only. A non-null `supportingTestIds` replaces
the existing list wholesale; pass `null` to leave it unchanged or an empty list to clear it.

**Response includes `supportingTestIds`** as a `List<UUID>`. The graph projection emits one
`SUPPORTED_BY` edge from the assessment to each `ControlTest` listed (plus the standard
`OF_CONTROL` edge to the parent control); edges pointing at non-resolving tests are skipped to
keep AGE materialization safe. `ControlTest` deletion is rejected with HTTP 409
`control_test_referenced` while any assessment still references the test.

### Test Cases (TC-001 / ADR-040)

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/test-cases` | TestCaseRequest | 201 | Create a project-scoped test-case definition |
| GET | `/test-cases` | — | 200 | List test cases in a project (ordered by `createdAt DESC`) |
| GET | `/test-cases/{id}` | — | 200 | Get a test case by UUID |
| GET | `/test-cases/uid/{uid}` | — | 200 | Get a test case by project-scoped UID |
| PUT | `/test-cases/{id}` | UpdateTestCaseRequest | 200 | Update mutable fields (null = no change) |
| PUT | `/test-cases/{id}/status` | TestCaseStatusTransitionRequest | 200 | Transition the lifecycle status |
| DELETE | `/test-cases/{id}` | — | 204 | Delete the test case |

The `TestCase` aggregate is a reusable, version-controlled, project-scoped definition of an
intended test. It is **definition-only** — it does not record executions, results, suites, or
defects. Those are future aggregates that reference test cases through the existing
project-scoped link patterns. See ADR-040.

**TestCaseRequest fields:** `uid` (required, max 50, unique per project), `title` (required,
max 200), `type` (required, `TestCaseType` enum: `MANUAL`, `AUTOMATED`, `HYBRID`),
`priority` (required, `TestCasePriority` enum: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`),
`description` (optional TEXT, Markdown by convention), `preconditions` (optional TEXT),
`postconditions` (optional TEXT), `estimatedDurationSeconds` (optional non-negative `Long`).

**UpdateTestCaseRequest fields:** `title`, `type`, `priority`, `description`, `preconditions`,
`postconditions`, `estimatedDurationSeconds` — all optional with null-means-no-change. `uid`
is create-only.

**TestCaseStatusTransitionRequest fields:** `status` (required, `TestCaseStatus` enum).
Valid lifecycle transitions are `DRAFT → APPROVED | ARCHIVED`,
`APPROVED → DEPRECATED | ARCHIVED`, `DEPRECATED → APPROVED | ARCHIVED`, with `ARCHIVED`
terminal. Invalid transitions surface as HTTP 422 `invalid_status_transition`. Duplicate UID
within a project returns HTTP 409. Negative `estimatedDurationSeconds` is rejected at the DTO
layer with HTTP 422.

Rich-text fields (`description`, `preconditions`, `postconditions`) are stored as plain text
and rendered as Markdown by clients; no HTML sanitizer is wired through this surface.

### Test Case Steps (TC-002 / ADR-041)

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/test-cases/{testCaseId}/steps` | TestCaseStepRequest | 201 | Create a step in a test case |
| GET | `/test-cases/{testCaseId}/steps` | — | 200 | List steps ordered by `stepNumber` ascending |
| GET | `/test-cases/{testCaseId}/steps/{stepId}` | — | 200 | Get one step |
| PUT | `/test-cases/{testCaseId}/steps/{stepId}` | UpdateTestCaseStepRequest | 200 | Update step fields (null = no change) |
| DELETE | `/test-cases/{testCaseId}/steps/{stepId}` | — | 204 | Delete a step |

Steps are an ordered child collection of a test case. Each step carries a `stepNumber` (unique
within its test case, positive), an `action` (what to do), an `expectedResult` (what should
happen), and an optional `actualResult` (what actually happened on the latest authored pass).
Rich-text fields use the same CommonMark Markdown convention as the parent test case;
inline images use the `![alt](url)` syntax with no backend-side fetching, sanitisation, or
binary storage (see ADR-041 §Rich text and inline images).

**TestCaseStepRequest fields:** `stepNumber` (required positive `Integer`), `action` (required,
max 10000), `expectedResult` (required, max 10000), `actualResult` (optional, max 10000).

**UpdateTestCaseStepRequest fields:** `stepNumber`, `action`, `expectedResult`, `actualResult`
— all optional with null-means-no-change — plus `clearActualResult: true` to wipe the
`actualResult` to null (same partial-update convention as `UpdateTestCaseRequest`).

Duplicate `stepNumber` within a test case returns HTTP 409. Non-positive `stepNumber` and
oversize rich-text fields return HTTP 422. A step request against a test case that is not in
the resolved project returns HTTP 404. Deleting the parent test case cascade-deletes its
steps service-side so Envers captures each step's delete revision.

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

All pack registry, trust policy, and pack install record routes require an
ADMIN-role bearer token: `Authorization: Bearer <token>`. Tokens and their
audit principal names are configured under the unified
`groundcontrol.security.credentials` list (`role: ADMIN`); see ADR-026 and
the deployment env-var reference. The repo-local MCP helper forwards
`GROUND_CONTROL_PACK_REGISTRY_ADMIN_TOKEN` when set.

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| POST | `/pack-registry` | RegisterPackRequest | 201 | Register pack version in catalog |
| POST | `/pack-registry/import` | multipart/form-data | 201 | Import and register a pack from uploaded JSON |
| GET | `/pack-registry` | — | 200 | List registry entries (optional `packType` filter) |
| GET | `/pack-registry/{packId}` | — | 200 | List versions of a pack |
| GET | `/pack-registry/{packId}/{version}` | — | 200 | Get specific pack version |
| PUT | `/pack-registry/{packId}/{version}` | UpdatePackRegistryEntryRequest | 200 | Update pack metadata |
| PUT | `/pack-registry/{packId}/{version}/withdraw` | — | 200 | Withdraw pack version |
| DELETE | `/pack-registry/{packId}/{version}` | — | 204 | Delete pack version |
| POST | `/pack-registry/resolve` | ResolvePackRequest | 200 | Resolve version from registry |
| POST | `/pack-registry/check-compatibility` | ResolvePackRequest | 200 | Check pack compatibility (returns boolean) |

For large catalogs, use `POST /pack-registry/import` instead of hand-authoring a
giant JSON request body. The endpoint accepts a multipart `file` part plus an
optional JSON `options` part. Supported formats are:

- `AUTO` — detect OSCAL catalog JSON vs Ground Control manifest JSON
- `OSCAL_JSON` — treat the file as an OSCAL catalog and flatten controls into a `CONTROL_PACK`
- `GC_MANIFEST` — treat the file as a Ground Control pack manifest and register it directly

`options` may override pack metadata such as `packId`, `version`, `publisher`,
`description`, `sourceUrl`, `checksum`, `signatureInfo`, `compatibility`,
`dependencies`, `provenance`, `registryMetadata`, and
`defaultControlFunction` for imported control entries.

Example multipart call:

```sh
curl -X POST "http://localhost:8000/api/v1/pack-registry/import?project=ground-control" \
  -H "Authorization: Bearer <token>" \
  -F "file=@/path/to/catalog.json;type=application/json" \
  -F 'options={"format":"OSCAL_JSON","packId":"nist-sp800-53-rev5","version":"5.1.0","publisher":"NIST"};type=application/json'
```

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
| POST | `/pack-install-records/install` | InstallPackRequest | 201, 422 | Install pack via registry with trust evaluation |
| POST | `/pack-install-records/upgrade` | InstallPackRequest | 200, 422 | Upgrade pack via registry with trust evaluation |
| GET | `/pack-install-records` | — | 200 | List install records (optional `packId` filter) |
| GET | `/pack-install-records/{id}` | — | 200 | Get install record |

### Admin Users (ADR-037)

Browser-session lifecycle for the JDBC user store. Gated by `ROLE_ADMIN` on the
same path matrix as the rest of `/api/v1/admin/**`. Bearer agents that hold an
`ADMIN`-role token may call these endpoints too; the typical caller is the SPA
admin page operating under the signed-in operator's session.

| Method | Path | Body | Status | Purpose |
|--------|------|------|--------|---------|
| GET | `/admin/users` | — | 200 | List users (`username`, `role`, `enabled`) |
| POST | `/admin/users` | `CreateUserRequest` | 201, 409, 422 | Create user. `409 user_exists` on duplicate username; `422 validation_error` for bad username / short password. |
| PATCH | `/admin/users/{username}/role` | `{"role":"USER"\|"ADMIN"}` | 200, 404, 409, 422 | Change role. `409 last_admin` refuses demoting the last enabled admin. |
| PATCH | `/admin/users/{username}/enabled` | `{"enabled":bool}` | 200, 404, 409, 422 | Enable / disable. `409 last_admin` refuses disabling the last enabled admin. |
| DELETE | `/admin/users/{username}` | — | 204, 404, 409 | Delete user. `409 last_admin` refuses deleting the last enabled admin. |

`CreateUserRequest`: `{"username":"<lowercase, 2-64 chars, matches /^[a-z][a-z0-9._-]{1,63}$/>", "password":"<12-200 chars>", "role":"USER"\|"ADMIN"}`. Passwords are BCrypt-hashed server-side; the JSON never echoes the password back. First-admin bootstrap is out of band — see `DEPLOYMENT.md`'s Web UI login section.

For control packs, use `/pack-registry/import` or `/pack-registry` to persist the
pack definition first, then call one of these routes with the `packId` and optional
version constraint.

`RegisterPackRequest` and `UpdatePackRegistryEntryRequest` accept
`controlPackEntries` for `CONTROL_PACK` artifacts. Registry-driven install and
upgrade now materialize that stored server-side content; `InstallPackRequest`
contains only `packId` and optional `versionConstraint`. The install record
`performedBy` value is derived server-side from the authenticated admin token,
not request JSON.

When a `checksum` is supplied, the server verifies it against the canonical
pack payload and normalizes the stored value to `sha256:<hex>`. Unsigned packs
may omit `checksum`; they still produce a computed `verifiedChecksum` during
trust evaluation and install recording, but they do not become
`checksumVerified=true` by registry round-trip alone.

`signatureInfo` is optional detached signature metadata with this shape:
`algorithm` (required, one of `SHA256withRSA`, `SHA384withRSA`,
`SHA512withRSA`, `SHA256withECDSA`, `SHA384withECDSA`, `SHA512withECDSA`,
`Ed25519`, or `Ed448`),
`publicKey` (required, base64 DER or PEM-encoded X.509 public key),
`signature` (required, base64 detached signature over the canonical pack
payload), and `keyAlgorithm` (optional when it can be inferred from
`algorithm`, otherwise required). A valid signature is cryptographic evidence
only. Trust policy must use `signerTrusted`, which becomes `true` only when the
signature public key matches a configured trusted signer under
`ground-control.pack-registry.security.trusted-signers`.

Install and upgrade return `422 Unprocessable Entity` when the request is
accepted syntactically but the resolved pack is rejected or fails to apply.

Trust policy rules may match not only raw pack metadata, but also verified
integrity fields exposed by the server: `verifiedChecksum`,
`checksumVerified`, and `signerTrusted`. The `signatureVerified` field is
informational and is rejected in trust policy rules. Regex policy rules are also
disabled; use bounded operators `EQUALS`, `NOT_EQUALS`, `CONTAINS`, and
`IN_LIST`.

## Interactive Docs

- Swagger UI: `http://localhost:8000/api/docs`
- OpenAPI JSON: `http://localhost:8000/api/openapi.json`
