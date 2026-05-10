# ADR-035: MCP Tool Catalog Curation and Read-Only Query Escape Hatch

## Status

Accepted

## Date

2026-05-11

## Context

Issue [#628](https://github.com/KeplerOps/Ground-Control/issues/628) (audit
finding A12-03, severity:medium) flagged that the Ground Control MCP server
registers 215 tools (203 at audit time) across 39 sections in
`mcp/ground-control/index.js`. Every tool's name, description, and Zod input
schema is loaded into every connected LLM client at session start, regardless
of whether the session is doing requirements work, an `/implement` run, risk
modelling, or pack-registry administration.

Two failure modes have been observed in agent sessions:

1. **Catalog noise.** The seven graph-traversal tools (`gc_get_relations`,
   `gc_get_descendants`, `gc_get_ancestors`, `gc_find_paths`,
   `gc_find_graph_paths`, `gc_extract_subgraph`, `gc_traverse_graph`,
   `gc_analyze_impact`) all answer the same shape of question. An agent that
   does not already know which one to call burns calls discovering it. This
   pattern repeats for asset, risk, threat-model, and control families.
2. **Long-tail self-service.** The curated set, however well-chosen, will always
   under-serve "I have a hypothesis, let me check it" queries that join across
   requirements, traceability, and recent changes. The only escape today is
   "load more tools," which makes the noise problem worse, or "ask the user to
   look it up," which defeats the agent's purpose.

The accepted boundaries (preflight notes, ADR-026, ADR-027, ADR-029, ADR-031,
ADR-034) constrain the solution shape:

- The Spring REST API (governed by ADR-026's bearer-token + path-matrix
  authorization chain) is the semantic authority. MCP tools are adapters over
  it; they must not become a second controller layer.
- Workflow tools that encode `/implement` gates — `gc_get_repo_ground_control_context`,
  `gc_codex_architecture_preflight`, `gc_post_implementation_plan`,
  `gc_codex_review`, `gc_codex_verify_finding` — must remain first-class and
  must not be hidden behind a generic proxy.
- Hiding a tool from a client catalog is a usability decision, not an
  authorization decision. Authorization remains the backend's responsibility.
- The MCP adapter must reuse `buildUrl`, `addAuthorizationHeader`, `request`,
  `RequestError`, and `parseErrorBody` from `mcp/ground-control/lib.js`. It
  must not introduce a second auth or error model.
- ADR-026's path matrix already gates `/api/v1/admin/**`, `/api/v1/embeddings/**`,
  `/api/v1/analysis/sweep/**`, and `/api/v1/pack-registry/**` to `ROLE_ADMIN`.
  Any read-only escape hatch sits behind that chain unchanged.

## Decision

Adopt a **two-part design**: a tag-based catalog inventory that selectively
registers MCP tools at startup, plus a read-only, allowlist-bounded `gc_query`
escape hatch for ad-hoc agent queries.

### 1. Catalog inventory (data-driven tool selection)

Every MCP tool is tagged with exactly one **catalog**, drawn from this fixed
taxonomy:

| Catalog | Purpose | Always registered? |
|---|---|---|
| `workflow` | `/implement` lifecycle: context, preflight, plan, review, verify, dashboards, GitHub sync, `gc_query` | Yes |
| `requirements` | requirements + relations + traceability + history + work-order + projects | Default |
| `documents` | ADRs, documents, sections, section content, reading order, grammars | Default |
| `analysis` | coverage gaps, orphans, completeness, status drift, similarity, cross-wave, sweep, baselines, semantic analysis, quality gates, graph traversal | Default |
| `assets` | operational assets, asset links, asset external IDs | Opt-in |
| `risk` | observations, risk scenarios, threat models, controls, methodology profiles, risk register, risk assessment results, treatment plans, verification results | Opt-in |
| `admin` | embeddings, plugins, pack registry, trust policies, pack install records, materialize-graph | Opt-in |

The mapping lives in `mcp/ground-control/catalogs.js` as a single static
object. Adding a new tool requires adding a single entry; the mapping is the
authoritative source.

**Selection.** A new environment variable, `GC_MCP_CATALOGS`, is a comma-
separated list of catalog names. Special value `all` registers every tool.
When unset, the default selection is `workflow,requirements,documents,analysis`
— sized to cover an `/implement` run end-to-end without loading risk, asset,
or pack-registry surfaces. The `workflow` catalog is always registered
regardless of `GC_MCP_CATALOGS`'s contents (workflow tools are non-negotiable
per preflight).

**Implementation seam.** A single helper, `registerCatalogTool(server, catalog,
name, description, schema, handler)`, wraps `server.tool(...)`. The helper
consults the active selection set and skips registration when the catalog is
not selected. Tool registration code in `index.js` changes only by replacing
`server.tool(` with `registerCatalogTool(server, "<catalog>",`. Registration
shape, Zod schemas, descriptions, and handlers are unchanged.

**Validation.** The catalog inventory is validated at startup: every name
appearing in a `registerCatalogTool` call must appear in the catalogs map, and
every name in the catalogs map must correspond to a registered tool. A
mismatch throws on startup so drift is caught immediately. A unit test
enforces the same invariant.

**Why this shape, not split servers, and not dynamic tool-list-change
notifications.**

- Split servers (one binary per catalog) preserve the maintenance problem
  under more files. The MCP SDK bootstrap, env loading, error handlers, and
  Zod schemas would all need to be cloned. Tagging is the smallest change
  that captures the actual decision.
- Dynamic tool-list-change notifications depend on client support that is
  uneven across MCP clients (Claude Code is in scope; other clients may not
  honour the notification). A startup selection works on every client and is
  trivial to fall back from.
- A `mcp.profile` field in `.ground-control.yaml` was considered. Rejected:
  catalog selection is a per-launch concern of the MCP client, not a repo
  property. A single repo's `/implement` and risk-modelling sessions may want
  different catalogs.

### 2. `gc_query` (read-only REST escape hatch)

A new MCP tool, `gc_query`, registered under the `workflow` catalog (always
on). It accepts:

- `path` — a relative path matching `^/api/v1/[A-Za-z0-9/_-]+$`. Absolute
  URLs, protocol-relative URLs, paths containing `..`, query strings embedded
  in the path string, and any path outside `/api/v1/` are rejected before any
  network call.
- `params` — an optional flat object of query parameters. Values are
  `string | number | boolean | null`. Arrays and nested objects are rejected.
  Construction goes through `buildUrl(path, params)`.

The tool **only issues GET requests**. There is no `method` parameter; HTTP
verb cannot be supplied or overridden by the caller. Headers cannot be
supplied either; the existing `addAuthorizationHeader` adds the bearer token,
and the request always emits `Accept: application/json`.

**Path allowlist.** `gc_query` enforces an explicit prefix allowlist over
`/api/v1/`. The allowlist covers the read-oriented prefixes for the curated
catalogs *plus* the read prefixes for the hidden risk catalog so `gc_query`
remains a real read fallback when an operator narrows the catalog
selection: `/api/v1/adrs`, `/api/v1/analysis`, `/api/v1/assets`,
`/api/v1/audit`, `/api/v1/baselines`, `/api/v1/controls`,
`/api/v1/dashboard`, `/api/v1/documents`, `/api/v1/graph`,
`/api/v1/methodology-profiles`, `/api/v1/observations`, `/api/v1/projects`,
`/api/v1/quality-gates`, `/api/v1/relations`, `/api/v1/requirements`,
`/api/v1/risk-assessment-results`, `/api/v1/risk-register-records`,
`/api/v1/risk-scenarios`, `/api/v1/sections`, `/api/v1/threat-models`,
`/api/v1/timeline`, `/api/v1/traceability`, `/api/v1/treatment-plans`,
`/api/v1/verification-results`. The four `ROLE_ADMIN`-restricted prefixes
from ADR-026 (`/api/v1/admin/**`, `/api/v1/embeddings/**`,
`/api/v1/analysis/sweep/**`, `/api/v1/pack-registry/**`) are explicitly
rejected by `gc_query` even though backend authorization would also reject
them — defense in depth, and clearer error messages ("path not in gc_query
allowlist") versus a 403 from the backend. The allowlist is exported as
`GC_QUERY_PATH_ALLOWLIST` from `mcp/ground-control/catalogs.js` and a
test in `mcp/ground-control/gc-query.test.js` asserts every prefix is
mentioned in this ADR and in `mcp/ground-control/README.md`.

**Cost cap.** `gc_query` enforces a 30-second timeout via `AbortController`,
mirroring the timeout pattern used by `request()`. Response bodies above
1 MiB are truncated with a clear marker so an accidental unbounded GET on
a 100k-row export does not flood the agent's context. Both limits are
constants, tested, and documented.

**Error semantics.** `gc_query` reuses `RequestError` for non-2xx responses
and the `err()` renderer in `index.js`. A path-validation rejection is a
synthetic `RequestError` with `status: 0` and `code: "invalid_query_path"`
so the same client-side error surface applies.

**What `gc_query` is NOT.** It is not a Cypher proxy (ADR-032 forbids
caller-constructed Cypher). It is not a generic `gc_call_api(path, method, body)`
— writes go through curated tools so each carries its own Zod schema and
domain semantics. It is not a session for the agent to rummage through
admin endpoints; the allowlist is intentionally narrower than the backend's
authenticated read surface.

### 3. Advertise the loaded catalog in `gc_get_repo_ground_control_context`

The MCP-side wrapper of `gc_get_repo_ground_control_context` augments the
backend response with an `mcp` field describing the running adapter:

```json
{
  ...,
  "mcp": {
    "catalogs_active": ["workflow", "requirements", "documents", "analysis"],
    "catalogs_available": ["workflow", "requirements", "documents", "analysis", "assets", "risk", "admin"],
    "tool_count": 78,
    "gc_query_available": true
  }
}
```

The augmentation is purely MCP-side; no backend change. Agents can call this
tool first to learn which surface they have, decide whether to ask the user
to widen `GC_MCP_CATALOGS`, and discover that `gc_query` is available for
ad-hoc reads.

## Consequences

### Positive

- Default agent session loads ~40% fewer tool definitions while keeping every
  `/implement`-relevant capability. The seven-tool graph-traversal cluster
  drops to one default-on family (`analysis` catalog), and the assets / risk /
  admin clusters disappear from the default LLM context until explicitly
  requested.
- `gc_query` closes the long-tail "let me check this hypothesis" gap without
  expanding the write surface. An agent that finds the curated tools don't
  pre-bake a join it needs can issue a constrained GET and read the answer.
- Catalog tagging is a data structure, not an architecture. Adding a new
  curated catalog (e.g., a future `compliance` tag) is a configuration
  change, not a server clone.
- ADR-026's path matrix and audit chain are unchanged; this PR is purely
  about the LLM-visible adapter surface.

### Negative

- The catalog inventory must be kept in sync with tool registrations. The
  startup validator and a unit test guard against drift, but adding a tool
  means touching `catalogs.js` in addition to `index.js`.
- `gc_query`'s path allowlist must be maintained as the backend surface
  evolves. New read endpoints land outside the agent's reach until someone
  adds them to the allowlist. This is intentional ("default deny") and the
  ADR records the maintenance discipline rather than papering over it.
- The default `GC_MCP_CATALOGS=workflow,requirements,documents,analysis`
  hides `assets`, `risk`, and `admin` from agents that didn't know to widen
  it. The `mcp` field on `gc_get_repo_ground_control_context` makes the
  hidden catalogs discoverable as a name, but the agent still has to ask the
  operator to flip the env var.

### Risks

- **Allowlist drift.** A new admin endpoint that doesn't follow the existing
  prefix conventions could land outside both the gc_query allowlist (which
  would correctly reject it) and the gc_query denylist (which doesn't matter
  because the allowlist is the gate). The greater risk is a new *read*
  endpoint that ought to be agent-reachable but lives at an unexpected
  prefix; it would silently be unavailable until the allowlist is updated.
  Mitigation: a simple grep test enumerates `/api/v1/**` controller paths
  and reports any not covered by either the allowlist or the documented
  denylist. The grep test is informational, not a hard gate, because the
  default-deny posture is correct even when it lags.
- **Catalog over-shrinking.** A misconfigured `GC_MCP_CATALOGS` value (e.g.,
  `GC_MCP_CATALOGS=workflow` alone) would leave an agent with only workflow
  tools and `gc_query`. That is by design — `gc_query` is the read-only
  fallback for everything that catalog selection hides. But agents would
  experience a degraded UX without knowing why. Mitigation: the `mcp` field
  on `gc_get_repo_ground_control_context` advertises which catalogs are
  active vs available, and the README's "Tuning the catalog" section
  documents the recommended defaults.
- **Header injection at gc_query input.** The Zod schema for `gc_query`
  forbids a `headers` field entirely; there is no parameter at the MCP
  surface for caller-supplied headers, and the implementation does not
  read any. The existing `addAuthorizationHeader` writes the bearer
  unconditionally based on path. A regression where someone wires a
  `headers` field through later would be caught by the `gc_query` Zod-
  schema test (which asserts the schema rejects extra keys via `.strict()`).
- **Body-size truncation hides material data.** The 1 MiB body cap could
  truncate a legitimately-large response. A clear truncation marker plus a
  `truncated: true` flag in the returned envelope makes the truncation
  visible. Agents that need the full body should use the catalog tool
  designed for that resource, which paginates.

## Alternatives considered

- **Split into multiple MCP server binaries** (`ground-control-workflow`,
  `ground-control-requirements`, ...). Rejected: clones bootstrap, env
  loading, error handlers, and schemas; multiplies the maintenance problem.
- **Dynamic tool-list-change notifications.** Rejected as the *primary*
  mechanism: client support varies. May be added later as an enhancement on
  top of static selection.
- **Per-repo catalog selection in `.ground-control.yaml`.** Rejected:
  catalog selection is a per-launch concern of the MCP client. A repo's
  `/implement` and `assets` sessions may legitimately want different
  catalogs.
- **Generic `gc_call_api(path, method, body)` escape hatch.** Rejected: an
  agent that can issue arbitrary writes via a generic proxy bypasses the
  per-tool Zod schema that captures the domain shape (and the agent's
  contract). Splitting into a *read-only* `gc_query` plus curated writes
  preserves the schema contract on the write side.
- **Cypher passthrough via `gc_query`.** Rejected by ADR-032: caller-
  constructed Cypher is not a security boundary the system can hold.

## References

- Issue [#628](https://github.com/KeplerOps/Ground-Control/issues/628) — A12-03 audit finding
- ADR-026 (REST API Access Control)
- ADR-027 (Agent-Neutral Implement Workflow Packaging)
- ADR-029 (Issue-Thread Gate Model)
- ADR-031 (Codex Review Stopping Model)
- ADR-032 (AGE Query Construction Boundary)
- ADR-034 (API Enum Contract Single Source)
- `architecture/notes/mcp-tool-catalog-surface-preflight.md`
- `mcp/ground-control/lib.js` (`buildUrl`, `addAuthorizationHeader`, `request`, `RequestError`, `parseErrorBody`)
- `mcp/ground-control/index.js` (39 catalog sections, 215 `server.tool` registrations)
