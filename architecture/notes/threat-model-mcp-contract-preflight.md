# Threat Model MCP Contract Preflight

Issue: #875
Requirement: none

This is architecture guardrail guidance for restoring
`gc_threat_model` create/update usability. It is not an implementation plan.

## Boundary

The backend threat-model REST contract is already the semantic source of truth:
`ThreatModelRequest`, `UpdateThreatModelRequest`, `ThreatModelController`,
`ThreatModelService`, `ThreatModel`, and the Flyway threat-model tables already
encode the domain shape, validation, project scoping, auditing, and persistence.

The change belongs at the MCP adapter boundary. `gc_threat_model` should expose
the backend DTO fields in snake_case, pick only allowed entity-body fields, and
let `mcp/ground-control/lib.js` convert them to the backend camelCase JSON body.
Do not rename backend fields, add a backend compatibility `description` field,
or weaken backend validation to work around an adapter schema bug.

## Incumbents To Reuse

- Threat-model boundary: ADR-024. Threat models stay distinct from risk
  scenarios, risk assessments, treatment plans, and risk register records.
- MCP consolidation pattern: ADR-035. Keep the single action-discriminated
  `gc_threat_model` tool, the flat Zod schema, handler-side `reqArg`, and the
  `pick(args, ENTITY_FIELDS)` request-body allowlist.
- MCP transport helpers: `buildUrl`, `addAuthorizationHeader`, `request`,
  `RequestError`, `parseErrorBody`, `toCamelCase`, and `TO_CAMEL` in
  `mcp/ground-control/lib.js`. The adapter should not add feature-local fetch,
  auth-header, error parsing, or snake-to-camel logic.
- Backend validation: Bean Validation on request records plus
  `ThreatModelService` semantic validation for partial update blank rejection.
  MCP-side required-field checks are caller guidance, not the source of truth.
- Error envelope: backend errors flow through `GlobalExceptionHandler` and
  `ErrorResponse`; MCP surfaces them via `RequestError` and `err()`.
- Security and audit: ADR-026 and ADR-033. `/api/v1/threat-models/**` remains
  under the shared API path matrix, bearer-token handling, actor provenance, and
  MDC/request logging. MCP must continue to send `X-Actor: mcp-server` only as
  the existing dev/test fallback; production actor identity comes from the
  authenticated principal.
- Tests and gates: focused MCP regression coverage belongs under
  `mcp/ground-control` using the existing `node --test` harness. The backend
  controller/service tests already cover the REST DTO mapping and validation.
  Application-source changes need a changelog fragment, and `make policy` is the
  repo-native completion guardrail.

## Cross-Cutting Layers

- MCP public schema: expose `threat_source`, `threat_event`, `effect`,
  `narrative`, and the existing clear flags consistently with backend DTO
  semantics. Keep unknown/control fields out of request bodies via the
  `ENTITY_FIELDS` allowlist.
- Request serialization: rely on `toCamelCase` and `TO_CAMEL` for snake_case to
  camelCase conversion. Add field mappings only where names actually differ
  (`threat_source`, `threat_event`, `stride_category`, `clear_*`); do not create
  a second serializer.
- Backend parser and validators: Jackson enum binding handles STRIDE values;
  `@NotBlank` / `@Size` handle create shape; service-level
  `rejectBlankIfPresent` handles update shape. Do not duplicate these rules as a
  parallel validation system in MCP.
- Project scoping: the adapter passes the optional `project` query parameter
  through existing lib helpers. `ProjectService.resolveProjectId` remains the
  project boundary.
- Security/config/OS exposure: this change should require no new env vars,
  secrets, subprocesses, shell commands, or argv-token handling. Existing MCP
  env bindings are `GC_BASE_URL`, `GROUND_CONTROL_API_TOKEN`, and
  `GROUND_CONTROL_PACK_REGISTRY_ADMIN_TOKEN`; tokens stay in headers and out of
  logs/errors.
- Error leakage: preserve the existing 401/403 stable messages and backend
  validation envelopes. Do not reflect raw request bodies, bearer tokens, or
  Authorization headers through MCP errors.
- Persistence/audit/logging: no migration, repository, entity, or audit-table
  change is needed. Envers, `ActorFilter`, `ActorHolder`, `RequestLoggingFilter`,
  and service-level SLF4J lifecycle logs remain the only audit/observability
  path.

## Extensibility

The extension seam is the MCP DTO mirror itself: the tool schema, the
per-entity body allowlist, and `TO_CAMEL` field mapping. If this class of drift
is generalized beyond #875, use a small static contract inventory modeled after
ADR-034's `ENUM_CONTRACT_INVENTORY` so backend request-record fields and MCP
mirrors can be checked without adding runtime reflection, generated schemas, or
a second controller layer.

Adjacent risk-scenario MCP create/update fields appear to carry a similar DTO
mirror risk. Treat that as a separate scoped repair unless #875 is explicitly
expanded; do not accidentally conflate threat-model semantics with risk-scenario
semantics while touching nearby consolidated-tool code.

## Gotchas And Anti-Patterns

- Do not map `description` to a new backend field. Threat models have
  `narrative`; `description` is a leaky generic CRUD convention from other
  entities.
- Do not send `status` on create as a substitute for lifecycle transition.
  Creation defaults to `DRAFT`; status changes use the existing transition
  action.
- Do not put `target_identifier` into threat-model create/update bodies.
  Link fields belong only to `link_create`.
- Do not make MCP `metadata` authoritative for fields the backend models
  explicitly.
- Do not relax Zod to passthrough arbitrary keys to make this work; the allowlist
  is the adapter boundary that prevents action/control fields from leaking into
  REST bodies.
- Do not add a threat-model-specific exception hierarchy, auth path, logger,
  REST endpoint, graph write path, or persistence workaround.
- Do not broaden enum or DTO contract policy casually. ADR-034 currently covers
  selected enums; a DTO-shape gate should be explicit and inventory-driven.

## Non-Goals

- No backend domain model, controller, service, repository, migration, or
  REST-contract redesign.
- No new threat-model fields beyond the backend DTOs already in source.
- No OpenAPI/codegen project, frontend type migration, or generic MCP schema
  framework as part of #875.
- No remediation of adjacent `gc_risk_scenario` contract drift unless it is
  taken on as a separate issue or an explicit scope expansion.
- No change to ADR-024, ADR-026, ADR-033, ADR-034, ADR-035, or the repo
  workflow policy.
