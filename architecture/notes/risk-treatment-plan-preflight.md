# Risk Treatment Plan Preflight

Issue: #825
Implementation issue: #259
Requirement: GC-T004

This is architecture guardrail guidance for verification or follow-up
implementation of GC-T004. It is not an implementation plan.

## Boundary

`TreatmentPlan` is the risk-governance action record: the selected treatment
strategy, owner-facing actions, due dates, status, rationale, and reassessment
signals for a risk register record. It must stay separate from:

- `RiskRegisterRecord`, which owns the governance/register entry and its linked
  scenarios, category, review cadence, and decision metadata.
- `RiskScenario`, which owns the scoped loss scenario statement.
- `RiskAssessmentResult`, which owns methodology input factors, computed
  outputs, confidence, uncertainty, evidence references, observations, and
  approval state.
- `OperationalAsset`, `AssetLink`, `ControlLink`, and graph projections, which
  already provide project-scoped operational scope, mitigation-control context,
  and traversal.

Do not turn treatment plans into assessments, threat-model entries, control
implementations, or asset-scope containers. A treatment plan may reference those
concepts, but their authoritative state remains in their existing aggregates and
link surfaces.

## Incumbents To Reuse

- REST shape: `TreatmentPlanController`, request/response records, `@Valid`, and
  `ProjectService.resolveProjectId/requireProjectId`.
- Domain shape: `TreatmentPlan`, `TreatmentPlanService`, command records,
  `TreatmentPlanRepository`, and `TreatmentPlanStatus.transitionStatus` via the
  enum transition pattern.
- Strategy contract: `TreatmentStrategy` is the API/persistence enum for
  mitigate, accept, transfer, share, avoid, and the current methodology-specific
  escape hatch `OTHER`. Do not add parallel strategy strings inside action
  items or metadata.
- Scenario linkage: use `TreatmentPlan.riskRegisterRecord` as mandatory and
  `TreatmentPlan.riskScenario` as optional. When a scenario is supplied, service
  validation must keep it in the same project and, when the register record has
  scenario links, within that record's scenario set.
- Operational asset scope: use `AssetLink` targeting `TREATMENT_PLAN`,
  `RISK_REGISTER_RECORD`, or `RISK_SCENARIO` for graph-native asset/boundary
  context. Narrative summaries remain secondary context only.
- Controls implementing mitigation: use `ControlLink` targeting
  `TREATMENT_PLAN`, `RISK_REGISTER_RECORD`, or `RISK_SCENARIO`, plus existing
  control status/effectiveness fields. Do not create a treatment-specific
  control join table unless the generic link contract is proven insufficient and
  captured in an ADR.
- Internal target validation: use or extend `GraphTargetResolverService` when a
  link surface accepts first-class internal targets. `targetEntityId` is for
  modeled entities; `targetIdentifier` is only for external or not-yet-modeled
  artifacts.
- JSON text columns: `actionItems` and `reassessmentTriggers` use
  `JacksonTextCollectionConverters`. Do not add feature-local `ObjectMapper`
  parsing or a second JSON schema system.
- Graph: treatment-plan nodes and `TREATS` edges belong in
  `RiskGraphProjectionContributor`, using `GraphEntityType` and `GraphIds`.
  Asset/control/scope edges should flow through the existing link projection
  paths, not a treatment-only graph materializer.
- Audit and persistence: audited entities extend `BaseEntity`, use Envers,
  Flyway migrations, audit-table parity, project scope, and unique constraints
  consistent with the existing risk, asset, and control models.

## Cross-Cutting Layers

- Security: `/api/v1/treatment-plans/**` stays inside the `ApiSecurityConfig`
  path matrix. In production the request must pass `IpAllowlistFilter`,
  `BearerTokenAuthFilter`, Spring authorization, and then `ActorFilter`.
  Controllers should not implement feature-local auth, actor overrides, or
  routes outside `/api/v1/**`.
- Request parsing and validation: Jackson enum binding and Bean Validation own
  DTO shape. Service-layer validation owns duplicate UIDs, project scoping,
  scenario/register consistency, status transitions, and internal-vs-external
  link target rules.
- Error envelope: use `NotFoundException`, `ConflictException`, and
  `DomainValidationException`; `GlobalExceptionHandler` and
  `shared.web.ErrorResponse` are the only HTTP error contract. Do not return ad
  hoc treatment-plan error bodies or leak raw JSON payloads in errors.
- Audit and actor provenance: Envers plus `ActorFilter`, `ActorHolder`, and
  `GroundControlRevisionListener` provide revision history and actor identity.
  Do not accept actor, reviewer, or approver identity from treatment-plan
  request bodies unless a separate workflow requirement defines that contract.
- Observability: use SLF4J lifecycle events only for material mutations and keep
  them stable and low-cardinality. Request IDs and actors come from
  `RequestLoggingFilter` and MDC; never log bearer tokens, raw request bodies,
  full action-item payloads, or full evidence payloads.
- Configuration and OS/runtime exposure: GC-T004 should not require new secrets,
  environment bindings, subprocesses, shell-outs, network clients, or CLI argv
  exposure. A future external-ticket or workflow integration must use
  `@ConfigurationProperties` with startup validation and keep secrets out of
  argv, logs, and error envelopes.
- Tests and policy: controller changes need `@WebMvcTest`; semantic rules need
  service tests; graph/link behavior needs resolver and projection tests;
  schema changes need migration smoke coverage. `make policy` remains the
  completion guardrail.

## Extensibility

The extension seam is a typed treatment action item, not a new treatment-plan
aggregate. If action items need richer lifecycle later, introduce one canonical
action-item value shape or child entity with explicit `owner`, `dueDate`,
`status`, and optional assignee identity, then map current JSON payloads into
that shape. Avoid separate action schemas for FAIR, NIST, ISO, controls, and
assets.

Reassessment triggers should remain expressed as trigger categories plus target
references when they become machine-actionable: treatment progress,
asset-state change, control-state change, assessment refresh, or
methodology-specific trigger. The target-resolution seam belongs in
`GraphTargetResolverService` and graph projections so API, MCP, and future
analysis/sweep surfaces can reuse the same project-scoped validation.

## Gotchas And Anti-Patterns

- Do not treat a treatment plan as satisfying asset scope or control mitigation
  linkage merely because it has free-text action items.
- Do not duplicate `AssetLink`, `ControlLink`, `RiskScenarioLink`, graph IDs,
  traceability links, or audit tables to make treatment-specific variants.
- Do not hide methodology-specific strategy values in arbitrary action-item
  keys when the API enum or methodology profile should carry the contract.
- Do not let a treatment plan reference a risk scenario outside the linked risk
  register record when the record already constrains scenarios.
- Do not add workflow-engine concepts for the existing five-state
  `TreatmentPlanStatus` lifecycle.
- Do not introduce endpoint-local exception mapping, security checks, JSON
  parsing, or logging conventions.
- Do not transition GC-T004 to ACTIVE unless each clause is evidenced against
  the canonical artifacts, including asset/control linkage and reassessment
  trigger behavior.

## Non-Goals

- Implementing GC-T004 gaps as part of verification issue #825.
- Designing a generic workflow engine, ticketing integration, or background
  scheduler.
- Replacing existing asset, control, scenario, risk-register, assessment, or
  traceability aggregates.
- Defining first-class control effectiveness assessment behavior; that belongs
  to the control-evaluation requirement family.
