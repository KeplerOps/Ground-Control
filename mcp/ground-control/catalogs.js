// MCP tool catalog inventory and selection (ADR-035).
//
// This module is the single source of truth for which MCP tools belong to
// which catalog tag. `index.js` registers every tool through
// `registerCatalogTool`, which consults the active selection and skips
// registration when the catalog is not requested. The active selection comes
// from the `GC_MCP_CATALOGS` environment variable; when unset or empty, the
// `DEFAULT_CATALOGS` set is used. The `workflow` catalog is non-negotiable —
// it is added regardless of what the env value names.
//
// This module also owns the `gc_query` read-only escape hatch's path /
// params validators, body truncation cap, and timeout. Keeping them here
// (instead of in `gc-query.js` or `lib.js`) means they ship with the catalog
// inventory they protect: the same allowlist that justifies hiding admin
// catalogs from the default surface also justifies blocking admin paths
// from gc_query.

// ---------------------------------------------------------------------------
// Catalog inventory
// ---------------------------------------------------------------------------

/**
 * Tool name -> catalog tag, expressed as catalog -> sorted list of tool names.
 *
 * Every tool registered by `index.js` MUST appear in exactly one catalog. The
 * `validateInventory` helper enforces this at startup so drift surfaces
 * immediately rather than silently shipping a tool in zero catalogs (which
 * would render it permanently unreachable).
 */
export const TOOL_CATALOGS = Object.freeze({
  // ---- workflow: /implement lifecycle, always registered --------------------
  workflow: Object.freeze([
    "gc_codex_architecture_preflight",
    "gc_codex_review",
    "gc_codex_verify_finding",
    "gc_create_github_issue",
    "gc_dashboard_stats",
    "gc_get_repo_ground_control_context",
    "gc_post_implementation_plan",
    "gc_query",
  ]),

  // ---- requirements: requirements, projects, relations, traceability, history
  requirements: Object.freeze([
    "gc_archive_requirement",
    "gc_bulk_transition_status",
    "gc_clone_requirement",
    "gc_create_project",
    "gc_create_relation",
    "gc_create_requirement",
    "gc_create_traceability_link",
    "gc_delete_relation",
    "gc_delete_traceability_link",
    "gc_export_audit_timeline",
    "gc_export_document",
    "gc_export_requirements",
    "gc_export_sweep_report",
    "gc_get_project_timeline",
    "gc_get_relation_history",
    "gc_get_relations",
    "gc_get_requirement",
    "gc_get_requirement_diff",
    "gc_get_requirement_history",
    "gc_get_timeline",
    "gc_get_traceability",
    "gc_get_traceability_by_artifact",
    "gc_get_traceability_link_history",
    "gc_get_work_order",
    "gc_list_projects",
    "gc_list_requirements",
    "gc_transition_status",
    "gc_update_requirement",
  ]),

  // ---- documents: ADRs, documents, sections, grammars ----------------------
  documents: Object.freeze([
    "gc_add_section_content",
    "gc_create_adr",
    "gc_create_document",
    "gc_create_section",
    "gc_delete_adr",
    "gc_delete_document",
    "gc_delete_document_grammar",
    "gc_delete_section",
    "gc_delete_section_content",
    "gc_get_adr",
    "gc_get_adr_requirements",
    "gc_get_document",
    "gc_get_document_grammar",
    "gc_get_document_reading_order",
    "gc_get_section",
    "gc_get_section_tree",
    "gc_list_adrs",
    "gc_list_documents",
    "gc_list_section_content",
    "gc_list_sections",
    "gc_set_document_grammar",
    "gc_transition_adr_status",
    "gc_update_adr",
    "gc_update_document",
    "gc_update_section",
    "gc_update_section_content",
  ]),

  // ---- analysis: graph traversal, coverage, baselines, quality gates -------
  analysis: Object.freeze([
    "gc_analyze_completeness",
    "gc_analyze_consistency",
    "gc_analyze_coverage_gaps",
    "gc_analyze_cross_wave",
    "gc_analyze_cycles",
    "gc_analyze_impact",
    "gc_analyze_orphans",
    "gc_analyze_similarity",
    "gc_analyze_status_drift",
    "gc_compare_baselines",
    "gc_create_baseline",
    "gc_create_quality_gate",
    "gc_delete_baseline",
    "gc_delete_quality_gate",
    "gc_evaluate_quality_gates",
    "gc_extract_subgraph",
    "gc_find_graph_paths",
    "gc_find_paths",
    "gc_get_ancestors",
    "gc_get_baseline",
    "gc_get_baseline_snapshot",
    "gc_get_descendants",
    "gc_get_graph_visualization",
    "gc_get_quality_gate",
    "gc_list_baselines",
    "gc_list_quality_gates",
    "gc_traverse_graph",
    "gc_update_quality_gate",
  ]),

  // ---- assets: operational assets, asset links, external IDs ---------------
  assets: Object.freeze([
    "gc_archive_asset",
    "gc_asset_impact_analysis",
    "gc_create_asset",
    "gc_create_asset_external_id",
    "gc_create_asset_link",
    "gc_create_asset_relation",
    "gc_delete_asset",
    "gc_delete_asset_external_id",
    "gc_delete_asset_link",
    "gc_delete_asset_relation",
    "gc_detect_asset_cycles",
    "gc_extract_asset_subgraph",
    "gc_find_asset_by_external_id",
    "gc_get_asset",
    "gc_get_asset_by_uid",
    "gc_get_asset_links",
    "gc_get_asset_links_by_target",
    "gc_get_asset_relations",
    "gc_list_asset_external_ids",
    "gc_list_assets",
    "gc_update_asset",
    "gc_update_asset_external_id",
  ]),

  // ---- risk: scenarios, threat models, controls, treatments, verification --
  risk: Object.freeze([
    "gc_create_control",
    "gc_create_control_link",
    "gc_create_methodology_profile",
    "gc_create_observation",
    "gc_create_risk_assessment_result",
    "gc_create_risk_register_record",
    "gc_create_risk_scenario",
    "gc_create_risk_scenario_link",
    "gc_create_threat_model",
    "gc_create_threat_model_link",
    "gc_create_treatment_plan",
    "gc_create_verification_result",
    "gc_delete_control",
    "gc_delete_control_link",
    "gc_delete_methodology_profile",
    "gc_delete_observation",
    "gc_delete_risk_assessment_result",
    "gc_delete_risk_register_record",
    "gc_delete_risk_scenario",
    "gc_delete_risk_scenario_link",
    "gc_delete_threat_model",
    "gc_delete_threat_model_link",
    "gc_delete_treatment_plan",
    "gc_delete_verification_result",
    "gc_get_control",
    "gc_get_methodology_profile",
    "gc_get_observation",
    "gc_get_risk_assessment_result",
    "gc_get_risk_register_record",
    "gc_get_risk_scenario",
    "gc_get_risk_scenario_requirements",
    "gc_get_threat_model",
    "gc_get_treatment_plan",
    "gc_get_verification_result",
    "gc_list_control_links",
    "gc_list_controls",
    "gc_list_latest_observations",
    "gc_list_methodology_profiles",
    "gc_list_observations",
    "gc_list_risk_assessment_results",
    "gc_list_risk_register_records",
    "gc_list_risk_scenario_links",
    "gc_list_risk_scenarios",
    "gc_list_threat_model_links",
    "gc_list_threat_models",
    "gc_list_treatment_plans",
    "gc_list_verification_results",
    "gc_transition_control_status",
    "gc_transition_risk_assessment_approval_state",
    "gc_transition_risk_register_record_status",
    "gc_transition_risk_scenario_status",
    "gc_transition_threat_model_status",
    "gc_transition_treatment_plan_status",
    "gc_update_control",
    "gc_update_methodology_profile",
    "gc_update_observation",
    "gc_update_risk_assessment_result",
    "gc_update_risk_register_record",
    "gc_update_risk_scenario",
    "gc_update_threat_model",
    "gc_update_treatment_plan",
    "gc_update_verification_result",
  ]),

  // ---- admin: imports, embeddings, plugins, packs, trust, materialize ------
  admin: Object.freeze([
    "gc_check_pack_compatibility",
    "gc_create_control_pack_override",
    "gc_create_trust_policy",
    "gc_delete_control_pack_override",
    "gc_delete_pack_registry_entry",
    "gc_delete_trust_policy",
    "gc_deprecate_control_pack",
    "gc_embed_project",
    "gc_embed_requirement",
    "gc_get_control_pack",
    "gc_get_control_pack_entry",
    "gc_get_embedding_status",
    "gc_get_pack_install_record",
    "gc_get_pack_registry_entry",
    "gc_get_plugin",
    "gc_get_trust_policy",
    "gc_import_pack_registry_entry",
    "gc_import_reqif",
    "gc_import_strictdoc",
    "gc_install_pack_from_registry",
    "gc_list_control_pack_entries",
    "gc_list_control_pack_overrides",
    "gc_list_control_packs",
    "gc_list_pack_install_records",
    "gc_list_pack_registry_entries",
    "gc_list_pack_versions",
    "gc_list_plugins",
    "gc_list_trust_policies",
    "gc_materialize_graph",
    "gc_register_pack_registry_entry",
    "gc_register_plugin",
    "gc_remove_control_pack",
    "gc_resolve_pack",
    "gc_run_sweep",
    "gc_run_sweep_all",
    "gc_sync_github",
    "gc_sync_github_prs",
    "gc_unregister_plugin",
    "gc_update_pack_registry_entry",
    "gc_update_trust_policy",
    "gc_upgrade_pack_from_registry",
    "gc_withdraw_pack_registry_entry",
  ]),
});

export const CATALOG_NAMES = Object.freeze(Object.keys(TOOL_CATALOGS).sort());

/**
 * Default catalogs registered when GC_MCP_CATALOGS is unset or empty.
 *
 * Sized to cover an `/implement` run end-to-end without loading risk, asset,
 * or admin surfaces. Per ADR-035, `workflow` is always present regardless.
 */
export const DEFAULT_CATALOGS = Object.freeze([
  "workflow",
  "requirements",
  "documents",
  "analysis",
]);

// Reverse map: tool name -> catalog (built once, immutable).
const TOOL_TO_CATALOG = (() => {
  const map = new Map();
  for (const [catalog, names] of Object.entries(TOOL_CATALOGS)) {
    for (const name of names) {
      map.set(name, catalog);
    }
  }
  return map;
})();

// ---------------------------------------------------------------------------
// Selection
// ---------------------------------------------------------------------------

/**
 * Parse the GC_MCP_CATALOGS env value into a Set of catalog names.
 *
 * Rules (ADR-035):
 *   - Undefined or empty value -> the DEFAULT_CATALOGS set.
 *   - "all" (in any position) -> every catalog.
 *   - Comma-separated names -> the named catalogs, plus "workflow" always.
 *   - Whitespace around names is trimmed; empty entries are dropped.
 *   - Unknown names throw — no partial selection, drift caught at startup.
 *
 * @param {string|undefined|null} envValue
 * @returns {Set<string>}
 */
export function selectCatalogs(envValue) {
  if (envValue === undefined || envValue === null) {
    return new Set(DEFAULT_CATALOGS);
  }
  const parts = envValue
    .split(",")
    .map((s) => s.trim())
    .filter((s) => s.length > 0);
  if (parts.length === 0) {
    // Whitespace-only or comma-only input is treated as unset rather than as
    // "the user named zero catalogs"; otherwise a typo in `.mcp.json` (e.g.
    // `"GC_MCP_CATALOGS": "   "`) silently strips requirements/documents/
    // analysis from the default surface.
    return new Set(DEFAULT_CATALOGS);
  }
  if (parts.includes("all")) {
    return new Set(CATALOG_NAMES);
  }
  const unknown = parts.filter((p) => !CATALOG_NAMES.includes(p));
  if (unknown.length > 0) {
    throw new Error(
      `Unknown catalog(s) in GC_MCP_CATALOGS: ${unknown.join(", ")}. ` +
        `Valid catalogs: ${CATALOG_NAMES.join(", ")}, or "all".`,
    );
  }
  const selected = new Set(parts);
  selected.add("workflow"); // non-negotiable per preflight + ADR-035
  return selected;
}

/**
 * @param {string} toolName
 * @param {Set<string>} activeCatalogs
 * @returns {boolean}
 */
export function isToolEnabled(toolName, activeCatalogs) {
  const catalog = TOOL_TO_CATALOG.get(toolName);
  if (catalog === undefined) {
    throw new Error(
      `Tool ${toolName} is not in the catalog inventory. Add it to mcp/ground-control/catalogs.js or fix the typo.`,
    );
  }
  return activeCatalogs.has(catalog);
}

/**
 * Wrap `server.tool(...)` so registration is gated by the active catalog set.
 * Drift catches:
 *   - unknown catalog name -> throw (typo or missing inventory entry)
 *   - tool name not in the inventory under the named catalog -> throw
 *
 * @param {{tool: function}} server  An MCP server instance with `.tool(name, desc, schema, handler)`.
 * @param {string} catalog
 * @param {string} name
 * @param {string} description
 * @param {object} schema
 * @param {function} handler
 * @param {Set<string>} activeCatalogs
 */
export function registerCatalogTool(
  server,
  catalog,
  name,
  description,
  schema,
  handler,
  activeCatalogs,
) {
  if (!CATALOG_NAMES.includes(catalog)) {
    throw new Error(
      `registerCatalogTool: unknown catalog '${catalog}' for tool '${name}'. Valid catalogs: ${CATALOG_NAMES.join(", ")}.`,
    );
  }
  const inventoryCatalog = TOOL_TO_CATALOG.get(name);
  if (inventoryCatalog !== catalog) {
    throw new Error(
      `registerCatalogTool: tool '${name}' is registered under catalog '${catalog}' but the inventory has it under '${inventoryCatalog ?? "none"}'. Fix mcp/ground-control/catalogs.js or the registration call.`,
    );
  }
  if (!activeCatalogs.has(catalog)) {
    return false; // not selected for this run
  }
  server.tool(name, description, schema, handler);
  return true;
}

/**
 * Compare the set of tool names actually passed to `registerCatalogTool` (or,
 * equivalently, recorded as registered) against the inventory. Returns a list
 * of human-readable error messages. An empty list means the inventory is
 * consistent with the registration code.
 *
 * @param {Set<string>} registeredNames
 * @returns {string[]}
 */
export function validateInventory(registeredNames) {
  const errors = [];
  for (const name of TOOL_TO_CATALOG.keys()) {
    if (!registeredNames.has(name)) {
      errors.push(
        `Inventory entry '${name}' was never registered. Either remove it from mcp/ground-control/catalogs.js or add the missing registration in mcp/ground-control/index.js.`,
      );
    }
  }
  for (const name of registeredNames) {
    if (!TOOL_TO_CATALOG.has(name)) {
      errors.push(
        `Tool '${name}' was registered but is missing from the catalog inventory. Add it to mcp/ground-control/catalogs.js with the correct catalog tag.`,
      );
    }
  }
  return errors;
}

// ---------------------------------------------------------------------------
// gc_query: path / params validation, body truncation, cost cap
// ---------------------------------------------------------------------------

/**
 * Read-oriented `/api/v1/**` prefixes that gc_query is allowed to GET. This
 * is intentionally narrower than the backend's authenticated read surface:
 * default-deny for ad-hoc agent queries. Adding a new prefix is a deliberate
 * decision; the README documents the maintenance step.
 */
export const GC_QUERY_PATH_ALLOWLIST = Object.freeze([
  "/api/v1/adrs",
  "/api/v1/analysis",
  "/api/v1/assets",
  "/api/v1/audit",
  "/api/v1/baselines",
  "/api/v1/controls",
  "/api/v1/dashboard",
  "/api/v1/documents",
  "/api/v1/graph",
  "/api/v1/methodology-profiles",
  "/api/v1/observations",
  "/api/v1/projects",
  "/api/v1/quality-gates",
  "/api/v1/relations",
  "/api/v1/requirements",
  "/api/v1/risk-assessment-results",
  "/api/v1/risk-register-records",
  "/api/v1/risk-scenarios",
  "/api/v1/sections",
  "/api/v1/threat-models",
  "/api/v1/timeline",
  "/api/v1/traceability",
  "/api/v1/treatment-plans",
  "/api/v1/verification-results",
]);

/**
 * Prefixes that gc_query refuses even though they live under /api/v1/. The
 * backend's path matrix (ADR-026) also rejects them for non-ROLE_ADMIN
 * principals; defense in depth is the point. The denylist takes precedence
 * over the allowlist when they overlap (e.g., /api/v1/analysis is allowed
 * but /api/v1/analysis/sweep is denied).
 */
export const GC_QUERY_PATH_DENYLIST = Object.freeze([
  "/api/v1/admin",
  "/api/v1/analysis/sweep",
  "/api/v1/embeddings",
  "/api/v1/pack-registry",
]);

/** 1 MiB body cap for gc_query responses. */
export const GC_QUERY_BODY_BYTE_CAP = 1024 * 1024;

/** 30s wall-clock timeout for gc_query requests. */
export const GC_QUERY_TIMEOUT_MS = 30_000;

class GcQueryValidationError extends Error {
  constructor(code, message) {
    super(`${code}: ${message}`);
    this.name = "GcQueryValidationError";
    this.code = code;
  }
}

/**
 * Throw GcQueryValidationError unless `path` is a valid gc_query target.
 *
 * Valid paths:
 *   - non-empty string
 *   - starts with one of GC_QUERY_PATH_ALLOWLIST entries (exact prefix match
 *     with a trailing `/`-or-end boundary)
 *   - does NOT start with any GC_QUERY_PATH_DENYLIST entry
 *   - contains no `..`, no `?`, no `#`, no protocol
 *
 * @param {string} path
 */
export function validateGcQueryPath(path) {
  if (typeof path !== "string" || path.length === 0) {
    throw new GcQueryValidationError(
      "invalid_query_path",
      "path must be a non-empty string",
    );
  }
  // Reject anything that looks like an absolute or protocol-relative URL.
  if (path.startsWith("//") || /^[a-z][a-z0-9+.-]*:/i.test(path)) {
    throw new GcQueryValidationError(
      "invalid_query_path",
      `absolute or protocol-relative URLs are not allowed: ${path}`,
    );
  }
  if (path.includes("..")) {
    throw new GcQueryValidationError(
      "invalid_query_path",
      `path must not contain '..': ${path}`,
    );
  }
  if (path.includes("?") || path.includes("#")) {
    throw new GcQueryValidationError(
      "invalid_query_path",
      `path must not contain '?' or '#'; use the params field instead: ${path}`,
    );
  }
  // Strict character whitelist — no percent-encoding (`%2e%2e` would otherwise
  // canonicalize past the literal-`..` check at fetch time and bypass the
  // denylist), no backslash, no `@/;` etc. Backend resource IDs are uuid /
  // uid-shaped: alphanumerics, hyphens, dots, underscores, slashes only.
  if (!/^[A-Za-z0-9/_.-]+$/.test(path)) {
    throw new GcQueryValidationError(
      "invalid_query_path",
      `path contains characters outside [A-Za-z0-9/_.-]; use 'params' for any query value: ${path}`,
    );
  }
  // Denylist takes precedence.
  for (const denied of GC_QUERY_PATH_DENYLIST) {
    if (path === denied || path.startsWith(`${denied}/`)) {
      throw new GcQueryValidationError(
        "invalid_query_path",
        `path is in the gc_query denylist (${denied}): ${path}`,
      );
    }
  }
  // Allowlist: must match a prefix at a /-boundary.
  const allowed = GC_QUERY_PATH_ALLOWLIST.some(
    (prefix) => path === prefix || path.startsWith(`${prefix}/`),
  );
  if (!allowed) {
    throw new GcQueryValidationError(
      "invalid_query_path",
      `path is not in the gc_query allowlist: ${path}`,
    );
  }
}

/**
 * Throw GcQueryValidationError unless `params` is a valid gc_query params
 * object: undefined, null, or a flat object whose values are
 * string|number|boolean|null.
 *
 * @param {unknown} params
 */
export function validateGcQueryParams(params) {
  if (params === undefined || params === null) return;
  if (typeof params !== "object" || Array.isArray(params)) {
    throw new GcQueryValidationError(
      "invalid_query_params",
      "params must be a flat object",
    );
  }
  for (const [k, v] of Object.entries(params)) {
    // undefined / null are dropped at URL build time (mirrors buildUrl's
    // existing behavior); accept them here so callers can pass
    // `{ wave: maybeUndefined }` without conditional spread tricks.
    if (v === undefined || v === null) continue;
    const t = typeof v;
    if (t === "string" || t === "number" || t === "boolean") continue;
    throw new GcQueryValidationError(
      "invalid_query_params",
      `params.${k}: only string|number|boolean|null are allowed (got ${Array.isArray(v) ? "array" : t})`,
    );
  }
}

/**
 * Cap a response body string at GC_QUERY_BODY_BYTE_CAP bytes, returning the
 * original body unchanged when under the cap. When over the cap, returns a
 * truncated body with a clear marker so the agent sees the truncation rather
 * than thinking it received a complete response.
 *
 * Uses byte length (not string length) so multi-byte characters can't sneak
 * past the cap.
 *
 * @param {string} body
 * @returns {{body: string, truncated: boolean, original_byte_length: number}}
 */
export function truncateBody(body) {
  const originalByteLength = Buffer.byteLength(body, "utf8");
  if (originalByteLength <= GC_QUERY_BODY_BYTE_CAP) {
    return { body, truncated: false, original_byte_length: originalByteLength };
  }
  // Byte-safe truncation: cut the underlying buffer, then decode with the
  // replacement character handler so a partial UTF-8 sequence at the boundary
  // becomes U+FFFD instead of throwing.
  const buf = Buffer.from(body, "utf8").subarray(0, GC_QUERY_BODY_BYTE_CAP);
  const head = buf.toString("utf8");
  const marker = `\n\n... [gc_query response truncated at ${GC_QUERY_BODY_BYTE_CAP} bytes; original was ${originalByteLength} bytes]`;
  return {
    body: `${head}${marker}`,
    truncated: true,
    original_byte_length: originalByteLength,
  };
}

// ---------------------------------------------------------------------------
// Context augmentation
// ---------------------------------------------------------------------------

/**
 * Append an `mcp` field to the backend's `gc_get_repo_ground_control_context`
 * response so agents can see which catalogs are loaded, which are available,
 * and whether `gc_query` is reachable. Pure function, does not mutate the
 * backend payload.
 *
 * @param {object} backendPayload
 * @param {Set<string>} activeCatalogs
 * @param {number} toolCount
 * @returns {object}
 */
export function augmentGroundControlContext(backendPayload, activeCatalogs, toolCount) {
  return {
    ...backendPayload,
    mcp: {
      catalogs_active: [...activeCatalogs].sort(),
      catalogs_available: [...CATALOG_NAMES].sort(),
      tool_count: toolCount,
      gc_query_available: activeCatalogs.has("workflow"),
    },
  };
}
