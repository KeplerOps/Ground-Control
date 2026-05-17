# Test Plan Entity Preflight

Issue: #673
Requirement: TC-006

This note records architecture guardrails for the upcoming Test Plan entity.
It is not an implementation plan.

## Boundary

`TestPlan` is the top-level planning container for a testing effort. It owns
scope metadata, schedule metadata, and release coordinates. It is not a test
case, test-case folder, authored test format, verification result, control
test, traceability link, or graph projection.

The entity belongs inside the existing `testcases` domain boundary unless a
broader `testmanagement` package is introduced by a separate architectural
decision. Keep the vocabulary `TestPlan`; do not rename folders, suites, or
test cases to satisfy TC-006.

Required fields should be modeled directly on the plan:

- project scope via `Project`;
- project-unique `uid`;
- `name` and optional `description`;
- product/version/build release coordinates as bounded scalar fields;
- a dedicated `TestPlanStatus` enum;
- nullable `startDate` / `endDate` schedule fields, with an `endDate >=
  startDate` invariant when both are present.

There is no existing `Product`, `Release`, `Version`, or `Build` aggregate in
the repo. Do not invent one for TC-006. Treat product/version/build as release
coordinate text for now; a future release-management requirement can promote
those coordinates behind a separate aggregate without changing the plan's
identity or project scope.

## Incumbents To Reuse

- **Test-management domain:** build on `domain/testcases/{model,state,
  repository,service}` and `api/testcases`; keep `TestCase`, `TestCaseStep`,
  `TestCaseGherkin`, and `TestCaseFolder` intact.
- **Project scope:** resolve the project through `ProjectService`; every
  repository lookup must include `projectId`, mirroring `TestCaseService` and
  `TreatmentPlanService`.
- **Persistence:** use Flyway migrations, `BaseEntity` UUID/timestamps,
  `(project_id, uid)` uniqueness, useful project/status indexes, and an Envers
  `_audit` table when the entity is `@Audited`.
- **Audit:** add `@NotAudited` for the `Project` `@ManyToOne`; update
  `AuditRetentionJob.AUDIT_TABLES`, `MigrationSmokeTest`, and
  `RequirementsE2EIntegrationTest` when adding the audited table.
- **Validation and errors:** use request-record Bean Validation for wire shape,
  service/domain validation for cross-field invariants, and the existing
  `DomainValidationException`, `ConflictException`, and `NotFoundException`
  hierarchy so `GlobalExceptionHandler` emits the standard `ErrorResponse`.
- **Status contract:** follow the local enum pattern (`TestCaseStatus`,
  `TreatmentPlanStatus`) with `validTargets()` / `canTransitionTo()` only if
  TC-006 exposes transitions. Do not reuse `TestCaseStatus`,
  `Requirement.Status`, or `TreatmentPlanStatus`; their words describe
  different lifecycle objects.
- **API/MCP/frontend parity:** controller changes must update `docs/API.md`,
  `mcp/ground-control/lib.js`, `mcp/ground-control/index.js`, and a matching
  `@WebMvcTest`. API-visible enums must be mirrored in
  `frontend/src/types/api.ts` per ADR-034.

## Cross-Cutting Layers

- **Auth:** routes should stay under `/api/v1/test-plans/**` or
  `/api/v1/test-cases/plans/**` so bearer traffic passes `IpAllowlistFilter`,
  `BearerTokenAuthFilter`, `ApiPathMatrix`, and `ActorFilter`; browser traffic
  passes the ADR-037 session/CSRF chain and the same path matrix. No
  endpoint-local actor or token fields.
- **Config/env/OS exposure:** TC-006 needs no new secrets, environment
  variables, subprocesses, temp files, network clients, or process argv
  content. If date-window or list-size limits become configurable later, bind
  them through validated `@ConfigurationProperties`; do not read ad hoc env
  vars in controllers or services.
- **Shape checks:** Jackson owns UUID/date/enum binding; Bean Validation owns
  required fields and size bounds; service/domain logic owns project scope,
  unique UID handling, date ordering, and status transition legality.
- **Error envelope:** never add a parallel exception hierarchy or bespoke
  response body. Validation details may identify field names and expected
  bounds, but must not echo long descriptions, request bodies, auth headers,
  stack traces, or database constraint names.
- **Logging/observability:** use SLF4J lifecycle logs with low-cardinality
  fields such as plan UID/id, project identifier, status, product/version/build,
  and action. Do not log descriptions, request bodies, tokens, cookies, or
  authorization headers.
- **Policy:** application-source changes need the changelog fragment required
  by `.gc/plan-rules.md`; migrations must update the hardcoded migration smoke
  lists; completion must run `make policy`.

## Extensibility

The load-bearing seam is `TestPlan` as a parent of execution-time runs:
future `TestRun` rows should carry a `test_plan_id` FK, allowing many runs per
plan. Do not store run IDs as JSON on the plan or create placeholder run rows
inside the plan service.

Keep authored definitions and execution evidence separate. A plan can later
group runs that execute `TestCase` definitions, but a plan is not itself a
test-case folder, test suite, run result, step result, or defect container.

List endpoints should leave room for obvious filters (`status`, product,
version, build, active date window) without changing the schema. If filters are
implemented, they belong as query parameters over repository predicates, not as
new controllers per status.

## Gotchas And Anti-Patterns

- Do not add test-plan fields onto `TestCase`, `TestCaseFolder`,
  `Requirement`, `VerificationResult`, `ControlTest`, or traceability links.
- Do not reuse `documents.Section` or test-case folders as plans.
- Do not treat `TestCaseStep.actualResult` as execution history for a plan.
- Do not introduce a `Product`/`Release`/`Build` aggregate only for TC-006.
- Do not serialize run membership as a JSON array on `TestPlan`; use a FK on
  the run-side aggregate when runs exist.
- Do not use DB-level cascades for audited business deletions where Envers
  revisions are required.
- Do not add duplicate validation, exception, auth, audit, logging, MCP, or
  frontend API-client stacks.

## Non-Goals

- No implementation of TC-006 in this preflight.
- No `TestRun` execution aggregate, run-result model, defect model, automation
  runner integration, or evidence upload pipeline.
- No product/release/build catalog.
- No graph projection unless a separate requirement asks for graph traversal.
- No UI workflow or dashboard beyond any DTO/type mirrors required by policy.
