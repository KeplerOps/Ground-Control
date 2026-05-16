# Test Case Entity Preflight

Issue: #669
Requirement: TC-001

This is architecture guardrail guidance for implementing TC-001. It is not an
implementation plan.

## Boundary

A test case is the reusable test-definition record for a project. It owns its
stable identity, title, rich-text description, preconditions, postconditions,
priority, lifecycle status, type classification, estimated duration, and project
scope.

It must stay separate from:

- `ControlTest`, which is an executed control-evidence record with steps,
  expected results, actual results, conclusion, tester identity, and test date.
- `VerificationResult`, which is prover/tool output for requirements and
  traceability links.
- `TraceabilityLink`, whose `ArtifactType.TEST` points at external or
  repo-local test artifacts; it is not a first-class test-case row.
- `Requirement`, which owns product requirements, MoSCoW priority, and the
  DRAFT/ACTIVE/DEPRECATED/ARCHIVED lifecycle.
- Future test runs, executions, defects, evidence attachments, automation jobs,
  and suites. Those are adjacent aggregates or links, not TC-001 fields.

Do not satisfy TC-001 by adding test-case fields onto requirements, controls,
verification results, or traceability links. A test case is definition-time
content; execution history must remain modeled separately.

## Incumbents To Reuse

- Domain shape: use the existing `domain/<area>/{model,state,service,repository}`
  package pattern, audited JPA entities extending `BaseEntity`, command records,
  and service-owned semantic validation.
- Project scoping: follow ADR-016 and `ProjectService`; every lookup and list
  path must be project-scoped (`findByIdAndProjectId`, project-scoped UID
  checks). If a human-readable `uid` is exposed, it must be unique inside a
  project, not globally.
- REST shape: thin `api/*Controller` classes, request/response records, `@Valid`,
  Jackson enum binding, `ProjectService.resolveProjectId` for collection/create
  routes, and `ProjectService.requireProjectId` where ambiguity would be unsafe.
- Lifecycle shape: implement TC-001's DRAFT/APPROVED/DEPRECATED/ARCHIVED status
  as its own test-case status enum using the existing enum transition-table
  pattern. Do not reuse requirement `Status` and rename `ACTIVE` to "approved"
  in API or UI copy.
- Exception and validation shape: use `NotFoundException`, `ConflictException`,
  and `DomainValidationException`; DTOs own required fields, size bounds, enum
  parsing, and create/update null semantics; services own duplicate identity,
  status transitions, and project-scope checks.
- Persistence and audit: use Flyway migrations, Envers audit-table parity,
  `@NotAudited` for the `Project` reference, repository indexes for project,
  UID, status, priority, type, and duration/listing paths, `MigrationSmokeTest`,
  and `AuditRetentionJob.AUDIT_TABLES`.
- Graph: if test cases become graph-visible, use `GraphEntityType`, `GraphIds`,
  `GraphProjectionContributor`, `MixedGraphService`, and
  `GraphTargetResolverService` for same-project target validation. Do not write
  AGE rows directly from controllers or services.
- Frontend/API/MCP: mirror new API DTOs and API-visible enums through the same
  contract surfaces as existing entities. Extend the ADR-034 enum inventory if
  test-case enums are mirrored to `frontend/src/types/api.ts` or MCP schemas.
  Reuse `apiFetch`/`apiDelete` and the CSRF/session behavior in
  `frontend/src/lib/api-client.ts`.

## Cross-Cutting Layers

- Security: keep endpoints under `/api/v1/**` so bearer traffic passes
  `IpAllowlistFilter`, `BearerTokenAuthFilter`, Spring authorization, and
  `ActorFilter`; browser traffic passes the ADR-037 browser chain, CSRF gate for
  mutations, and the same `ApiPathMatrix`. Do not add endpoint-local auth,
  caller-supplied actor fields, caller-supplied tokens, or routes outside the
  shared matrix.
- Request parsing and validation: Jackson rejects unknown status/type/priority
  enum values before domain mutation. Bean Validation rejects malformed create
  bodies. Partial update DTOs must preserve null-means-no-change and reject
  blank-when-present required fields in the service without regex backtracking
  hazards.
- Rich text: there is no established rich-text sanitizer or markdown renderer in
  the app. Treat rich-text fields as stored text/markdown until a shared,
  sanitized renderer exists. Do not store or render arbitrary HTML through
  `dangerouslySetInnerHTML` or equivalent unsanitized sinks.
- Error envelope: all parser, validation, conflict, and not-found failures must
  route through `GlobalExceptionHandler` and `shared.web.ErrorResponse`. Error
  details may name fields, enum values, or IDs, but must not echo full rich-text
  bodies, preconditions, postconditions, secrets, headers, or stack traces.
- Audit and actor provenance: Envers plus `ActorFilter`, `ActorHolder`, and
  `GroundControlRevisionListener` provide authenticated mutation history. Any
  domain provenance fields added later must not replace the authenticated audit
  actor.
- Observability: use SLF4J lifecycle events with stable low-cardinality fields
  such as project identifier, UID, status, type, and UUID. Do not log full
  descriptions, preconditions, postconditions, raw rich text, tokens, or request
  bodies.
- Configuration and OS/runtime exposure: TC-001 should not require new secrets,
  environment variables, subprocesses, shell-outs, network clients, or process
  argv. A future automation adapter or test-runner integration belongs behind
  validated `@ConfigurationProperties` and must keep secrets out of argv, logs,
  database narrative fields, and error envelopes.
- Policy and tests: controller additions need matching `@WebMvcTest`; semantic
  rules need service tests; lifecycle rules need focused enum/property tests
  because this is workflow logic; migrations need smoke coverage. `make policy`
  remains the repo-native completion gate.

## Extensibility

The primary seam is execution: test cases define reusable intent, while future
test runs or executions record who/what ran, when, against which version or
environment, and with what result/evidence. Do not put execution result fields
on the test-case entity.

The second seam is automation. `Manual`, `Automated`, and `Hybrid` classify the
definition; they should leave room for a future automation binding such as a
repo-relative test identifier, CI job, or external tool reference without
requiring a new test-case table.

The third seam is duration. Persist a single explicit representation with an
unambiguous unit or standard format, and keep parsing/formatting at API/UI
boundaries. Future scheduling or planning views should not need to reinterpret
free-form text such as "about five minutes."

The fourth seam is linking. If requirements, controls, risks, findings, suites,
or external evidence link to test cases, extend the existing project-scoped link
and graph target resolver patterns instead of inventing a test-case-specific
relationship substrate.

## Gotchas And Anti-Patterns

- Do not conflate test cases with control tests, verification results, test
  executions, traceability artifacts, or requirements.
- Do not reuse `Requirement.Status` for the test-case lifecycle; APPROVED is not
  ACTIVE, and the transition rules may diverge.
- Do not reuse `Priority` blindly if test-case priority is not intended to be
  MoSCoW. If it is product-semantically different, model a separate enum and
  mirror it through the API contract surfaces.
- Do not make rich text an HTML injection vector. Until a shared sanitizer exists,
  render as plain text or sanitized markdown only.
- Do not create duplicate exception hierarchies, validation frameworks, JSON
  parsers, auth guards, audit writers, graph writers, or workflow engines.
- Do not allow cross-project reads or updates by resolving test cases by UUID or
  UID alone.
- Do not add generic `metadata` as an escape hatch for status, type, priority,
  preconditions, postconditions, duration, execution results, or automation
  binding.

## Non-Goals

- No implementation of TC-001 in this preflight.
- No test execution, test run, defect, suite, automation-runner, or evidence
  ingestion model.
- No replacement of requirements, controls, control tests, verification results,
  findings, risk entities, traceability links, or graph projection infrastructure.
- No new security scheme, audit model, sanitizer stack, external runner, or
  workflow engine.
