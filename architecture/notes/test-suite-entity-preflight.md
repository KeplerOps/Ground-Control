# Test Suite Entity Preflight

Issue: #674
Requirement: TC-007

This note records architecture guardrails for the upcoming Test Suite entity.
It is not an implementation plan.

## Boundary

`TestSuite` is the selection container for test cases inside a project. It is
not a test plan, test-case folder, requirement, query language, test run,
execution result, control test, verification result, or traceability link.

The suite belongs inside the existing `testcases` domain boundary unless a
separate architectural decision splits a wider test-management package. Keep
the vocabulary precise:

- `TestPlan` owns planning scope, schedule, and release coordinates.
- `TestCaseFolder` owns repository browsing and sibling order.
- `TestSuite` owns a population rule and the selected or resolved test-case
  members for that rule.

TC-007 requires three population modes:

- `STATIC`: manually curated test-case membership.
- `REQUIREMENTS_BASED`: membership is derived from test cases linked to one or
  more requirements in the same project.
- `QUERY_BASED`: membership is derived from bounded filter criteria and is
  resolved dynamically as matching test cases change.

Mode is a suite-level invariant. Do not mix static member rows, requirement
source rows, and query criteria as three simultaneous sources of truth for one
suite.

## Incumbents To Reuse

- **Test-management domain:** build on `domain/testcases/{model,state,
  repository,service}` and `api/testcases`; keep `TestCase`, `TestCaseFolder`,
  `TestCaseStep`, `TestCaseGherkin`, and `TestPlan` as sibling aggregates.
- **Project scope:** resolve projects through `ProjectService`. Every suite,
  member, requirement-source, and resolved test-case lookup must include
  `projectId`; cross-project membership is invalid even if UUIDs exist.
- **Requirements and links:** use `RequirementRepository` /
  `RequirementService` project-scoped lookups and the existing
  `TraceabilityLink` model where requirements-based suites depend on existing
  requirement-to-test linkage. Do not create a second requirement-link table
  just for suites unless it represents suite source requirements rather than
  test-case coverage.
- **Test-case filters:** if query-based suites filter test cases, introduce or
  reuse a typed filter record in the test-case service/repository layer,
  analogous to `RequirementFilter` plus repository predicates. Do not accept
  caller-supplied SQL, JPQL, Cypher, AGE fragments, JSONPath, regex programs, or
  arbitrary field names.
- **Validation and errors:** use request-record Bean Validation for wire shape,
  service/domain validation for mode invariants and project ownership, and the
  existing `DomainValidationException`, `ConflictException`, and
  `NotFoundException` hierarchy so `GlobalExceptionHandler` emits the shared
  `ErrorResponse`.
- **Persistence and audit:** use Flyway migrations, `BaseEntity`
  UUID/timestamps, project-scoped UID uniqueness if a human-readable suite UID
  is exposed, Envers audit tables for audited suite and membership/source rows,
  `@NotAudited` on `Project` references, `MigrationSmokeTest`, and
  `AuditRetentionJob.AUDIT_TABLES`.
- **API/MCP/frontend parity:** controller additions must update `docs/API.md`,
  `mcp/ground-control/lib.js`, `mcp/ground-control/index.js`, MCP adapter tests,
  frontend DTO/enum mirrors in `frontend/src/types/api.ts`, and controller
  tests. API-visible enums such as `TestSuitePopulationMode` belong in the
  ADR-034 mirror discipline.

## Cross-Cutting Layers

- **Auth and actor provenance:** routes should stay under
  `/api/v1/test-suites/**` or an explicitly documented test-management path
  under `/api/v1/**` so bearer requests pass `IpAllowlistFilter`,
  `BearerTokenAuthFilter`, `ApiPathMatrix`, and `ActorFilter`, while browser
  requests pass the ADR-037 session/CSRF chain and the same path matrix. Do not
  add endpoint-local token checks, actor request fields, or a suite-specific
  role model.
- **Request parsing and shape checks:** Jackson owns UUID/date/enum binding;
  Bean Validation owns required fields, size limits, non-negative numeric
  bounds, and list-size caps; services own project ownership, duplicate
  membership, mode-specific field compatibility, and source/criteria validity.
- **Dynamic query security:** query-based suites must compile only from typed
  allowlisted criteria into Spring Data predicates or repository methods. Any
  future graph-aware suite query must go through the existing graph service and
  AGE adapter contracts from ADR-032, including bounded depth and adapter-side
  token allowlists.
- **Error envelope:** parser, validation, conflict, and not-found failures must
  route through `GlobalExceptionHandler` / `ErrorResponse`. Ownership
  mismatches should use existing 404 concealment patterns. Error details may
  name fields and expected bounds, but must not echo query payloads beyond safe
  scalar criteria, rich text, auth headers, cookies, tokens, stack traces, or
  database constraint names.
- **Logging/observability:** use SLF4J lifecycle events with low-cardinality
  fields such as suite id/uid, project identifier, mode, action, and affected
  counts. Do not log raw query criteria when they contain free text, test-case
  descriptions, Gherkin source, request bodies, tokens, cookies, or
  authorization headers.
- **Config/env/OS exposure:** TC-007 should need no secrets, subprocesses,
  shell-outs, temp files, external network clients, or process argv content. If
  membership limits, query-result caps, or refresh behavior become
  configurable, bind them through validated `@ConfigurationProperties`; do not
  read ad hoc environment variables from controllers or services.
- **Policy:** application-source changes need the changelog fragment required
  by `.gc/plan-rules.md`; migrations must update smoke tests; completion must
  run `make policy`.

## Extensibility

The load-bearing seam is the population source:

- static suites own explicit `suite -> test_case` membership rows;
- requirements-based suites own explicit `suite -> requirement` source rows and
  resolve member test cases from existing requirement/test linkage;
- query-based suites own structured criteria and resolve member test cases at
  read/evaluation time.

Keep the resolved-member operation parameterized for obvious future variation:
archived/deprecated inclusion, pagination/result caps, folder scoping,
status/type/priority/format filters, and eventual run creation from a suite.
Do not persist dynamic query results as the canonical membership unless a
future caching requirement defines invalidation, audit semantics, and staleness
rules.

If future test runs execute a suite, the run-side aggregate should carry a
`test_suite_id` FK plus an immutable snapshot of resolved test-case IDs for the
run. The suite itself should remain the selection definition, not execution
evidence.

## Gotchas And Anti-Patterns

- Do not implement suites by adding fields to `TestPlan`, `TestCaseFolder`,
  `Requirement`, `TraceabilityLink`, `VerificationResult`, or `ControlTest`.
- Do not conflate a plan (when/what release), folder (where displayed), suite
  (which cases selected), and run (what executed).
- Do not store query-based suite filters as raw SQL/JPQL/Cypher/AGE, arbitrary
  JSON predicates, regexes, or user-authored scripts.
- Do not let one suite combine static members, requirement sources, and query
  criteria without a single authoritative mode and invariant checks.
- Do not create duplicate link, graph, exception, validation, auth, audit,
  logging, MCP serialization, or frontend API-client stacks.
- Do not allow cross-project suite membership, cross-project requirement
  sources, or cross-project query results.
- Do not make requirements-based suites depend on requirement UID strings alone;
  use stable IDs with project-scope checks and expose UIDs only as display or
  lookup conveniences.
- Do not treat dynamic query membership as audit history of executed tests.
  Execution snapshots belong to future run/result aggregates.

## Non-Goals

- No implementation of TC-007 in this preflight.
- No test-run execution aggregate, run-result model, defect model, automation
  runner integration, scheduling engine, or evidence upload pipeline.
- No new Product/Release/Build catalog, generic query language, graph DSL, or
  workflow engine.
- No replacement of existing test-case folders, test plans, requirements,
  traceability links, graph projection infrastructure, auth stack, audit model,
  or error envelope.
