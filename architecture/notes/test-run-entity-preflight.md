# Test Run Entity Preflight

Issue: #675
Requirement: TC-008

This note records architecture guardrails for the upcoming Test Run entity.
It is not an implementation plan.

## Boundary

`TestRun` is the execution-time record for one pass through a test suite
inside a project. It is not a test case, test-case step, test-case folder,
test plan, test suite definition, verification result, control test,
traceability link, defect, automation job, or evidence upload.

Keep the test-management vocabulary precise:

- `TestCase` owns reusable authored intent.
- `TestCaseStep` and `TestCaseGherkin` own authored test content.
- `TestCaseFolder` owns repository browsing and sibling order.
- `TestSuite` owns selection rules and resolved candidate cases.
- `TestPlan` owns planning scope, schedule, and release coordinates.
- `TestRun` owns execution evidence for one suite pass against one plan,
  environment, build/version, time window, assigned testers, and per-case
  results.

The aggregate belongs inside the existing `domain/testcases` boundary unless a
separate ADR splits a wider test-management package. TC-008 is the first
execution-time aggregate in that boundary, so do not satisfy it by expanding
definition-time tables or by overloading verification/control-evidence tables.

## Incumbents To Reuse

- **Test-management domain:** build on `domain/testcases/{model,state,
  repository,service}` and `api/testcases`; keep `TestPlan`, `TestSuite`, and
  `TestCase` as sibling/input aggregates rather than parents with embedded run
  arrays.
- **Plan and suite seams:** use the ADR-044 `TestPlan.id` parent seam and the
  ADR-047 `TestSuite.id` suite seam. A run should carry explicit FKs to the
  project-scoped plan and the suite being executed; do not infer the suite only
  from a plan or from dynamic query criteria.
- **Resolved-case snapshot:** use `TestSuiteService.resolveTestCases` or its
  repository-level building blocks as the canonical resolution logic. A run
  must snapshot the resolved test-case IDs/order on the run side before storing
  results; do not keep re-reading a dynamic query-based suite as historical
  evidence after execution starts.
- **Project scope:** resolve projects through `ProjectService`. Every run,
  assigned tester row, per-case result, plan lookup, suite lookup, and test-case
  lookup must include `projectId`; cross-project execution evidence is invalid
  even when UUIDs exist.
- **Assigned testers:** treat assigned testers as bounded domain-provenance
  values unless a future people/team domain exists. The JDBC `users` /
  `authorities` tables are security-principal storage per ADR-037, not a
  business-user model to extend for assignment metadata. Multiple testers
  should be represented as normalized run-side data, not comma-separated text.
- **Release coordinates and environment:** follow `TestPlan`'s bounded scalar
  precedent for `environment`, `version`, and `build`. Do not introduce a
  Product / Release / Build / Environment catalog for TC-008.
- **Validation and errors:** use request-record Bean Validation for wire shape,
  service/domain validation for cross-aggregate ownership and lifecycle
  invariants, and the existing `DomainValidationException`,
  `ConflictException`, and `NotFoundException` hierarchy so
  `GlobalExceptionHandler` emits the shared `ErrorResponse`.
- **Persistence and audit:** use Flyway migrations, `BaseEntity`
  UUID/timestamps, project-scoped UID uniqueness if a human-readable run UID is
  exposed, Envers audit tables for the run and execution-result children, and
  `@NotAudited` or `targetAuditMode = NOT_AUDITED` deliberately on
  references. Update `AuditRetentionJob.AUDIT_TABLES`,
  `MigrationSmokeTest`, and `RequirementsE2EIntegrationTest` for audited
  tables and new migration versions.
- **API/MCP/frontend parity:** controller additions must update `docs/API.md`,
  `mcp/ground-control/lib.js`, `mcp/ground-control/index.js`, MCP adapter tests,
  frontend DTO/enum mirrors in `frontend/src/types/api.ts`, and
  `@WebMvcTest` controller tests. API-visible run/result enums belong in the
  ADR-034 mirror discipline even where the current inventory is manual.

## Cross-Cutting Layers

- **Auth and actor provenance:** routes should live under
  `/api/v1/test-runs/**` so bearer requests pass `IpAllowlistFilter`,
  `BearerTokenAuthFilter`, `ApiPathMatrix`, and `ActorFilter`, while browser
  requests pass the ADR-037 session/CSRF chain and the same path matrix. Do not
  add endpoint-local bearer-token checks, actor override fields, or a run-local
  role model.
- **Request parsing and shape checks:** Jackson owns UUID, `Instant`, date, and
  enum binding; Bean Validation owns required fields, size limits, timestamp
  presence rules, list-size caps, and non-negative durations/counts; services
  own plan/suite/test-case project ownership, duplicate result rows, status
  transition legality, tester assignment validity, and `end >= start`
  invariants.
- **Status contract:** introduce dedicated execution vocabulary for run and
  per-case result status. Do not reuse `TestCaseStatus`, `TestPlanStatus`,
  `TestSuitePopulationMode`, `VerificationStatus`, or
  `ControlTestConclusion`; those words describe different lifecycle objects.
- **Error envelope:** parser, validation, conflict, and not-found failures must
  route through `GlobalExceptionHandler` / `ErrorResponse`. Ownership
  mismatches should use existing 404 concealment patterns. Error details may
  name fields and expected bounds, but must not echo raw result notes,
  environment secrets, request bodies, auth headers, cookies, stack traces, or
  database constraint names.
- **Logging/observability:** use SLF4J lifecycle logs with low-cardinality
  fields such as run id/uid, project identifier, plan id, suite id, status,
  action, result counts, and duration. Do not log raw result notes, test-case
  descriptions, Gherkin source, request bodies, tokens, cookies,
  authorization headers, or secret-bearing environment labels.
- **Config/env/OS exposure:** TC-008 should need no new secrets, subprocesses,
  shell-outs, temp files, external network clients, or process argv content.
  If snapshot limits, result-bulk limits, or stale-suite policies become
  configurable, bind them through validated `@ConfigurationProperties`; do not
  read ad hoc environment variables from controllers or services.
- **Policy and workflow:** `.ground-control.yaml`, `.gc/plan-rules.md`,
  `bin/policy`, `tools/policy/checks.py`, controller-contract checks, migration
  smoke tests, ADR drift checks, and changelog-fragment rules are in scope for
  application-source implementation. Completion must run `make policy`.

## Extensibility

The load-bearing seam is the run-side snapshot:

- `TestRun` references the `TestPlan` and `TestSuite` that authorized the run.
- Run result children represent the immutable set of test cases selected for
  this execution, with each row carrying a test-case reference plus enough
  snapshot metadata (for example UID/title/order) that later edits to
  `TestCase` or `TestSuite` do not rewrite historical evidence.
- Result rows own execution status and actual result notes for that run. They
  are not authored expected results and they are not defect records.

Keep run creation and result updates parameterized for obvious future
variation: manual versus automated origin, reruns, partial execution,
pagination/bulk limits for result updates, defect linkage, evidence
attachments, and automation-run adapters. The likely seam is explicit fields
or child rows on the run/result aggregate, not JSON blobs on `TestPlan`,
`TestSuite`, or `TestCase`.

If a future automation runner lands, it should be an infrastructure adapter
feeding this domain service through bounded request DTOs. It must not bypass
project-scope validation, audit actor provenance, run status transitions, or
the shared error envelope.

## Gotchas And Anti-Patterns

- Do not add run fields or result fields to `TestCase`, `TestCaseStep`,
  `TestSuite`, `TestPlan`, `Requirement`, `TraceabilityLink`,
  `VerificationResult`, or `ControlTest`.
- Do not treat `TestCaseStep.actualResult` or Gherkin text as execution
  history for a run.
- Do not rely on a query-based suite's live resolution as historical run
  membership after the run is created; snapshot on the run side.
- Do not store per-case results, assigned testers, or run membership as
  comma-separated strings or unbounded JSON when rows are required for
  validation, audit, and querying.
- Do not extend Spring Security's `users` table with domain assignment fields
  or let assigned tester values spoof the authenticated audit actor.
- Do not create duplicate validation, exception, auth, audit, logging, MCP,
  frontend API-client, enum-mirror, or suite-resolution stacks.
- Do not allow cross-project plan/suite/case/result assignment.
- Do not add a Product / Release / Build / Environment catalog without a
  separate requirement.
- Do not shell out to test runners, read arbitrary filesystem paths, or call
  external networks from the run entity/service just to satisfy TC-008.

## Non-Goals

- No implementation of TC-008 in this preflight.
- No automation runner integration, scheduling engine, defect lifecycle,
  evidence upload pipeline, Product / Release / Build / Environment catalog,
  People / Team directory, or graph projection unless a separate requirement
  asks for it.
- No replacement of existing test-case definitions, test suites, test plans,
  requirements, traceability links, verification results, control tests, auth
  stack, audit model, or error envelope.
