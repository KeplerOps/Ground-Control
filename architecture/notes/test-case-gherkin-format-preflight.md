# Test Case Gherkin Format Preflight

Issue: #671
Requirement: TC-004

TC-004 adds BDD/Gherkin authoring support for test cases. This note records
architecture guardrails only; it is not an implementation plan.

## Boundary

Gherkin is an authored test-case format, not a new test-management subsystem.
It must extend the test-case domain established by ADR-040 and remain separate
from:

- `TestCaseType` (`MANUAL`, `AUTOMATED`, `HYBRID`), which classifies how a
  test is performed. It is not a format discriminator.
- `TestCaseStep`, which models ordered action / expected / actual-result steps
  for the step-based format from ADR-041.
- `Requirement.statement`, `Requirement.customFields`, `TraceabilityLink`,
  `VerificationResult.evidence`, and document section content. These must not
  become alternate Gherkin stores.
- Test execution, Cucumber glue code, automation runner configuration, results,
  defects, suites, and evidence attachments. TC-004 is definition-time content.

The implementation must preserve native Gherkin concepts: Given / When / Then
steps, `Scenario`, `Scenario Outline`, and `Examples` tables. Do not flatten
them into `TestCaseStep.action` / `expectedResult` rows or markdown bullets in a
way that loses scenario outlines, example parameters, tags, or source order.

## Incumbents To Reuse

- Domain package: keep the work under `domain/testcases/{model,state,service,repository}`
  and `api/testcases`, with command records and thin controllers.
- Parent aggregate: `TestCase` remains the reusable, project-scoped definition
  anchor. If a format discriminator is needed, model it as a dedicated
  test-case format value, not as `TestCaseType` and not as inferred state from
  child rows.
- Project scoping: use `ProjectService.resolveProjectId` / `requireProjectId`
  and repository methods that include `projectId` or validate the parent
  `TestCase` inside the resolved project before any Gherkin read or write.
- Persistence: use Flyway as schema authority, UUID identity via `BaseEntity`,
  Envers for authored Gherkin records, matching `_audit` migrations, explicit
  parent lookup indexes, `MigrationSmokeTest`, and `AuditRetentionJob`.
- Validation: keep DTO shape checks in request records with `jakarta.validation`
  and semantic Gherkin parsing/validation in the service layer before
  persistence.
- Parser: use a maintained Gherkin parser / AST library rather than regex or a
  line-oriented parser. Parser output is validation and metadata; the stored
  source remains the user's canonical authored text unless a future import /
  export requirement explicitly introduces canonical reformatting.
- Errors: use `DomainValidationException`, `ConflictException`, and
  `NotFoundException` so `GlobalExceptionHandler` emits the shared
  `ErrorResponse` envelope.
- Logging and audit identity: rely on `RequestLoggingFilter`, `ActorFilter`,
  MDC, `ActorHolder`, and Envers. Log IDs, UID, project, format, and parser
  outcome only; never log full feature source or examples table values.
- REST / MCP / frontend parity: controller changes require `docs/API.md`,
  `mcp/ground-control/lib.js`, `mcp/ground-control/index.js`, and matching
  `@WebMvcTest` updates. Extend the existing `gc_test_case` surface unless a
  broader MCP contract is explicitly justified. If an API-visible enum is added,
  mirror it in `frontend/src/types/api.ts` and the MCP constants that expose it.
- Workflow: database migrations must update the hardcoded migration lists named
  in `.gc/plan-rules.md`; source changes need a changelog fragment; completion
  must run `make policy`.

## Security Layers In Scope

- **Auth path matrix:** keep routes under `/api/v1/test-cases/**` so bearer
  requests pass `IpAllowlistFilter`, `BearerTokenAuthFilter`, Spring
  authorization, and `ActorFilter`, while browser requests pass the ADR-037
  session / CSRF chain and the same `ApiPathMatrix`.
- **Request parsing:** JSON enters through Jackson and `@Valid`. Bound the
  Gherkin source and any parser-derived fields with explicit size/count limits
  before parsing or persistence.
- **Gherkin parser:** parse as inert text only. Do not invoke Cucumber runtime,
  load glue code, expand examples into executable tests, evaluate expressions,
  resolve external includes, fetch remote resources, or execute hooks.
- **Examples tables:** table cells are untrusted user content. Treat parameter
  values as text, constrain row/cell sizes, and avoid echoing full cells in
  validation errors or logs.
- **Error envelope:** parser and validation failures must use stable
  `ErrorResponse` codes/details. Details may identify line, column, keyword, or
  field names, but must not reflect whole feature files, examples tables,
  stack traces, filesystem paths, headers, or tokens.
- **Rendering:** Gherkin source is untrusted content. Browser rendering must be
  escaped text or use a safe syntax highlighter. Do not introduce raw HTML,
  `dangerouslySetInnerHTML`, or markdown/HTML rendering as part of TC-004.
- **Configuration:** if limits such as max source length, max scenarios, or max
  examples rows become configurable, bind them through validated
  `@ConfigurationProperties`; do not read ad hoc environment variables in
  controllers, services, or parser code.
- **OS/runtime exposure:** TC-004 should not require shell-outs, subprocesses,
  temp files, external network calls, file-system watchers, or credentials.
  Gherkin source must never be passed through process argv.

## Extensibility

The load-bearing seam is the authored format discriminator on the `TestCase`
definition. It should let future formats or vendor import modes be added
without overloading `TestCaseType`, rewriting step-based tests, or introducing a
second test-case aggregate.

The Gherkin-specific seam is source plus parsed structure. Persist enough stable
metadata to validate and query the format that TC-004 requires, but keep parser
AST classes out of API DTOs and JPA entities. This avoids coupling the database
and public API to a library's internal node model.

Future execution support must reference the authored test case, scenario /
scenario-outline identity, and example row identity as execution targets. It
must not mutate the authored Gherkin body into the only runtime result record.

## Gotchas And Anti-Patterns

- Do not use regex splitting as the Gherkin parser.
- Do not treat `Scenario Outline` as plain `Scenario` text and drop
  `Examples`.
- Do not map Given / When / Then into step `action` / `expectedResult` fields.
- Do not make `TestCaseType.AUTOMATED` mean "Gherkin"; type and format are
  different axes.
- Do not store duplicate Gherkin schemas in `customFields`, frontend-only
  types, MCP-only Zod shapes, or vendor-specific DTOs.
- Do not create duplicate exception, validation, auth, audit, logging, graph,
  import, or workflow infrastructure.
- Do not allow cross-project reads by resolving Gherkin content by UUID without
  checking the parent test case's project.
- Do not expose parser stack traces, full source text, table values, or
  generated executable snippets in error responses.

## Non-Goals

- No implementation of TC-004 in this preflight.
- No Cucumber runtime, glue-code execution, runner orchestration, CI binding,
  or pass/fail result model.
- No vendor import/export mapping for Xray, Zephyr Scale, PractiTest, qTest, or
  `.feature` files beyond the base Gherkin authoring contract.
- No replacement of ADR-040 `TestCase`, ADR-041 `TestCaseStep`, requirements,
  traceability links, verification results, documents, graph projection, or
  control-test execution records.
- No new authentication scheme, sanitizer stack, parser service process,
  workflow engine, or generic content-management abstraction.
