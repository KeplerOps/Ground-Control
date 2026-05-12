#!/usr/bin/env node
// Ground Control MCP Server
//
// Environment variables consumed by this server (see mcp/ground-control/lib.js):
//   GC_BASE_URL                              Base URL of the Ground Control backend.
//   GROUND_CONTROL_API_TOKEN                 Bearer token forwarded on every
//                                             /api/v1/** request when the backend
//                                             has groundcontrol.security.enabled=true.
//   GROUND_CONTROL_PACK_REGISTRY_ADMIN_TOKEN Legacy admin-only token; forwarded only
//                                             on paths requiring ROLE_ADMIN. Fallback
//                                             when GROUND_CONTROL_API_TOKEN is unset.
//
// These values are read from the consumer repo's `.env` file at startup.
//
// ============================================================================
// CONSOLIDATED TOOL SURFACE (ADR-035)
// ============================================================================
//
// Every REST endpoint used to have its own MCP tool — 215 in total. After
// ADR-035 the surface is consolidated to ~33 named tools plus the read-only
// `gc_query` escape hatch:
//
//   Workflow primitives the /implement skill calls by name (unchanged):
//     gc_get_repo_ground_control_context, gc_codex_architecture_preflight,
//     gc_codex_review, gc_codex_verify_finding, gc_post_implementation_plan,
//     gc_create_github_issue, gc_dashboard_stats, gc_query,
//     gc_get_requirement, gc_get_traceability, gc_get_traceability_by_artifact,
//     gc_create_traceability_link, gc_delete_traceability_link,
//     gc_transition_status, gc_bulk_transition_status
//
//   Consolidated entity tools (one per entity, action-discriminated):
//     gc_requirement, gc_relation, gc_adr, gc_document, gc_section,
//     gc_asset, gc_observation, gc_risk_scenario, gc_threat_model,
//     gc_control, gc_risk_governance, gc_analyze, gc_graph, gc_baseline,
//     gc_quality_gate, gc_admin, gc_pack
//
// Pure GETs (history, timeline, exports, list-by-X) are NOT registered as
// named tools — they're reachable via `gc_query` with the right /api/v1/*
// path. The allowlist covers every read prefix the curated tools used to
// expose; agents discover them via gc_get_repo_ground_control_context's
// catalog field.

import { readFileSync } from "node:fs";
import { join } from "node:path";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import {
  // ---- project/requirement/relation/traceability ----
  listProjects, createProject,
  getRequirementByUid, listRequirements, createRequirement, updateRequirement,
  transitionStatus, bulkTransitionStatus, archiveRequirement, cloneRequirement,
  createRelation, getRelations, deleteRelation,
  getTraceabilityLinks, getTraceabilityByArtifact, createTraceabilityLink,
  deleteTraceabilityLink,
  // ---- analysis ----
  detectCycles, findOrphans, findCoverageGaps, impactAnalysis,
  crossWaveValidation, detectConsistencyViolations, analyzeCompleteness,
  analyzeStatusDrift, analyzeSemanticSimilarity, getWorkOrder,
  getDashboardStats,
  // ---- history / exports (kept for completeness even though tools route to gc_query) ----
  getRequirementHistory, getRelationHistory, getTraceabilityLinkHistory,
  getRequirementTimeline, getRequirementDiff, getProjectTimeline,
  exportAuditTimeline, exportRequirements, exportSweepReport, exportDocument,
  // ---- graph ----
  materializeGraph, getAncestors, getDescendants, findPaths,
  getGraphVisualization, extractSubgraph, traverseGraph, findGraphPaths,
  // ---- github / codex workflow ----
  createGitHubIssue, formatIssueBody,
  runSweep, runSweepAll,
  getRepoGroundControlContext,
  runCodexArchitecturePreflight, runCodexReview, runCodexVerifyFinding,
  runTestQualityReview, TEST_QUALITY_REVIEW_HARD_CAP,
  runPostImplementationPlan,
  runPostDecisionRecord, runPostFinalReport, runRenderPrBody, runLogStepTelemetry,
  DECISION_RECORD_REVIEWERS, DECISION_RECORD_DECISIONS, DECISION_RECORD_CLASSIFICATIONS,
  PR_BODY_CHANGE_CLASSES, PR_REQUIREMENT_RE, EXACT_REQUIREMENT_UID_RE,
  TELEMETRY_TIERS, TELEMETRY_OUTCOMES,
  buildCodexReviewToolDescription, buildCodexReviewOverrideCapDescription,
  buildCodexReviewOverrideReasonDescription,
  CODEX_REVIEW_HARD_CAP, CODEX_REVIEW_PREPUSH_HARD_CAP,
  // ---- embeddings ----
  embedRequirement, getEmbeddingStatus, embedProject,
  // ---- baselines + quality gates ----
  createBaseline, listBaselines, getBaseline, getBaselineSnapshot,
  compareBaselines, deleteBaseline,
  createQualityGate, listQualityGates, getQualityGate, updateQualityGate,
  deleteQualityGate, evaluateQualityGates,
  // ---- documents / sections / ADRs ----
  createDocument, listDocuments, getDocument, updateDocument, deleteDocument,
  createSection, listSections, getSectionTree, getSection, updateSection,
  deleteSection,
  addSectionContent, listSectionContent, updateSectionContent, deleteSectionContent,
  getDocumentReadingOrder,
  setDocumentGrammar, getDocumentGrammar, deleteDocumentGrammar,
  createAdr, listAdrs, getAdr, updateAdr, deleteAdr, transitionAdrStatus,
  getAdrRequirements,
  // ---- imports + sync ----
  importStrictdoc, importReqif, syncGithub, syncGithubPrs,
  // ---- assets ----
  createAsset, listAssets, getAsset, getAssetByUid, updateAsset, deleteAsset,
  archiveAsset, createAssetRelation, getAssetRelations, deleteAssetRelation,
  detectAssetCycles, assetImpactAnalysis, extractAssetSubgraph,
  createAssetLink, getAssetLinks, deleteAssetLink, getAssetLinksByTarget,
  createAssetExternalId, getAssetExternalIds, updateAssetExternalId,
  deleteAssetExternalId, findAssetByExternalId,
  // ---- risk domain ----
  createObservation, listObservations, getObservation, updateObservation,
  deleteObservation, listLatestObservations,
  createRiskScenario, listRiskScenarios, getRiskScenario, updateRiskScenario,
  deleteRiskScenario, transitionRiskScenarioStatus, getRiskScenarioRequirements,
  createRiskScenarioLink, listRiskScenarioLinks, deleteRiskScenarioLink,
  createThreatModel, listThreatModels, getThreatModel, updateThreatModel,
  deleteThreatModel, transitionThreatModelStatus,
  createThreatModelLink, listThreatModelLinks, deleteThreatModelLink,
  createControl, listControls, getControl, updateControl, deleteControl,
  transitionControlStatus, createControlLink, listControlLinks, deleteControlLink,
  createMethodologyProfile, listMethodologyProfiles, getMethodologyProfile,
  updateMethodologyProfile, deleteMethodologyProfile,
  createRiskRegisterRecord, listRiskRegisterRecords, getRiskRegisterRecord,
  updateRiskRegisterRecord, transitionRiskRegisterRecordStatus, deleteRiskRegisterRecord,
  createRiskAssessmentResult, listRiskAssessmentResults, getRiskAssessmentResult,
  updateRiskAssessmentResult, transitionRiskAssessmentApprovalState,
  deleteRiskAssessmentResult,
  createTreatmentPlan, listTreatmentPlans, getTreatmentPlan, updateTreatmentPlan,
  transitionTreatmentPlanStatus, deleteTreatmentPlan,
  createVerificationResult, listVerificationResults, getVerificationResult,
  updateVerificationResult, deleteVerificationResult,
  // ---- packs + plugins ----
  listPlugins, getPlugin, registerPlugin, unregisterPlugin,
  listControlPacks, getControlPack, deprecateControlPack, removeControlPack,
  listControlPackEntries, getControlPackEntry,
  createControlPackOverride, listControlPackOverrides, deleteControlPackOverride,
  registerPackRegistryEntry, importPackRegistryEntry, listPackRegistryEntries,
  listPackVersions, getPackRegistryEntry, updatePackRegistryEntry,
  withdrawPackRegistryEntry, deletePackRegistryEntry, resolvePack,
  checkPackCompatibility,
  createTrustPolicy, listTrustPolicies, getTrustPolicy, updateTrustPolicy,
  deleteTrustPolicy,
  installPackFromRegistry, upgradePackFromRegistry, listPackInstallRecords,
  getPackInstallRecord,
  // createAdminUser is intentionally NOT imported — passwords must not flow
  // through MCP tool-call payloads (ADR-037, codex security finding).
  listAdminUsers, updateAdminUserRole, updateAdminUserEnabled, deleteAdminUser,
  // ---- enums ----
  STATUSES, REQUIREMENT_TYPES, PRIORITIES, RELATION_TYPES,
  ARTIFACT_TYPES, LINK_TYPES, CHANGE_CATEGORIES, CONFIDENCE_LEVELS,
  METRIC_TYPES, COMPARISON_OPERATORS, ADR_STATUSES,
  ASSET_TYPES, ASSET_RELATION_TYPES, ASSET_LINK_TARGET_TYPES, ASSET_LINK_TYPES,
  OBSERVATION_CATEGORIES, RISK_SCENARIO_STATUSES,
  METHODOLOGY_FAMILIES, METHODOLOGY_PROFILE_STATUSES,
  RISK_REGISTER_STATUSES, RISK_ASSESSMENT_APPROVAL_STATUSES,
  TREATMENT_PLAN_STATUSES, TREATMENT_STRATEGIES,
  RISK_SCENARIO_LINK_TARGET_TYPES, RISK_SCENARIO_LINK_TYPES,
  THREAT_MODEL_STATUSES, STRIDE_CATEGORIES,
  THREAT_MODEL_LINK_TARGET_TYPES, THREAT_MODEL_LINK_TYPES,
  CONTROL_STATUSES, CONTROL_FUNCTIONS, CONTROL_LINK_TARGET_TYPES, CONTROL_LINK_TYPES,
  VERIFICATION_STATUSES, ASSURANCE_LEVELS,
  PLUGIN_TYPES, PLUGIN_LIFECYCLE_STATES,
  CONTROL_PACK_LIFECYCLE_STATES, CONTROL_PACK_ENTRY_STATUSES,
  PACK_TYPES, PACK_IMPORT_FORMATS, CATALOG_STATUSES,
  TRUST_OUTCOMES, INSTALL_OUTCOMES,
  TRUST_POLICY_FIELDS, TRUST_POLICY_RULE_OPERATORS,
} from "./lib.js";
import {
  executeGcQuery,
  gcQueryToolHandler,
  gcQuerySchema,
  GC_QUERY_BODY_BYTE_CAP,
  GC_QUERY_TIMEOUT_MS,
  GC_QUERY_PATH_ALLOWLIST,
  GC_QUERY_PATH_DENYLIST,
} from "./gc-query.js";

// Load .env from cwd before any auth header is composed.
function loadDotenvFromCwd() {
  let body;
  try {
    body = readFileSync(join(process.cwd(), ".env"), "utf-8");
  } catch (err) {
    if (err.code === "ENOENT") return;
    throw err;
  }
  for (const rawLine of body.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#")) continue;
    const eq = line.indexOf("=");
    if (eq <= 0) continue;
    const key = line.slice(0, eq).trim();
    let value = line.slice(eq + 1).trim();
    if ((value.startsWith('"') && value.endsWith('"')) ||
        (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    if (process.env[key] === undefined || process.env[key] === "") {
      process.env[key] = value;
    }
  }
}
loadDotenvFromCwd();

function ok(text) {
  return { content: [{ type: "text", text }] };
}

function err(e) {
  let text = `Error: ${e.message}`;
  if (e && e.name === "RequestError") {
    if (e.code) text += ` (${e.code})`;
    if (e.detail && typeof e.detail === "object" && Object.keys(e.detail).length > 0) {
      text += `\nDetail: ${JSON.stringify(e.detail, null, 2)}`;
    }
  }
  return { content: [{ type: "text", text }], isError: true };
}

function reqArg(args, key, action) {
  const v = args[key];
  if (v === undefined || v === null || v === "") {
    throw new Error(`'${key}' is required for action='${action}'`);
  }
  return v;
}

/**
 * Build the backend DTO for create/update actions by picking ONLY the listed
 * entity fields from the MCP args. This is the adapter boundary — MCP control
 * fields (action, kind, mode, subsystem, entity, id, project, paging filters,
 * etc.) must NOT leak into request bodies.
 */
function pick(args, keys) {
  const out = {};
  for (const k of keys) {
    if (args[k] !== undefined) out[k] = args[k];
  }
  return out;
}

// Admin gating: gc_admin and gc_pack expose ROLE_ADMIN-only write/mutating
// operations. They register only when GC_MCP_ADMIN is set, so a default MCP
// session does not surface admin operations even if the launching env happens
// to have an admin bearer token configured.
const ADMIN_TOOLS_ENABLED =
  process.env.GC_MCP_ADMIN === "1" ||
  process.env.GC_MCP_ADMIN === "true" ||
  process.env.GC_MCP_ADMIN === "yes";

const server = new McpServer({ name: "ground-control", version: "1.0.0" });

// ============================================================================
// WORKFLOW PRIMITIVES — kept by name; /implement and /ship skills call these.
// ============================================================================

// `gc_query` is the only tool in this file registered with a constructed
// `z.object(...).strict()` (rather than a raw shape) so the SDK preserves
// the strict-rejection contract. The deprecated `server.tool(name, desc,
// schema, cb)` overload routes a constructed Zod object into the
// `annotations` slot instead of `inputSchema`, which makes the SDK call
// the handler with its `extra` object (containing `signal`) in the args
// position — issue #874's root cause. `server.registerTool` takes
// `inputSchema` explicitly, so the strict schema actually gates the call
// and `signal` stays in `extra` where it belongs.
server.registerTool(
  "gc_query",
  {
    description:
      `Read-only ad-hoc GET against the Ground Control REST API (ADR-035). Use this when no curated tool covers the read you need. ` +
      `Path must be a relative '/api/v1/...' string under one of the allowlisted prefixes: ${GC_QUERY_PATH_ALLOWLIST.join(", ")}. ` +
      `Admin prefixes (${GC_QUERY_PATH_DENYLIST.join(", ")}) are rejected. ` +
      `GET only; pass query params via the structured 'params' object (flat, primitive values only). ` +
      `Body cap: ${GC_QUERY_BODY_BYTE_CAP} bytes; timeout: ${GC_QUERY_TIMEOUT_MS}ms.`,
    inputSchema: gcQuerySchema,
  },
  async (args) => {
    try { return ok(JSON.stringify(await gcQueryToolHandler(args), null, 2)); }
    catch (e) { return err(e); }
  },
);

server.tool(
  "gc_get_repo_ground_control_context",
  "Read the repo's .ground-control.yaml and return the workflow config: project, github_repo, workflow commands, sonarcloud, knowledge paths, and inlined plan-rules content. Returns validation errors when the file is missing or invalid.",
  { repo_path: z.string().describe("Absolute path to the target Git repository") },
  async ({ repo_path }) => {
    try { return ok(JSON.stringify(await getRepoGroundControlContext(repo_path), null, 2)); }
    catch (e) { return err(e); }
  },
);

server.tool(
  "gc_dashboard_stats",
  "Aggregate project health snapshot: requirement counts by status/wave, traceability coverage percentages, recent changes.",
  { project: z.string().optional().describe("Project identifier (auto-resolved if only one project)") },
  async ({ project }) => {
    try { return ok(JSON.stringify(await getDashboardStats(project), null, 2)); }
    catch (e) { return err(e); }
  },
);

server.tool(
  "gc_get_requirement",
  "Get a requirement by its human-readable UID (e.g. 'GC-O007').",
  {
    uid: z.string().describe("Requirement UID"),
    project: z.string().optional(),
  },
  async ({ uid, project }) => {
    try { return ok(JSON.stringify(await getRequirementByUid(uid, project), null, 2)); }
    catch (e) { return err(e); }
  },
);

server.tool(
  "gc_get_traceability",
  "Get all traceability links for a requirement (by UUID).",
  { id: z.string().uuid().describe("Requirement UUID") },
  async ({ id }) => {
    try { return ok(JSON.stringify(await getTraceabilityLinks(id), null, 2)); }
    catch (e) { return err(e); }
  },
);

server.tool(
  "gc_get_traceability_by_artifact",
  "Reverse lookup: find all traceability links for an artifact (file path, issue number, etc.).",
  {
    artifact_type: z.enum(ARTIFACT_TYPES),
    artifact_identifier: z.string(),
  },
  async ({ artifact_type, artifact_identifier }) => {
    try { return ok(JSON.stringify(await getTraceabilityByArtifact(artifact_type, artifact_identifier), null, 2)); }
    catch (e) { return err(e); }
  },
);

server.tool(
  "gc_create_traceability_link",
  "Link an artifact to a requirement.",
  {
    requirement_id: z.string().uuid(),
    artifact_type: z.enum(ARTIFACT_TYPES),
    artifact_identifier: z.string(),
    link_type: z.enum(LINK_TYPES),
    artifact_url: z.string().optional(),
    artifact_title: z.string().optional(),
  },
  async (args) => {
    try {
      const data = pick(args, ["artifact_type", "artifact_identifier", "link_type", "artifact_url", "artifact_title"]);
      return ok(JSON.stringify(await createTraceabilityLink(args.requirement_id, data), null, 2));
    } catch (e) { return err(e); }
  },
);

server.tool(
  "gc_delete_traceability_link",
  "Delete a traceability link.",
  {
    requirement_id: z.string().uuid(),
    link_id: z.string().uuid(),
  },
  async ({ requirement_id, link_id }) => {
    try { await deleteTraceabilityLink(requirement_id, link_id); return ok("Deleted"); }
    catch (e) { return err(e); }
  },
);

server.tool(
  "gc_transition_status",
  "Transition a requirement's status. Valid: DRAFT->ACTIVE, ACTIVE->DEPRECATED, ACTIVE->ARCHIVED, DEPRECATED->ARCHIVED.",
  {
    id: z.string().uuid(),
    status: z.enum(STATUSES),
    reason: z.string().optional(),
  },
  async ({ id, status, reason }) => {
    try { return ok(JSON.stringify(await transitionStatus(id, status, reason), null, 2)); }
    catch (e) { return err(e); }
  },
);

server.tool(
  "gc_bulk_transition_status",
  "Transition multiple requirements (by UUID) to the same status. Best-effort: valid succeed, invalid collected as failures.",
  {
    ids: z.array(z.string().uuid()).describe("Requirement UUIDs"),
    status: z.enum(STATUSES),
    reason: z.string().optional(),
  },
  async ({ ids, status, reason }) => {
    try { return ok(JSON.stringify(await bulkTransitionStatus(ids, status, reason), null, 2)); }
    catch (e) { return err(e); }
  },
);

server.tool(
  "gc_create_github_issue",
  "Create a GitHub issue from a requirement and auto-link it back. Required for /implement's UID-first path. Auto-link uses IMPLEMENTS for ACTIVE requirements; DRAFT requirements need a manual DOCUMENTS link afterwards.",
  {
    uid: z.string(),
    project: z.string().optional(),
    repo: z.string().regex(/^[a-zA-Z0-9][a-zA-Z0-9._-]*\/[a-zA-Z0-9][a-zA-Z0-9._-]*$/).optional(),
    labels: z.array(z.string()).optional(),
    extra_body: z.string().optional(),
  },
  async (args) => {
    try { return ok(JSON.stringify(await createGitHubIssue(args), null, 2)); }
    catch (e) { return err(e); }
  },
);

server.tool(
  "gc_codex_architecture_preflight",
  "Run Codex architecture preflight before implementation. Codex inspects the requirement and/or issue plus the repository, updates ADRs/design guidance when needed, and returns guardrails and changed files. At least one of requirement_uid or issue_number must be supplied.",
  {
    requirement_uid: z.string().optional(),
    repo_path: z.string(),
    project: z.string().optional(),
    issue_number: z.number().int().positive().optional(),
    repo: z.string().regex(/^[a-zA-Z0-9][a-zA-Z0-9._-]*\/[a-zA-Z0-9][a-zA-Z0-9._-]*$/).optional(),
  },
  async ({ requirement_uid, repo_path, project, issue_number, repo }) => {
    try {
      return ok(JSON.stringify(await runCodexArchitecturePreflight({
        requirementUid: requirement_uid, repoPath: repo_path, project,
        issueNumber: issue_number ?? null, repo: repo ?? null,
      }), null, 2));
    } catch (e) { return err(e); }
  },
);

server.tool(
  "gc_post_implementation_plan",
  "Post the implementation plan as a comment on the GitHub issue. Refuses unless a 'preflight' phase marker exists for the issue. Writes a 'plan' phase marker on success.",
  {
    repo_path: z.string(),
    issue_number: z.number().int().positive(),
    plan_body: z.string().min(1),
    override: z.boolean().optional(),
    override_reason: z.string().optional(),
  },
  async ({ repo_path, issue_number, plan_body, override, override_reason }) => {
    try {
      return ok(JSON.stringify(await runPostImplementationPlan({
        repoPath: repo_path, issueNumber: issue_number, planBody: plan_body,
        override: Boolean(override), overrideReason: override_reason ?? null,
      }), null, 2));
    } catch (e) { return err(e); }
  },
);

const CODEX_REVIEW_CAPS = { postPushCap: CODEX_REVIEW_HARD_CAP, prepushCap: CODEX_REVIEW_PREPUSH_HARD_CAP };

server.tool(
  "gc_codex_review",
  buildCodexReviewToolDescription(CODEX_REVIEW_CAPS),
  {
    repo_path: z.string(),
    base_branch: z.string().optional(),
    uncommitted: z.boolean().optional(),
    pr_number: z.number().int().positive().optional(),
    issue_number: z.number().int().positive().optional(),
    override_cap: z.boolean().optional().describe(buildCodexReviewOverrideCapDescription(CODEX_REVIEW_CAPS)),
    override_reason: z.string().optional().describe(buildCodexReviewOverrideReasonDescription(CODEX_REVIEW_CAPS)),
    override_phase_gate: z.boolean().optional(),
    override_phase_reason: z.string().optional(),
  },
  async ({ repo_path, base_branch, uncommitted, pr_number, issue_number, override_cap, override_reason, override_phase_gate, override_phase_reason }) => {
    try {
      return ok(JSON.stringify(await runCodexReview({
        repoPath: repo_path, baseBranch: base_branch ?? null,
        uncommitted: Boolean(uncommitted),
        prNumber: pr_number != null ? pr_number : null,
        issueNumber: issue_number != null ? issue_number : null,
        overrideCap: Boolean(override_cap),
        overrideReason: override_reason ?? null,
        overridePhaseGate: Boolean(override_phase_gate),
        overridePhaseReason: override_phase_reason ?? null,
      }), null, 2));
    } catch (e) { return err(e); }
  },
);

server.tool(
  "gc_test_quality_review",
  `Run the canonical /implement Step 13 test-quality review against the diff vs the base branch. ` +
    `Shells out to the \`claude\` CLI (Sonnet 4.6 by default) with the review-tests rubric and the ` +
    `changed test-file paths, parses the structured JSON output (validated by --json-schema), posts ` +
    `the durable findings record + cycle marker to the issue thread, and returns a structured ` +
    `envelope: \`{ ok, finding_count, findings, cycle, cap, next_action, findings_comment_url, ... }\`. ` +
    `The \`next_action\` field is "fix_findings_and_reinvoke" / "post_clean_decision_record_and_advance_to_step_14" / ` +
    `"fix_findings_then_summarize_and_escalate" / "post_summary_and_escalate_to_user" — the parent ` +
    `/implement workflow reads it as a directive. Replaces the prior Skill("review-tests") boundary, ` +
    `which produced prose findings that the autoregressive parent agent kept echoing back to the user ` +
    `instead of fixing in-turn (issue #884 v1 regression). Cycle cap: ${TEST_QUALITY_REVIEW_HARD_CAP} per ` +
    `issue, server-side; cycle ${TEST_QUALITY_REVIEW_HARD_CAP + 1} requires override_cap=true + override_reason. ` +
    `Authentication: the CLI invocation strips ANTHROPIC_API_KEY from the subprocess env so claude uses ` +
    `the host's OAuth session — see docs/DEVELOPMENT_WORKFLOW.md "Test-quality review engine".`,
  {
    repo_path: z.string(),
    base_branch: z.string().optional(),
    issue_number: z.number().int().positive().optional(),
    pr_number: z.number().int().positive().optional(),
    override_cap: z.boolean().optional(),
    override_reason: z.string().optional(),
    model: z.string().optional(),
  },
  async ({ repo_path, base_branch, issue_number, pr_number, override_cap, override_reason, model }) => {
    try {
      return ok(JSON.stringify(await runTestQualityReview({
        repoPath: repo_path,
        // Pass null when not supplied so the runner resolves from
        // .ground-control.yaml; the runner falls back to "dev" only if
        // YAML doesn't declare workflow.base_branch.
        baseBranch: base_branch ?? null,
        issueNumber: issue_number != null ? issue_number : null,
        prNumber: pr_number != null ? pr_number : null,
        overrideCap: Boolean(override_cap),
        overrideReason: override_reason ?? null,
        ...(model ? { model } : {}),
      }), null, 2));
    } catch (e) { return err(e); }
  },
);

server.tool(
  "gc_post_decision_record",
  "Post the canonical review-cycle decision record as a comment on the GitHub issue (per ADR-029, the issue thread is the durable record). Renders structured findings into the standard decision-record Markdown layout; rejects 'defer' decisions and any body containing detected secrets. Replaces free-prose decision comments from the Step 6.5 review loop. Returns the posted comment's URL and id.",
  {
    repo_path: z.string(),
    issue_number: z.number().int().positive(),
    cycle: z.number().int().positive(),
    reviewer: z.enum(DECISION_RECORD_REVIEWERS),
    findings: z.array(z.object({
      id: z.string().min(1),
      title: z.string().min(1),
      classification: z.enum(DECISION_RECORD_CLASSIFICATIONS),
      decision: z.enum(DECISION_RECORD_DECISIONS),
      rationale: z.string().min(1),
      // Required at runtime when decision === "wontfix" — see ADR-029. The
      // Zod object cannot conditionally require a field, so the validator in
      // lib.js performs the conditional check; expose the field here so MCP
      // callers can supply it. Pass a URL to the user's authorization
      // comment on the issue thread OR a verbatim quote with comment id.
      user_authorization: z.string().optional(),
      location: z.string().optional(),
      comment_url: z.string().optional(),
      instances: z.array(z.string().min(1)).optional(),
    })),
  },
  async ({ repo_path, issue_number, cycle, reviewer, findings }) => {
    try {
      return ok(JSON.stringify(await runPostDecisionRecord({
        repoPath: repo_path, issueNumber: issue_number, cycle, reviewer, findings,
      }), null, 2));
    } catch (e) { return err(e); }
  },
);

server.tool(
  "gc_post_final_report",
  "Post the canonical /implement Step 19 final report as a comment on the GitHub issue. Renders structured input (in-scope requirements, files-by-change-kind, reviews, traceability reconciliation, CI/SonarCloud status) into the standard final-report Markdown layout. Replaces free-prose Step 19 comments. Returns the posted comment's URL and id.",
  {
    repo_path: z.string(),
    issue_number: z.number().int().positive(),
    pr_number: z.number().int().positive(),
    requirements: z.array(z.object({
      // Anchored UID match — `requirements[].uid` must BE a UID (codex cycle-4 F2).
      uid: z.string().regex(EXACT_REQUIREMENT_UID_RE),
      title: z.string().min(1),
      status: z.string().min(1),
      note: z.string().optional(),
    })),
    files: z.object({
      added: z.array(z.string().min(1)).optional(),
      modified: z.array(z.string().min(1)).optional(),
      renamed: z.array(z.string().min(1)).optional(),
      deleted: z.array(z.string().min(1)).optional(),
    }).optional(),
    reviews: z.array(z.object({
      reviewer: z.string().min(1),
      summary: z.string().min(1),
    })),
    traceability: z.object({
      added: z.array(z.string()).optional(),
      updated: z.array(z.string()).optional(),
      deleted: z.array(z.string()).optional(),
      notes: z.string().optional(),
    }).optional(),
    ci_status: z.enum(["green", "red", "skipped"]),
    sonar_status: z.enum(["passed", "failed", "skipped"]),
    plan_comment_url: z.string().optional(),
    summary: z.string().optional(),
  },
  async ({ repo_path, issue_number, pr_number, requirements, files, reviews, traceability, ci_status, sonar_status, plan_comment_url, summary }) => {
    try {
      return ok(JSON.stringify(await runPostFinalReport({
        repoPath: repo_path,
        issueNumber: issue_number,
        prNumber: pr_number,
        requirements,
        files: files ?? {},
        reviews,
        traceability: traceability ?? {},
        ciStatus: ci_status,
        sonarStatus: sonar_status,
        planCommentUrl: plan_comment_url ?? null,
        summary: summary ?? null,
      }), null, 2));
    } catch (e) { return err(e); }
  },
);

server.tool(
  "gc_render_pr_body",
  "Render a PR body that satisfies the Ground Control policy gates (template sections, requirement UIDs, ADR impact, three Ground Control Checks, IMPLEMENTS/TESTS markers, no defer language). Returns the rendered body string for the caller to pass to `gh pr create --body`. change_class shapes a few cells: doc-only marks integration tests / changelog fragment N/A; source requires changelog fragment; source+migration adds the MigrationSmokeTest reminder.",
  {
    repo_path: z.string(),
    issue_number: z.number().int().positive(),
    change_class: z.enum(PR_BODY_CHANGE_CLASSES),
    // Use the ANCHORED EXACT_REQUIREMENT_UID_RE for structured UID fields
    // (codex cycle-4 F2). The unanchored PR_REQUIREMENT_RE is a body-search
    // predicate; here each array element must BE a UID, not contain one.
    requirement_uids: z.array(z.string().regex(EXACT_REQUIREMENT_UID_RE)),
    adr_refs: z.array(z.string().min(1)),
    summary: z.string().min(1),
    changes: z.array(z.string().min(1)),
    traceability: z.object({
      implements: z.array(z.string()),
      tests: z.array(z.string()),
    }),
    changelog_fragment: z.string().optional(),
    test_notes: z.string().optional(),
  },
  async ({ repo_path, issue_number, change_class, requirement_uids, adr_refs, summary, changes, traceability, changelog_fragment, test_notes }) => {
    try {
      return ok(JSON.stringify(await runRenderPrBody({
        repoPath: repo_path,
        issueNumber: issue_number,
        changeClass: change_class,
        requirementUids: requirement_uids,
        adrRefs: adr_refs,
        summary,
        changes,
        traceability,
        changelogFragment: changelog_fragment ?? null,
        testNotes: test_notes ?? null,
      }), null, 2));
    } catch (e) { return err(e); }
  },
);

server.tool(
  "gc_log_step_telemetry",
  "Append a single JSONL telemetry record for a /implement step to `.gc/telemetry/<issue>-<sanitized-branch>.jsonl`. Operational measurement only — NOT workflow state (per ADR-036). wall_time_ms is mandatory; input_tokens / output_tokens are optional. Path is repo-relative and validated for containment.",
  {
    repo_path: z.string(),
    issue_number: z.number().int().positive(),
    branch: z.string().min(1),
    step: z.string().min(1),
    tier: z.enum(TELEMETRY_TIERS),
    model: z.string().min(1),
    wall_time_ms: z.number().int().nonnegative(),
    input_tokens: z.number().int().nonnegative().nullable().optional(),
    output_tokens: z.number().int().nonnegative().nullable().optional(),
    outcome: z.enum(TELEMETRY_OUTCOMES),
    ts: z.string().optional(),
  },
  async ({ repo_path, issue_number, branch, step, tier, model, wall_time_ms, input_tokens, output_tokens, outcome, ts }) => {
    try {
      return ok(JSON.stringify(await runLogStepTelemetry({
        repoPath: repo_path,
        issueNumber: issue_number,
        branch,
        step,
        tier,
        model,
        wallTimeMs: wall_time_ms,
        inputTokens: input_tokens ?? null,
        outputTokens: output_tokens ?? null,
        outcome,
        ts: ts ?? null,
      }), null, 2));
    } catch (e) { return err(e); }
  },
);

server.tool(
  "gc_codex_verify_finding",
  "Ask Codex to verify whether a specific PR review finding has been resolved. RESOLVED → mark thread resolved; UNRESOLVED → post threaded reply. Per-finding cap of 2 verify calls.",
  {
    repo_path: z.string(),
    pr_number: z.number().int().positive(),
    comment_id: z.number().int().positive(),
    override_cap: z.boolean().optional(),
    override_reason: z.string().optional(),
  },
  async ({ repo_path, pr_number, comment_id, override_cap, override_reason }) => {
    try {
      return ok(JSON.stringify(await runCodexVerifyFinding({
        repoPath: repo_path, prNumber: pr_number, commentId: comment_id,
        overrideCap: Boolean(override_cap), overrideReason: override_reason ?? null,
      }), null, 2));
    } catch (e) { return err(e); }
  },
);

// ============================================================================
// CONSOLIDATED ENTITY TOOLS — action-discriminated CRUD per entity.
// Pure GETs (list, get-by-id, history, timeline, exports) live on gc_query
// against the allowlisted /api/v1/* prefixes.
// ============================================================================

const REQUIREMENT_ACTIONS = ["list", "create", "update", "delete", "archive", "clone"];

server.tool(
  "gc_requirement",
  `Requirement operations (action-discriminated). Actions: ${REQUIREMENT_ACTIONS.join(", ")}. ` +
    `Reads (list/get/history/diff/timeline) route through gc_query against /api/v1/requirements. ` +
    `Status transitions live on gc_transition_status / gc_bulk_transition_status (workflow primitives). ` +
    `Required fields per action: create→{uid,title,statement}; update→{id}; delete/archive→{id}; clone→{source_uid,new_uid}.`,
  {
    action: z.enum(REQUIREMENT_ACTIONS),
    // identifiers
    id: z.string().uuid().optional(),
    uid: z.string().optional(),
    source_uid: z.string().optional(),
    new_uid: z.string().optional(),
    // create/update fields
    project: z.string().optional(),
    title: z.string().optional(),
    statement: z.string().optional(),
    rationale: z.string().optional(),
    requirement_type: z.enum(REQUIREMENT_TYPES).optional(),
    priority: z.enum(PRIORITIES).optional(),
    wave: z.number().int().optional(),
    status: z.enum(STATUSES).optional(),
    // list filtering
    type: z.enum(REQUIREMENT_TYPES).optional(),
    search: z.string().optional(),
    page: z.number().int().optional(),
    size: z.number().int().optional(),
    sort: z.string().optional(),
    // clone
    copy_relations: z.boolean().optional(),
  },
  async (args) => {
    try {
      const ENTITY_FIELDS = ["uid", "title", "statement", "rationale", "requirement_type", "priority", "wave", "status"];
      switch (args.action) {
        case "list": {
          const filter = pick(args, ["status", "type", "priority", "wave", "search", "page", "size", "sort", "project"]);
          return ok(JSON.stringify(await listRequirements(filter), null, 2));
        }
        case "create": {
          reqArg(args, "uid", "create"); reqArg(args, "title", "create"); reqArg(args, "statement", "create");
          return ok(JSON.stringify(await createRequirement(pick(args, ENTITY_FIELDS), args.project), null, 2));
        }
        case "update": {
          reqArg(args, "id", "update");
          return ok(JSON.stringify(await updateRequirement(args.id, pick(args, ENTITY_FIELDS)), null, 2));
        }
        case "delete": {
          reqArg(args, "id", "delete");
          await archiveRequirement(args.id);
          return ok("Archived (the backend has no hard delete for requirements; transitioned to ARCHIVED)");
        }
        case "archive": {
          reqArg(args, "id", "archive");
          return ok(JSON.stringify(await archiveRequirement(args.id), null, 2));
        }
        case "clone": {
          // Note: lib.js cloneRequirement signature is (id, newUid, copyRelations).
          // The `id` is the SOURCE requirement's UUID. Look it up from source_uid
          // if the caller only knows the human-readable UID.
          reqArg(args, "new_uid", "clone");
          let sourceId = args.id;
          if (!sourceId) {
            reqArg(args, "source_uid", "clone");
            const src = await getRequirementByUid(args.source_uid, args.project);
            sourceId = src?.id;
            if (!sourceId) throw new Error(`clone: source requirement '${args.source_uid}' not found`);
          }
          return ok(JSON.stringify(await cloneRequirement(sourceId, args.new_uid, args.copy_relations ?? false), null, 2));
        }
        default: return err(new Error(`Unknown action: ${args.action}`));
      }
    } catch (e) { return err(e); }
  },
);

const RELATION_ACTIONS = ["create", "get", "delete"];

server.tool(
  "gc_relation",
  `Requirement-to-requirement relations. Actions: ${RELATION_ACTIONS.join(", ")}. ` +
    `Reads (history) route through gc_query.`,
  {
    action: z.enum(RELATION_ACTIONS),
    id: z.string().uuid().optional(),
    requirement_id: z.string().uuid().optional(),
    source_id: z.string().uuid().optional(),
    target_id: z.string().uuid().optional(),
    relation_type: z.enum(RELATION_TYPES).optional(),
  },
  async (args) => {
    try {
      switch (args.action) {
        case "create": {
          reqArg(args, "source_id", "create"); reqArg(args, "target_id", "create"); reqArg(args, "relation_type", "create");
          return ok(JSON.stringify(await createRelation(args.source_id, args.target_id, args.relation_type), null, 2));
        }
        case "get": {
          reqArg(args, "requirement_id", "get");
          return ok(JSON.stringify(await getRelations(args.requirement_id), null, 2));
        }
        case "delete": {
          // lib.js signature: deleteRelation(reqId, relId)
          reqArg(args, "requirement_id", "delete"); reqArg(args, "id", "delete");
          await deleteRelation(args.requirement_id, args.id);
          return ok("Deleted");
        }
        default: return err(new Error(`Unknown action: ${args.action}`));
      }
    } catch (e) { return err(e); }
  },
);

const ADR_ACTIONS = ["create", "update", "delete", "transition", "requirements"];

server.tool(
  "gc_adr",
  `ADR operations. Actions: ${ADR_ACTIONS.join(", ")}. ` +
    `Reads (list, get) route through gc_query.`,
  {
    action: z.enum(ADR_ACTIONS),
    id: z.string().uuid().optional(),
    uid: z.string().optional(),
    project: z.string().optional(),
    title: z.string().optional(),
    status: z.enum(ADR_STATUSES).optional(),
    decision_date: z.string().optional(),
    context: z.string().optional(),
    decision: z.string().optional(),
    consequences: z.string().optional(),
    superseded_by: z.string().uuid().nullable().optional(),
  },
  async (args) => {
    try {
      const ENTITY_FIELDS = ["uid", "title", "status", "decision_date", "context", "decision", "consequences", "superseded_by"];
      switch (args.action) {
        case "create": {
          reqArg(args, "uid", "create"); reqArg(args, "title", "create");
          return ok(JSON.stringify(await createAdr(pick(args, ENTITY_FIELDS), args.project), null, 2));
        }
        case "update": {
          reqArg(args, "id", "update");
          return ok(JSON.stringify(await updateAdr(args.id, pick(args, ENTITY_FIELDS)), null, 2));
        }
        case "delete": {
          reqArg(args, "id", "delete");
          await deleteAdr(args.id);
          return ok("Deleted");
        }
        case "transition": {
          // lib.js signature: transitionAdrStatus(id, status). superseded_by lands via update.
          reqArg(args, "id", "transition"); reqArg(args, "status", "transition");
          return ok(JSON.stringify(await transitionAdrStatus(args.id, args.status), null, 2));
        }
        case "requirements": {
          reqArg(args, "id", "requirements");
          return ok(JSON.stringify(await getAdrRequirements(args.id), null, 2));
        }
        default: return err(new Error(`Unknown action: ${args.action}`));
      }
    } catch (e) { return err(e); }
  },
);

const DOCUMENT_ACTIONS = ["create", "update", "delete", "grammar_set", "grammar_delete", "reading_order"];

server.tool(
  "gc_document",
  `Document operations + grammar + reading-order. Actions: ${DOCUMENT_ACTIONS.join(", ")}. ` +
    `Reads (list, get, grammar_get) route through gc_query.`,
  {
    action: z.enum(DOCUMENT_ACTIONS),
    id: z.string().uuid().optional(),
    project: z.string().optional(),
    title: z.string().optional(),
    description: z.string().optional(),
    grammar: z.record(z.any()).optional(),
  },
  async (args) => {
    try {
      const ENTITY_FIELDS = ["title", "description"];
      switch (args.action) {
        case "create": {
          reqArg(args, "title", "create");
          return ok(JSON.stringify(await createDocument(pick(args, ENTITY_FIELDS), args.project), null, 2));
        }
        case "update": {
          reqArg(args, "id", "update");
          return ok(JSON.stringify(await updateDocument(args.id, pick(args, ENTITY_FIELDS)), null, 2));
        }
        case "delete": {
          reqArg(args, "id", "delete");
          await deleteDocument(args.id);
          return ok("Deleted");
        }
        case "grammar_set": {
          reqArg(args, "id", "grammar_set"); reqArg(args, "grammar", "grammar_set");
          return ok(JSON.stringify(await setDocumentGrammar(args.id, args.grammar), null, 2));
        }
        case "grammar_delete": {
          reqArg(args, "id", "grammar_delete");
          await deleteDocumentGrammar(args.id);
          return ok("Grammar deleted");
        }
        case "reading_order": {
          reqArg(args, "id", "reading_order");
          return ok(JSON.stringify(await getDocumentReadingOrder(args.id), null, 2));
        }
        default: return err(new Error(`Unknown action: ${args.action}`));
      }
    } catch (e) { return err(e); }
  },
);

const SECTION_ACTIONS = ["create", "update", "delete", "tree", "content_add", "content_update", "content_delete"];

server.tool(
  "gc_section",
  `Section + section-content operations. Actions: ${SECTION_ACTIONS.join(", ")}. ` +
    `Reads (list, get, content_list) route through gc_query.`,
  {
    action: z.enum(SECTION_ACTIONS),
    id: z.string().uuid().optional(),
    document_id: z.string().uuid().optional(),
    parent_section_id: z.string().uuid().nullable().optional(),
    title: z.string().optional(),
    description: z.string().optional(),
    ordinal: z.number().int().optional(),
    content_id: z.string().uuid().optional(),
    content_type: z.string().optional(),
    requirement_id: z.string().uuid().optional(),
    text: z.string().optional(),
    project: z.string().optional(),
  },
  async (args) => {
    try {
      const SECTION_ENTITY_FIELDS = ["parent_section_id", "title", "description", "ordinal"];
      const CONTENT_ENTITY_FIELDS = ["content_type", "requirement_id", "text", "ordinal"];
      switch (args.action) {
        case "create": {
          reqArg(args, "document_id", "create"); reqArg(args, "title", "create");
          return ok(JSON.stringify(await createSection(args.document_id, pick(args, SECTION_ENTITY_FIELDS)), null, 2));
        }
        case "update": {
          reqArg(args, "id", "update");
          return ok(JSON.stringify(await updateSection(args.id, pick(args, SECTION_ENTITY_FIELDS)), null, 2));
        }
        case "delete": {
          reqArg(args, "id", "delete");
          await deleteSection(args.id);
          return ok("Deleted");
        }
        case "tree": {
          reqArg(args, "document_id", "tree");
          return ok(JSON.stringify(await getSectionTree(args.document_id), null, 2));
        }
        case "content_add": {
          reqArg(args, "id", "content_add"); reqArg(args, "content_type", "content_add");
          return ok(JSON.stringify(await addSectionContent(args.id, pick(args, CONTENT_ENTITY_FIELDS)), null, 2));
        }
        case "content_update": {
          reqArg(args, "content_id", "content_update");
          return ok(JSON.stringify(await updateSectionContent(args.content_id, pick(args, CONTENT_ENTITY_FIELDS)), null, 2));
        }
        case "content_delete": {
          reqArg(args, "content_id", "content_delete");
          await deleteSectionContent(args.content_id);
          return ok("Content deleted");
        }
        default: return err(new Error(`Unknown action: ${args.action}`));
      }
    } catch (e) { return err(e); }
  },
);

const ANALYZE_KINDS = [
  "cycles", "orphans", "coverage_gaps", "impact", "cross_wave",
  "consistency", "completeness", "status_drift", "similarity", "work_order",
];

server.tool(
  "gc_analyze",
  `Compute-heavy analysis operations. Kinds: ${ANALYZE_KINDS.join(", ")}. ` +
    `Required fields per kind: coverage_gaps→{link_type}; impact→{id}; status_drift→{minimum_confidence?}; similarity→{threshold?}. Others take {project?}.`,
  {
    kind: z.enum(ANALYZE_KINDS),
    project: z.string().optional(),
    id: z.string().uuid().optional(),
    link_type: z.enum(LINK_TYPES).optional(),
    minimum_confidence: z.enum(CONFIDENCE_LEVELS).optional(),
    threshold: z.number().optional(),
  },
  async (args) => {
    try {
      switch (args.kind) {
        case "cycles": return ok(JSON.stringify(await detectCycles(args.project), null, 2));
        case "orphans": return ok(JSON.stringify(await findOrphans(args.project), null, 2));
        case "coverage_gaps": {
          reqArg(args, "link_type", "coverage_gaps");
          return ok(JSON.stringify(await findCoverageGaps(args.link_type, args.project), null, 2));
        }
        case "impact": {
          reqArg(args, "id", "impact");
          return ok(JSON.stringify(await impactAnalysis(args.id), null, 2));
        }
        case "cross_wave": return ok(JSON.stringify(await crossWaveValidation(args.project), null, 2));
        case "consistency": return ok(JSON.stringify(await detectConsistencyViolations(args.project), null, 2));
        case "completeness": return ok(JSON.stringify(await analyzeCompleteness(args.project), null, 2));
        case "status_drift": return ok(JSON.stringify(await analyzeStatusDrift({ project: args.project, minimumConfidence: args.minimum_confidence }), null, 2));
        case "similarity": return ok(JSON.stringify(await analyzeSemanticSimilarity({ project: args.project, threshold: args.threshold }), null, 2));
        case "work_order": return ok(JSON.stringify(await getWorkOrder(args.project), null, 2));
        default: return err(new Error(`Unknown kind: ${args.kind}`));
      }
    } catch (e) { return err(e); }
  },
);

const GRAPH_MODES = ["ancestors", "descendants", "paths", "subgraph", "visualization", "traverse", "find_paths"];

server.tool(
  "gc_graph",
  `Graph traversal. Modes: ${GRAPH_MODES.join(", ")}. ` +
    `Required: ancestors/descendants→{uid}; paths/find_paths→{source,target}; subgraph/traverse→{roots}; visualization→{project?}. entity_types/max_depth are optional refinements.`,
  {
    mode: z.enum(GRAPH_MODES),
    project: z.string().optional(),
    uid: z.string().optional(),
    source: z.string().optional(),
    target: z.string().optional(),
    roots: z.array(z.string()).optional(),
    depth: z.number().int().optional(),
    entity_types: z.array(z.string()).optional(),
    max_depth: z.number().int().optional(),
  },
  async (args) => {
    try {
      switch (args.mode) {
        case "ancestors": {
          reqArg(args, "uid", "ancestors");
          return ok(JSON.stringify(await getAncestors(args.uid, args.depth, args.project), null, 2));
        }
        case "descendants": {
          reqArg(args, "uid", "descendants");
          return ok(JSON.stringify(await getDescendants(args.uid, args.depth, args.project), null, 2));
        }
        case "paths": {
          reqArg(args, "source", "paths"); reqArg(args, "target", "paths");
          return ok(JSON.stringify(await findPaths(args.source, args.target, args.project), null, 2));
        }
        case "find_paths": {
          // lib.js: findGraphPaths(sourceNodeId, targetNodeId, project, entityTypes, maxDepth)
          reqArg(args, "source", "find_paths"); reqArg(args, "target", "find_paths");
          return ok(JSON.stringify(await findGraphPaths(args.source, args.target, args.project, args.entity_types, args.max_depth), null, 2));
        }
        case "subgraph": {
          // lib.js: extractSubgraph(rootNodeIds, project, entityTypes, maxDepth)
          reqArg(args, "roots", "subgraph");
          return ok(JSON.stringify(await extractSubgraph(args.roots, args.project, args.entity_types, args.max_depth), null, 2));
        }
        case "traverse": {
          // lib.js: traverseGraph(rootNodeIds, project, entityTypes, maxDepth)
          reqArg(args, "roots", "traverse");
          return ok(JSON.stringify(await traverseGraph(args.roots, args.project, args.entity_types, args.max_depth), null, 2));
        }
        case "visualization": {
          return ok(JSON.stringify(await getGraphVisualization(args.project, args.entity_types), null, 2));
        }
        default: return err(new Error(`Unknown mode: ${args.mode}`));
      }
    } catch (e) { return err(e); }
  },
);

const BASELINE_ACTIONS = ["create", "delete", "snapshot", "compare"];

server.tool(
  "gc_baseline",
  `Baseline operations. Actions: ${BASELINE_ACTIONS.join(", ")}. ` +
    `Reads (list, get) route through gc_query. Required: create→{name}; delete/snapshot→{id}; compare→{baseline_a, baseline_b}.`,
  {
    action: z.enum(BASELINE_ACTIONS),
    id: z.string().uuid().optional(),
    project: z.string().optional(),
    name: z.string().optional(),
    description: z.string().optional(),
    baseline_a: z.string().uuid().optional(),
    baseline_b: z.string().uuid().optional(),
  },
  async (args) => {
    try {
      const ENTITY_FIELDS = ["name", "description"];
      switch (args.action) {
        case "create": {
          reqArg(args, "name", "create");
          return ok(JSON.stringify(await createBaseline(pick(args, ENTITY_FIELDS), args.project), null, 2));
        }
        case "delete": {
          reqArg(args, "id", "delete");
          await deleteBaseline(args.id);
          return ok("Deleted");
        }
        case "snapshot": {
          reqArg(args, "id", "snapshot");
          return ok(JSON.stringify(await getBaselineSnapshot(args.id), null, 2));
        }
        case "compare": {
          reqArg(args, "baseline_a", "compare"); reqArg(args, "baseline_b", "compare");
          return ok(JSON.stringify(await compareBaselines(args.baseline_a, args.baseline_b), null, 2));
        }
        default: return err(new Error(`Unknown action: ${args.action}`));
      }
    } catch (e) { return err(e); }
  },
);

const QUALITY_GATE_ACTIONS = ["create", "update", "delete", "evaluate"];

server.tool(
  "gc_quality_gate",
  `Quality gate operations. Actions: ${QUALITY_GATE_ACTIONS.join(", ")}. ` +
    `Reads (list, get) route through gc_query.`,
  {
    action: z.enum(QUALITY_GATE_ACTIONS),
    id: z.string().uuid().optional(),
    project: z.string().optional(),
    name: z.string().optional(),
    description: z.string().optional(),
    metric_type: z.enum(METRIC_TYPES).optional(),
    comparison_operator: z.enum(COMPARISON_OPERATORS).optional(),
    threshold: z.number().optional(),
    enabled: z.boolean().optional(),
  },
  async (args) => {
    try {
      const ENTITY_FIELDS = ["name", "description", "metric_type", "comparison_operator", "threshold", "enabled"];
      switch (args.action) {
        case "create": {
          reqArg(args, "name", "create"); reqArg(args, "metric_type", "create");
          return ok(JSON.stringify(await createQualityGate(pick(args, ENTITY_FIELDS), args.project), null, 2));
        }
        case "update": {
          reqArg(args, "id", "update");
          return ok(JSON.stringify(await updateQualityGate(args.id, pick(args, ENTITY_FIELDS)), null, 2));
        }
        case "delete": {
          reqArg(args, "id", "delete");
          await deleteQualityGate(args.id);
          return ok("Deleted");
        }
        case "evaluate":
          return ok(JSON.stringify(await evaluateQualityGates(args.project), null, 2));
        default: return err(new Error(`Unknown action: ${args.action}`));
      }
    } catch (e) { return err(e); }
  },
);

const ASSET_ACTIONS = [
  "create", "update", "delete", "archive",
  "relation_create", "relation_delete", "detect_cycles", "impact_analysis", "extract_subgraph",
  "link_create", "link_delete",
  "external_id_create", "external_id_update", "external_id_delete",
];

server.tool(
  "gc_asset",
  `Operational asset operations incl. relations, links, external IDs. Actions: ${ASSET_ACTIONS.join(", ")}. ` +
    `Reads (list, get, get_by_uid, find_by_external_id, links, external_ids) route through gc_query.`,
  {
    action: z.enum(ASSET_ACTIONS),
    id: z.string().uuid().optional(),
    uid: z.string().optional(),
    project: z.string().optional(),
    name: z.string().optional(),
    description: z.string().optional(),
    asset_type: z.enum(ASSET_TYPES).optional(),
    parent_id: z.string().uuid().nullable().optional(),
    // relations
    source_id: z.string().uuid().optional(),
    target_id: z.string().uuid().optional(),
    relation_type: z.enum(ASSET_RELATION_TYPES).optional(),
    relation_id: z.string().uuid().optional(),
    // links
    asset_id: z.string().uuid().optional(),
    target_type: z.enum(ASSET_LINK_TARGET_TYPES).optional(),
    target_identifier: z.string().optional(),
    link_type: z.enum(ASSET_LINK_TYPES).optional(),
    link_id: z.string().uuid().optional(),
    // external IDs
    namespace: z.string().optional(),
    external_id: z.string().optional(),
    external_id_record_id: z.string().uuid().optional(),
    roots: z.array(z.string()).optional(),
    max_depth: z.number().int().optional(),
  },
  async (args) => {
    try {
      const ASSET_FIELDS = ["name", "description", "asset_type", "parent_id"];
      const RELATION_FIELDS = ["source_id", "target_id", "relation_type"];
      const LINK_FIELDS = ["target_type", "target_identifier", "link_type"];
      const EXT_ID_FIELDS = ["namespace", "external_id"];
      switch (args.action) {
        case "create": {
          reqArg(args, "name", "create"); reqArg(args, "asset_type", "create");
          return ok(JSON.stringify(await createAsset(pick(args, ASSET_FIELDS), args.project), null, 2));
        }
        case "update": {
          reqArg(args, "id", "update");
          return ok(JSON.stringify(await updateAsset(args.id, pick(args, ASSET_FIELDS), args.project), null, 2));
        }
        case "delete": {
          reqArg(args, "id", "delete");
          await deleteAsset(args.id, args.project);
          return ok("Deleted");
        }
        case "archive": {
          reqArg(args, "id", "archive");
          return ok(JSON.stringify(await archiveAsset(args.id, args.project), null, 2));
        }
        case "relation_create": {
          // lib.js: createAssetRelation(assetId, data, project)
          reqArg(args, "source_id", "relation_create"); reqArg(args, "target_id", "relation_create"); reqArg(args, "relation_type", "relation_create");
          return ok(JSON.stringify(await createAssetRelation(args.source_id, pick(args, RELATION_FIELDS), args.project), null, 2));
        }
        case "relation_delete": {
          // lib.js: deleteAssetRelation(assetId, relationId, project)
          reqArg(args, "asset_id", "relation_delete"); reqArg(args, "relation_id", "relation_delete");
          await deleteAssetRelation(args.asset_id, args.relation_id, args.project);
          return ok("Deleted");
        }
        case "detect_cycles": return ok(JSON.stringify(await detectAssetCycles(args.project), null, 2));
        case "impact_analysis": {
          reqArg(args, "id", "impact_analysis");
          return ok(JSON.stringify(await assetImpactAnalysis(args.id, args.project), null, 2));
        }
        case "extract_subgraph": {
          reqArg(args, "roots", "extract_subgraph");
          return ok(JSON.stringify(await extractAssetSubgraph({ roots: args.roots, maxDepth: args.max_depth }, args.project), null, 2));
        }
        case "link_create": {
          // lib.js: createAssetLink(assetId, data, project)
          reqArg(args, "asset_id", "link_create"); reqArg(args, "target_type", "link_create"); reqArg(args, "target_identifier", "link_create"); reqArg(args, "link_type", "link_create");
          return ok(JSON.stringify(await createAssetLink(args.asset_id, pick(args, LINK_FIELDS), args.project), null, 2));
        }
        case "link_delete": {
          // lib.js: deleteAssetLink(assetId, linkId, project)
          reqArg(args, "asset_id", "link_delete"); reqArg(args, "link_id", "link_delete");
          await deleteAssetLink(args.asset_id, args.link_id, args.project);
          return ok("Deleted");
        }
        case "external_id_create": {
          // lib.js: createAssetExternalId(assetId, data, project)
          reqArg(args, "asset_id", "external_id_create"); reqArg(args, "namespace", "external_id_create"); reqArg(args, "external_id", "external_id_create");
          return ok(JSON.stringify(await createAssetExternalId(args.asset_id, pick(args, EXT_ID_FIELDS), args.project), null, 2));
        }
        case "external_id_update": {
          // lib.js: updateAssetExternalId(assetId, extIdId, data, project)
          reqArg(args, "asset_id", "external_id_update"); reqArg(args, "external_id_record_id", "external_id_update");
          return ok(JSON.stringify(await updateAssetExternalId(args.asset_id, args.external_id_record_id, pick(args, EXT_ID_FIELDS), args.project), null, 2));
        }
        case "external_id_delete": {
          // lib.js: deleteAssetExternalId(assetId, extIdId, project)
          reqArg(args, "asset_id", "external_id_delete"); reqArg(args, "external_id_record_id", "external_id_delete");
          await deleteAssetExternalId(args.asset_id, args.external_id_record_id, args.project);
          return ok("Deleted");
        }
        default: return err(new Error(`Unknown action: ${args.action}`));
      }
    } catch (e) { return err(e); }
  },
);

const OBSERVATION_ACTIONS = ["create", "update", "delete", "latest"];

server.tool(
  "gc_observation",
  `Time-bounded state observations. Actions: ${OBSERVATION_ACTIONS.join(", ")}. ` +
    `Reads (list, get) route through gc_query.`,
  {
    action: z.enum(OBSERVATION_ACTIONS),
    id: z.string().uuid().optional(),
    project: z.string().optional(),
    asset_id: z.string().uuid().optional(),
    category: z.enum(OBSERVATION_CATEGORIES).optional(),
    title: z.string().optional(),
    statement: z.string().optional(),
    observed_at: z.string().optional(),
    valid_until: z.string().optional(),
    metadata: z.record(z.any()).optional(),
  },
  async (args) => {
    try {
      const ENTITY_FIELDS = ["category", "title", "statement", "observed_at", "valid_until", "metadata"];
      switch (args.action) {
        case "create": {
          // lib.js: createObservation(assetId, data, project)
          reqArg(args, "asset_id", "create"); reqArg(args, "category", "create"); reqArg(args, "title", "create");
          return ok(JSON.stringify(await createObservation(args.asset_id, pick(args, ENTITY_FIELDS), args.project), null, 2));
        }
        case "update": {
          // lib.js: updateObservation(assetId, observationId, data, project)
          reqArg(args, "asset_id", "update"); reqArg(args, "id", "update");
          return ok(JSON.stringify(await updateObservation(args.asset_id, args.id, pick(args, ENTITY_FIELDS), args.project), null, 2));
        }
        case "delete": {
          // lib.js: deleteObservation(assetId, observationId, project)
          reqArg(args, "asset_id", "delete"); reqArg(args, "id", "delete");
          await deleteObservation(args.asset_id, args.id, args.project);
          return ok("Deleted");
        }
        case "latest": {
          // lib.js: listLatestObservations(assetId, project) — requires assetId
          reqArg(args, "asset_id", "latest");
          return ok(JSON.stringify(await listLatestObservations(args.asset_id, args.project), null, 2));
        }
        default: return err(new Error(`Unknown action: ${args.action}`));
      }
    } catch (e) { return err(e); }
  },
);

const RISK_SCENARIO_ACTIONS = ["create", "update", "delete", "transition", "requirements", "link_create", "link_delete"];

server.tool(
  "gc_risk_scenario",
  `Risk scenarios + their links. Actions: ${RISK_SCENARIO_ACTIONS.join(", ")}. ` +
    `Reads (list, get, links_list) route through gc_query.`,
  {
    action: z.enum(RISK_SCENARIO_ACTIONS),
    id: z.string().uuid().optional(),
    uid: z.string().optional(),
    project: z.string().optional(),
    title: z.string().optional(),
    description: z.string().optional(),
    status: z.enum(RISK_SCENARIO_STATUSES).optional(),
    methodology_profile_id: z.string().uuid().nullable().optional(),
    metadata: z.record(z.any()).optional(),
    // links
    scenario_id: z.string().uuid().optional(),
    target_type: z.enum(RISK_SCENARIO_LINK_TARGET_TYPES).optional(),
    target_identifier: z.string().optional(),
    link_type: z.enum(RISK_SCENARIO_LINK_TYPES).optional(),
    link_id: z.string().uuid().optional(),
  },
  async (args) => {
    try {
      const ENTITY_FIELDS = ["uid", "title", "description", "status", "methodology_profile_id", "metadata"];
      const LINK_FIELDS = ["target_type", "target_identifier", "link_type"];
      switch (args.action) {
        case "create": {
          reqArg(args, "title", "create");
          return ok(JSON.stringify(await createRiskScenario(pick(args, ENTITY_FIELDS), args.project), null, 2));
        }
        case "update": {
          reqArg(args, "id", "update");
          return ok(JSON.stringify(await updateRiskScenario(args.id, pick(args, ENTITY_FIELDS), args.project), null, 2));
        }
        case "delete": {
          reqArg(args, "id", "delete");
          await deleteRiskScenario(args.id, args.project);
          return ok("Deleted");
        }
        case "transition": {
          reqArg(args, "id", "transition"); reqArg(args, "status", "transition");
          return ok(JSON.stringify(await transitionRiskScenarioStatus(args.id, args.status, args.project), null, 2));
        }
        case "requirements": {
          reqArg(args, "id", "requirements");
          return ok(JSON.stringify(await getRiskScenarioRequirements(args.id, args.project), null, 2));
        }
        case "link_create": {
          // lib.js: createRiskScenarioLink takes (scenarioId, data, project) per consistency with asset patterns; verify by spot-check
          reqArg(args, "scenario_id", "link_create"); reqArg(args, "target_type", "link_create"); reqArg(args, "target_identifier", "link_create"); reqArg(args, "link_type", "link_create");
          return ok(JSON.stringify(await createRiskScenarioLink(args.scenario_id, pick(args, LINK_FIELDS), args.project), null, 2));
        }
        case "link_delete": {
          reqArg(args, "scenario_id", "link_delete"); reqArg(args, "link_id", "link_delete");
          await deleteRiskScenarioLink(args.scenario_id, args.link_id, args.project);
          return ok("Deleted");
        }
        default: return err(new Error(`Unknown action: ${args.action}`));
      }
    } catch (e) { return err(e); }
  },
);

const THREAT_MODEL_ACTIONS = ["create", "update", "delete", "transition", "link_create", "link_delete"];

server.tool(
  "gc_threat_model",
  `Threat models + their links. Actions: ${THREAT_MODEL_ACTIONS.join(", ")}. ` +
    `Reads (list, get, links_list) route through gc_query.`,
  {
    action: z.enum(THREAT_MODEL_ACTIONS),
    id: z.string().uuid().optional(),
    uid: z.string().optional(),
    project: z.string().optional(),
    title: z.string().optional(),
    description: z.string().optional(),
    status: z.enum(THREAT_MODEL_STATUSES).optional(),
    stride_category: z.enum(STRIDE_CATEGORIES).optional(),
    metadata: z.record(z.any()).optional(),
    // links
    threat_model_id: z.string().uuid().optional(),
    target_type: z.enum(THREAT_MODEL_LINK_TARGET_TYPES).optional(),
    target_identifier: z.string().optional(),
    link_type: z.enum(THREAT_MODEL_LINK_TYPES).optional(),
    link_id: z.string().uuid().optional(),
  },
  async (args) => {
    try {
      const ENTITY_FIELDS = ["uid", "title", "description", "status", "stride_category", "metadata"];
      const LINK_FIELDS = ["target_type", "target_identifier", "link_type"];
      switch (args.action) {
        case "create": {
          reqArg(args, "title", "create");
          return ok(JSON.stringify(await createThreatModel(pick(args, ENTITY_FIELDS), args.project), null, 2));
        }
        case "update": {
          reqArg(args, "id", "update");
          return ok(JSON.stringify(await updateThreatModel(args.id, pick(args, ENTITY_FIELDS), args.project), null, 2));
        }
        case "delete": {
          reqArg(args, "id", "delete");
          await deleteThreatModel(args.id, args.project);
          return ok("Deleted");
        }
        case "transition": {
          reqArg(args, "id", "transition"); reqArg(args, "status", "transition");
          return ok(JSON.stringify(await transitionThreatModelStatus(args.id, args.status, args.project), null, 2));
        }
        case "link_create": {
          reqArg(args, "threat_model_id", "link_create"); reqArg(args, "target_type", "link_create"); reqArg(args, "target_identifier", "link_create"); reqArg(args, "link_type", "link_create");
          return ok(JSON.stringify(await createThreatModelLink(args.threat_model_id, pick(args, LINK_FIELDS), args.project), null, 2));
        }
        case "link_delete": {
          reqArg(args, "threat_model_id", "link_delete"); reqArg(args, "link_id", "link_delete");
          await deleteThreatModelLink(args.threat_model_id, args.link_id, args.project);
          return ok("Deleted");
        }
        default: return err(new Error(`Unknown action: ${args.action}`));
      }
    } catch (e) { return err(e); }
  },
);

const CONTROL_ACTIONS = ["create", "update", "delete", "transition", "link_create", "link_delete"];

server.tool(
  "gc_control",
  `Controls + their links. Actions: ${CONTROL_ACTIONS.join(", ")}. ` +
    `Reads (list, get, links_list) route through gc_query.`,
  {
    action: z.enum(CONTROL_ACTIONS),
    id: z.string().uuid().optional(),
    uid: z.string().optional(),
    project: z.string().optional(),
    title: z.string().optional(),
    description: z.string().optional(),
    status: z.enum(CONTROL_STATUSES).optional(),
    control_function: z.enum(CONTROL_FUNCTIONS).optional(),
    metadata: z.record(z.any()).optional(),
    // links
    control_id: z.string().uuid().optional(),
    target_type: z.enum(CONTROL_LINK_TARGET_TYPES).optional(),
    target_identifier: z.string().optional(),
    link_type: z.enum(CONTROL_LINK_TYPES).optional(),
    link_id: z.string().uuid().optional(),
  },
  async (args) => {
    try {
      const ENTITY_FIELDS = ["uid", "title", "description", "status", "control_function", "metadata"];
      const LINK_FIELDS = ["target_type", "target_identifier", "link_type"];
      switch (args.action) {
        case "create": {
          reqArg(args, "title", "create");
          return ok(JSON.stringify(await createControl(pick(args, ENTITY_FIELDS), args.project), null, 2));
        }
        case "update": {
          reqArg(args, "id", "update");
          return ok(JSON.stringify(await updateControl(args.id, pick(args, ENTITY_FIELDS), args.project), null, 2));
        }
        case "delete": {
          reqArg(args, "id", "delete");
          await deleteControl(args.id, args.project);
          return ok("Deleted");
        }
        case "transition": {
          reqArg(args, "id", "transition"); reqArg(args, "status", "transition");
          return ok(JSON.stringify(await transitionControlStatus(args.id, args.status, args.project), null, 2));
        }
        case "link_create": {
          reqArg(args, "control_id", "link_create"); reqArg(args, "target_type", "link_create"); reqArg(args, "target_identifier", "link_create"); reqArg(args, "link_type", "link_create");
          return ok(JSON.stringify(await createControlLink(args.control_id, pick(args, LINK_FIELDS), args.project), null, 2));
        }
        case "link_delete": {
          reqArg(args, "control_id", "link_delete"); reqArg(args, "link_id", "link_delete");
          await deleteControlLink(args.control_id, args.link_id, args.project);
          return ok("Deleted");
        }
        default: return err(new Error(`Unknown action: ${args.action}`));
      }
    } catch (e) { return err(e); }
  },
);

const RISK_GOVERNANCE_ENTITIES = [
  "methodology_profile", "risk_register_record", "risk_assessment_result",
  "treatment_plan", "verification_result",
];
const RISK_GOVERNANCE_ACTIONS = ["create", "update", "delete", "transition", "transition_approval"];

// Per-entity field allowlists for gc_risk_governance create/update DTOs.
const GOVERNANCE_FIELDS = {
  methodology_profile: ["name", "description", "family", "status", "metadata"],
  risk_register_record: ["uid", "title", "description", "scenario_id", "owner", "status", "review_cadence", "next_review_at", "metadata"],
  risk_assessment_result: ["uid", "title", "description", "scenario_id", "approval_state", "quantitative_value", "qualitative_value", "metadata"],
  treatment_plan: ["uid", "title", "description", "scenario_id", "risk_register_record_id", "status", "strategy", "owner", "due_at", "metadata"],
  verification_result: ["uid", "title", "description", "outcome", "status", "assurance_level", "verified_at", "metadata"],
};

server.tool(
  "gc_risk_governance",
  `Methodology profiles, risk register records, risk assessments, treatment plans, verification results. ` +
    `Entity: ${RISK_GOVERNANCE_ENTITIES.join(", ")}. Actions: ${RISK_GOVERNANCE_ACTIONS.join(", ")}. ` +
    `Reads (list, get) route through gc_query. Entity-specific fields are validated against the per-entity allowlist; unknown fields are dropped (NOT forwarded to the backend).`,
  {
    entity: z.enum(RISK_GOVERNANCE_ENTITIES),
    action: z.enum(RISK_GOVERNANCE_ACTIONS),
    id: z.string().uuid().optional(),
    project: z.string().optional(),
    status: z.string().optional(),
    approval_state: z.enum(RISK_ASSESSMENT_APPROVAL_STATUSES).optional(),
    // Shared entity fields. Per-entity allowlist (GOVERNANCE_FIELDS) gates which
    // ones reach the backend on create/update, so unrelated MCP control fields
    // (action, entity, id, project) don't leak into the DTO.
    uid: z.string().optional(),
    name: z.string().optional(),
    title: z.string().optional(),
    description: z.string().optional(),
    family: z.enum(METHODOLOGY_FAMILIES).optional(),
    scenario_id: z.string().uuid().optional(),
    risk_register_record_id: z.string().uuid().optional(),
    owner: z.string().optional(),
    review_cadence: z.string().optional(),
    next_review_at: z.string().optional(),
    quantitative_value: z.number().optional(),
    qualitative_value: z.string().optional(),
    strategy: z.enum(TREATMENT_STRATEGIES).optional(),
    due_at: z.string().optional(),
    outcome: z.string().optional(),
    assurance_level: z.enum(ASSURANCE_LEVELS).optional(),
    verified_at: z.string().optional(),
    metadata: z.record(z.any()).optional(),
  },
  async (args) => {
    try {
      const data = pick(args, GOVERNANCE_FIELDS[args.entity] ?? []);
      switch (args.entity) {
        case "methodology_profile": {
          switch (args.action) {
            case "create": return ok(JSON.stringify(await createMethodologyProfile(data, args.project), null, 2));
            case "update": reqArg(args, "id", "update"); return ok(JSON.stringify(await updateMethodologyProfile(args.id, data, args.project), null, 2));
            case "delete": reqArg(args, "id", "delete"); await deleteMethodologyProfile(args.id, args.project); return ok("Deleted");
            default: return err(new Error(`Action '${args.action}' not valid for methodology_profile`));
          }
        }
        case "risk_register_record": {
          switch (args.action) {
            case "create": return ok(JSON.stringify(await createRiskRegisterRecord(data, args.project), null, 2));
            case "update": reqArg(args, "id", "update"); return ok(JSON.stringify(await updateRiskRegisterRecord(args.id, data, args.project), null, 2));
            case "delete": reqArg(args, "id", "delete"); await deleteRiskRegisterRecord(args.id, args.project); return ok("Deleted");
            case "transition": reqArg(args, "id", "transition"); reqArg(args, "status", "transition"); return ok(JSON.stringify(await transitionRiskRegisterRecordStatus(args.id, args.status, args.project), null, 2));
            default: return err(new Error(`Action '${args.action}' not valid for risk_register_record`));
          }
        }
        case "risk_assessment_result": {
          switch (args.action) {
            case "create": return ok(JSON.stringify(await createRiskAssessmentResult(data, args.project), null, 2));
            case "update": reqArg(args, "id", "update"); return ok(JSON.stringify(await updateRiskAssessmentResult(args.id, data, args.project), null, 2));
            case "delete": reqArg(args, "id", "delete"); await deleteRiskAssessmentResult(args.id, args.project); return ok("Deleted");
            case "transition_approval": reqArg(args, "id", "transition_approval"); reqArg(args, "approval_state", "transition_approval"); return ok(JSON.stringify(await transitionRiskAssessmentApprovalState(args.id, args.approval_state, args.project), null, 2));
            default: return err(new Error(`Action '${args.action}' not valid for risk_assessment_result`));
          }
        }
        case "treatment_plan": {
          switch (args.action) {
            case "create": return ok(JSON.stringify(await createTreatmentPlan(data, args.project), null, 2));
            case "update": reqArg(args, "id", "update"); return ok(JSON.stringify(await updateTreatmentPlan(args.id, data, args.project), null, 2));
            case "delete": reqArg(args, "id", "delete"); await deleteTreatmentPlan(args.id, args.project); return ok("Deleted");
            case "transition": reqArg(args, "id", "transition"); reqArg(args, "status", "transition"); return ok(JSON.stringify(await transitionTreatmentPlanStatus(args.id, args.status, args.project), null, 2));
            default: return err(new Error(`Action '${args.action}' not valid for treatment_plan`));
          }
        }
        case "verification_result": {
          switch (args.action) {
            case "create": return ok(JSON.stringify(await createVerificationResult(data, args.project), null, 2));
            case "update": reqArg(args, "id", "update"); return ok(JSON.stringify(await updateVerificationResult(args.id, data, args.project), null, 2));
            case "delete": reqArg(args, "id", "delete"); await deleteVerificationResult(args.id, args.project); return ok("Deleted");
            default: return err(new Error(`Action '${args.action}' not valid for verification_result`));
          }
        }
        default: return err(new Error(`Unknown entity: ${args.entity}`));
      }
    } catch (e) { return err(e); }
  },
);

const ADMIN_ACTIONS = [
  "import_strictdoc", "import_reqif", "sync_github", "sync_github_prs",
  "embed_requirement", "embed_project", "embedding_status",
  "materialize_graph", "create_project", "list_projects",
  "run_sweep", "run_sweep_all",
  "export_audit_timeline", "export_requirements", "export_sweep_report", "export_document",
];

if (ADMIN_TOOLS_ENABLED) {
  server.tool(
    "gc_admin",
    `Admin operations: imports, GitHub sync, embeddings, materialization, project create, sweep, exports. ` +
      `Registered only when GC_MCP_ADMIN=1 (these operations require ROLE_ADMIN at the backend per ADR-026). ` +
      `Actions: ${ADMIN_ACTIONS.join(", ")}.`,
    {
      action: z.enum(ADMIN_ACTIONS),
      project: z.string().optional(),
      file_path: z.string().optional(),
      owner: z.string().optional(),
      repo: z.string().optional(),
      requirement_id: z.string().uuid().optional(),
      force: z.boolean().optional(),
      identifier: z.string().optional(),
      name: z.string().optional(),
      description: z.string().optional(),
      document_id: z.string().uuid().optional(),
      format: z.string().optional(),
      from: z.string().optional(),
      to: z.string().optional(),
    },
    async (args) => {
      try {
        switch (args.action) {
          case "import_strictdoc": reqArg(args, "file_path", "import_strictdoc"); return ok(JSON.stringify(await importStrictdoc(args.file_path, args.project), null, 2));
          case "import_reqif": reqArg(args, "file_path", "import_reqif"); return ok(JSON.stringify(await importReqif(args.file_path, args.project), null, 2));
          case "sync_github": reqArg(args, "owner", "sync_github"); reqArg(args, "repo", "sync_github"); return ok(JSON.stringify(await syncGithub(args.owner, args.repo), null, 2));
          case "sync_github_prs": reqArg(args, "owner", "sync_github_prs"); reqArg(args, "repo", "sync_github_prs"); return ok(JSON.stringify(await syncGithubPrs(args.owner, args.repo), null, 2));
          case "embed_requirement": reqArg(args, "requirement_id", "embed_requirement"); return ok(JSON.stringify(await embedRequirement(args.requirement_id), null, 2));
          case "embed_project": return ok(JSON.stringify(await embedProject(args.project, args.force), null, 2));
          case "embedding_status": reqArg(args, "requirement_id", "embedding_status"); return ok(JSON.stringify(await getEmbeddingStatus(args.requirement_id), null, 2));
          case "materialize_graph": return ok(JSON.stringify(await materializeGraph(), null, 2));
          case "list_projects": return ok(JSON.stringify(await listProjects(), null, 2));
          case "create_project": {
            reqArg(args, "identifier", "create_project"); reqArg(args, "name", "create_project");
            return ok(JSON.stringify(await createProject({ identifier: args.identifier, name: args.name, description: args.description }), null, 2));
          }
          case "run_sweep": return ok(JSON.stringify(await runSweep(args.project), null, 2));
          case "run_sweep_all": return ok(JSON.stringify(await runSweepAll(), null, 2));
          case "export_audit_timeline": return ok(JSON.stringify(await exportAuditTimeline(pick(args, ["project", "from", "to", "format"])), null, 2));
          case "export_requirements": return ok(JSON.stringify(await exportRequirements(args.project, args.format), null, 2));
          case "export_sweep_report": return ok(JSON.stringify(await exportSweepReport(args.project, args.format), null, 2));
          case "export_document": reqArg(args, "document_id", "export_document"); return ok(JSON.stringify(await exportDocument(args.document_id, args.format), null, 2));
          default: return err(new Error(`Unknown action: ${args.action}`));
        }
      } catch (e) { return err(e); }
    },
  );

  // ADR-037 admin-user lifecycle. Registered alongside gc_admin so an
  // ADMIN-role bearer token can drive user management programmatically.
  // Humans manage users via the curl/session flow documented in
  // DEPLOYMENT.md — this PR does not ship a SPA user-management page.
  //
  // **`create_user` is intentionally NOT exposed via MCP.** Passing a new
  // account password as a JSON-RPC tool argument means the password lands in
  // agent transcripts, client logs, debug output, and any observability trace
  // that captures tool-call payloads. Create users via the DEPLOYMENT.md
  // curl flow where the password stays in a mode-600 file. The actions
  // surfaced here mutate state but never accept password material;
  // createAdminUser is exported from lib.js for callers that have an
  // out-of-band secret channel, not for agents.
  const USER_ADMIN_ACTIONS = [
    "list_users", "update_role", "update_enabled", "delete_user",
  ];
  server.tool(
    "gc_user_admin",
    `Admin user lifecycle (ADR-037): list / change-role / enable-disable / delete. ` +
      `Registered only when GC_MCP_ADMIN=1; backend enforces ROLE_ADMIN. ` +
      `User CREATION is intentionally not exposed here — see DEPLOYMENT.md. ` +
      `Actions: ${USER_ADMIN_ACTIONS.join(", ")}.`,
    {
      action: z.enum(USER_ADMIN_ACTIONS),
      username: z.string().optional(),
      role: z.enum(["USER", "ADMIN"]).optional(),
      enabled: z.boolean().optional(),
    },
    async (args) => {
      try {
        switch (args.action) {
          case "list_users":
            return ok(JSON.stringify(await listAdminUsers(), null, 2));
          case "update_role":
            reqArg(args, "username", "update_role");
            reqArg(args, "role", "update_role");
            return ok(JSON.stringify(await updateAdminUserRole(args.username, args.role), null, 2));
          case "update_enabled":
            reqArg(args, "username", "update_enabled");
            if (typeof args.enabled !== "boolean") {
              return err(new Error("update_enabled requires boolean 'enabled'"));
            }
            return ok(JSON.stringify(await updateAdminUserEnabled(args.username, args.enabled), null, 2));
          case "delete_user":
            reqArg(args, "username", "delete_user");
            await deleteAdminUser(args.username);
            return ok(`Deleted user '${args.username}'`);
          default:
            return err(new Error(`Unknown action: ${args.action}`));
        }
      } catch (e) {
        return err(e);
      }
    },
  );
}

const PACK_SUBSYSTEMS = ["plugin", "control_pack", "registry", "trust_policy", "install"];
const PACK_ACTIONS = [
  // plugin
  "register", "unregister", "list_plugins", "get_plugin",
  // control_pack
  "deprecate", "remove", "list_control_packs", "get_control_pack",
  "list_control_pack_entries", "get_control_pack_entry",
  "override_create", "override_delete", "list_control_pack_overrides",
  // registry
  "registry_register", "import", "registry_update", "withdraw",
  "registry_delete", "resolve", "check_compatibility",
  "list_pack_registry_entries", "list_pack_versions", "get_pack_registry_entry",
  // trust_policy
  "create_trust_policy", "update_trust_policy", "delete_trust_policy",
  "list_trust_policies", "get_trust_policy",
  // install
  "install", "upgrade", "list_pack_install_records", "get_pack_install_record",
];

// Per-subsystem field allowlists for create/update DTOs.
const PACK_FIELDS = {
  plugin: ["name", "plugin_type", "version", "endpoint_url", "config", "metadata"],
  control_pack_override: ["status", "rationale", "metadata"],
  registry: ["pack_id", "pack_type", "version", "description", "metadata", "signature", "source_url"],
  trust_policy: ["name", "field", "operator", "value", "outcome", "priority", "metadata"],
  install: ["pack_id", "version", "scope", "config", "metadata"],
};

if (ADMIN_TOOLS_ENABLED) {
  server.tool(
    "gc_pack",
    `Pack ecosystem: plugins, control packs, pack registry, trust policies, install records. ` +
      `Registered only when GC_MCP_ADMIN=1 (these endpoints are ROLE_ADMIN per ADR-026 and denylisted by gc_query). ` +
      `Subsystem: ${PACK_SUBSYSTEMS.join(", ")}. Actions: ${PACK_ACTIONS.join(", ")}.`,
    {
      subsystem: z.enum(PACK_SUBSYSTEMS),
      action: z.enum(PACK_ACTIONS),
      project: z.string().optional(),
      // plugin
      name: z.string().optional(),
      plugin_type: z.enum(PLUGIN_TYPES).optional(),
      capability: z.string().optional(),
      version: z.string().optional(),
      endpoint_url: z.string().optional(),
      config: z.record(z.any()).optional(),
      metadata: z.record(z.any()).optional(),
      // control_pack
      pack_id: z.string().uuid().optional(),
      entry_uid: z.string().optional(),
      override_id: z.string().uuid().optional(),
      status: z.enum(CONTROL_PACK_ENTRY_STATUSES).optional(),
      rationale: z.string().optional(),
      // registry
      pack_type: z.enum(PACK_TYPES).optional(),
      file_path: z.string().optional(),
      description: z.string().optional(),
      signature: z.string().optional(),
      source_url: z.string().optional(),
      // trust_policy
      policy_id: z.string().uuid().optional(),
      field: z.enum(TRUST_POLICY_FIELDS).optional(),
      operator: z.enum(TRUST_POLICY_RULE_OPERATORS).optional(),
      value: z.string().optional(),
      outcome: z.enum(TRUST_OUTCOMES).optional(),
      priority: z.number().int().optional(),
      // install
      install_record_id: z.string().uuid().optional(),
      scope: z.string().optional(),
    },
    async (args) => {
      try {
        switch (args.subsystem) {
          case "plugin": {
            const data = pick(args, PACK_FIELDS.plugin);
            switch (args.action) {
              case "register": return ok(JSON.stringify(await registerPlugin(data, args.project), null, 2));
              case "unregister": reqArg(args, "name", "unregister"); await unregisterPlugin(args.name, args.project); return ok("Unregistered");
              case "list_plugins": return ok(JSON.stringify(await listPlugins(pick(args, ["plugin_type", "capability", "project"])), null, 2));
              case "get_plugin": reqArg(args, "name", "get_plugin"); return ok(JSON.stringify(await getPlugin(args.name), null, 2));
              default: return err(new Error(`Action '${args.action}' not valid for plugin`));
            }
          }
          case "control_pack": {
            const overrideData = pick(args, PACK_FIELDS.control_pack_override);
            switch (args.action) {
              case "deprecate": reqArg(args, "pack_id", "deprecate"); return ok(JSON.stringify(await deprecateControlPack(args.pack_id, args.project), null, 2));
              case "remove": reqArg(args, "pack_id", "remove"); await removeControlPack(args.pack_id, args.project); return ok("Removed");
              case "list_control_packs": return ok(JSON.stringify(await listControlPacks(args.project), null, 2));
              case "get_control_pack": reqArg(args, "pack_id", "get_control_pack"); return ok(JSON.stringify(await getControlPack(args.pack_id, args.project), null, 2));
              case "list_control_pack_entries": reqArg(args, "pack_id", "list_control_pack_entries"); return ok(JSON.stringify(await listControlPackEntries(args.pack_id, args.project), null, 2));
              case "get_control_pack_entry": reqArg(args, "pack_id", "get_control_pack_entry"); reqArg(args, "entry_uid", "get_control_pack_entry"); return ok(JSON.stringify(await getControlPackEntry(args.pack_id, args.entry_uid, args.project), null, 2));
              case "override_create": reqArg(args, "pack_id", "override_create"); reqArg(args, "entry_uid", "override_create"); return ok(JSON.stringify(await createControlPackOverride(args.pack_id, args.entry_uid, overrideData, args.project), null, 2));
              case "override_delete": reqArg(args, "pack_id", "override_delete"); reqArg(args, "entry_uid", "override_delete"); reqArg(args, "override_id", "override_delete"); await deleteControlPackOverride(args.pack_id, args.entry_uid, args.override_id, args.project); return ok("Deleted");
              case "list_control_pack_overrides": reqArg(args, "pack_id", "list_control_pack_overrides"); reqArg(args, "entry_uid", "list_control_pack_overrides"); return ok(JSON.stringify(await listControlPackOverrides(args.pack_id, args.entry_uid, args.project), null, 2));
              default: return err(new Error(`Action '${args.action}' not valid for control_pack`));
            }
          }
          case "registry": {
            const data = pick(args, PACK_FIELDS.registry);
            switch (args.action) {
              case "registry_register": return ok(JSON.stringify(await registerPackRegistryEntry(data, args.project), null, 2));
              case "import": reqArg(args, "file_path", "import"); return ok(JSON.stringify(await importPackRegistryEntry(args.file_path, data, args.project), null, 2));
              case "registry_update": reqArg(args, "pack_id", "registry_update"); reqArg(args, "version", "registry_update"); return ok(JSON.stringify(await updatePackRegistryEntry(args.pack_id, args.version, data, args.project), null, 2));
              case "withdraw": reqArg(args, "pack_id", "withdraw"); reqArg(args, "version", "withdraw"); return ok(JSON.stringify(await withdrawPackRegistryEntry(args.pack_id, args.version, args.project), null, 2));
              case "registry_delete": reqArg(args, "pack_id", "registry_delete"); reqArg(args, "version", "registry_delete"); await deletePackRegistryEntry(args.pack_id, args.version, args.project); return ok("Deleted");
              case "resolve": return ok(JSON.stringify(await resolvePack(data, args.project), null, 2));
              case "check_compatibility": return ok(JSON.stringify(await checkPackCompatibility(data, args.project), null, 2));
              case "list_pack_registry_entries": return ok(JSON.stringify(await listPackRegistryEntries(args.project, pick(args, ["pack_type"])), null, 2));
              case "list_pack_versions": reqArg(args, "pack_id", "list_pack_versions"); return ok(JSON.stringify(await listPackVersions(args.pack_id, args.project), null, 2));
              case "get_pack_registry_entry": reqArg(args, "pack_id", "get_pack_registry_entry"); reqArg(args, "version", "get_pack_registry_entry"); return ok(JSON.stringify(await getPackRegistryEntry(args.pack_id, args.version, args.project), null, 2));
              default: return err(new Error(`Action '${args.action}' not valid for registry`));
            }
          }
          case "trust_policy": {
            const data = pick(args, PACK_FIELDS.trust_policy);
            switch (args.action) {
              case "create_trust_policy": return ok(JSON.stringify(await createTrustPolicy(data, args.project), null, 2));
              case "update_trust_policy": reqArg(args, "policy_id", "update_trust_policy"); return ok(JSON.stringify(await updateTrustPolicy(args.policy_id, data), null, 2));
              case "delete_trust_policy": reqArg(args, "policy_id", "delete_trust_policy"); await deleteTrustPolicy(args.policy_id); return ok("Deleted");
              case "list_trust_policies": return ok(JSON.stringify(await listTrustPolicies(args.project), null, 2));
              case "get_trust_policy": reqArg(args, "policy_id", "get_trust_policy"); return ok(JSON.stringify(await getTrustPolicy(args.policy_id), null, 2));
              default: return err(new Error(`Action '${args.action}' not valid for trust_policy`));
            }
          }
          case "install": {
            const data = pick(args, PACK_FIELDS.install);
            switch (args.action) {
              case "install": return ok(JSON.stringify(await installPackFromRegistry(data, args.project), null, 2));
              case "upgrade": return ok(JSON.stringify(await upgradePackFromRegistry(data, args.project), null, 2));
              case "list_pack_install_records": return ok(JSON.stringify(await listPackInstallRecords(args.project, pick(args, ["pack_id"])), null, 2));
              case "get_pack_install_record": reqArg(args, "install_record_id", "get_pack_install_record"); return ok(JSON.stringify(await getPackInstallRecord(args.install_record_id), null, 2));
              default: return err(new Error(`Action '${args.action}' not valid for install`));
            }
          }
          default: return err(new Error(`Unknown subsystem: ${args.subsystem}`));
        }
      } catch (e) { return err(e); }
    },
  );
}

// ============================================================================
// Startup
// ============================================================================

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  const adminNote = ADMIN_TOOLS_ENABLED
    ? "gc_admin + gc_pack enabled (GC_MCP_ADMIN=1)"
    : "gc_admin + gc_pack NOT registered (set GC_MCP_ADMIN=1 to enable)";
  console.error(
    `[ground-control] consolidated MCP surface (ADR-035): ~25-27 tools (was 215). ${adminNote}. Read-only ad-hoc queries via gc_query.`,
  );
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
