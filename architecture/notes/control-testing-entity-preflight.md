# Control Testing Entity Preflight

Issue: #270
Requirements: GC-I012 and bundled GC-I013

This is architecture guardrail guidance for implementation of the control test
and control effectiveness assessment surface. It is not an implementation plan.

## Boundary

A control test is a durable audit/evidence record for a control or scoped
control implementation at a point in time. It owns test methodology, test steps,
expected results, actual results, conclusion, tester identity, test date,
optional tested asset scope, and explicit evidence contribution to assessment
dimensions or FAIR factors.

A control effectiveness assessment is the durable rating and influence record
for a control or scoped control implementation. It owns design-effectiveness and
operating-effectiveness ratings, the methodology/profile context used to
interpret them, the affected qualitative dimensions or FAIR-aligned factors, the
assessment scope, and the links to risk assessment results that consumed the
assessment. It may summarize supporting control tests, but it is not the test
row itself.

It must stay separate from:

- `Control`, which owns catalog-level identity, objective, status, owner,
  implementation-scope summary, methodology factors, and the current
  effectiveness summary.
- `ControlLink`, `RiskScenarioLink`, `AssetLink`, and graph projections, which
  provide project-scoped traversal and target references, not the test result
  payload.
- `Observation`, which records time-bounded facts about operational assets. A
  test may use or produce observations, but it is not itself an asset
  observation.
- `RiskAssessmentResult`, which owns methodology-specific risk assessment
  inputs, outputs, evidence refs, observations, and approval state. Control test
  conclusions or effectiveness assessments may feed it, but they are not risk
  assessments and must not overwrite it as a single residual-score field.
- `VerificationResult`, which is a prover/tool result for requirements and
  traceability links. It is not a control-operating-effectiveness assessment.

Do not collapse control tests into mutable fields on `Control.effectiveness`.
`Control.effectiveness` may summarize the latest or current assessment state,
but each test result needs its own audited row and linkable identity.
The same rule applies to effectiveness assessments: the latest rating may be
projected onto the control for convenience, but the authoritative assessment
history and methodology-specific influence payload need their own linkable,
audited identity.

## Effectiveness Assessment Guardrails

GC-I013 is broader than "effective / ineffective on the control." The model must
represent at least these separate concepts without conflating them:

- **Design effectiveness**: whether the control is designed to reduce the risk
  dimension it claims to address.
- **Operating effectiveness**: whether observed operation, usually supported by
  one or more control tests or observations, shows the control works as designed.
- **Rating**: the control-facing conclusion, such as effective, partially
  effective, or ineffective.
- **Risk influence**: the methodology-specific effect on qualitative likelihood
  or consequence criteria, or on FAIR-aligned frequency/magnitude factors.
- **Scope**: the operational population where the result applies, such as a
  control implementation, asset population, observation set, or evidence set.
- **Consumption**: which `RiskAssessmentResult` rows were influenced by this
  assessment and which input/output factors were affected.

Do not encode those as unrelated free-form keys inside `Control.effectiveness`.
If a summary field remains on `Control`, it must be treated as a projection of
the latest/current assessment state, not as the source of truth or as the only
input to residual risk.

## Incumbents To Reuse

- REST shape: thin `api/*Controller` classes, request/response records,
  `@Valid`, Jackson enum binding, and `ProjectService.requireProjectId`.
- Domain shape: `domain/controls`, command records, repositories, service-owned
  semantic validation, audited JPA entities extending `BaseEntity`, and the
  existing enum transition pattern where lifecycle state is introduced.
- Risk assessment shape: reuse `RiskAssessmentResult`,
  `RiskAssessmentResultService`, `RiskAssessmentResultRepository`,
  `MethodologyProfile`, seeded FAIR/NIST/ISO profile schemas,
  `risk_assessment_result_observation`, and the approval-state lifecycle when
  effectiveness feeds assessment results. Do not create a parallel residual-risk
  table or update risk outputs from a controller.
- Project-scoped control resolution: use `ControlService.getById(projectId, id)`
  or `ControlRepository.existsByIdAndProjectId`; never resolve controls by UUID
  alone.
- Project-scoped target validation: extend `GraphTargetResolverService` for any
  new first-class internal target type. `targetEntityId` is for modeled
  entities; `targetIdentifier` is only for external or not-yet-modeled evidence.
- Existing link semantics: use `ControlLink` for control-side links to assets,
  observations, risk scenarios, risk register records, risk assessment results,
  and external evidence where those existing target types are enough. Add a new
  link target type only when a first-class control-test entity needs to be
  addressable from adjacent aggregates.
- Evidence conventions: model structured asset facts as `Observation` links and
  keep unmodeled artifacts as evidence identifiers or external link targets.
  Do not create a second observation or evidence-reference schema inside the
  test or assessment aggregate.
- JSON text columns: use `JacksonTextCollectionConverters` for structured test
  steps, assessment scope, dimension/factor evidence, consumed factor deltas,
  and method-specific payloads. Do not add feature-local `ObjectMapper` parsing
  or stringly encoded JSON.
- Graph: add any control-test node or edge through `GraphEntityType`,
  `GraphIds`, and a graph projection contributor. If effectiveness assessments
  become first-class, they need the same projection path and same-project target
  validation as controls, observations, and risk assessment results. Do not
  write AGE or graph rows directly from controllers or services.
- Audit and persistence: every new audited entity needs Flyway schema and audit
  table parity, project scope, indexes for project/control/date read paths, and
  an `AuditRetentionJob.AUDIT_TABLES` entry.

## Cross-Cutting Layers

- Security: keep endpoints under `/api/v1/**` so bearer traffic passes
  `IpAllowlistFilter`, `BearerTokenAuthFilter`, Spring authorization, and then
  `ActorFilter`; browser traffic passes the ADR-037 browser chain and shared
  path matrix. Do not add feature-local auth, actor override fields, caller
  supplied tokens, or routes outside the matrix.
- Request parsing and validation: DTOs own required fields, size bounds, enum
  parsing, and collection-shape validation. Services own same-project checks,
  duplicate-test semantics, valid control/scope/asset relationships, conclusion
  rules, assessment-to-result linkage, methodology/factor validation, and any
  effectiveness rollup validation.
- Methodology schema validation: `MethodologyProfile.inputSchema` and
  `outputSchema` already store FAIR/NIST/ISO schemas, but the backend does not
  currently have one canonical JSON Schema validation service. If GC-I013 adds
  schema enforcement, put it behind one reusable service boundary keyed by
  `MethodologyProfile` and payload role (`designEffect`, `operatingEffect`,
  risk-assessment `inputFactors`, risk-assessment `computedOutputs`). Do not
  branch per methodology in controllers or silently accept unknown factor keys as
  "methodology-specific."
- Error envelope: use `NotFoundException`, `ConflictException`, and
  `DomainValidationException`; `GlobalExceptionHandler` and
  `shared.web.ErrorResponse` are the only HTTP error contract. Validation
  details may name fields, factor keys, or schema paths, but must not echo full
  test steps, raw results, raw assessment payloads, risk inputs, or evidence
  payloads.
- Audit and actor provenance: Envers plus `ActorFilter`, `ActorHolder`, and
  `GroundControlRevisionListener` provide authenticated revision history.
  `testerIdentity` is domain provenance and must not replace or spoof the
  authenticated audit actor.
- Observability: use SLF4J lifecycle events with stable IDs and low-cardinality
  fields. Never log bearer tokens, raw request bodies, full test results, full
  assessment payloads, full risk inputs/outputs, full evidence refs, or
  arbitrary FAIR factor payloads.
- Configuration and OS/runtime exposure: GC-I012/GC-I013 should not require new
  secrets, env bindings, subprocesses, shell-outs, network clients, or CLI argv.
  A later external evidence collector or calculation adapter must use
  `@ConfigurationProperties` with startup validation and keep secrets out of
  process argv, logs, and error envelopes.
- API enum mirrors: any API-visible enum for methodology, conclusion,
  effectiveness rating, dimension, factor, link target, or graph entity must
  respect ADR-034's single-source enum contract and update backend tests plus
  frontend/MCP mirrors where applicable.
- Tests and policy: controller additions need `@WebMvcTest`; semantic rules need
  service tests; enum state rules need focused enum tests; project-scoped target
  behavior needs resolver tests; graph changes need projection tests; migrations
  need smoke coverage. `make policy` remains the repo-native completion gate.

## Extensibility

The primary extension seam is the evidence contribution payload keyed by
methodology family/profile and by evidence target (`assessmentDimension`,
`fairFactor`, or future quantitative factor key). Adding a new methodology or
factor should require extending enum/profile data and validation tests, not
adding another control-test table or controller branch.

The second seam is scoped control implementation identity. If scoped
implementations become first-class, they should be a project-scoped target that
the test entity can reference, and `GraphTargetResolverService` should own its
same-project validation. Until then, use existing `Control` plus asset/boundary
links rather than inventing a free-text scoped implementation identifier.

The third seam is effectiveness rollup. Operating effectiveness ratings can be
derived from one or more control tests, while design effectiveness can be
recorded separately on the same control aggregate family. Keep the derivation
behind a domain service so future residual-risk scoring can consume the same
canonical assessment result without re-parsing test rows.

The fourth seam is risk-assessment consumption. A future qualitative method can
consume likelihood/consequence influence, while FAIR can consume frequency or
magnitude factor influence. That seam belongs at the link between the control
effectiveness assessment and `RiskAssessmentResult`, parameterized by
methodology profile and factor key. Adding a new methodology or factor should
require profile data, enum/mirror updates where exposed, and validation tests,
not new residual-score columns or controller-specific scoring code.

## Gotchas And Anti-Patterns

- Do not treat `VerificationResult` as satisfying control testing or operating
  effectiveness; it is requirement/prover evidence.
- Do not treat `Observation` as the control test entity; observations can
  support tests but do not carry methodology, steps, conclusion, or tester.
- Do not store test history only inside `Control.effectiveness`,
  `methodologyFactors`, `RiskAssessmentResult.evidenceRefs`, or arbitrary link
  metadata.
- Do not store effectiveness assessment history only inside
  `Control.effectiveness`, `Control.methodologyFactors`,
  `RiskRegisterRecord.decisionMetadata`, or `RiskAssessmentResult.computedOutputs`.
- Do not let `testerIdentity` spoof the authenticated audit actor or replace
  Envers revision provenance.
- Do not add a duplicate exception hierarchy, JSON parser, security filter,
  audit writer, graph materializer, evidence schema, or workflow engine.
- Do not log full test outputs, actual results, evidence blobs, or factor maps.
- Do not link first-class controls, assets, observations, scenarios, registers,
  or assessments by raw identifiers when project-scoped UUID targets exist.
- Do not make FAIR factor evidence a one-off map with no stable keys or
  validation seam; the requirement depends on reusing evidence across
  qualitative and quantitative methods.
- Do not reduce GC-I013 to a single residual-risk score, percentage, or
  aggregate control-strength number. The requirement needs dimension/factor
  influence and scoped consumption by linked assessment results.
- Do not conflate design effectiveness with operating effectiveness or infer one
  from the other without an explicit service rule and persisted provenance.
- Do not treat `ControlStatus.OPERATIONAL` as evidence of operating
  effectiveness; status is lifecycle, not assessment.
- Do not update residual-risk scores directly from controllers. Risk scoring
  integration belongs behind the existing risk assessment/scoring service
  boundary when GC-T003/GC-I013 behavior consumes the control-test conclusion.

## Non-Goals

- No implementation of control test CRUD, migrations, graph projection, MCP, or
  frontend surfaces in this preflight.
- No new risk calculation engine, Monte Carlo runner, or automatic residual-risk
  scorer.
- No implementation of design-effectiveness or operating-effectiveness CRUD,
  migrations, graph projection, MCP, frontend, or residual-risk feed behavior in
  this preflight.
- No replacement of `Control`, `ControlLink`, `Observation`,
  `RiskAssessmentResult`, `RiskRegisterRecord`, `RiskScenario`, or
  `VerificationResult`.
- No new external evidence ingestion, background scheduler, ticketing
  integration, or workflow engine.
- No change to ADR-026 security, ADR-033 actor provenance, ADR-034 enum policy,
  or repo workflow gates.
