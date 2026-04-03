# Asset/Risk Groundwork Review

Date: 2026-04-02

## Scope

This review was requested for the "last four PR merges to dev" related to the new asset and risk groundwork. The `dev` history has a merge anomaly:

- `#470` (`238286b`, 2026-04-01) is a second merge from the same branch as `#469` and contains mostly workflow movement, not the substantive asset-domain code.

For the substantive review, I treated the groundwork set as:

- `#469` (`9b3f661`, 2026-04-01): asset cross-entity linking
- `#472` (`e16a62b`, 2026-04-01): external identifiers and source provenance
- `#474` (`d7c962c`, 2026-04-01): observations / state facts
- `#476` (`2d7f23e`, 2026-04-02): risk scenario entity

I also reviewed `#467` (`d452eb2`, 2026-03-31) as prerequisite context because it introduced the core `OperationalAsset` and `AssetRelation` model that the later PRs extend.

## Method

- Reviewed merge diffs, current domain model, controllers, repositories, migrations, API docs, ADRs, graph integration code, and relevant tests.
- Ran targeted backend tests for asset/risk areas and a full backend `./gradlew test` pass.
- Cross-checked the design against authoritative external guidance from NIST, OWASP, W3C, Apache AGE, and Neo4j official docs.

## Executive Summary

The new groundwork is directionally strong in three ways:

- It creates a coherent asset domain instead of scattering topology into ad hoc fields.
- It introduces provenance-oriented concepts early (`sourceSystem`, `collectedAt`, `confidence`, observations).
- It explicitly recognizes that risk should be scenario-based rather than just a severity label.

The main concern is that the code is currently building three parallel worlds rather than one composable model:

- a requirements-only graph stack,
- a relational asset topology stack,
- and a narrative risk stack with only partial structured linking.

That split is manageable today, but if not corrected before the next wave of work, it will make future graph integration, authorization, provenance, and query semantics materially harder.

## Findings

### 1. High: project boundaries are not enforced on object-ID routes, which breaks isolation now and sets up BOLA/IDOR risk later

Why it matters:

- The codebase already has project scoping as a first-class concept.
- However, many read/write paths fetch by raw UUID and never prove that the object belongs to the caller's intended project.
- This is already a data-isolation bug at the application boundary and becomes a security vulnerability the moment per-project access control is added.

Evidence:

- `backend/src/main/java/com/keplerops/groundcontrol/api/assets/AssetController.java:69`
- `backend/src/main/java/com/keplerops/groundcontrol/api/assets/AssetController.java:80`
- `backend/src/main/java/com/keplerops/groundcontrol/api/assets/AssetController.java:86`
- `backend/src/main/java/com/keplerops/groundcontrol/domain/assets/service/AssetService.java:85`
- `backend/src/main/java/com/keplerops/groundcontrol/api/assets/ObservationController.java:23`
- `backend/src/main/java/com/keplerops/groundcontrol/api/riskscenarios/RiskScenarioController.java:63`
- `backend/src/main/java/com/keplerops/groundcontrol/domain/riskscenarios/service/RiskScenarioService.java:156`
- `backend/src/main/java/com/keplerops/groundcontrol/api/admin/GraphController.java:32`
- `backend/src/main/java/com/keplerops/groundcontrol/api/admin/GraphController.java:99`

What is happening:

- Assets, observations, and risk scenarios are commonly addressed by global UUID.
- Graph endpoints accept a `project` parameter but `ancestors`, `descendants`, and `paths` do not apply it to the underlying query.
- The result is an API surface where project resolution is sometimes mandatory, sometimes ignored, and sometimes bypassed entirely.

Why the literature says this matters:

- OWASP API Security Top 10 2023 API1 says every endpoint that takes an object ID should enforce object-level authorization checks, and that failures lead to unauthorized disclosure, modification, or destruction.

Recommendations:

1. Make project membership a mandatory part of every asset/risk/graph lookup path, not just list endpoints.
2. Change service methods from `getById(id)` to `getById(projectId, id)` for project-scoped entities.
3. Add repository methods that join through `project_id` and fail closed.
4. Add negative tests proving cross-project UUID access is rejected for every by-ID route.
5. Treat the current global-UUID access pattern as technical debt that must be retired before auth is introduced.

### 2. High: the graph foundation is still requirements-only, so assets and risk are not actually graph-ready yet

Why it matters:

- The stated goal is to start adding assets and risk to the graph.
- But the graph infrastructure, graph API, graph response types, and graph UI are still hard-coded to requirements.
- If more work lands on top of the current shape, the team will either duplicate graph logic for assets/risk or break the existing graph contract later.

Evidence:

- `backend/src/main/java/com/keplerops/groundcontrol/infrastructure/age/AgeGraphService.java:53`
- `backend/src/main/java/com/keplerops/groundcontrol/infrastructure/age/AgeGraphService.java:68`
- `backend/src/main/java/com/keplerops/groundcontrol/domain/requirements/service/GraphVisualizationResult.java:1`
- `backend/src/main/java/com/keplerops/groundcontrol/api/admin/GraphController.java:54`
- `backend/src/main/java/com/keplerops/groundcontrol/api/admin/GraphController.java:84`
- `frontend/src/pages/graph.tsx:19`
- `docs/API.md:514`

What is happening:

- AGE materialization creates only `Requirement` nodes and requirement relations.
- `GraphVisualizationResult` only carries `Requirement` and `RequirementRelation`.
- The `entityTypes` filter in `GraphController` is post-filtering a requirements-only response and does not filter edges consistently.
- The frontend graph page still assumes requirement-shaped nodes.

Why the literature says this matters:

- Neo4j's official modeling guide emphasizes defining entities, unique identifiers, relationship types, and use-case-driven traversal shape before scaling the graph model.
- Apache AGE's official docs show the graph query surface is explicit and schema decisions affect how you project and query graph data.

Recommendations:

1. Introduce a graph projection abstraction now, before more asset/risk analysis lands.
2. Model graph responses as generic node/edge references with `entityType`, stable identifiers, and typed properties instead of requirement-specific records.
3. Decide explicitly whether PostgreSQL tables or AGE materialization are the source of truth for cross-entity graph traversal.
4. Make `entityTypes` filtering semantic, not cosmetic: node filtering must also filter edges.
5. Do not add more graph endpoints for assets/risk until the graph contract is generalized.

### 3. High: the risk scenario lifecycle is more mature than the risk data model

Why it matters:

- The status machine says a scenario can be `ASSESSED`, `TREATED`, and `ACCEPTED`.
- The entity itself only stores the narrative scenario statement and a few free-text context fields.
- There is nowhere to store assessed likelihood, impact, residual risk, treatment decision, owner, acceptance rationale, or review cadence.

Evidence:

- `backend/src/main/java/com/keplerops/groundcontrol/domain/riskscenarios/state/RiskScenarioStatus.java:5`
- `backend/src/main/java/com/keplerops/groundcontrol/domain/riskscenarios/model/RiskScenario.java:37`
- `backend/src/main/java/com/keplerops/groundcontrol/domain/riskscenarios/service/CreateRiskScenarioCommand.java:5`

What is happening:

- The current entity is a good scenario statement.
- It is not yet a full assessment or treatment object.
- That means the workflow semantics are ahead of the persisted model semantics.

Why the literature says this matters:

- NIST SP 800-30 explicitly treats likelihood and impact as core parts of risk assessment.
- The same guidance also treats vulnerabilities and predisposing conditions as structured inputs to risk determination.

Recommendations:

1. Keep `RiskScenario` as the scenario statement.
2. Add a separate `RiskAssessment` or `RiskDisposition` entity before the team starts using `ASSESSED`, `TREATED`, or `ACCEPTED` in real workflows.
3. Store at least:
   - assessment date,
   - likelihood,
   - impact,
   - residual risk,
   - treatment option,
   - decision owner,
   - acceptance rationale,
   - review/expiry date.
4. Until then, consider limiting the lifecycle to `DRAFT` and `IDENTIFIED`.

### 4. Medium-High: observations are described as time-bounded observed facts, but the model lets callers rewrite the fact after the fact

Why it matters:

- The domain comment describes observations as captured state facts with source, observed-at time, freshness window, and evidence.
- The API allows updating `observationValue`, which mutates the historical fact instead of appending a new observation.
- `update()` also omits the temporal validation that `create()` performs, so callers can make an observation invalid after creation.

Evidence:

- `backend/src/main/java/com/keplerops/groundcontrol/domain/assets/model/Observation.java:17`
- `backend/src/main/java/com/keplerops/groundcontrol/domain/assets/service/ObservationService.java:28`
- `backend/src/main/java/com/keplerops/groundcontrol/domain/assets/service/ObservationService.java:59`
- `backend/src/main/java/com/keplerops/groundcontrol/api/assets/UpdateObservationRequest.java:6`

What is happening:

- A record observed at `T1` can later be changed to a different value while still claiming it was observed at `T1`.
- An update can also set `expiresAt < observedAt`, which `create()` rejects.

Why the literature says this matters:

- W3C PROV treats provenance as information about entities, activities, and people used to assess quality, reliability, and trustworthiness.
- Versioning and provenance of provenance are first-class concerns in that model.

Recommendations:

1. Make observations append-only for value-bearing fields.
2. Restrict updates to metadata corrections only, or create a correction/amendment mechanism explicitly.
3. Re-run the same `expiresAt >= observedAt` validation in `update()`.
4. Add tests that prove invalid temporal updates are rejected.

### 5. Medium-High: the link model lacks canonical target references and now has two sources of truth for context

Why it matters:

- `AssetLink` and `RiskScenarioLink` use raw `targetType + targetIdentifier` strings.
- Several target types are now first-class entities, but links still do not validate existence or define a canonical identifier scheme.
- `RiskScenario` simultaneously keeps free-text `observationRefs` and `topologyContext`, even though `RiskScenarioLink` exists to model observations, assets, evidence, and requirements structurally.

Evidence:

- `backend/src/main/java/com/keplerops/groundcontrol/domain/assets/model/AssetLink.java:21`
- `backend/src/main/java/com/keplerops/groundcontrol/domain/riskscenarios/model/RiskScenarioLink.java:22`
- `backend/src/main/java/com/keplerops/groundcontrol/api/assets/AssetLinkRequest.java:9`
- `backend/src/main/java/com/keplerops/groundcontrol/api/riskscenarios/RiskScenarioLinkRequest.java:9`
- `backend/src/main/java/com/keplerops/groundcontrol/domain/riskscenarios/model/RiskScenario.java:65`

What is happening:

- The system now supports target types like `RISK_SCENARIO`, `ASSET`, `OBSERVATION`, and `REQUIREMENT`.
- But those targets do not all share a globally unique human identifier.
- Example: assets and risk scenarios are project-scoped; observations are UUID-based and do not have a user-facing UID at all.

Design consequence:

- Traversal, reverse lookup, validation, and future graph projection will all need ad hoc conventions for each target type.
- The free-text `observationRefs` and `topologyContext` fields guarantee drift from the structured links over time.

Recommendations:

1. Define a canonical reference scheme for first-class targets now.
2. For first-class entities, prefer typed foreign-key-backed link tables or a typed reference object that can carry:
   - target entity type,
   - target UUID,
   - target project identifier when needed,
   - optional display UID/title.
3. Deprecate `observationRefs` and `topologyContext` once structured links exist for those concepts.
4. Keep raw string targets only for genuinely external or not-yet-materialized entities.

### 6. Medium: audit provenance is currently client-asserted and therefore spoofable

Why it matters:

- The system records `actor` in Envers revisions and `createdBy` on risk scenarios.
- That identity is sourced from the `X-Actor` request header, defaulting to `anonymous`.
- In other words, the audit trail is not a trustworthy statement of who performed a change.

Evidence:

- `backend/src/main/java/com/keplerops/groundcontrol/shared/web/ActorFilter.java:15`
- `backend/src/main/java/com/keplerops/groundcontrol/domain/audit/GroundControlRevisionListener.java:8`
- `backend/src/main/java/com/keplerops/groundcontrol/domain/riskscenarios/service/RiskScenarioService.java:55`
- `docs/architecture/ARCHITECTURE.md:118`

What is happening:

- Any caller can claim any actor name by sending an HTTP header.
- That may be acceptable for local development, but it is not acceptable for security, compliance, or accountability claims.

Recommendations:

1. Treat the current actor mechanism as a development-only placeholder.
2. When auth is introduced, bind audit identity to authenticated principal claims, not caller-controlled headers.
3. If a service-to-service actor override is needed, sign it and validate it at the trust boundary.
4. Mark current audit fields as non-authoritative in docs until that is fixed.

### 7. Medium: asset-topology analysis currently ignores relationship semantics and will get noisy as the graph grows

Why it matters:

- The asset model correctly distinguishes `CONTAINS`, `DEPENDS_ON`, `COMMUNICATES_WITH`, `TRUST_BOUNDARY`, `SUPPORTS`, `ACCESSES`, and `DATA_FLOW`.
- The analysis layer mostly ignores those distinctions and treats all edges alike for cycle detection, impact analysis, and connected-subgraph extraction.
- ADR-019 explicitly notes that some asset relation types legitimately form cycles.

Evidence:

- `architecture/adrs/019-asset-topology-model.md:24`
- `backend/src/main/java/com/keplerops/groundcontrol/domain/assets/service/AssetTopologyService.java:33`
- `backend/src/main/java/com/keplerops/groundcontrol/domain/assets/service/AssetTopologyService.java:71`
- `backend/src/main/java/com/keplerops/groundcontrol/domain/requirements/service/GraphAlgorithms.java:35`

What is happening:

- `detectCycles()` runs across all active relation types.
- `impactAnalysis()` also traverses all active relation types.
- In practice, that means expected communication cycles can dominate the signal and impact propagation can become semantically questionable.

Recommendations:

1. Make analysis edge-type-aware.
2. Add explicit relation-type filters for cycle detection and impact analysis.
3. For cycle reporting, consider SCC-based summarization or canonical cycle deduplication rather than back-edge reporting if dense communication graphs are expected.
4. Decide which relation types imply impact propagation and document that contract.

### 8. Medium-Low: identifier and documentation consistency are drifting across the new domains

Why it matters:

- Assets normalize UIDs to uppercase and query case-insensitively.
- Risk scenarios do neither.
- API and architecture docs are already out of sync with the actual surface added by the latest PRs.

Evidence:

- `backend/src/main/java/com/keplerops/groundcontrol/domain/assets/service/AssetService.java:53`
- `backend/src/main/java/com/keplerops/groundcontrol/domain/riskscenarios/service/RiskScenarioService.java:42`
- `backend/src/main/java/com/keplerops/groundcontrol/domain/riskscenarios/repository/RiskScenarioRepository.java:11`
- `docs/API.md:401`
- `docs/architecture/ARCHITECTURE.md:106`

Examples:

- `docs/API.md` documents asset links and topology, but not the newer external-ID, observation, or risk-scenario endpoints.
- `docs/API.md` advertises graph entity-type filtering as if multiple entity types are already present.
- `docs/architecture/ARCHITECTURE.md` still describes the system primarily as requirements-only.

Recommendations:

1. Choose one UID policy for all project-scoped human identifiers.
2. Update docs as part of each domain PR, not as a later cleanup.
3. Add contract tests or docs checks for newly exposed endpoints.

## Recommended Fix Roadmap

### Phase 1: before more asset/risk graph work lands

- Enforce project-scoped lookups for all asset/risk/graph by-ID operations.
- Generalize the graph projection contract away from requirements-only types.
- Decide and document canonical identifiers for first-class cross-entity links.
- Fix observation update invariants and make a call on append-only vs mutable semantics.

### Phase 2: before risk workflows are used beyond prototyping

- Split scenario statement from assessment/disposition state.
- Add structured risk assessment fields or sub-entities.
- Replace client-supplied audit identity with principal-bound identity.

### Phase 3: before scale-up

- Make topology analysis relation-type-aware.
- Add pagination/filtering strategy for large inventories and fact tables.
- Consider database-level integrity reinforcement for cross-project invariants where service-only enforcement is too fragile.

## Tests Run

- `cd backend && ./gradlew test --tests '*Asset*' --tests '*Observation*' --tests '*RiskScenario*'`
- `cd backend && ./gradlew test`

Result:

- Both completed successfully on 2026-04-02.

## Authoritative External References

- NIST SP 800-30 Rev. 1, "Guide for Conducting Risk Assessments"
  https://csrc.nist.gov/pubs/sp/800/30/r1/final
  Key alignment point: risk assessment explicitly depends on structured likelihood and impact determination, not only scenario narrative.

- NIST SP 800-30 Rev. 1 PDF
  https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-30r1.pdf
  Key alignment point: vulnerability and predisposing conditions, likelihood, and impact are explicit parts of the assessment workflow.

- NISTIR 8011 Vol. 2 / Vol. 4 (CM-8 automation narratives surfaced in search snippets)
  https://nvlpubs.nist.gov/nistpubs/ir/2017/nist.ir.8011-2.pdf
  https://nvlpubs.nist.gov/nistpubs/ir/2020/NIST.IR.8011-4.pdf
  Key alignment point: authoritative inventory practice stresses up-to-date, complete, accurate, readily available component inventories and accountability information.

- OWASP API Security Top 10 2023, API1: Broken Object Level Authorization
  https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/
  Key alignment point: every endpoint that accepts an object ID should enforce object-level authorization.

- W3C PROV Overview / PROV family
  https://www.w3.org/TR/prov-overview/
  Key alignment point: provenance is about entities, activities, and people used to assess quality, reliability, and trustworthiness; versioning and provenance-of-provenance matter.

- Apache AGE documentation, "The AGE Cypher Query Format"
  https://age.apache.org/age-manual/v0.6.0/intro/cypher.html
  Key alignment point: graph query shape is explicit and should be treated as a designed interface, not an accidental byproduct.

- Neo4j official data-modeling tutorial
  https://neo4j.com/docs/getting-started/data-modeling/tutorial-data-modeling/
  Key alignment points: define use cases first, enforce unique identifiers, and choose specific relationship types that match traversal semantics.
