# Ground Control — API & Plugin Architecture Specification

**Version:** 1.0.0
**Date:** 2026-03-07

---

## 1. API Design Principles

| Principle | Implementation |
|---|---|
| **REST-first** | Standard HTTP verbs, resource-oriented URLs, JSON payloads |
| **Versioned** | URL-based versioning (`/api/v1/`) with deprecation policy |
| **Consistent** | Uniform response envelopes, error formats, pagination |
| **Discoverable** | OpenAPI 3.1 spec auto-generated and served at `/api/v1/openapi.json` |
| **Filterable** | Query parameters for filtering, sorting, field selection, includes |
| **Idempotent** | PUT/DELETE are idempotent; POST supports `Idempotency-Key` header |
| **Agent-Friendly** | Structured inputs/outputs, clear schemas, webhook support |

---

## 2. Authentication

### 2.1 Token Endpoint

```
POST /api/v1/auth/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials
&client_id=agent-001
&client_secret=xxxxx
&scope=assessments:read assessments:write
```

**Response:**
```json
{
  "access_token": "eyJhbGciOi...",
  "token_type": "bearer",
  "expires_in": 3600,
  "scope": "assessments:read assessments:write"
}
```

### 2.2 Using Tokens

All API requests include:
```
Authorization: Bearer <access_token>
X-Tenant-ID: <tenant-uuid>          (optional if single-tenant)
```

### 2.3 API Keys (Alternative)

For simpler integrations:
```
Authorization: ApiKey <key>
```

---

## 3. Response Format

### 3.1 Single Resource

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "ref_id": "RISK-001",
  "title": "Unauthorized access to production database",
  "category": "access_management",
  "status": "open",
  "inherent_likelihood": 4,
  "inherent_impact": 5,
  "inherent_score": 20,
  "owner_id": "...",
  "created_at": "2026-03-01T10:00:00Z",
  "updated_at": "2026-03-07T14:30:00Z"
}
```

### 3.2 Collection

```json
{
  "items": [ ... ],
  "meta": {
    "total": 142,
    "page": 1,
    "per_page": 25,
    "total_pages": 6
  },
  "links": {
    "self": "/api/v1/risks?page=1&per_page=25",
    "next": "/api/v1/risks?page=2&per_page=25",
    "prev": null,
    "first": "/api/v1/risks?page=1&per_page=25",
    "last": "/api/v1/risks?page=6&per_page=25"
  }
}
```

### 3.3 Error Response

```json
{
  "error": {
    "code": "validation_error",
    "message": "Request validation failed",
    "details": [
      {
        "field": "inherent_likelihood",
        "message": "Value must be between 1 and 5",
        "code": "out_of_range"
      }
    ],
    "request_id": "req_abc123"
  }
}
```

**HTTP Status Codes:**

| Code | Usage |
|---|---|
| 200 | Success (GET, PUT, PATCH) |
| 201 | Created (POST) |
| 204 | No Content (DELETE) |
| 400 | Bad Request (malformed input) |
| 401 | Unauthorized (missing/invalid token) |
| 403 | Forbidden (insufficient permissions) |
| 404 | Not Found |
| 409 | Conflict (duplicate, optimistic lock) |
| 422 | Unprocessable Entity (validation errors) |
| 429 | Too Many Requests (rate limited) |
| 500 | Internal Server Error |

### 3.4 PATCH Semantics

PATCH requests use JSON Merge Patch ([RFC 7396](https://tools.ietf.org/html/rfc7396)). Send only the fields to update:

```
PATCH /api/v1/risks/{id}
Content-Type: application/merge-patch+json

{
  "status": "mitigated",
  "residual_likelihood": 2
}
```

Fields not included in the request body are left unchanged. Set a field to `null` to clear it.

---

## 4. Core API Endpoints

### 4.1 Risks

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/risks` | List risks (filterable, paginated) |
| POST | `/api/v1/risks` | Create a risk |
| GET | `/api/v1/risks/{id}` | Get a risk by ID |
| PUT | `/api/v1/risks/{id}` | Replace a risk |
| PATCH | `/api/v1/risks/{id}` | Partial update a risk |
| POST | `/api/v1/risks/{id}/archive` | Archive a risk (soft-delete) |
| DELETE | `/api/v1/risks/{id}` | Delete a risk (hard-delete, admin only) |
| GET | `/api/v1/risks/{id}/treatments` | List treatment plans for a risk |
| POST | `/api/v1/risks/{id}/treatments` | Create a treatment plan |
| GET | `/api/v1/risks/{id}/controls` | List linked controls |
| GET | `/api/v1/risks/{id}/artifacts` | List linked evidence |
| GET | `/api/v1/risks/{id}/audit-log` | Get audit history for a risk |

**Filter parameters:**
```
GET /api/v1/risks?category=access_management&status=open&inherent_score[gte]=15&owner_id=...&sort=-inherent_score&fields=id,title,inherent_score&include=owner,controls&page=1&per_page=25
```

### 4.2 Controls

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/controls` | List controls |
| POST | `/api/v1/controls` | Create a control |
| GET | `/api/v1/controls/{id}` | Get a control |
| PUT | `/api/v1/controls/{id}` | Replace a control |
| PATCH | `/api/v1/controls/{id}` | Partial update |
| POST | `/api/v1/controls/{id}/retire` | Retire a control |
| DELETE | `/api/v1/controls/{id}` | Delete a control (hard-delete, admin only) |
| GET | `/api/v1/controls/{id}/mappings` | List framework mappings |
| POST | `/api/v1/controls/{id}/mappings` | Add a framework mapping |
| DELETE | `/api/v1/controls/{id}/mappings/{mapping_id}` | Remove mapping |
| POST | `/api/v1/controls/{id}/suggest-mappings` | AI agent suggests mappings |
| GET | `/api/v1/controls/{id}/test-history` | Historical test results |

### 4.3 Frameworks

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/frameworks` | List frameworks |
| GET | `/api/v1/frameworks/{id}` | Get framework details |
| GET | `/api/v1/frameworks/{id}/requirements` | List requirements (hierarchical) |
| GET | `/api/v1/frameworks/{id}/coverage` | Coverage matrix (mapped vs. unmapped) |

### 4.4 Assessment Campaigns

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/assessments` | List campaigns |
| POST | `/api/v1/assessments` | Create a campaign |
| GET | `/api/v1/assessments/{id}` | Get campaign |
| PATCH | `/api/v1/assessments/{id}` | Update campaign |
| POST | `/api/v1/assessments/{id}/finalize` | Finalize (lock) campaign |
| GET | `/api/v1/assessments/{id}/progress` | Campaign progress summary |
| GET | `/api/v1/assessments/{id}/test-procedures` | List test procedures |
| POST | `/api/v1/assessments/{id}/test-procedures` | Add test procedure |

### 4.5 Test Procedures

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/test-procedures/{id}` | Get test procedure with steps |
| PATCH | `/api/v1/test-procedures/{id}` | Update procedure |
| POST | `/api/v1/test-procedures/{id}/results` | Submit results (human or agent) |
| GET | `/api/v1/test-procedures/{id}/steps` | List test steps |
| PATCH | `/api/v1/test-procedures/{id}/steps/{step_id}` | Update a step result |
| POST | `/api/v1/test-procedures/{id}/submit-for-review` | Submit for review |
| POST | `/api/v1/test-procedures/{id}/approve` | Approve workpaper |
| POST | `/api/v1/test-procedures/{id}/reject` | Reject with comments |

### 4.6 Artifacts (Evidence)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/artifacts` | List artifacts |
| POST | `/api/v1/artifacts` | Upload artifact (multipart) |
| GET | `/api/v1/artifacts/{id}` | Get artifact metadata |
| GET | `/api/v1/artifacts/{id}/download` | Download artifact (pre-signed URL redirect) |
| GET | `/api/v1/artifacts/{id}/versions` | List versions |
| POST | `/api/v1/artifacts/{id}/versions` | Upload new version |
| GET | `/api/v1/artifacts/{id}/links` | List entity links |
| POST | `/api/v1/artifacts/{id}/links` | Link to entity |
| DELETE | `/api/v1/artifacts/{id}/links/{link_id}` | Unlink |
| GET | `/api/v1/artifacts/{id}/lineage` | Full chain of custody |

**Upload flow:**
```
1. POST /api/v1/artifacts/upload-url
   → Returns pre-signed S3 URL for direct upload

2. Client uploads file directly to S3

3. POST /api/v1/artifacts
   Body: { "storage_key": "...", "filename": "...", "size_bytes": ..., "sha256_hash": "..." }
   → Creates artifact record, verifies hash
```

### 4.7 Evidence Requests

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/evidence-requests` | List requests |
| POST | `/api/v1/evidence-requests` | Create a request |
| PATCH | `/api/v1/evidence-requests/{id}` | Update request |
| POST | `/api/v1/evidence-requests/{id}/submit` | Control owner submits evidence |
| POST | `/api/v1/evidence-requests/{id}/accept` | Auditor accepts |
| POST | `/api/v1/evidence-requests/{id}/reject` | Auditor rejects |

### 4.8 Findings

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/findings` | List findings |
| POST | `/api/v1/findings` | Create a finding |
| GET | `/api/v1/findings/{id}` | Get finding |
| PATCH | `/api/v1/findings/{id}` | Update finding |
| GET | `/api/v1/findings/{id}/remediation` | Get remediation plan |
| POST | `/api/v1/findings/{id}/remediation` | Create remediation plan |
| POST | `/api/v1/findings/{id}/validate` | Validate remediation |
| POST | `/api/v1/findings/{id}/close` | Close finding |

### 4.9 Agents

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/agents` | List registered agents |
| POST | `/api/v1/agents` | Register an agent |
| GET | `/api/v1/agents/{id}` | Get agent details |
| PATCH | `/api/v1/agents/{id}` | Update agent config |
| DELETE | `/api/v1/agents/{id}` | Revoke agent |
| GET | `/api/v1/agents/{id}/assignments` | Get pending assignments |
| GET | `/api/v1/agents/{id}/history` | Agent action history |

### 4.10 Common Control Library

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/ccl` | Browse CCL entries |
| GET | `/api/v1/ccl/{id}` | Get CCL entry with mappings |
| POST | `/api/v1/ccl/{id}/adopt` | Adopt CCL entry into org catalog |
| GET | `/api/v1/ccl/search` | Search CCL by keyword or framework |

### 4.11 Taxonomy

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/taxonomy` | List all taxonomy types |
| GET | `/api/v1/taxonomy/{type}` | List values for a type |
| POST | `/api/v1/taxonomy/{type}` | Add a value |
| PATCH | `/api/v1/taxonomy/{type}/{id}` | Update a value |

### 4.12 Reports

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/reports/templates` | List report templates |
| POST | `/api/v1/reports/generate` | Generate a report |
| GET | `/api/v1/reports/{id}` | Get report status/download |
| GET | `/api/v1/reports/dashboards/risk-posture` | Risk posture dashboard data |
| GET | `/api/v1/reports/dashboards/control-health` | Control health dashboard data |
| GET | `/api/v1/reports/dashboards/assessment-progress` | Assessment progress data |

### 4.13 Audit Logs

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/audit-logs` | Query audit logs (filterable) |
| GET | `/api/v1/audit-logs/export` | Export logs (CSV/JSON) |

### 4.14 Search

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/search?q=...&type=...` | Full-text search across entities |

---

## 5. GraphQL API

Deferred. The REST API with `include`, `fields`, and filter parameters covers current query needs. GraphQL may be added in a future version for complex relational queries.

---

## 6. Webhook Events

External systems can subscribe to events:

```
POST /api/v1/webhooks
{
  "url": "https://example.com/webhook",
  "events": ["finding.opened", "assessment.completed", "risk.score_changed"],
  "secret": "whsec_..."
}
```

**Webhook payload:**
```json
{
  "id": "evt_abc123",
  "type": "finding.opened",
  "timestamp": "2026-03-07T15:00:00Z",
  "tenant_id": "...",
  "data": {
    "finding": {
      "id": "...",
      "ref_id": "FIND-042",
      "title": "Excessive admin access in production",
      "classification": "significant_deficiency",
      "risk_rating": "high"
    }
  }
}
```

**Security:** Webhooks are signed with HMAC-SHA256 using the shared secret. Header: `X-GC-Signature`.

---

## 7. Rate Limiting

| Client Type | Default Limit |
|---|---|
| Authenticated user | 1000 req/min |
| Agent | 500 req/min |
| API key (service account) | 2000 req/min |

Rate limit headers:
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 950
X-RateLimit-Reset: 1709820600
```

---

## 8. Plugin Architecture

### 8.1 Plugin Package Structure

```
my-plugin/
├── plugin.yaml          # Plugin manifest
├── src/
│   ├── main.py          # Entry point (Python) or main.ts (TypeScript)
│   ├── handlers.py      # Event handlers
│   ├── api.py           # Custom API endpoints
│   └── ui/              # Optional micro-frontend components
├── schemas/
│   ├── config.json      # Configuration schema (JSON Schema)
│   └── entities.json    # Custom entity definitions (if any)
├── frameworks/          # Framework definitions (for framework plugins)
│   └── iso27001.yaml
├── tests/
│   └── test_main.py
└── README.md
```

### 8.2 Plugin Manifest (`plugin.yaml`)

```yaml
name: aws-evidence-collector
version: 1.2.0
type: evidence_collector
author: Ground Control Community
description: Collects evidence from AWS Config, SecurityHub, and CloudTrail
license: MIT

requires:
  ground_control: ">=0.4.0"

permissions:
  - artifacts:write
  - controls:read
  - assessments:read
  - events:subscribe:assessment.started

config_schema: schemas/config.json

events:
  subscribes:
    - assessment.started
    - evidence_collection.scheduled
  publishes:
    - evidence_collection.completed
    - evidence_collection.failed

api_routes:
  - path: /plugins/aws-collector/status
    method: GET
    handler: api:get_status
  - path: /plugins/aws-collector/collect-now
    method: POST
    handler: api:trigger_collection

ui_components:
  - name: AWSCollectorConfig
    mount_point: plugin_settings
    source: ui/AWSCollectorConfig.jsx

signature: "ed25519:base64..."
```

### 8.3 Plugin SDK

Plugins use the Ground Control Plugin SDK:

```python
from ground_control.plugin import Plugin, event_handler, api_route
from ground_control.sdk import GroundControlClient

class AWSCollectorPlugin(Plugin):
    """Collects evidence from AWS services."""

    def on_activate(self, config: dict):
        """Called when plugin is enabled."""
        self.aws_region = config["aws_region"]
        self.gc = GroundControlClient(self.context)

    @event_handler("assessment.started")
    async def on_assessment_started(self, event):
        """Auto-collect evidence when assessment starts."""
        campaign = event.data["campaign"]
        controls = await self.gc.controls.list(
            campaign_id=campaign["id"],
            tags=["aws"]
        )
        for control in controls:
            await self.collect_for_control(control)

    @event_handler("evidence_collection.scheduled")
    async def on_scheduled(self, event):
        """Handle scheduled collection runs."""
        await self.collect_all()

    @api_route("GET", "/plugins/aws-collector/status")
    async def get_status(self, request):
        """Custom API endpoint for collection status."""
        return {"last_run": self.last_run, "status": "healthy"}

    async def collect_for_control(self, control):
        """Collect AWS Config snapshot for a control."""
        # ... AWS API calls ...
        artifact = await self.gc.artifacts.upload(
            file_data=snapshot_data,
            filename=f"aws-config-{control['ref_id']}.json",
            tags=["aws", "config", "auto-collected"]
        )
        await self.gc.artifacts.link(
            artifact_id=artifact["id"],
            entity_type="control",
            entity_id=control["id"],
            context_note="Auto-collected AWS Config snapshot"
        )
        self.publish_event("evidence_collection.completed", {
            "control_id": control["id"],
            "artifact_id": artifact["id"]
        })
```

### 8.4 Plugin Lifecycle

```
Install → Configure → Enable → Running → Disable → Uninstall
                                  │
                                  ├── Event: plugin receives domain events
                                  ├── API: plugin custom endpoints are active
                                  ├── UI: plugin components are mounted
                                  └── Health: periodic health checks
```

### 8.5 Plugin Sandboxing

| Mechanism | Description |
|---|---|
| **Process isolation** | Plugins run in separate processes (default for Python plugins) |
| **Scoped SDK** | Plugin SDK only exposes permitted operations based on declared permissions |
| **Resource limits** | CPU time, memory, and API call rate limits per plugin |
| **Audit trail** | All plugin actions logged with plugin identity |

### 8.6 Framework Plugin Example

```yaml
# frameworks/pci-dss-v4.yaml
framework:
  name: "PCI-DSS v4.0"
  version: "4.0"
  description: "Payment Card Industry Data Security Standard"

requirements:
  - ref_id: "1"
    title: "Install and Maintain Network Security Controls"
    children:
      - ref_id: "1.1"
        title: "Processes and mechanisms for installing and maintaining network security controls are defined and understood"
        children:
          - ref_id: "1.1.1"
            title: "All security policies and operational procedures identified in Requirement 1 are documented, kept up to date, in use, and known to all affected parties"
      - ref_id: "1.2"
        title: "Network security controls (NSCs) are configured and maintained"
        children:
          - ref_id: "1.2.1"
            title: "Configuration standards for NSC rulesets are defined, implemented, maintained"
  # ... continues for all 12 requirements and sub-requirements

ccl_mappings:
  - requirement: "7.1"
    ccl_entries: ["CC-AM-001", "CC-AM-002"]
  - requirement: "7.2"
    ccl_entries: ["CC-AM-001", "CC-AM-003"]
```

---

## 9. Agent SDK

### 9.1 Python Agent SDK

```python
from ground_control.agent import AgentClient

async def main():
    # Authenticate
    client = AgentClient(
        base_url="https://gc.example.com",
        client_id="agent-sox-tester",
        client_secret="..."
    )

    # Get assignments
    assignments = await client.get_assignments(
        status="pending",
        campaign_type="sox_itgc"
    )

    for assignment in assignments:
        procedure = await client.get_test_procedure(assignment.procedure_id)

        # Perform testing logic
        results = await perform_test(procedure)

        # Submit results
        await client.submit_results(
            procedure_id=procedure.id,
            steps=[
                {
                    "step_number": 1,
                    "actual_result": "Verified access review completed on 2026-02-15",
                    "conclusion": "pass",
                    "evidence_ids": ["artifact-uuid-1"]
                },
                {
                    "step_number": 2,
                    "actual_result": "Found 3 accounts without recent review",
                    "conclusion": "fail",
                    "evidence_ids": ["artifact-uuid-2"]
                }
            ],
            conclusion="ineffective",
            confidence=0.85,
            notes="3 accounts identified without access review in 90+ days"
        )
```

### 9.2 TypeScript Agent SDK

```typescript
import { AgentClient } from '@ground-control/agent-sdk';

const client = new AgentClient({
  baseUrl: 'https://gc.example.com',
  clientId: 'agent-sox-tester',
  clientSecret: '...'
});

const assignments = await client.getAssignments({ status: 'pending' });

for (const assignment of assignments) {
  const procedure = await client.getTestProcedure(assignment.procedureId);
  const results = await performTest(procedure);
  await client.submitResults(procedure.id, results);
}
```

---

## 10. API Versioning & Deprecation

| Policy | Detail |
|---|---|
| Version format | URL path: `/api/v1/`, `/api/v2/` |
| Deprecation notice | 6 months before removal; `Sunset` header on deprecated endpoints |
| Breaking changes | New major version only; non-breaking changes within version |
| Backward compatibility | New optional fields, new endpoints, new enum values are non-breaking |
