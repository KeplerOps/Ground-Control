# ADR-024: Threat Model Entry Boundary

## Status

Accepted

## Date

2026-04-11

## Context

GC-H001 requires first-class threat modeling entities that distinguish threat source, threat event, affected operational asset or boundary, and effect or consequence. The implementation must also keep threat-model outputs linkable to assets, topology or boundary context, risk scenarios, requirements, controls, observations, evidence, architecture models, code, and issues.

The current codebase already has adjacent concepts that are easy to conflate:

- `RiskScenario` stores `threatSource`, `threatEvent`, `affectedObject`, and `consequence`, but it is the risk-domain scenario statement used by risk register, assessment, and treatment flows.
- `AssetLinkTargetType` already reserves `THREAT_MODEL_ENTRY` and `RiskScenarioLinkTargetType` already reserves `THREAT_MODEL`, so the graph/linking model already assumes threat modeling is distinct from risk scenarios.
- The newer cross-entity linking path (`AssetLink`, `RiskScenarioLink`, `GraphTargetResolverService`) supports project-scoped internal targets via `targetEntityId` and leaves raw identifiers only for external or not-yet-modeled targets.

Without an explicit architectural decision, the likely implementation failure modes are:

- overloading `RiskScenario` until it becomes both threat record and risk record
- copying the looser raw-string link pattern into a new threat feature instead of reusing the newer validated internal-target pattern
- inventing a separate graph or traceability substrate for threat artifacts

## Decision

### 1. Threat modeling is a separate aggregate from risk management

Introduce a dedicated threat-model aggregate for GC-H001. It must not be implemented by extending the semantics of:

- `RiskScenario`
- `RiskAssessmentResult`
- `RiskRegisterRecord`
- `TreatmentPlan`

Threat-model entries are upstream security-analysis artifacts. Risk scenarios remain scoped loss scenarios. Risk assessments and risk register records remain the place for likelihood, impact, confidence, residual risk, approval, treatment, and governance state.

Do not add quantified risk fields, treatment state, approval state, or register ownership semantics to the threat-model aggregate.

### 2. Reuse the established domain and persistence shape

Threat modeling follows the same package shape already used by other domains:

```text
domain/threatmodeling/
    model/
    state/
    service/
    repository/
```

Threat-model entities must:

- extend `BaseEntity`
- be project-scoped through `Project`
- mark the `Project` relation `@NotAudited`
- use Hibernate Envers for historical state if the entity or its links affect analysis or graph traversal

If the aggregate needs lifecycle state, use the existing simple enum-transition pattern already used in `requirements`, `controls`, and `riskscenarios`. Do not introduce a workflow engine or state-machine framework.

### 3. Scope and boundary context must reuse the asset model

Affected system scope must reuse `OperationalAsset`, including `AssetType.BOUNDARY` for trust or system boundaries.

Do not create a separate boundary table or a threat-specific asset clone. Do not treat a free-text field as the authoritative affected-system reference when a project-scoped asset or boundary exists.

Narrative text is still allowed for analyst context, but graph-native system scope must come from structured links or references to existing asset records.

### 4. Threat-model outputs use one threat-owned link mechanism

Threat-model links must be anchored on the threat-model entry itself, not retrofitted into `TraceabilityLink`, `AssetLink`, or `RiskScenarioLink`.

For link targets:

- use `targetEntityId` for first-class internal entities
- validate internal targets through a shared resolver path, extending `GraphTargetResolverService` rather than re-implementing project checks in controllers
- use `targetIdentifier` only for genuinely external or not-yet-modeled targets such as architecture-model artifacts, code artifacts, GitHub issues, or external evidence references

When external targets represent artifact categories that already have canonical identifier rules, reuse those conventions from ADR-011 rather than inventing new prefixes or mini-schemas.

### 5. Graph integration reuses the mixed-graph projection path

JPA remains the source of truth. Threat-model nodes and internal link edges must flow into the existing mixed-graph projection via `GraphEntityType`, `GraphIds`, and a dedicated `GraphProjectionContributor`.

Do not add a bespoke graph endpoint, a threat-only graph materialization path, or direct AGE writes from controllers or services.

External threat-model links remain non-nodal references until their targets become first-class entities.

### 6. Validation, exception handling, and observability stay shared

Threat-model API requests should keep only shape validation at the DTO boundary (`@NotBlank`, `@Size`, enum parsing).

Semantic validation such as project scoping, duplicate detection, target existence, and cross-entity consistency must stay in the service layer and use the existing exception hierarchy:

- `DomainValidationException`
- `ConflictException`
- `NotFoundException`

HTTP mapping must continue through `GlobalExceptionHandler`. Logging must continue through the existing SLF4J/MDC setup (`RequestLoggingFilter`, `ActorFilter`) instead of feature-local logging conventions.

## Consequences

### Positive

- Threat modeling stays distinct from quantified risk and risk-governance records.
- Existing project scoping, auditing, validation, graph projection, and error mapping can be reused instead of cloned.
- Asset and boundary grounding stays graph-native and queryable.

### Negative

- GC-H001 requires a new aggregate and likely a threat-owned link entity instead of piggybacking on existing risk tables.
- Graph target resolution and graph projection enums will need coordinated updates when the threat domain is added.

### Risks

- If implementation copies `RiskScenario` fields and lifecycle wholesale, the platform will blur threat analysis with risk assessment again.
- If raw string identifiers are used for first-class internal targets, reverse lookup and graph materialization will drift from the current validated-link direction.
- If threat-model links are split across `TraceabilityLink`, `AssetLink`, and a new threat link surface, ownership and deduplication rules will become inconsistent.
