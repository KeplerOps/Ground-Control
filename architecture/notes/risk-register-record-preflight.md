# Risk Register Record Preflight

Issue: #823
Requirement: GC-T001

## Boundary

`RiskRegisterRecord` is the governance and decision record for one canonical
risk scenario or a deliberately grouped set of related scenarios. It must stay
separate from:

- `RiskScenario`, which owns the scenario statement and semantic scope.
- `RiskAssessmentResult`, which owns qualitative or quantitative assessment
  values, input factors, computed outputs, confidence, uncertainty, approval,
  observations, and evidence references.
- `TreatmentPlan`, which owns treatment strategy, action items, status, due
  dates, and reassessment triggers.
- `OperationalAsset`, `AssetLink`, `ControlLink`, and graph projections, which
  already provide project-scoped cross-entity context and traversal.

Do not add likelihood, impact, residual risk, FAIR/NIST/ISO-specific scoring, or
methodology-specific fields to `RiskRegisterRecord`. If a value depends on an
assessment method or analysis run, it belongs on a linked
`RiskAssessmentResult`.

## Incumbents To Reuse

- REST shape: `api/riskscenarios/*Controller`, request/response records,
  `@Valid`, and `ProjectService.resolveProjectId/requireProjectId`.
- Domain shape: `domain/riskscenarios/model`, `repository`, `service`,
  command records, and enum transition methods that throw
  `DomainValidationException`.
- Link validation: `GraphTargetResolverService` for internal target existence
  and same-project checks; extend it when a new first-class target is added.
- Asset context: use `AssetLink` with `AssetLinkTargetType.RISK_REGISTER_RECORD`
  for graph-native asset or asset-group context. Keep `assetScopeSummary` as
  narrative context only, not the authoritative asset reference.
- Controls: use `ControlLink` with
  `ControlLinkTargetType.RISK_REGISTER_RECORD`; do not introduce a
  risk-register-specific control join table unless the generic link contract is
  insufficient and an ADR explains why.
- Treatments: use `TreatmentPlan.riskRegisterRecord`; the register record
  should not embed treatment fields.
- Evidence and findings: use existing observation/evidence references through
  `RiskAssessmentResult` and generic link target types for unmodeled artifacts.
  Add first-class evidence or finding tables only under their own requirement.
- JSON text columns: use `JacksonTextCollectionConverters`; do not add
  feature-local `ObjectMapper` parsing.
- Graph: update `GraphEntityType`, `GraphIds`, and the relevant
  `GraphProjectionContributor` instead of adding feature-specific graph paths.
- Audit and retention: audited JPA entities use Envers and the existing
  `AuditRetentionJob` allowlist when new audit tables are introduced.
- Migrations: Flyway remains the schema source of truth; preserve project scope,
  indexes, unique constraints, and audit-table parity.

## Cross-Cutting Gates

- Security: `/api/v1/**` is protected by `ApiSecurityConfig` when security is
  enabled. Do not add controller-local authorization or endpoints outside the
  established path matrix without updating ADR-026.
- Request validation: DTOs perform shape validation only. Semantic validation
  such as same-project target checks, duplicate detection, status transitions,
  and scenario/register consistency belongs in services.
- Error envelope: use the existing exception hierarchy
  (`NotFoundException`, `ConflictException`, `DomainValidationException`) so
  `GlobalExceptionHandler` emits the shared `ErrorResponse` contract. Do not
  create a risk-specific exception hierarchy.
- Observability: use SLF4J structured events only for material lifecycle
  changes. Let `RequestLoggingFilter`, `ActorFilter`, MDC, and Envers provide
  request and actor context; never log bearer tokens, raw request bodies, or
  decision-metadata payloads.
- Configuration and OS exposure: GC-T001 should not require new environment
  variables, secrets, subprocesses, CLI arguments, network clients, or shell-outs.
  Any future integration that does must use `@ConfigurationProperties` with
  startup validation and must keep secrets out of argv and logs.
- Tests: mirror the existing split: `@WebMvcTest` controller tests with filters
  disabled for slices, service unit tests for semantic rules, graph resolver and
  projection tests for link behavior, and migration/integration coverage when
  schema changes alter persistence contracts.

## Extensibility

The extension seam is target resolution plus typed links: when evidence,
findings, asset groups, or additional governance artifacts become first-class
entities, add a target enum value and resolver branch, then project it through
the existing graph contributor path. Do not pre-create nullable foreign keys or
duplicate link tables on `RiskRegisterRecord` for every anticipated target.

Review cadence is currently a free-text field. If cadence semantics become
machine-enforced, introduce one canonical cadence value object or enum and
migrate all API, persistence, and validation surfaces together.

## Anti-Patterns

- Treating a register record as the risk assessment result.
- Storing methodology-specific assessment outputs in `decisionMetadata`.
- Using `assetScopeSummary` instead of project-scoped `AssetLink` records when
  the asset exists.
- Creating duplicate control, evidence, finding, or treatment link mechanisms.
- Validating project ownership in controllers instead of services or
  `GraphTargetResolverService`.
- Returning ad hoc HTTP error bodies or leaking client-controlled identifiers in
  unexpected 500 responses.
- Adding a workflow engine for the seven-value risk-register status lifecycle.
