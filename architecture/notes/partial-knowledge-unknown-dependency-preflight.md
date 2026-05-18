# Partial Knowledge and Unknown Dependency Preflight

Issue: #726
Requirement: GC-M018

This is architecture guardrail guidance for implementation of GC-M018. It is
not an implementation plan.

## Boundary

GC-M018 extends the existing asset knowledge model so workflows can tell the
difference between confirmed facts, provisional facts, and missing coverage. It
must build on the current project-scoped asset, observation, topology, link,
risk, threat, control, evidence, graph, audit, and MCP surfaces.

Keep these concepts separate:

- Asset identity: `OperationalAsset` remains the canonical asset aggregate.
  A manually asserted asset is still an asset row with normal project scope,
  audit history, UID uniqueness, links, observations, external IDs, and graph
  identity.
- Classification: `AssetType`, `subtype`, and subtype `metadata` describe what
  an asset is. Unknown or unclassified coverage is a knowledge state, not a
  subtype metadata key and not automatically the same as `AssetType.OTHER`.
- Topology fact: `AssetRelation` is an asserted edge between two modeled assets.
  It already carries `sourceSystem`, `externalSourceId`, `collectedAt`, and
  `confidence` provenance fields.
- Observed fact: `Observation` records time-bounded evidence about an asset. It
  can support or challenge a topology assertion, but it is not the topology edge
  itself.
- External/source identity: `AssetExternalId` maps modeled assets to external
  inventories. It is not a placeholder for an unknown dependency.
- Cross-entity scope: `AssetLink`, `RiskScenarioLink`, `ThreatModelLink`, and
  `ControlLink` connect first-class entities. They are not a replacement for
  asset-to-asset topology.
- Assessment uncertainty: `RiskAssessmentResult.confidence` and
  `uncertaintyMetadata` describe risk-assessment confidence, not asset-model
  completeness by themselves.

## Incumbents To Reuse

- Asset aggregate and API: `OperationalAsset`, `AssetService`,
  `OperationalAssetRepository`, `AssetController`, `AssetRequest`,
  `UpdateAssetRequest`, `AssetResponse`, and the existing clear-flag
  convention for nullable asset fields.
- Topology: `AssetRelation`, `AssetRelationType`, `AssetTopologyService`,
  `AssetRelationRequest`, `UpdateAssetRelationRequest`, and
  `AssetGraphProjectionContributor`.
- Evidence and provenance: `Observation`, `ObservationService`,
  `ObservationCategory`, `AssetExternalId`, source / collected-at / confidence
  fields, Envers, `ActorFilter`, `ActorHolder`, and MDC.
- Classification and metadata: `AssetType`, `subtype`,
  `AssetSubtypeSchema`, `AssetSubtypeValidator`, and
  `JacksonTextCollectionConverters.StringObjectMapConverter`. Do not add
  feature-local JSON parsing or schema validation.
- Cross-entity target validation: `GraphTargetResolverService` remains the
  same-project internal-target gate for asset, risk, threat, control, finding,
  evidence, and observation links.
- Risk / threat / control boundaries: `RiskScenario`, `RiskAssessmentResult`,
  `RiskRegisterRecord`, `ThreatModel`, `Control`, `ControlTest`, and
  `ControlEffectivenessAssessment` already separate scenario statements,
  assessments, governance records, threat entries, controls, tests, and
  effectiveness assessments.
- Graph: `GraphEntityType`, `GraphIds`, `GraphProjectionContributor`,
  `AssetGraphProjectionContributor`, `RiskGraphProjectionContributor`,
  `ThreatModelGraphProjectionContributor`, and `ControlGraphProjectionContributor`.
  Do not write AGE rows directly from asset services or controllers.
- API / MCP mirrors: `docs/API.md`, frontend API types, `mcp/ground-control/lib.js`,
  the consolidated `gc_asset` tool, `link-create.js`, `pick`, `reqArg`,
  `toCamelCase`, `TO_CAMEL`, `RequestError`, and `addAuthorizationHeader`.
- Persistence and audit: Flyway migrations, Envers audit-table parity,
  project-scoped indexes, `MigrationSmokeTest`, audit retention allowlists where
  applicable, and the repo policy checks.

## Cross-Cutting Layers

- Security: keep new or changed endpoints under `/api/v1/assets/**` or an
  existing analysis/query surface unless an ADR expands the path matrix.
  Bearer traffic must pass `IpAllowlistFilter`, `BearerTokenAuthFilter`,
  Spring authorization via `ApiPathMatrix`, and `ActorFilter`. Browser/session
  traffic must pass the ADR-037 browser chain, same path matrix, and CSRF
  protection for cookie-authenticated mutations. Do not add controller-local
  auth, role enums, actor override fields, caller-supplied tokens, or routes
  outside the shared matrix.
- Project scoping: every read and write must resolve a project through
  `ProjectService` and use project-scoped repositories or
  `GraphTargetResolverService`. Do not use deprecated UUID-only asset service
  overloads on new paths, and do not let provisional or unresolved references
  bypass same-project checks.
- Request parsing and validation: Jackson enum binding and Bean Validation own
  DTO shape. Services own semantic rules: same-project checks, duplicate
  detection, clear-vs-assign behavior, placeholder/provisional compatibility,
  confidence vocabulary or bounds, and any "confirmed vs provisional vs missing"
  state transition rules.
- Metadata validation: subtype metadata still goes through
  `AssetSubtypeValidator`. Do not store model completeness, topology gaps,
  placeholder identity, source provenance, links, observations, owner/scope, or
  lifecycle state in the subtype metadata bag.
- Error envelope: use `NotFoundException`, `ConflictException`, and
  `DomainValidationException`; `GlobalExceptionHandler` and
  `shared.web.ErrorResponse` remain the only HTTP error contract. Validation
  details may include stable field names, IDs, confidence values, and reason
  codes, but must not echo raw metadata maps, raw observation values, request
  bodies, credentials, tokens, or stack traces.
- Observability: rely on `RequestLoggingFilter`, `ActorFilter`, MDC, Envers,
  and low-cardinality service logs. Log stable IDs, UIDs, state names,
  confidence bands, and source-system labels only when useful. Never log bearer
  tokens, session IDs, CSRF tokens, raw topology metadata, raw subtype metadata,
  full observations, or full risk/control payloads.
- Persistence: schema changes must use Flyway and Envers audit-table parity for
  audited entities. Preserve project scope, uniqueness invariants, delete
  guardrails for inbound references, and repository-level indexes for asset
  list, topology traversal, and graph projection read paths.
- MCP transport: extend the existing consolidated `gc_asset` surface and shared
  link helpers where the wire contract changes. Do not add asset-specific HTTP
  clients, custom auth headers, custom token arguments, ad hoc snake/camel
  mappers, or duplicated target-type validation in MCP.
- Configuration and OS/runtime exposure: GC-M018 should not require new
  secrets, env bindings, subprocesses, shell-outs, local file scans, network
  calls, or token-in-argv patterns. A future inventory importer belongs behind
  `@ConfigurationProperties`, startup validation, and an infrastructure adapter,
  with secrets kept out of argv, logs, and error envelopes.

## Extensibility

The main seam is a low-cardinality, queryable knowledge/completeness dimension
on the canonical asset/topology surfaces, with provenance and confidence kept
close to the assertion it qualifies. The next reasonable changes are filtered
views for "show unknown dependencies," "show provisional assets," "show
coverage gaps below confidence X," and "promote a provisional assertion to a
confirmed fact." Those should require repository filters, graph properties, and
service rules over the same canonical artifacts, not new workflow-specific
tables.

If confidence becomes machine-comparable, use one canonical vocabulary and one
normalization/validation path. The existing repo already has `HIGH / MEDIUM /
LOW` confidence bands for derived analysis and free-text `confidence` fields on
asset relations, external IDs, observations, evidence, and risk assessments.
Do not introduce separate `AssetConfidence`, `TopologyConfidence`,
`RiskConfidence`, and MCP-only confidence parsers. If a shared enum/package move
is needed, update backend, docs, frontend, and MCP mirrors together and keep
ArchUnit package rules satisfied.

Unknown dependency modeling needs an explicit unresolved-target seam. An
unknown dependency must not be silently represented as a confirmed relation to a
real-looking asset unless the target asset is explicitly marked placeholder or
provisional. Conversely, do not put unresolved dependency strings only in
`Observation.observationValue` or `RiskScenario.affectedObject`; those are
supporting context, not graph topology.

## Gotchas And Anti-Patterns

- Do not create a second inventory, generic graph-node table, CMDB adapter, or
  workflow-specific asset model for partial knowledge.
- Do not treat `AssetType.OTHER`, `subtype = null`, `metadata = null`, or
  `scopeDesignation = null` as interchangeable signals. "Other,"
  "unclassified," "unknown," "not scoped," and "not yet modeled" are different
  questions.
- Do not fake precision by forcing unknown dependencies to point at arbitrary
  real assets, or by creating placeholder assets that look confirmed in graph,
  risk, threat, or control views.
- Do not make provisional topology a risk-scenario field, threat-model narrative
  convention, control implementation-scope convention, or arbitrary
  `decisionMetadata` / `methodologyFactors` key.
- Do not duplicate target validation, project ownership checks, exception
  hierarchies, error envelopes, JSON parsers, graph materializers, audit writers,
  security filters, or MCP transport helpers.
- Do not log raw metadata, observation values, evidence refs, external
  identifiers that may contain secrets, request bodies, or auth material.
- Do not let coverage reports count provisional or missing assertions as
  confirmed control/risk/threat coverage without an explicit filter or status in
  the response.
- Do not add a workflow engine for promotion from unknown to tentative to
  confirmed unless a later requirement introduces lifecycle automation.

## Non-Goals

- No implementation of GC-M018 in this preflight note.
- No external CMDB, cloud inventory, scanner, import scheduler, or reconciliation
  worker.
- No replacement of `OperationalAsset`, `AssetRelation`, `Observation`,
  `AssetExternalId`, `AssetLink`, risk/threat/control links, or graph
  contributors.
- No change to ADR-026 / ADR-037 security, actor provenance, or the shared
  `ErrorResponse` envelope.
- No new risk scoring engine, threat modeling method, control coverage engine,
  or automatic promotion of provisional facts.
- No frontend redesign or visualization overhaul beyond reflecting whatever
  backend contract the implementation later adds.
