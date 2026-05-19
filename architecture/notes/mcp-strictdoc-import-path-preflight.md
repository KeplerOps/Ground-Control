# MCP StrictDoc Import Path Preflight

Issue: #246
Requirement: none

This is architecture guardrail guidance for closing the local-file read and
upload risk in `gc_admin`'s `import_strictdoc` action. It is not an
implementation plan.

## Boundary

The backend import endpoint is already the semantic authority for parsing and
persisting StrictDoc content: `ImportController`, `ImportService`,
`SdocParser`, `RequirementImport`, and `ImportResultResponse` own the REST
contract, parser behavior, project scoping, audit persistence, and import
result shape.

The issue belongs at the MCP host-filesystem boundary. The MCP adapter is the
only layer that sees a local path before bytes leave the operator workstation.
It must validate the selected file before `readFileSync()`, before `FormData`
construction, and before any request to `/api/v1/admin/import/strictdoc`.

## Incumbents To Reuse

- MCP consolidation and admin gating: ADR-035. Keep imports on the consolidated
  `gc_admin` tool, gated by `GC_MCP_ADMIN`, with backend authorization still
  enforced by ADR-026. Catalog gating is not a substitute for local path
  validation.
- MCP transport helpers: `request`, `buildUrl`, `addAuthorizationHeader`,
  `RequestError`, and `parseErrorBody` in `mcp/ground-control/lib.js`. Do not
  add feature-local fetch, auth-header, or backend-error parsing.
- Repo/workspace path patterns: reuse or generalize the existing
  `resolveRepoRelativePath` / `assertRealpathInRepo` containment pattern rather
  than open-coding `join`, `relative`, or `realpath` checks. StrictDoc upload is
  stricter than `.ground-control.yaml` knowledge paths: final symlinks and
  non-regular files must be rejected, not merely canonicalized inside the repo.
- MCP validation style: keep the Zod schema narrow, use handler-side
  `reqArg` for action-required fields, and add focused pure helper coverage in
  `mcp/ground-control/lib.test.js`.
- Backend validation and errors: let `ImportService` and `SdocParser` validate
  StrictDoc content. Local file-selection failures should be MCP validation
  failures with stable codes/messages; backend validation failures still flow
  through `GlobalExceptionHandler` / `ErrorResponse` and surface via
  `RequestError`.
- Security and audit: ADR-026 and ADR-033 still own bearer/session
  authentication, `ROLE_ADMIN`, actor provenance, MDC, and request logging. MCP
  continues to send `X-Actor: mcp-server` only as the existing dev/test fallback.

## Cross-Cutting Layers

- **MCP public schema:** `file_path` must not mean "any absolute path." The
  adapter should accept only a path that resolves to an approved import root,
  names a regular `.sdoc` file, and passes the symlink policy. If the public
  input later moves to repo-relative paths or a file picker, keep the same
  resolver behind it.
- **Workspace containment:** the default approved root is the MCP launch
  workspace (the consumer repo cwd, preferably its Git top-level when
  available). Every accepted path must be canonicalized and proven inside an
  approved root before reading. `..`, root-equal paths, sibling repos, and
  absolute paths outside the workspace must fail.
- **File kind and extension:** require a `.sdoc` basename, reject directories,
  FIFOs, devices, sockets, and final symlinks via `lstat` / `stat` checks. Do
  not rely on MIME type, filename alone, or backend parser rejection as the
  local-file exfiltration control.
- **Auth and network:** `/api/v1/admin/import/strictdoc` remains `ROLE_ADMIN`
  under ADR-026. This does not protect local files when `GC_BASE_URL` points to
  a remote instance, so the local resolver is the decisive exfiltration gate.
- **Secret handling and OS exposure:** bearer tokens stay in environment/header
  handling and must not appear in tool arguments, logs, errors, or command
  lines. The import path should use Node filesystem APIs directly; do not shell
  out to inspect files.
- **Error leakage:** expected local refusals should not include file contents,
  stack traces, Authorization headers, or environment data. Keep messages
  actionable but bounded to path validation facts.
- **Observability:** no new audit store is needed. Backend request logging and
  import audit records cover accepted uploads; MCP may return a local refusal
  but should not log rejected path contents as telemetry or compliance evidence.

## Extensibility

The reusable seam is a small typed local-file resolver: `(workspaceRoot,
allowedRoots, allowedExtensions, symlinkPolicy) -> { path, basename, bytes }`.
For #246 the caller supplies `.sdoc` and a no-symlink policy. The same seam can
later serve `.reqif`, pack-registry import, or a user-approved file picker
without each action inventing its own path traversal and file-kind checks.

If approved roots become configurable, add one strict MCP-side config parser for
that surface and document it in `mcp/ground-control/README.md`; do not scatter
environment parsing across individual import actions.

## Gotchas And Anti-Patterns

- Do not patch only the `gc_admin` switch while leaving
  `readOperatorSuppliedFile()` as a generally trusted arbitrary-path helper.
- Do not use `path.normalize()` or extension checks before realpath/`lstat` as
  the security boundary.
- Do not allow in-repo symlinks for uploads just because config path validation
  allows some in-repo symlinks. Uploading bytes to a remote backend is a
  different risk class.
- Do not replace backend parser validation with MCP-side StrictDoc parsing. MCP
  validates file selection; backend validates content semantics.
- Do not add a new backend endpoint, exception hierarchy, auth model, actor
  source, import persistence table, or alternate error envelope for this issue.
- Do not make `GC_MCP_ADMIN=1` the only control. It controls catalog exposure,
  not whether a prompt-injected agent can read a local file once the tool is
  available.

## Non-Goals

- No redesign of StrictDoc import semantics, idempotency, hierarchy handling,
  relation handling, or import audit persistence.
- No replacement for ADR-026 backend authorization or ADR-033 audit actor
  provenance.
- No generic MCP schema framework, OpenAPI/codegen project, browser file picker,
  or cross-platform secret scanner as part of #246.
- No change to `gc_query`; it is read-only REST and already denylisted away from
  admin import paths.
