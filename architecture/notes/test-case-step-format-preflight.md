# Test Case Step Format Preflight

Issue: #670
Requirement: TC-002

TC-002 introduces a step-based test case format. This note records the
architecture guardrails for the implementation that follows. It is not an
implementation plan.

## Boundary

Step-based test cases are structured test-design artifacts. They must remain
separate from:

- `Requirement`, which owns requirement identity, statement, rationale,
  lifecycle, priority, wave, custom fields, and requirement relations.
- `TraceabilityLink`, which links requirements to external artifacts and must
  not become an embedded test-case schema.
- `VerificationResult`, which records verification outcomes and evidence after
  evaluation. It is not the authored test procedure.
- `Document`, `Section`, and `SectionContent`, which own document organization
  and text blocks, not executable test-case structure.

Do not store ordered steps as markdown conventions inside
`Requirement.statement`, as ad hoc JSON in `Requirement.customFields`, as
`VerificationResult.evidence`, or as `TEXT_BLOCK` section content. A step-based
test case needs one canonical domain contract with ordered steps, per-step
fields, ownership, validation, audit behavior, and project scoping.

## Incumbents To Reuse

- **Domain shape:** follow the existing domain package pattern:
  `model`, `repository`, `service`, command records, state enums only when the
  state has real domain semantics.
- **Requirement association:** use `RequirementRepository.findByIdAndProjectId`
  or the same same-project validation pattern already used by
  `VerificationResultService`; do not rely on globally unique requirement UIDs.
- **Project scope:** route API calls through `ProjectService.resolveProjectId`
  or `requireProjectId` as the adjacent controllers do. Every test case and
  step query must be project-scoped.
- **Persistence:** use Flyway migrations as the schema authority, UUID primary
  keys via `BaseEntity`, explicit indexes for project and parent lookups, and
  service-owned writes through Spring Data repositories.
- **Audit:** if authored test cases and steps are business records, annotate
  them with Envers and add matching `_audit` migrations. Add `@NotAudited` on
  `@ManyToOne` references to non-audited entities such as `Project`.
- **Validation:** keep DTO shape validation in request records with
  `jakarta.validation`, and semantic validation in services. Existing examples:
  `RequirementRequest`, `SectionContentService.validateContentType`, and
  project mismatch checks in `VerificationResultService`.
- **Errors:** throw the existing exception hierarchy (`NotFoundException`,
  `ConflictException`, `DomainValidationException`) so
  `GlobalExceptionHandler` emits the shared `ErrorResponse` envelope.
- **Logging and audit identity:** use SLF4J lifecycle logs sparingly and rely on
  `RequestLoggingFilter`, `ActorFilter`, MDC, and Envers for request/actor
  context. Do not log rich text bodies, image payloads, bearer tokens, or raw
  multipart metadata.
- **Graph:** if test cases become graph-visible, add a `GraphEntityType`,
  `GraphIds` usage, and a `GraphProjectionContributor`; do not add feature-local
  graph endpoints or AGE write paths.
- **Frontend contract:** mirror backend DTOs in `frontend/src/types/api.ts` and
  keep hooks near `use-requirements.ts` style. If new enums are exposed, extend
  ADR-034's enum contract inventory and frontend constants instead of adding
  component-local literal lists.
- **API documentation:** update `docs/API.md` only for public endpoint contract
  changes. Do not document internal persistence tables as API.
- **Policy and workflow:** source or test changes require the repo's normal
  changelog fragment rule. Database migrations must update the hardcoded
  migration lists named in `.gc/plan-rules.md`. Completion must run
  `make policy`.

## Security Layers In Scope

- **Spring Security path matrix:** new endpoints under `/api/v1/**` are
  authenticated by ADR-026 through `ApiSecurityConfig`. Do not mount test-case
  APIs outside that namespace or add controller-local authorization.
- **Request parser and Bean Validation:** JSON requests pass through Jackson and
  `@Valid`. Step number, action description, expected result, actual result,
  ordering, attachment references, and rich-text format must be shape-checked at
  the DTO boundary and semantically checked in the service before persistence.
- **Rich text rendering:** rich text is untrusted user content. The backend must
  preserve a constrained representation rather than trusting arbitrary HTML; any
  HTML export or browser rendering must escape or sanitize through one
  canonical renderer. Do not introduce `dangerouslySetInnerHTML` without an
  explicit sanitizer and tests.
- **Inline images:** image bytes or references are untrusted content. Enforce
  allowed media types, size limits, ownership, and same-project access before
  storage or rendering. Do not accept remote image URLs as implicit fetch
  instructions from the backend.
- **Multipart or upload handling:** if image upload is introduced, reuse
  Spring's existing multipart handling and `GlobalExceptionHandler` missing-part
  responses. Avoid passing filenames, content, tokens, or paths through process
  argv, shell commands, logs, or error bodies.
- **Error envelope:** validation and parser failures must use stable
  `ErrorResponse` bodies and must not reflect raw rich text, filenames, image
  bytes, stack traces, filesystem paths, or sanitizer internals.
- **Configuration:** if storage limits or allowed MIME types become configurable,
  use `@ConfigurationProperties` with startup validation. Do not read ad hoc
  environment variables in services or controllers.
- **OS/runtime exposure:** the design should not require shell-outs, external
  image fetches, temporary files in predictable paths, or executable content.
  Any unavoidable filesystem storage needs explicit path confinement and
  retention/backup implications before implementation.

## Extensibility

The next likely changes are importing test cases from external tools,
per-step execution status, attachments beyond inline images, and test-case
reuse across requirements. The seam belongs in the authored test-case model:
step content should carry a constrained rich-text representation plus
attachment references, not inline binary blobs or parser-specific HTML. Step
ordering should be explicit and re-orderable without rewriting the whole test
case.

If execution tracking is added later, model it as an execution/result layer that
references the authored test case and step identities. Do not mutate the
authored step's `actualResult` into the only execution record if multiple runs,
actors, environments, or timestamps are expected.

## Gotchas and Anti-Patterns

- Do not conflate authored test cases with verification results. Test cases are
  planned procedures; verification results are observed outcomes/evidence.
- Do not represent steps as a newline parser over `statement` or markdown list
  syntax. That loses ordering invariants, per-field validation, and per-step
  auditability.
- Do not create duplicate validation, exception, logging, or auth layers for
  test cases. Extend the existing Spring/Bean Validation/service/error stack.
- Do not make `stepNumber` the database identity. Use stable UUID identity plus
  a unique per-test-case order/number constraint so reordering and future
  references remain possible.
- Do not allow gaps, duplicates, negative numbers, or cross-test-case step
  ownership unless the service has an explicit rule and test coverage for it.
- Do not embed base64 image data in every list/detail response by default.
  Return references and metadata; fetch bytes through an explicit, authorized
  path if the implementation adds binary storage.
- Do not add broad nullable columns for every vendor field. Vendor import
  metadata belongs in a constrained metadata field only after first-class
  identity, ordering, content, and attachment semantics are modeled.
- Do not let frontend-only validation become the enforcement layer. Backend
  DTOs and services remain authoritative.

## Non-Goals

- No new workflow engine, state machine library, or generic content-management
  abstraction for TC-002.
- No change to requirement lifecycle states or traceability link semantics.
- No external test-management integration, importer, or vendor-specific schema
  unless covered by a separate requirement.
- No image processing pipeline, OCR, remote URL fetcher, or antivirus subsystem
  as part of the base step-format requirement.
- No replacement of the existing document model, ReqIF export, or
  `SectionContent` grammar.
- No new authentication scheme, exception hierarchy, policy runner, or
  frontend validation framework.
