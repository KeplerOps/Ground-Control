# MCP GRC Analysis Tools Preflight

Issue: #219
Requirement: GC-L007

This note records architecture guardrails for exposing GRC-specific analysis
capabilities through MCP. It is not an implementation plan.

## Boundary

GC-L007 is an analysis-interface requirement. The Spring backend remains the
semantic authority for methodology execution, graph traversal, project scoping,
validation, persistence, audit provenance, and error envelopes. MCP is an
adapter over those backend contracts, not an analysis engine, graph client,
schema validator, or reporting database.

Keep these concepts separate:

- `MethodologyProfile` defines reusable methodology identity and input/output
  schemas. It does not store assessment results.
- `RiskAssessmentResult` is the durable methodology-scoped assessment snapshot.
  It owns inputs, outputs, observations, evidence refs, confidence,
  uncertainty, timing, analyst identity, and approval state.
- FAIR, FAIR-CAM, NIST SP 800-30, ISO, and custom profiles produce outputs with
  different units and scales. Numeric FAIR loss output, FAIR-CAM control
  analytics, NIST ordinal risk levels, compliance posture percentages, evidence
  freshness, and graph reachability must not be normalized into one generic
  score without an explicit method label and conversion rule.
- `Control`, `ControlTest`, and `ControlEffectivenessAssessment` remain
  separate. A control's catalog status is not proof of effectiveness, and
  effectiveness is not residual risk by itself.
- `Observation` records time-bounded asset/system facts. Current state is a
  projection over observations, not a mutation of historical facts.
- `EvidenceArtifact` is append-only summarized evidence. Evidence freshness
  analysis must use `derivedAt`, `supersededByArtifactId`, source references,
  observation `observedAt` / `expiresAt`, and control-test dates. Envers is
  mutation audit, not the business freshness model.
- Vendors are currently modeled as `OperationalAsset` rows with
  `AssetType.THIRD_PARTY`, subtype/metadata, observations, links, findings, and
  evidence. Do not invent a vendor aggregate in the MCP analysis layer.
- Compliance frameworks are currently external audit link targets
  (`AuditLinkTargetType.FRAMEWORK`) and control-pack/control metadata, not a
  universal first-class framework aggregate. Cross-framework analysis must
  label which substrate it used and must not pretend a missing mapping
  aggregate exists.

The issue body for #219 is narrower than the current GC-L007 requirement
payload. Treat the Ground Control payload as authoritative for scope and
methodology integrity.

## Incumbents To Reuse

- MCP transport and error handling: `mcp/ground-control/lib.js`
  (`buildUrl`, `request`, `addAuthorizationHeader`, `RequestError`,
  `parseErrorBody`, `pick`, `reqArg`, `toCamelCase`, `toSnakeCase`).
- MCP analysis and graph surfaces: `gc_analyze`, `gc_graph`, and `gc_query`
  from `mcp/ground-control/index.js` / `gc-query.js`. Keep GRC analysis on a
  consolidated, discriminator-based surface; do not add one tool per report or
  per methodology.
- MCP catalog policy: ADR-035. Pure reads can use `gc_query`; compute-heavy
  operations should use curated named tools that call fixed backend endpoints.
- Backend REST pattern: thin controllers, request/response records, `@Valid`,
  Bean Validation, Jackson enum binding, and project resolution through
  `ProjectService.resolveProjectId` or `requireProjectId`.
- Domain analysis examples: `AnalysisService`, `StatusDriftService`,
  `SimilarityService`, `AssetTopologyService`, and `MixedGraphService`.
- Methodology substrate: `MethodologyProfile`, seeded FAIR / NIST / ISO /
  legacy profiles, `RiskAssessmentResult`, approval-state transitions, and the
  existing JSON TEXT converters in `JacksonTextCollectionConverters`.
- Control analytics substrate: `ControlTest`,
  `ControlEffectivenessAssessment`, `ControlEffectivenessRating`, and
  `ControlEffectivenessAssessmentGraphProjectionContributor`.
- Evidence substrate: `Observation`, `EvidenceArtifact`,
  `EvidenceSourceRef`, `EvidenceArtifactService`, `listLatest` current-state
  projection, and ADR-045's append-only evidence boundary.
- Graph substrate: `MixedGraphService`, `MixedGraphClient`,
  `GraphTraversalLimits`, `GraphEntityType`, `GraphIds`,
  `GraphProjectionContributor` implementations, `GraphTargetResolverService`,
  and ADR-032's AGE construction boundary.
- Compliance/audit substrate: `Audit`, `AuditLink`,
  `AuditLinkTargetType.FRAMEWORK`, `AuditGraphProjectionContributor`, control
  packs, and pack-registry trust/install records where framework posture uses
  pack content.
- Asset/vendor substrate: `OperationalAsset`, `AssetType.THIRD_PARTY`,
  `AssetRelation`, `AssetLink`, `AssetSubtypeValidator`, `KnowledgeState`,
  observations, findings, and evidence links.
- Cross-cutting contracts: `GlobalExceptionHandler`, `ErrorResponse`, domain
  exceptions, `ApiPathMatrix`, `IpAllowlistFilter`, `BearerTokenAuthFilter`,
  `ActorFilter`, `ActorHolder`, `RequestLoggingFilter`, MDC, Envers, Flyway,
  ArchUnit, and `make policy`.

## Cross-Cutting Layers

- MCP public schema: validate caller shape with Zod and handler-side `reqArg`,
  but treat backend DTOs and services as the semantic validators. Public MCP
  args should stay snake_case; request bodies should pass through shared
  allowlists and `toCamelCase`. Do not add caller-supplied headers, bearer
  tokens, absolute URLs, methods, Cypher strings, or raw SQL.
- MCP runtime and secrets: keep `GC_BASE_URL`, `GROUND_CONTROL_API_TOKEN`, and
  `GROUND_CONTROL_PACK_REGISTRY_ADMIN_TOKEN` in env or consumer-repo `.env`.
  GC-L007 must not require tokens in `.mcp.json`, tool arguments, logs, process
  argv, or returned error text.
- Backend auth: every new REST route should stay under `/api/v1/**` unless an
  ADR updates the path matrix. Bearer traffic must pass `IpAllowlistFilter`,
  `BearerTokenAuthFilter`, Spring authorization via `ApiPathMatrix`, then
  `ActorFilter`. Browser/session traffic uses the browser chain with the same
  path matrix. Analysis catalog visibility is not authorization.
- Project scoping: every analysis must resolve one project and use
  project-scoped repositories, graph projections, or `GraphTargetResolverService`.
  Do not read repo- or project-unscoped sync tables, caches, local files, or
  global process state from a project-scoped GRC analysis.
- Request validation: DTO annotations and Jackson enum binding own shape.
  Services own same-project checks, methodology/profile compatibility,
  framework identifier semantics, vendor/asset scope, evidence freshness
  windows, graph traversal bounds, duplicate rules, and method-specific output
  attribution.
- Methodology validation: profile schemas exist today, but there is no
  canonical JSON Schema validation service. If GC-L007 adds schema validation
  or methodology execution, introduce one reusable service behind the domain
  boundary with one dependency and one structured `DomainValidationException`
  detail shape. Do not copy schema checks into controllers, MCP handlers, or
  per-methodology switch blocks.
- Graph safety: graph analysis must use `MixedGraphService` /
  `MixedGraphClient` and `GraphTraversalLimits`. AGE access remains in
  `AgeGraphService`; dynamic labels, relationship types, property keys, graph
  names, depth, root sets, entity-type filters, and path counts must pass the
  existing allowlists and caps. Do not expose Cypher passthrough through MCP.
- Error envelope: throw existing domain exceptions and let
  `GlobalExceptionHandler` / `ErrorResponse` serialize them. MCP must preserve
  backend `RequestError` envelopes. Error detail may include stable field
  names, method keys, IDs, confidence bands, and summary counts, but must not
  echo raw evidence payloads, raw observations, full assessment inputs, stack
  traces, headers, response bodies, or tokens.
- Logging and observability: use SLF4J with low-cardinality event names and
  stable IDs. Request and actor context come from `RequestLoggingFilter`,
  `ActorFilter`, and MDC. Do not log raw methodology inputs/outputs, evidence
  summaries, observation values, questionnaire answers, free-text metadata,
  bearer tokens, session IDs, or large graph payloads.
- Persistence: durable analysis outputs that become records must follow the
  existing audited entity pattern: `BaseEntity`, project scope, Flyway,
  `_audit` table parity, Envers, indexes for primary reads, migration smoke
  list updates, and audit-retention updates when applicable. Pure analysis can
  remain read-only response DTOs.
- API/MCP enum mirrors: new API-visible methodology, framework, analysis-kind,
  rating, source-kind, graph-entity, or confidence enums must follow ADR-034's
  mirror policy. Update backend enum, MCP constants/Zod enums, frontend types,
  docs, and policy inventory together.
- Workflow gates: controller changes require `docs/API.md`,
  `mcp/ground-control/lib.js`, `mcp/ground-control/index.js`, and matching
  `@WebMvcTest` updates. MCP changes need adapter tests. Graph and methodology
  semantics need service tests. Run `make policy` before declaring work done.

## Result Contract

Every GC-L007 analysis result should be structured for agent use and carry
method attribution rather than a human report blob. At minimum, methodology
or semantics-bearing results need:

- `analysisKind` or equivalent discriminator.
- `project`, scope inputs, and as-of/evaluation timestamp.
- `methodologyProfileId` / `profileKey` / `family` / `version` when a risk
  methodology is involved.
- `model` or `derivationMethod` when an algorithm, formula, matrix, or
  freshness rule was used.
- `scale`, `units`, and `confidence` or `uncertainty` where relevant.
- Structured `inputs`, `outputs`, `findings`, `evidence`, and `graphPaths`
  sections with source entity IDs or canonical identifiers.
- Explicit limitations when the analysis uses an external framework identifier,
  a third-party asset rather than a vendor aggregate, incomplete knowledge
  state, missing evidence, or a methodology whose schema was not validated.

Do not return only prose. Do not collapse FAIR dollars, NIST ordinal bands,
control effectiveness ratings, compliance posture percentages, and graph
reachability into one undifferentiated `score`.

## Extensibility

The extension seam is a methodology-aware analysis registry behind the backend
service boundary, keyed by analysis kind plus method identity. The obvious next
change is adding another methodology or another framework-specific posture view.
That should require adding a handler/strategy and profile data, not changing
MCP transport, auth, graph adapters, error envelopes, or persistence policy.

Parameters that need to exist at the seam:

- `analysisKind`: risk execution, FAIR quantitative, FAIR-CAM controls,
  NIST workflow, compliance posture, vendor aggregation, cross-framework gap,
  evidence freshness, asset exposure, control state, graph traversal, path
  analysis.
- `project`: required or resolved once at the controller boundary.
- `methodologyProfileId` or `profileKey` plus optional `methodologyFamily` for
  risk-method executions.
- `frameworkIdentifier` / `frameworkVersion` for framework posture and gap
  analysis, clearly marked as external until a first-class framework aggregate
  exists.
- `assetId`, `assetType`, `knowledgeState`, or `vendorAssetId` for vendor and
  asset-aware analyses.
- `asOf`, `freshnessWindow`, and `includeSuperseded` for evidence freshness.
- `entityTypes`, `rootNodeIds`, `sourceNodeId`, `targetNodeId`, and `maxDepth`
  for graph/path analysis, bounded by `GraphTraversalLimits`.

On the MCP side, prefer one consolidated GRC analysis surface or carefully
bounded new `kind` values on `gc_analyze` over many report-specific tools. The
tool must call fixed REST endpoints through `request()` and reuse
`gc_graph`/`gc_query` for read/traversal cases where those already fit.

## Gotchas And Anti-Patterns

- Do not implement FAIR, NIST, ISO, compliance posture, vendor risk, and
  evidence freshness as one generic `risk_score`.
- Do not write assessment outputs back onto `RiskScenario`,
  `RiskRegisterRecord`, `Control`, or `OperationalAsset` as global truth.
  Store durable method-specific outputs in `RiskAssessmentResult` or a
  clearly scoped derived artifact.
- Do not treat `ControlStatus.OPERATIONAL` as evidence that a control is
  effective. Use `ControlTest` and `ControlEffectivenessAssessment`.
- Do not treat a stale evidence artifact, expired observation, superseded
  evidence artifact, or archived asset as current without an explicit
  parameter and response flag.
- Do not use Envers audit rows as the primary freshness/history model for GRC
  facts. Use domain timestamps and append-only/supersede semantics.
- Do not add MCP-local JSON schema validation, custom ObjectMappers, duplicate
  exception hierarchies, duplicate security filters, duplicate graph clients,
  or duplicate audit writers.
- Do not bypass `GraphTraversalLimits`, `AgeGraphService` property-key
  allowlists, or `GraphTargetResolverService` with ad hoc graph queries.
- Do not add a generic `gc_call_api` write tool, Cypher passthrough, direct
  database calls, shell-outs, local file scans, or network calls from MCP
  analysis handlers.
- Do not store framework mappings, vendor attributes, questionnaire answers, or
  remediation decisions in `metadata` solely because a first-class aggregate is
  missing.
- Do not forget AGE property-key registration and tests when new graph
  contributors or analysis-facing graph properties are added; materialization
  rejects unknown keys by design.
- Do not copy the existing `gc_analyze` object-call mismatch pattern for helper
  signatures. MCP helper signatures, Zod fields, and backend query parameter
  names must be locked by adapter tests.

## Whole-Repo Surfaces In Scope

- Backend packages: `api/admin`, `domain/riskscenarios`, `domain/controls`,
  `domain/evidence`, `domain/assets`, `domain/audits`, `domain/graph`,
  `domain/exception`, `shared/security`, `shared/logging`, `shared/web`, and
  `infrastructure/age`.
- MCP packages: `mcp/ground-control/lib.js`, `index.js`, `gc-query.js`,
  `gc-risk-governance.js`, `gc-control.js`, `gc-evidence.js`, `gc-audit.js`,
  and matching tests.
- Docs and policy: `docs/API.md`, `docs/architecture/ARCHITECTURE.md`,
  `docs/CODING_STANDARDS.md`, `architecture/adrs/032`, `034`, `035`, `039`,
  `045`, `046`, `.ground-control.yaml`, `.gc/plan-rules.md`,
  `tools/policy/checks.py`, and `Makefile`.
- Runtime/config: `application.yml`, `SecurityProperties`, `ApiPathMatrix`,
  Logback MDC keys, Flyway migrations, Envers revision actor provenance, AGE
  graph config, MCP env token loading, and OS process argv exposure.

## Non-Goals

- No implementation of GC-L007 behavior in this preflight.
- No new backend aggregate, migration, controller, repository, graph
  contributor, MCP tool, or calculation engine solely from this note.
- No first-class vendor, questionnaire, remediation-plan, or compliance
  framework mapping aggregate. Those require separate backend decisions.
- No replacement of `RiskAssessmentResult`, `MethodologyProfile`,
  `Observation`, `EvidenceArtifact`, `ControlTest`,
  `ControlEffectivenessAssessment`, `AuditLink`, `OperationalAsset`, or the
  mixed graph substrate.
- No change to ADR-026 security, ADR-032 AGE query construction, ADR-034 enum
  mirror policy, ADR-035 MCP curation, ADR-045 evidence semantics, or repo
  workflow gates.
