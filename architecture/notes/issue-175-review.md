# Issue 175 Review: Requirements Phase 1C Entities

## Summary

Issue 175 is directionally correct and mostly well implemented for the current repository state:

- `TraceabilityLink`, `GitHubIssueSync`, and `RequirementImport` fit the package layout and service-ownership model from ADR-011.
- The implementation matches the repo's current L0 standard: simple entities, repository interfaces, Flyway migrations, and focused persistence tests.
- `TraceabilityLink` being the only audited entity is the right practical choice, even though ADR-011 still contains older blanket-Envers wording.

The main follow-up work is about tightening data-model guarantees and reconciling documentation with the implemented design.

## Recommended Changes

### 1. Prevent duplicate traceability edges

Add a database-level unique constraint on exact duplicate `TraceabilityLink` rows.

Recommended shape:

`UNIQUE (requirement_id, artifact_type, artifact_identifier, link_type)`

Rationale:

- `TraceabilityLink` is an edge table in the same sense that `RequirementRelation` is.
- Duplicate rows would inflate future coverage queries, AGE materialization, and audit history without adding meaning.
- The repository already treats this as link data, not append-only event data.

### 2. Make the typed artifact identifier convention explicit as an ownership rule

Keep the entity as a simple L0 carrier, but document that canonical identifier validation belongs in the future `TraceabilityService`.

Rationale:

- ADR-011 and ADR-014 rely on stable, typed identifiers such as `github:#42`, `file:backend/src/...`, `adr:011`, `tla:...`, and `proof:...`.
- The current implementation stores arbitrary strings, and the current tests use non-canonical examples like `src/Main.java` and `ADR-011`.
- Without a canonical form, future graph queries and cross-tool traceability become more fragile.

Proposed rule:

- Entity persists the value.
- `TraceabilityService` validates and normalizes it before save.
- Tests should use canonical typed examples now so future validation does not require test churn.

### 3. Reconcile ADR-011's Envers wording with the actual design

Update ADR-011 to state that Envers is required for business entities whose historical state matters to analysis, while cache tables and self-auditing import records may track history without Envers.

Rationale:

- ADR-011 currently says all entities are audited.
- Issue 175 deliberately excludes `GitHubIssueSync` and `RequirementImport`, which is reasonable for cache and audit-log records.
- The code and the ADR should say the same thing before more entities are added.

### 4. Add one integration test that proves `TraceabilityLink` revisions are written

Keep the existing migration smoke test, but add a runtime audit assertion for one insert or update.

Rationale:

- Audit tables are hand-created in Flyway, so schema-exists checks are weaker than behavior checks.
- A small Envers round-trip test would catch drift between entity annotations and manual audit migrations.

## Non-Recommendations

The following parts of issue 175 are reasonable as-is:

- `GitHubIssueSync.issueNumber` unique without repository metadata, because the current repo is dogfooding a single GitHub repository.
- JSONB for labels, cross-references, import stats, and import errors.
- No new services in this issue.
- L0 treatment for all three entities.
