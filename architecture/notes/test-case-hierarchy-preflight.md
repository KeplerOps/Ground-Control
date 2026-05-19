# Test Case Hierarchy Preflight

Issue: #672
Requirement: TC-005

This note records architecture guardrails for hierarchical test-case
organization. It is not an implementation plan.

## Boundary

TC-005 organizes existing `TestCase` definitions from ADR-040. It does not
create a second test-case aggregate and it does not replace document sections.

Use the vocabulary `TestCaseFolder` for repository organization unless a future
product decision deliberately exposes another display label. `Section` already
means document structure under `domain/documents`; reusing that entity or its
API for test repositories would couple two unrelated ordering models and leak
document export semantics into test management.

The hierarchy owns only repository placement and browse order:

- folders with project scope, title, optional description, parent folder, and
  sibling order;
- test-case placement under either a folder or the project root, with sibling
  order;
- tree reads that return folders and test-case leaf nodes in deterministic order.

Test steps, Gherkin source, execution runs, results, defects, suites,
automation binding, and traceability links remain separate surfaces.

## Incumbents To Reuse

- **Test-case domain:** extend `domain/testcases/{model,repository,service}` and
  `api/testcases`; do not create `domain/testrepositories` unless a broader
  repository aggregate appears.
- **Tree precedent:** reuse the data-shaping pattern from
  `SectionService.getTree` and `DocumentReadingOrderService` (batch load,
  group by parent, return nested DTOs), but keep the implementation in the
  test-case domain and add cycle/project-scope checks missing from the older
  document section path.
- **Project scope:** resolve the project through `ProjectService` and require
  every folder and test-case lookup to include `projectId`. Moving or copying
  between folders is inside one project only; cross-project copy is a separate
  requirement because UID, audit, traceability, and authorization semantics all
  change.
- **Validation and errors:** use request-record Bean Validation for shape
  checks, service validation for parent ownership, cycle prevention, duplicate
  sibling names, and move/copy semantics, and the existing
  `DomainValidationException`, `ConflictException`, and `NotFoundException`
  hierarchy so `GlobalExceptionHandler` emits `ErrorResponse`.
- **Persistence:** use Flyway migrations, UUID identities via `BaseEntity`,
  explicit indexes for `(project_id, parent_id, sort_order)`, project-root
  lookups, and test-case listing by folder. If a new folder entity is audited,
  add the matching `_audit` migration, `MigrationSmokeTest` coverage, and
  `AuditRetentionJob.AUDIT_TABLES`.
- **Audit and deletion:** route folder deletion and descendant/test-case
  placement changes through services/Hibernate so Envers sees the mutations.
  Do not rely on DB cascades for audited business records.
- **API/MCP/frontend parity:** controller changes must update `docs/API.md`,
  `mcp/ground-control/lib.js`, `mcp/ground-control/index.js`, and a matching
  `@WebMvcTest`. Extend the existing `gc_test_case` tool for write actions and
  keep pure tree/list reads reachable through the `/api/v1/test-cases`
  `gc_query` allowlist. Mirror DTOs in `frontend/src/types/api.ts`; add enum
  mirrors only if new API-visible enums are introduced.

## Cross-Cutting Layers

- **Auth:** routes stay under `/api/v1/test-cases/**` so bearer traffic passes
  `IpAllowlistFilter`, `BearerTokenAuthFilter`, `ApiPathMatrix`, and
  `ActorFilter`; browser traffic passes the ADR-037 session/CSRF chain and the
  same path matrix. Do not add endpoint-local auth, caller-supplied actor
  fields, or routes outside the shared matrix.
- **Parser/validation:** Jackson and Bean Validation own UUID/body/enum shape.
  Services must reject a folder parent from another project, a test case from
  another project, a folder moved under itself or any descendant, negative
  ordering values, and duplicate sibling folder names.
- **Error envelope:** ownership mismatches should use the existing 404
  concealment pattern where revealing the real parent would leak data. Error
  details may name fields and expected constraints; they must not echo rich
  text, Gherkin source, request bodies, auth headers, or stack traces.
- **Config/OS exposure:** TC-005 needs no new secrets, env vars, network
  clients, subprocesses, temp files, or process argv. If future import/export
  limits become configurable, bind them through validated
  `@ConfigurationProperties`; do not read ad hoc environment variables from
  controllers or services.
- **Logging:** use SLF4J lifecycle events with low-cardinality fields such as
  project identifier, folder id, test-case id/uid, action, and target parent id.
  Do not log full descriptions, step bodies, Gherkin source, request payloads,
  tokens, or authorization headers.
- **Policy:** application-source changes need a changelog fragment per
  `.gc/plan-rules.md`; migrations must update the hardcoded migration smoke
  lists; completion must run `make policy`.

## Extensibility

The reusable seam is a project-scoped repository item placement model:
`parentFolderId` plus container-local `sortOrder`, with root represented as
`parentFolderId = null`. Keep folder ordering and test-case ordering
container-scoped so drag-and-drop can update one folder/root without rewriting
the whole repository.

Move semantics should preserve the moved test case's identity and audit history.
Copy semantics should create a new `TestCase` definition with a new UUID and a
project-unique UID, then copy authored children through their owning services
(`TestCaseStepService`, `TestCaseGherkinService`) so future child formats can
join by adding one copy collaborator instead of modifying unrelated controllers.

Tree reads should be parameterized for obvious future variation: include or
exclude archived/deprecated test cases, optionally choose a root folder, and
leave room for search/filter inputs without changing the folder schema.

## Gotchas And Anti-Patterns

- Do not reuse `documents.Section` or `SectionContent` for test-case folders.
- Do not encode hierarchy in UID prefixes, titles, paths, custom fields, or
  traceability links.
- Do not allow cross-project folder moves/copies as an accidental side effect
  of accepting arbitrary source and destination IDs.
- Do not make drag-and-drop update only frontend state; persisted sibling order
  must be deterministic and stable across clients.
- Do not use recursive service calls without cycle protection for an
  "unlimited nesting" tree; malformed data must not cause infinite recursion.
- Do not add duplicate exception hierarchies, validation frameworks, auth
  filters, audit writers, graph writers, MCP serializers, or frontend API
  clients.
- Do not make folder deletion silently delete test cases unless the API contract
  states that destructive cascade explicitly. Prefer explicit move/delete
  behavior with audit-visible service operations.

## Non-Goals

- No implementation of TC-005 in this preflight.
- No test execution, run/result, defect, suite, automation-runner, or evidence
  model.
- No cross-project copy/migration workflow.
- No replacement of ADR-040 `TestCase`, ADR-041 `TestCaseStep`, ADR-042
  `TestCaseGherkin`, document sections, requirements, traceability links,
  graph projection, or control-test records.
- No new security scheme, sanitizer stack, workflow engine, generic CMS, or
  repository-wide graph projection for test cases.
