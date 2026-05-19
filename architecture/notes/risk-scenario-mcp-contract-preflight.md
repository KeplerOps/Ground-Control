# Risk Scenario MCP Contract Preflight

Issue: #876
Requirement: none

This is architecture guardrail guidance for restoring `gc_risk_scenario`
create/update usability from MCP. It is not an implementation plan.

## Boundary

The backend risk-scenario REST contract is already the semantic source of
truth: `RiskScenarioRequest`, `UpdateRiskScenarioRequest`,
`RiskScenarioController`, `RiskScenarioService`, `RiskScenario`, and the
existing Flyway risk-scenario tables already encode the domain shape,
validation, project scoping, auditing, and persistence.

The change belongs at the MCP adapter boundary. `gc_risk_scenario` should
expose the backend DTO fields in snake_case, pick only action-appropriate
entity-body fields, and let `mcp/ground-control/lib.js` convert them to the
backend camelCase JSON body. Do not rename backend fields, add a backend
compatibility `description` field, or weaken backend validation to work around
an adapter schema bug.

## Incumbents To Reuse

- Risk-domain boundary: `RiskScenario` remains the scoped loss scenario
  statement used by risk assessment, register, and treatment flows. It is not a
  threat-model entry, assessment result, register record, treatment plan, or
  generic metadata bucket.
- MCP consolidation pattern: ADR-035. Keep the single action-discriminated
  `gc_risk_scenario` tool, the flat Zod schema, handler-side `reqArg`, and the
  `pick(args, BODY_FIELDS)` request-body allowlists.
- MCP transport helpers: `buildUrl`, `addAuthorizationHeader`, `request`,
  `RequestError`, `parseErrorBody`, `toCamelCase`, and `TO_CAMEL` in
  `mcp/ground-control/lib.js`. Do not add feature-local fetch, auth-header,
  error parsing, or snake-to-camel logic.
- Shared link create path: `link-create.js` owns `link_create` body allowlists
  and required-field semantics for `gc_asset`, `gc_threat_model`,
  `gc_risk_scenario`, and `gc_control`. Do not fold link fields into
  risk-scenario create/update bodies.
- Backend validation: Bean Validation on request records handles create shape;
  service-layer update semantics handle partial updates. MCP-side required-field
  checks are caller guidance, not the source of truth.
- Error envelope: backend errors flow through `GlobalExceptionHandler` and
  `shared.web.ErrorResponse`; MCP surfaces them via `RequestError` and `err()`.
- Security and audit: ADR-026 and ADR-033. `/api/v1/risk-scenarios/**` remains
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

- MCP public schema: expose `uid`, `title`, `threat_source`, `threat_event`,
  `affected_object`, `vulnerability`, `consequence`, and `time_horizon`
  consistently with backend DTO semantics. Keep unknown/control fields out of
  request bodies via action-scoped allowlists.
- Request serialization: rely on `toCamelCase` and `TO_CAMEL` for snake_case to
  camelCase conversion. Existing mappings already cover
  `threat_source`, `threat_event`, `affected_object`, `time_horizon`, and
  `methodology_profile_id`; add mappings only when a real backend wire name
  requires it.
- Backend parser and validators: Jackson and Bean Validation own the server-side
  DTO contract. Do not duplicate max-length or blank-string validation as a
  parallel MCP validation system beyond early `reqArg` checks for required
  create fields.
- Project scoping: the adapter passes the optional `project` query parameter
  through existing lib helpers. `ProjectService.resolveProjectId` /
  `requireProjectId` remains the project boundary.
- Security/config/OS exposure: this change should require no new env vars,
  secrets, subprocesses, shell commands, or argv-token handling. Existing MCP env
  bindings are `GC_BASE_URL`, `GROUND_CONTROL_API_TOKEN`, and
  `GROUND_CONTROL_PACK_REGISTRY_ADMIN_TOKEN`; tokens stay in headers and out of
  logs/errors.
- Error leakage: preserve backend validation envelopes and the MCP
  `RequestError` path. Do not reflect raw request bodies, bearer tokens,
  Authorization headers, or stack traces through MCP errors.
- Persistence/audit/logging: no migration, repository, entity, or audit-table
  change is needed. Envers, `ActorFilter`, `ActorHolder`,
  `RequestLoggingFilter`, and service-level SLF4J lifecycle logs remain the only
  audit/observability path.

## Extensibility

The extension seam is the MCP DTO mirror itself: the tool schema, the
action-scoped body allowlists, and `TO_CAMEL` field mapping. Create and update
must be separable because `uid` is create-only today while partial updates
should not leak create-only or transition-only fields.

If this class of drift keeps recurring beyond the threat-model and
risk-scenario tools, use a small static contract inventory modeled after
ADR-034's `ENUM_CONTRACT_INVENTORY` so backend request-record fields and MCP
mirrors can be checked without runtime reflection, generated schemas, or a
second controller layer.

## Gotchas And Anti-Patterns

- Do not map `description` to a backend risk-scenario field. The backend has
  explicit scenario-statement fields; `description` is a leaky generic CRUD
  convention from adjacent tools.
- Do not send `status`, `methodology_profile_id`, or `metadata` on risk-scenario
  create/update unless and until the backend DTOs actually accept those fields.
  Status changes use the existing transition action.
- Do not put `target_*` or `link_type` fields into risk-scenario create/update
  bodies. Link fields belong only to `link_create`.
- Do not make MCP `metadata` authoritative for fields the backend models
  explicitly.
- Do not relax Zod to passthrough arbitrary keys to make this work; the
  allowlist is the adapter boundary that prevents action/control fields from
  leaking into REST bodies.
- Do not add a risk-scenario-specific exception hierarchy, auth path, logger,
  REST endpoint, graph write path, or persistence workaround.
- Do not conflate risk-scenario semantics with threat-model semantics: risk
  scenarios use `consequence` and `timeHorizon`; threat models use `effect`,
  optional STRIDE, and `narrative`.

## Non-Goals

- No backend domain model, controller, service, repository, migration, or
  REST-contract redesign.
- No new risk-scenario fields beyond the backend DTOs already in source.
- No OpenAPI/codegen project, frontend type migration, or generic MCP schema
  framework as part of #876.
- No redesign of risk assessment, risk register, treatment, graph projection,
  traceability, or threat-model boundaries.
- No change to ADR-024, ADR-026, ADR-033, ADR-034, ADR-035, or the repo
  workflow policy.
