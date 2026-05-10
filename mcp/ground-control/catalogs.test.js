// Tests for the MCP tool catalog inventory and selection logic (ADR-035).
//
// Covers:
//   - selectCatalogs: turn GC_MCP_CATALOGS env value into the active set
//   - isToolEnabled / TOOL_CATALOGS inventory consistency
//   - registerCatalogTool: filter at registration time
//   - validateInventory: drift catch (every registered tool tagged exactly once)
//   - GC_QUERY_PATH_ALLOWLIST: explicit prefix list for gc_query
//   - validateGcQueryPath: path validator (allowlist + denylist + traversal/host checks)
//   - validateGcQueryParams: params validator (flat object only, primitive values)
//   - truncateBody: 1 MiB cap with `truncated: true` marker
//   - augmentGroundControlContext: appends mcp.{catalogs_active, catalogs_available, tool_count, gc_query_available}
import { describe, it } from "node:test";
import assert from "node:assert/strict";
import {
  selectCatalogs,
  isToolEnabled,
  TOOL_CATALOGS,
  CATALOG_NAMES,
  DEFAULT_CATALOGS,
  registerCatalogTool,
  validateInventory,
  GC_QUERY_PATH_ALLOWLIST,
  GC_QUERY_PATH_DENYLIST,
  validateGcQueryPath,
  validateGcQueryParams,
  GC_QUERY_BODY_BYTE_CAP,
  GC_QUERY_TIMEOUT_MS,
  truncateBody,
  augmentGroundControlContext,
} from "./catalogs.js";

// ---------------------------------------------------------------------------
// selectCatalogs
// ---------------------------------------------------------------------------

describe("selectCatalogs", () => {
  it("returns the default set when env value is undefined", () => {
    const got = selectCatalogs(undefined);
    assert.deepEqual([...got].sort(), [...DEFAULT_CATALOGS].sort());
    assert.ok(got.has("workflow"), "workflow must always be selected");
  });

  it("returns the default set when env value is empty string", () => {
    assert.deepEqual([...selectCatalogs("")].sort(), [...DEFAULT_CATALOGS].sort());
  });

  it("returns every catalog when env value is 'all'", () => {
    const got = selectCatalogs("all");
    assert.deepEqual([...got].sort(), [...CATALOG_NAMES].sort());
  });

  it("returns only the requested catalog plus workflow", () => {
    const got = selectCatalogs("requirements");
    assert.deepEqual([...got].sort(), ["requirements", "workflow"]);
  });

  it("dedupes and trims whitespace and empty entries", () => {
    const got = selectCatalogs(" requirements , , risk ,requirements ");
    assert.deepEqual([...got].sort(), ["requirements", "risk", "workflow"]);
  });

  it("ensures workflow is always present even when the user did not name it", () => {
    const got = selectCatalogs("admin");
    assert.ok(got.has("workflow"));
    assert.ok(got.has("admin"));
  });

  it("throws when an unknown catalog is named (no partial selection)", () => {
    assert.throws(
      () => selectCatalogs("requirements,not-a-real-catalog"),
      /not-a-real-catalog/,
    );
  });

  it("'all' overrides any other value present", () => {
    const got = selectCatalogs("admin,all");
    assert.deepEqual([...got].sort(), [...CATALOG_NAMES].sort());
  });

  it("treats whitespace-only input as unset (returns the default catalogs)", () => {
    // A `.mcp.json` typo that puts a literal `"   "` into GC_MCP_CATALOGS must
    // not silently strip requirements/documents/analysis from the default set.
    assert.deepEqual([...selectCatalogs("   ")].sort(), [...DEFAULT_CATALOGS].sort());
    assert.deepEqual([...selectCatalogs("\t\n")].sort(), [...DEFAULT_CATALOGS].sort());
    assert.deepEqual([...selectCatalogs(",,,")].sort(), [...DEFAULT_CATALOGS].sort());
    assert.deepEqual([...selectCatalogs(" , ,  , ")].sort(), [...DEFAULT_CATALOGS].sort());
  });
});

// ---------------------------------------------------------------------------
// isToolEnabled / TOOL_CATALOGS inventory
// ---------------------------------------------------------------------------

describe("default-catalog tools must not require ROLE_ADMIN at the backend", () => {
  // Tools whose backend path is in the ADR-026 ROLE_ADMIN matrix and which
  // therefore should NOT live in a default-on catalog (workflow / requirements
  // / documents / analysis). Any such tool that DOES live in workflow must be
  // explicitly listed here as an acknowledged contract requirement.
  const ADMIN_PATHED_TOOLS_IN_WORKFLOW_OK = new Set([
    // Required by the /implement skill — when /implement picks up a UID, it
    // creates the issue via /api/v1/admin/github/issues. Operators of a
    // security-enabled deployment must supply a ROLE_ADMIN token. Documented
    // in ADR-035 and mcp/ground-control/README.md.
    "gc_create_github_issue",
  ]);

  // Backend functions that POST/GET /api/v1/admin|embeddings|analysis/sweep|
  // pack-registry|trust-policies|pack-install-records — built once from
  // lib.js's requiresAdminRole() classification.
  const ADMIN_PATHED_TOOLS = new Set([
    "gc_create_github_issue",
    "gc_embed_project",
    "gc_embed_requirement",
    "gc_get_embedding_status",
    "gc_import_pack_registry_entry",
    "gc_import_reqif",
    "gc_import_strictdoc",
    "gc_install_pack_from_registry",
    "gc_materialize_graph",
    "gc_run_sweep",
    "gc_run_sweep_all",
    "gc_sync_github",
    "gc_sync_github_prs",
  ]);

  const DEFAULT_CATALOGS_SET = new Set(["workflow", "requirements", "documents", "analysis"]);

  it("admin-pathed tools are placed in 'admin' (or explicitly acknowledged in workflow)", () => {
    const violations = [];
    for (const [catalog, names] of Object.entries(TOOL_CATALOGS)) {
      for (const name of names) {
        if (!ADMIN_PATHED_TOOLS.has(name)) continue;
        if (catalog === "admin") continue;
        if (catalog === "workflow" && ADMIN_PATHED_TOOLS_IN_WORKFLOW_OK.has(name)) continue;
        violations.push(`${name} is in catalog '${catalog}' but its backend path requires ROLE_ADMIN`);
      }
    }
    assert.deepEqual(violations, []);
  });

  it("no admin-pathed tool sits in a default-on catalog (except documented exceptions)", () => {
    const violations = [];
    for (const [catalog, names] of Object.entries(TOOL_CATALOGS)) {
      if (!DEFAULT_CATALOGS_SET.has(catalog)) continue;
      for (const name of names) {
        if (!ADMIN_PATHED_TOOLS.has(name)) continue;
        if (catalog === "workflow" && ADMIN_PATHED_TOOLS_IN_WORKFLOW_OK.has(name)) continue;
        violations.push(`${name} is in default-on catalog '${catalog}' but its backend path requires ROLE_ADMIN`);
      }
    }
    assert.deepEqual(violations, []);
  });
});

describe("TOOL_CATALOGS inventory", () => {
  it("declares the seven catalogs from ADR-035", () => {
    assert.deepEqual(
      [...CATALOG_NAMES].sort(),
      ["admin", "analysis", "assets", "documents", "requirements", "risk", "workflow"],
    );
  });

  it("default catalogs are workflow + requirements + documents + analysis", () => {
    assert.deepEqual([...DEFAULT_CATALOGS].sort(), ["analysis", "documents", "requirements", "workflow"]);
  });

  it("every tool name appears in exactly one catalog", () => {
    const seen = new Map();
    for (const [catalog, names] of Object.entries(TOOL_CATALOGS)) {
      for (const name of names) {
        const prior = seen.get(name);
        assert.equal(prior, undefined, `tool ${name} appears in both ${prior} and ${catalog}`);
        seen.set(name, catalog);
      }
    }
  });

  it("workflow catalog includes the non-negotiable tools from preflight", () => {
    const workflow = new Set(TOOL_CATALOGS.workflow);
    for (const required of [
      "gc_get_repo_ground_control_context",
      "gc_codex_architecture_preflight",
      "gc_post_implementation_plan",
      "gc_codex_review",
      "gc_codex_verify_finding",
      "gc_query",
    ]) {
      assert.ok(workflow.has(required), `${required} must be in the workflow catalog`);
    }
  });
});

describe("isToolEnabled", () => {
  it("returns true when the tool's catalog is in the active set", () => {
    const active = new Set(["workflow", "requirements"]);
    assert.equal(isToolEnabled("gc_list_requirements", active), true);
  });

  it("returns false when the tool's catalog is not in the active set", () => {
    const active = new Set(["workflow", "requirements"]);
    // gc_list_risk_scenarios is in the risk catalog
    assert.equal(isToolEnabled("gc_list_risk_scenarios", active), false);
  });

  it("throws for an unknown tool name (drift catch)", () => {
    assert.throws(
      () => isToolEnabled("gc_not_a_real_tool", new Set(["workflow"])),
      /gc_not_a_real_tool/,
    );
  });
});

// ---------------------------------------------------------------------------
// registerCatalogTool
// ---------------------------------------------------------------------------

describe("registerCatalogTool", () => {
  function makeFakeServer() {
    const calls = [];
    return {
      calls,
      tool(...args) {
        calls.push(args);
      },
    };
  }

  it("registers when the catalog is active and returns true, forwarding all four args verbatim", () => {
    const server = makeFakeServer();
    const active = new Set(["workflow"]);
    const sentinelSchema = { path: "sentinel-schema" };
    const sentinelHandler = async () => ({ marker: "sentinel-handler" });
    const result = registerCatalogTool(
      server,
      "workflow",
      "gc_query",
      "desc",
      sentinelSchema,
      sentinelHandler,
      active,
    );
    assert.equal(server.calls.length, 1);
    assert.equal(server.calls[0][0], "gc_query");
    assert.equal(server.calls[0][1], "desc");
    // Assert schema and handler are forwarded by identity, not just shape — a
    // regression where the wrapper dropped or substituted either would fail.
    assert.strictEqual(server.calls[0][2], sentinelSchema);
    assert.strictEqual(server.calls[0][3], sentinelHandler);
    assert.equal(result, true);
  });

  it("skips registration when the catalog is inactive and returns false", () => {
    const server = makeFakeServer();
    const active = new Set(["workflow"]);
    const result = registerCatalogTool(server, "risk", "gc_list_risk_scenarios", "desc", {}, async () => ({}), active);
    assert.equal(server.calls.length, 0);
    assert.equal(result, false);
  });

  it("throws for an unknown catalog (drift catch)", () => {
    const server = makeFakeServer();
    const active = new Set(["workflow"]);
    assert.throws(
      () => registerCatalogTool(server, "not-a-catalog", "gc_x", "desc", {}, async () => ({}), active),
      /not-a-catalog/,
    );
  });

  it("throws when the tool name is not in the inventory under that catalog", () => {
    const server = makeFakeServer();
    const active = new Set(["workflow"]);
    // gc_list_risk_scenarios is in `risk`, not `workflow`
    assert.throws(
      () => registerCatalogTool(server, "workflow", "gc_list_risk_scenarios", "desc", {}, async () => ({}), active),
      /gc_list_risk_scenarios/,
    );
  });
});

describe("validateInventory", () => {
  it("returns no errors when every registered tool matches inventory", () => {
    const registered = new Set();
    for (const names of Object.values(TOOL_CATALOGS)) {
      for (const name of names) registered.add(name);
    }
    const errors = validateInventory(registered);
    assert.deepEqual(errors, []);
  });

  it("reports inventory entries that were never registered", () => {
    const registered = new Set(); // nothing was registered
    const errors = validateInventory(registered);
    assert.ok(errors.length > 0, "expected drift errors when no tools registered");
    assert.ok(errors.some((e) => e.includes("never registered")));
  });

  it("reports registered tools missing from inventory", () => {
    const registered = new Set();
    for (const names of Object.values(TOOL_CATALOGS)) {
      for (const name of names) registered.add(name);
    }
    registered.add("gc_phantom_tool");
    const errors = validateInventory(registered);
    assert.ok(errors.some((e) => e.includes("gc_phantom_tool")));
  });
});

// ---------------------------------------------------------------------------
// gc_query path validation
// ---------------------------------------------------------------------------

describe("validateGcQueryPath", () => {
  it("accepts allowlisted /api/v1 read prefixes", () => {
    for (const prefix of GC_QUERY_PATH_ALLOWLIST) {
      assert.doesNotThrow(() => validateGcQueryPath(prefix));
    }
  });

  it("accepts a sub-path under an allowlisted prefix", () => {
    assert.doesNotThrow(() => validateGcQueryPath("/api/v1/requirements/abc-123"));
    assert.doesNotThrow(() => validateGcQueryPath("/api/v1/traceability/by-artifact"));
  });

  it("rejects an absolute URL", () => {
    assert.throws(() => validateGcQueryPath("http://evil/api/v1/requirements"), /invalid_query_path/);
    assert.throws(() => validateGcQueryPath("https://evil/api/v1/requirements"), /invalid_query_path/);
  });

  it("rejects a protocol-relative URL", () => {
    assert.throws(() => validateGcQueryPath("//evil/api/v1/requirements"), /invalid_query_path/);
  });

  it("rejects path traversal", () => {
    assert.throws(() => validateGcQueryPath("/api/v1/requirements/../admin/users"), /invalid_query_path/);
    assert.throws(() => validateGcQueryPath("/api/v1/../etc/passwd"), /invalid_query_path/);
  });

  it("rejects paths outside /api/v1/", () => {
    assert.throws(() => validateGcQueryPath("/actuator/health"), /invalid_query_path/);
    assert.throws(() => validateGcQueryPath("/api/v2/requirements"), /invalid_query_path/);
    assert.throws(() => validateGcQueryPath("/swagger-ui/index.html"), /invalid_query_path/);
  });

  it("rejects denied admin prefixes even though they live under /api/v1/", () => {
    for (const denied of GC_QUERY_PATH_DENYLIST) {
      assert.throws(() => validateGcQueryPath(`${denied}/whatever`), /invalid_query_path/);
    }
  });

  it("rejects an embedded query string", () => {
    assert.throws(() => validateGcQueryPath("/api/v1/requirements?status=ACTIVE"), /invalid_query_path/);
  });

  it("rejects percent-encoded segments — no encoded-dot bypass of denylist (security)", () => {
    // Without canonicalization, `/api/v1/requirements/%2e%2e/admin/users` would
    // pass the allowlist and the literal '..' check. Reject any '%' in the path.
    assert.throws(
      () => validateGcQueryPath("/api/v1/requirements/%2e%2e/admin/users"),
      /invalid_query_path/,
    );
    assert.throws(
      () => validateGcQueryPath("/api/v1/requirements/%2E%2E/admin/users"),
      /invalid_query_path/,
    );
    assert.throws(() => validateGcQueryPath("/api/v1/requirements/%2f"), /invalid_query_path/);
    assert.throws(() => validateGcQueryPath("/api/v1/requirements/%41"), /invalid_query_path/);
  });

  it("rejects backslash and other non-path characters", () => {
    assert.throws(() => validateGcQueryPath("/api/v1/requirements\\admin"), /invalid_query_path/);
    assert.throws(() => validateGcQueryPath("/api/v1/requirements@admin"), /invalid_query_path/);
    assert.throws(() => validateGcQueryPath("/api/v1/requirements;admin"), /invalid_query_path/);
  });

  it("rejects a fragment", () => {
    assert.throws(() => validateGcQueryPath("/api/v1/requirements#x"), /invalid_query_path/);
  });

  it("rejects a path that is not in the allowlist", () => {
    assert.throws(() => validateGcQueryPath("/api/v1/some-future-endpoint"), /invalid_query_path/);
  });

  it("rejects empty / non-string path", () => {
    assert.throws(() => validateGcQueryPath(""), /invalid_query_path/);
    assert.throws(() => validateGcQueryPath(null), /invalid_query_path/);
    assert.throws(() => validateGcQueryPath(undefined), /invalid_query_path/);
    assert.throws(() => validateGcQueryPath(42), /invalid_query_path/);
  });

  it("includes the risk-domain read prefixes so gc_query is a real fallback for the hidden risk catalog", () => {
    const required = [
      "/api/v1/methodology-profiles",
      "/api/v1/risk-register-records",
      "/api/v1/risk-assessment-results",
      "/api/v1/treatment-plans",
      "/api/v1/verification-results",
    ];
    for (const prefix of required) {
      assert.ok(
        GC_QUERY_PATH_ALLOWLIST.includes(prefix),
        `${prefix} must be in GC_QUERY_PATH_ALLOWLIST so gc_query covers the risk catalog`,
      );
    }
  });

  it("denylist takes precedence over allowlist regardless of prefix overlap", () => {
    // /api/v1/analysis is allowlisted; /api/v1/analysis/sweep is denylisted.
    assert.doesNotThrow(() => validateGcQueryPath("/api/v1/analysis/coverage-gaps"));
    assert.throws(() => validateGcQueryPath("/api/v1/analysis/sweep/run"), /invalid_query_path/);
  });
});

// ---------------------------------------------------------------------------
// gc_query params validation
// ---------------------------------------------------------------------------

describe("validateGcQueryParams", () => {
  it("accepts undefined / null / {} as 'no params'", () => {
    assert.doesNotThrow(() => validateGcQueryParams(undefined));
    assert.doesNotThrow(() => validateGcQueryParams(null));
    assert.doesNotThrow(() => validateGcQueryParams({}));
  });

  it("accepts string, number, boolean, null primitive values", () => {
    assert.doesNotThrow(() => validateGcQueryParams({ a: "x", b: 1, c: true, d: null }));
  });

  it("rejects array values", () => {
    assert.throws(() => validateGcQueryParams({ a: [1, 2] }), /invalid_query_params/);
  });

  it("rejects nested object values", () => {
    assert.throws(() => validateGcQueryParams({ a: { b: 1 } }), /invalid_query_params/);
  });

  it("rejects function values", () => {
    assert.throws(() => validateGcQueryParams({ a: () => 1 }), /invalid_query_params/);
  });

  it("rejects non-object params (string, array, etc.)", () => {
    assert.throws(() => validateGcQueryParams("a=1"), /invalid_query_params/);
    assert.throws(() => validateGcQueryParams([1, 2]), /invalid_query_params/);
  });
});

// ---------------------------------------------------------------------------
// truncateBody
// ---------------------------------------------------------------------------

describe("truncateBody", () => {
  it("returns body unchanged when under the cap", () => {
    const body = "x".repeat(1024);
    const out = truncateBody(body);
    assert.equal(out.body, body);
    assert.equal(out.truncated, false);
    assert.equal(out.original_byte_length, 1024);
  });

  it("truncates body above the cap and marks it", () => {
    const body = "y".repeat(GC_QUERY_BODY_BYTE_CAP + 100);
    const out = truncateBody(body);
    // Truncation must produce something <= the cap (we allow a small marker overhead).
    assert.ok(Buffer.byteLength(out.body, "utf8") <= GC_QUERY_BODY_BYTE_CAP + 256);
    assert.equal(out.truncated, true);
    assert.equal(out.original_byte_length, GC_QUERY_BODY_BYTE_CAP + 100);
    assert.match(out.body, /truncated/i, "truncation marker must be present in body");
  });

  it("uses byte length, not string length, for the cap (multi-byte safety)", () => {
    // 4-byte utf-8 emoji repeated. String length is half the byte length.
    const ch = "💥"; // 4 bytes
    const repeats = Math.ceil(GC_QUERY_BODY_BYTE_CAP / 4) + 10;
    const body = ch.repeat(repeats);
    const out = truncateBody(body);
    assert.equal(out.truncated, true);
    assert.equal(out.original_byte_length, Buffer.byteLength(body, "utf8"));
  });

  it("exposes a positive integer cap and timeout", () => {
    assert.ok(Number.isInteger(GC_QUERY_BODY_BYTE_CAP) && GC_QUERY_BODY_BYTE_CAP > 0);
    assert.ok(Number.isInteger(GC_QUERY_TIMEOUT_MS) && GC_QUERY_TIMEOUT_MS > 0);
  });
});

// ---------------------------------------------------------------------------
// augmentGroundControlContext
// ---------------------------------------------------------------------------

describe("augmentGroundControlContext", () => {
  it("appends mcp.{catalogs_active, catalogs_available, tool_count, gc_query_available}", () => {
    const backend = { project: "ground-control", status: "ok" };
    const active = new Set(["workflow", "requirements"]);
    const out = augmentGroundControlContext(backend, active, 42);
    assert.equal(out.project, "ground-control");
    assert.equal(out.status, "ok");
    assert.deepEqual([...out.mcp.catalogs_active].sort(), ["requirements", "workflow"]);
    assert.deepEqual([...out.mcp.catalogs_available].sort(), [...CATALOG_NAMES].sort());
    assert.equal(out.mcp.tool_count, 42);
    assert.equal(out.mcp.gc_query_available, true);
  });

  it("does not mutate the backend payload", () => {
    const backend = { project: "ground-control" };
    const active = new Set(["workflow"]);
    augmentGroundControlContext(backend, active, 1);
    assert.equal(backend.mcp, undefined);
  });

  it("flags gc_query as unavailable when workflow is somehow excluded", () => {
    // Defensive: workflow is non-negotiable, but if a future bug excluded it,
    // the agent must see gc_query_available = false rather than the misleading default.
    const backend = { project: "ground-control" };
    const active = new Set(["requirements"]);
    const out = augmentGroundControlContext(backend, active, 1);
    assert.equal(out.mcp.gc_query_available, false);
  });
});
