#!/usr/bin/env node
// Ground Control MCP Server
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import {
  listProjects,
  createProject,
  getRequirementByUid,
  listRequirements,
  createRequirement,
  updateRequirement,
  transitionStatus,
  bulkTransitionStatus,
  archiveRequirement,
  cloneRequirement,
  createRelation,
  getRelations,
  getTraceabilityLinks,
  createTraceabilityLink,
  detectCycles,
  findOrphans,
  findCoverageGaps,
  impactAnalysis,
  crossWaveValidation,
  detectConsistencyViolations,
  analyzeCompleteness,
  getDashboardStats,
  getWorkOrder,
  importStrictdoc,
  importReqif,
  syncGithub,
  syncGithubPrs,
  getRequirementHistory,
  getRelationHistory,
  getTraceabilityLinkHistory,
  getRequirementTimeline,
  getRequirementDiff,
  getProjectTimeline,
  exportAuditTimeline,
  exportRequirements,
  exportSweepReport,
  exportDocument,
  deleteRelation,
  deleteTraceabilityLink,
  materializeGraph,
  getAncestors,
  getDescendants,
  findPaths,
  getGraphVisualization,
  extractSubgraph,
  traverseGraph,
  findGraphPaths,
  createGitHubIssue,
  formatIssueBody,
  runSweep,
  runSweepAll,
  getRepoGroundControlContext,
  runCodexArchitecturePreflight,
  runCodexReview,
  embedRequirement,
  getEmbeddingStatus,
  embedProject,
  deleteEmbedding,
  analyzeSemanticSimilarity,
  createBaseline,
  listBaselines,
  getBaseline,
  getBaselineSnapshot,
  compareBaselines,
  deleteBaseline,
  createQualityGate,
  listQualityGates,
  getQualityGate,
  updateQualityGate,
  deleteQualityGate,
  evaluateQualityGates,
  createDocument,
  listDocuments,
  getDocument,
  updateDocument,
  deleteDocument,
  createSection,
  listSections,
  getSectionTree,
  getSection,
  updateSection,
  deleteSection,
  addSectionContent,
  listSectionContent,
  updateSectionContent,
  deleteSectionContent,
  getDocumentReadingOrder,
  setDocumentGrammar,
  getDocumentGrammar,
  deleteDocumentGrammar,
  createAdr,
  listAdrs,
  getAdr,
  getAdrByUid,
  updateAdr,
  deleteAdr,
  transitionAdrStatus,
  getAdrRequirements,
  createAsset,
  listAssets,
  getAsset,
  getAssetByUid,
  updateAsset,
  deleteAsset,
  archiveAsset,
  createAssetRelation,
  getAssetRelations,
  deleteAssetRelation,
  detectAssetCycles,
  assetImpactAnalysis,
  extractAssetSubgraph,
  createAssetLink,
  getAssetLinks,
  deleteAssetLink,
  getAssetLinksByTarget,
  createAssetExternalId,
  getAssetExternalIds,
  updateAssetExternalId,
  deleteAssetExternalId,
  findAssetByExternalId,
  createObservation,
  listObservations,
  getObservation,
  updateObservation,
  deleteObservation,
  listLatestObservations,
  createRiskScenario,
  listRiskScenarios,
  getRiskScenario,
  getRiskScenarioByUid,
  updateRiskScenario,
  deleteRiskScenario,
  transitionRiskScenarioStatus,
  getRiskScenarioRequirements,
  createRiskScenarioLink,
  listRiskScenarioLinks,
  deleteRiskScenarioLink,
  createControl,
  listControls,
  getControl,
  getControlByUid,
  updateControl,
  deleteControl,
  transitionControlStatus,
  createControlLink,
  listControlLinks,
  deleteControlLink,
  CONTROL_STATUSES,
  CONTROL_FUNCTIONS,
  CONTROL_LINK_TARGET_TYPES,
  CONTROL_LINK_TYPES,
  createMethodologyProfile,
  listMethodologyProfiles,
  getMethodologyProfile,
  updateMethodologyProfile,
  deleteMethodologyProfile,
  createRiskRegisterRecord,
  listRiskRegisterRecords,
  getRiskRegisterRecord,
  updateRiskRegisterRecord,
  transitionRiskRegisterRecordStatus,
  deleteRiskRegisterRecord,
  createRiskAssessmentResult,
  listRiskAssessmentResults,
  getRiskAssessmentResult,
  updateRiskAssessmentResult,
  transitionRiskAssessmentApprovalState,
  deleteRiskAssessmentResult,
  createTreatmentPlan,
  listTreatmentPlans,
  getTreatmentPlan,
  updateTreatmentPlan,
  transitionTreatmentPlanStatus,
  deleteTreatmentPlan,
  STATUSES,
  REQUIREMENT_TYPES,
  PRIORITIES,
  RELATION_TYPES,
  ARTIFACT_TYPES,
  LINK_TYPES,
  METRIC_TYPES,
  COMPARISON_OPERATORS,
  ADR_STATUSES,
  ASSET_TYPES,
  ASSET_RELATION_TYPES,
  ASSET_LINK_TARGET_TYPES,
  ASSET_LINK_TYPES,
  OBSERVATION_CATEGORIES,
  RISK_SCENARIO_STATUSES,
  METHODOLOGY_FAMILIES,
  METHODOLOGY_PROFILE_STATUSES,
  RISK_REGISTER_STATUSES,
  RISK_ASSESSMENT_APPROVAL_STATUSES,
  TREATMENT_PLAN_STATUSES,
  TREATMENT_STRATEGIES,
  RISK_SCENARIO_LINK_TARGET_TYPES,
  RISK_SCENARIO_LINK_TYPES,
  createVerificationResult,
  listVerificationResults,
  getVerificationResult,
  updateVerificationResult,
  deleteVerificationResult,
  VERIFICATION_STATUSES,
  ASSURANCE_LEVELS,
  listPlugins,
  getPlugin,
  registerPlugin,
  unregisterPlugin,
  PLUGIN_TYPES,
  PLUGIN_LIFECYCLE_STATES,
  installControlPack,
  upgradeControlPack,
  listControlPacks,
  getControlPack,
  deprecateControlPack,
  removeControlPack,
  listControlPackEntries,
  getControlPackEntry,
  createControlPackOverride,
  listControlPackOverrides,
  deleteControlPackOverride,
  CONTROL_PACK_LIFECYCLE_STATES,
  CONTROL_PACK_ENTRY_STATUSES,
} from "./lib.js";

function ok(text) {
  return { content: [{ type: "text", text }] };
}

function err(e) {
  return { content: [{ type: "text", text: `Error: ${e.message}` }], isError: true };
}

const server = new McpServer({ name: "ground-control", version: "1.0.0" });

// ==========================================================================
// Project tools
// ==========================================================================

server.tool(
  "gc_list_projects",
  "List all projects in Ground Control.",
  {},
  async () => {
    try {
      return ok(JSON.stringify(await listProjects(), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_create_project",
  "Create a new project. Identifier must be lowercase alphanumeric with hyphens (e.g. 'my-project').",
  {
    identifier: z.string().describe("Unique project identifier (lowercase, hyphens allowed, e.g. 'my-project')"),
    name: z.string().describe("Human-readable project name"),
    description: z.string().optional().describe("Project description"),
  },
  async ({ identifier, name, description }) => {
    try {
      const data = { identifier, name };
      if (description !== undefined) data.description = description;
      return ok(JSON.stringify(await createProject(data), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Requirement tools
// ==========================================================================

server.tool(
  "gc_get_requirement",
  "Get a requirement by its human-readable UID (e.g. 'REQ-001').",
  {
    uid: z.string().describe("Requirement UID (e.g. 'REQ-001')"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ uid, project }) => {
    try {
      return ok(JSON.stringify(await getRequirementByUid(uid, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_requirements",
  "List requirements with optional filtering by status, type, priority, wave, or free-text search. Returns paginated results.",
  {
    status: z.enum(STATUSES).optional().describe("Filter by status"),
    type: z.enum(REQUIREMENT_TYPES).optional().describe("Filter by requirement type"),
    priority: z.enum(PRIORITIES).optional().describe("Filter by priority (MoSCoW)"),
    wave: z.number().int().optional().describe("Filter by wave number"),
    search: z.string().optional().describe("Free-text search in title and statement"),
    page: z.number().int().optional().describe("Page number (0-based)"),
    size: z.number().int().optional().describe("Page size (default 20)"),
    sort: z.string().optional().describe("Sort expression (e.g. 'uid,asc' or 'title,desc')"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ status, type, priority, wave, search, page, size, sort, project }) => {
    try {
      return ok(JSON.stringify(await listRequirements({ status, type, priority, wave, search, page, size, sort, project }), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_create_requirement",
  "Create a new requirement. Status defaults to DRAFT.",
  {
    uid: z.string().describe("Unique human-readable ID (e.g. 'REQ-081')"),
    title: z.string().describe("Short title"),
    statement: z.string().describe("Full requirement statement"),
    rationale: z.string().optional().describe("Rationale for the requirement"),
    requirement_type: z.enum(REQUIREMENT_TYPES).optional().describe("Type (default FUNCTIONAL)"),
    priority: z.enum(PRIORITIES).optional().describe("MoSCoW priority"),
    wave: z.number().int().optional().describe("Implementation wave number"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ uid, title, statement, rationale, requirement_type, priority, wave, project }) => {
    try {
      const data = { uid, title, statement };
      if (rationale !== undefined) data.rationale = rationale;
      if (requirement_type !== undefined) data.requirement_type = requirement_type;
      if (priority !== undefined) data.priority = priority;
      if (wave !== undefined) data.wave = wave;
      return ok(JSON.stringify(await createRequirement(data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_update_requirement",
  "Update an existing requirement's fields. Pass only the fields to change.",
  {
    id: z.string().uuid().describe("Requirement UUID"),
    title: z.string().optional().describe("New title"),
    statement: z.string().optional().describe("New statement"),
    rationale: z.string().optional().describe("New rationale"),
    requirement_type: z.enum(REQUIREMENT_TYPES).optional().describe("New type"),
    priority: z.enum(PRIORITIES).optional().describe("New MoSCoW priority"),
    wave: z.number().int().optional().describe("New wave number"),
  },
  async ({ id, title, statement, rationale, requirement_type, priority, wave }) => {
    try {
      const data = {};
      if (title !== undefined) data.title = title;
      if (statement !== undefined) data.statement = statement;
      if (rationale !== undefined) data.rationale = rationale;
      if (requirement_type !== undefined) data.requirement_type = requirement_type;
      if (priority !== undefined) data.priority = priority;
      if (wave !== undefined) data.wave = wave;
      return ok(JSON.stringify(await updateRequirement(id, data), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_transition_status",
  "Transition a requirement's status. Valid transitions: DRAFT->ACTIVE, ACTIVE->DEPRECATED, ACTIVE->ARCHIVED, DEPRECATED->ARCHIVED.",
  {
    id: z.string().uuid().describe("Requirement UUID"),
    status: z.enum(STATUSES).describe("Target status"),
    reason: z.string().optional().describe("Optional reason for the transition (recorded in audit trail)"),
  },
  async ({ id, status, reason }) => {
    try {
      return ok(JSON.stringify(await transitionStatus(id, status, reason), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_archive_requirement",
  "Archive a requirement (shortcut for transitioning to ARCHIVED).",
  {
    id: z.string().uuid().describe("Requirement UUID"),
  },
  async ({ id }) => {
    try {
      return ok(JSON.stringify(await archiveRequirement(id), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_bulk_transition_status",
  "Transition multiple requirements to the same status in a single operation. Best-effort: valid transitions succeed, invalid ones are collected as failures. Accepts UIDs (human-readable IDs like 'GC-A008'), not UUIDs.",
  {
    uids: z.array(z.string()).min(1).describe("Requirement UIDs (e.g. ['GC-A001', 'GC-A002'])"),
    status: z.enum(STATUSES).describe("Target status for all requirements"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
    reason: z.string().optional().describe("Optional reason for the transition (recorded in audit trail)"),
  },
  async ({ uids, status, project, reason }) => {
    try {
      const ids = [];
      const resolutionFailures = [];
      for (const uid of uids) {
        try {
          const req = await getRequirementByUid(uid, project);
          ids.push(req.id);
        } catch (e) {
          resolutionFailures.push({ id: uid, error: `UID resolution failed: ${e.message}` });
        }
      }
      if (ids.length === 0) {
        return ok(JSON.stringify({ succeeded: [], failed: resolutionFailures, total_requested: uids.length, total_succeeded: 0, total_failed: resolutionFailures.length }, null, 2));
      }
      const result = await bulkTransitionStatus(ids, status, reason);
      if (resolutionFailures.length > 0) {
        result.failed = [...(result.failed || []), ...resolutionFailures];
        result.total_failed = (result.total_failed || 0) + resolutionFailures.length;
        result.total_requested = (result.total_requested || 0) + resolutionFailures.length;
      }
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_clone_requirement",
  "Clone an existing requirement with a new UID. Copies all content fields (title, statement, rationale, type, priority, wave). The clone starts in DRAFT status. Optionally copies outgoing relations.",
  {
    uid: z.string().describe("UID of the requirement to clone (e.g. 'GC-A007')"),
    new_uid: z.string().describe("UID for the cloned requirement (must be unique)"),
    copy_relations: z.boolean().optional().default(false).describe("Whether to copy outgoing relations to the clone"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ uid, new_uid, copy_relations, project }) => {
    try {
      const source = await getRequirementByUid(uid, project);
      return ok(JSON.stringify(await cloneRequirement(source.id, new_uid, copy_relations), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_create_relation",
  "Create a directed relation between two requirements.",
  {
    source_id: z.string().uuid().describe("Source requirement UUID"),
    target_id: z.string().uuid().describe("Target requirement UUID"),
    relation_type: z.enum(RELATION_TYPES).describe("Relation type"),
  },
  async ({ source_id, target_id, relation_type }) => {
    try {
      return ok(JSON.stringify(await createRelation(source_id, target_id, relation_type), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_relations",
  "Get all relations (incoming and outgoing) for a requirement.",
  {
    id: z.string().uuid().describe("Requirement UUID"),
  },
  async ({ id }) => {
    try {
      const relations = await getRelations(id);
      if (Array.isArray(relations) && relations.length === 0) return ok("No relations found.");
      return ok(JSON.stringify(relations, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_relation",
  "Delete a relation between two requirements.",
  {
    requirement_id: z.string().uuid().describe("Source requirement UUID"),
    relation_id: z.string().uuid().describe("Relation UUID to delete"),
  },
  async ({ requirement_id, relation_id }) => {
    try {
      await deleteRelation(requirement_id, relation_id);
      return ok("Relation deleted successfully.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Traceability tools
// ==========================================================================

server.tool(
  "gc_get_traceability",
  "Get all traceability links for a requirement (code files, tests, ADRs, issues, etc.).",
  {
    id: z.string().uuid().describe("Requirement UUID"),
  },
  async ({ id }) => {
    try {
      const links = await getTraceabilityLinks(id);
      if (Array.isArray(links) && links.length === 0) return ok("No traceability links found.");
      return ok(JSON.stringify(links, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_create_traceability_link",
  "Link an artifact (code file, test, ADR, GitHub issue, etc.) to a requirement.",
  {
    requirement_id: z.string().uuid().describe("Requirement UUID"),
    artifact_type: z.enum(ARTIFACT_TYPES).describe("Type of artifact"),
    artifact_identifier: z.string().describe("Artifact identifier (e.g. file path, issue number)"),
    link_type: z.enum(LINK_TYPES).describe("How the artifact relates to the requirement"),
    artifact_url: z.string().optional().describe("URL to the artifact"),
    artifact_title: z.string().optional().describe("Human-readable title"),
  },
  async ({ requirement_id, artifact_type, artifact_identifier, link_type, artifact_url, artifact_title }) => {
    try {
      const data = { artifact_type, artifact_identifier, link_type };
      if (artifact_url !== undefined) data.artifact_url = artifact_url;
      if (artifact_title !== undefined) data.artifact_title = artifact_title;
      return ok(JSON.stringify(await createTraceabilityLink(requirement_id, data), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_traceability_link",
  "Delete a traceability link from a requirement.",
  {
    requirement_id: z.string().uuid().describe("Requirement UUID"),
    link_id: z.string().uuid().describe("Traceability link UUID to delete"),
  },
  async ({ requirement_id, link_id }) => {
    try {
      await deleteTraceabilityLink(requirement_id, link_id);
      return ok("Traceability link deleted successfully.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// GitHub integration tools
// ==========================================================================

server.tool(
  "gc_create_github_issue",
  "Create a GitHub issue from a requirement and auto-link it via traceability.",
  {
    uid: z.string().describe("Requirement UID (e.g. 'GC-D007')"),
    extra_body: z.string().optional().describe("Additional markdown to append to the issue body"),
    labels: z.array(z.string()).optional().describe("GitHub labels to apply"),
    repo: z.string().regex(/^[a-zA-Z0-9][a-zA-Z0-9._-]*\/[a-zA-Z0-9][a-zA-Z0-9._-]*$/).optional().describe("GitHub repo as 'owner/repo' (defaults to GH_REPO env var)"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ uid, extra_body, labels, repo, project }) => {
    try {
      // Fetch requirement to build issue body
      const req = await getRequirementByUid(uid, project);
      const title = `${req.uid}: ${req.title}`;
      const body = formatIssueBody(req, extra_body);

      // Create issue using local gh CLI
      const { url, number } = await createGitHubIssue({ title, body, labels, repo });

      // Auto-link via traceability
      try {
        await createTraceabilityLink(req.id, {
          artifact_type: "GITHUB_ISSUE",
          artifact_identifier: String(number),
          artifact_url: url,
          artifact_title: title,
          link_type: "IMPLEMENTS",
        });
      } catch (linkErr) {
        return ok(JSON.stringify({ url, number, warning: `Issue created but traceability link failed: ${linkErr.message}` }, null, 2));
      }

      return ok(JSON.stringify({ url, number, traceability_linked: true }, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Codex workflow tools
// ==========================================================================

server.tool(
  "gc_get_repo_ground_control_context",
  "Read the current repository's AGENTS.md and return the standardized Ground Control context used by workflow automation, including the project identifier and validation errors when the convention is missing or invalid.",
  {
    repo_path: z.string().describe("Absolute path to the target Git repository"),
  },
  async ({ repo_path }) => {
    try {
      return ok(JSON.stringify(await getRepoGroundControlContext(repo_path), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_codex_architecture_preflight",
  "Run Codex architecture preflight before implementation. Codex inspects the requirement and repository, updates ADRs/design guidance when needed, and returns guardrails and changed files.",
  {
    requirement_uid: z.string().describe("Requirement UID (for example 'GC-J001')"),
    repo_path: z.string().describe("Absolute path to the target Git repository"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
    issue_number: z.number().int().positive().optional().describe("Optional GitHub issue number for extra implementation context"),
    repo: z.string().regex(/^[a-zA-Z0-9][a-zA-Z0-9._-]*\/[a-zA-Z0-9][a-zA-Z0-9._-]*$/).optional().describe("GitHub repo as 'owner/repo' (defaults to GH_REPO env var)"),
  },
  async ({ requirement_uid, repo_path, project, issue_number, repo }) => {
    try {
      return ok(JSON.stringify(
        await runCodexArchitecturePreflight({
          requirementUid: requirement_uid,
          repoPath: repo_path,
          project,
          issueNumber: issue_number,
          repo,
        }),
        null,
        2,
      ));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_codex_review",
  "Run Codex review against the current repository with an exhaustive, no-triage review prompt focused on production-grade maintainability, reliability, security, and avoidance of concept confusion.",
  {
    repo_path: z.string().describe("Absolute path to the target Git repository"),
    base_branch: z.string().optional().describe("Base branch to review against (defaults to 'dev')"),
    uncommitted: z.boolean().optional().describe("Review staged/unstaged/untracked changes instead of committed branch history"),
  },
  async ({ repo_path, base_branch, uncommitted }) => {
    try {
      return ok(JSON.stringify(
        await runCodexReview({
          repoPath: repo_path,
          baseBranch: base_branch || "dev",
          uncommitted: Boolean(uncommitted),
        }),
        null,
        2,
      ));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// History tools
// ==========================================================================

server.tool(
  "gc_get_requirement_history",
  "Get the full audit history for a requirement, showing all revisions with timestamps and actors.",
  {
    id: z.string().uuid().describe("Requirement UUID"),
  },
  async ({ id }) => {
    try {
      const history = await getRequirementHistory(id);
      if (Array.isArray(history) && history.length === 0) return ok("No history found.");
      return ok(JSON.stringify(history, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_relation_history",
  "Get the full audit history for a relation between requirements.",
  {
    requirement_id: z.string().uuid().describe("Requirement UUID"),
    relation_id: z.string().uuid().describe("Relation UUID"),
  },
  async ({ requirement_id, relation_id }) => {
    try {
      const history = await getRelationHistory(requirement_id, relation_id);
      if (Array.isArray(history) && history.length === 0) return ok("No history found.");
      return ok(JSON.stringify(history, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_traceability_link_history",
  "Get the full audit history for a traceability link.",
  {
    requirement_id: z.string().uuid().describe("Requirement UUID"),
    link_id: z.string().uuid().describe("Traceability link UUID"),
  },
  async ({ requirement_id, link_id }) => {
    try {
      const history = await getTraceabilityLinkHistory(requirement_id, link_id);
      if (Array.isArray(history) && history.length === 0) return ok("No history found.");
      return ok(JSON.stringify(history, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_timeline",
  "Get a unified audit timeline for a requirement, merging requirement, relation, and traceability link changes with field-level diffs. Paginated (default 100 entries).",
  {
    id: z.string().uuid().describe("Requirement UUID"),
    change_category: z.enum(["REQUIREMENT", "RELATION", "TRACEABILITY_LINK"]).optional().describe("Filter by change category"),
    actor: z.string().optional().describe("Filter by actor (exact match)"),
    from: z.string().optional().describe("Start of date range (ISO-8601 instant)"),
    to: z.string().optional().describe("End of date range (ISO-8601 instant)"),
    limit: z.number().int().min(1).max(500).optional().describe("Max entries to return (default 100)"),
    offset: z.number().int().min(0).optional().describe("Number of entries to skip (default 0)"),
  },
  async ({ id, change_category, actor, from, to, limit, offset }) => {
    try {
      const timeline = await getRequirementTimeline(id, change_category, actor, from, to, limit, offset);
      if (Array.isArray(timeline) && timeline.length === 0) return ok("No timeline entries found.");
      return ok(JSON.stringify(timeline, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_requirement_diff",
  "Get a structured diff between two revisions of a requirement, showing per-field changes, added/removed relations, and added/removed traceability links.",
  {
    id: z.string().uuid().describe("Requirement UUID"),
    from_revision: z.number().int().min(1).describe("Starting revision number (the 'before')"),
    to_revision: z.number().int().min(1).describe("Ending revision number (the 'after')"),
  },
  async ({ id, from_revision, to_revision }) => {
    try {
      const diff = await getRequirementDiff(id, from_revision, to_revision);
      return ok(JSON.stringify(diff, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_project_timeline",
  "Get a unified audit timeline across all requirements in a project. Merges requirement, relation, and traceability link changes with field-level diffs. Paginated (default 100 entries).",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
    change_category: z.enum(["REQUIREMENT", "RELATION", "TRACEABILITY_LINK"]).optional().describe("Filter by change category"),
    actor: z.string().optional().describe("Filter by actor (exact match)"),
    from: z.string().optional().describe("Start of date range (ISO-8601 instant)"),
    to: z.string().optional().describe("End of date range (ISO-8601 instant)"),
    limit: z.number().int().min(1).max(500).optional().describe("Max entries to return (default 100)"),
    offset: z.number().int().min(0).optional().describe("Number of entries to skip (default 0)"),
  },
  async ({ project, change_category, actor, from, to, limit, offset }) => {
    try {
      const timeline = await getProjectTimeline(project, change_category, actor, from, to, limit, offset);
      if (Array.isArray(timeline) && timeline.length === 0) return ok("No timeline entries found.");
      return ok(JSON.stringify(timeline, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_export_audit_timeline",
  "Export the project audit timeline as CSV for compliance reporting. Returns CSV text with columns: timestamp, actor, reason, change_category, revision_type, entity_id, changes.",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
    change_category: z.enum(["REQUIREMENT", "RELATION", "TRACEABILITY_LINK"]).optional().describe("Filter by change category"),
    actor: z.string().optional().describe("Filter by actor (exact match)"),
    from: z.string().optional().describe("Start of date range (ISO-8601 instant)"),
    to: z.string().optional().describe("End of date range (ISO-8601 instant)"),
    limit: z.number().int().min(1).max(10000).optional().describe("Max entries to export (default 10000)"),
  },
  async ({ project, change_category, actor, from, to, limit }) => {
    try {
      const csv = await exportAuditTimeline(project, change_category, actor, from, to, limit);
      return ok(csv);
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_export_requirements",
  "Export all requirements for a project as CSV, Excel (.xlsx), or PDF. CSV is returned as text; binary formats (xlsx, pdf) are returned as base64-encoded data.",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
    format: z.enum(["csv", "xlsx", "pdf"]).optional().describe("Export format (default: csv)"),
  },
  async ({ project, format }) => {
    try {
      const result = await exportRequirements(project, format);
      return ok(result);
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_export_sweep_report",
  "Run a comprehensive sweep analysis and export results as CSV, Excel (.xlsx), or PDF. Includes cycles, orphans, coverage gaps, violations, and quality gate results. CSV is returned as text; binary formats are returned as base64-encoded data.",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
    format: z.enum(["csv", "xlsx", "pdf"]).optional().describe("Export format (default: csv)"),
  },
  async ({ project, format }) => {
    try {
      const result = await exportSweepReport(project, format);
      return ok(result);
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_export_document",
  "Export a document to StrictDoc (.sdoc), HTML, PDF, or ReqIF 1.2 format. Text formats return content directly; PDF returns base64-encoded data.",
  {
    document_id: z.string().uuid().describe("Document UUID to export"),
    format: z.enum(["sdoc", "html", "pdf", "reqif"]).optional().describe("Export format (default: sdoc)"),
  },
  async ({ document_id, format }) => {
    try {
      const result = await exportDocument(document_id, format);
      return ok(result);
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Analysis tools
// ==========================================================================

server.tool(
  "gc_analyze_cycles",
  "Detect dependency cycles in the requirements graph. Returns list of cycles (each cycle is a list of requirement UIDs).",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ project }) => {
    try {
      const cycles = await detectCycles(project);
      if (Array.isArray(cycles) && cycles.length === 0) return ok("No cycles detected.");
      return ok(JSON.stringify(cycles, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_analyze_orphans",
  "Find requirements with no relations to other requirements (no parent, no dependencies, not depended upon).",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ project }) => {
    try {
      const orphans = await findOrphans(project);
      if (Array.isArray(orphans) && orphans.length === 0) return ok("No orphan requirements found.");
      return ok(JSON.stringify(orphans, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_analyze_coverage_gaps",
  "Find requirements missing a specific traceability link type (e.g. requirements with no IMPLEMENTS link).",
  {
    link_type: z.enum(LINK_TYPES).describe("Link type to check coverage for"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ link_type, project }) => {
    try {
      const gaps = await findCoverageGaps(link_type, project);
      if (Array.isArray(gaps) && gaps.length === 0) return ok("No coverage gaps found.");
      return ok(JSON.stringify(gaps, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_analyze_impact",
  "Transitive impact analysis: find all requirements that would be affected by a change to the given requirement.",
  {
    id: z.string().uuid().describe("Requirement UUID to analyze impact for"),
  },
  async ({ id }) => {
    try {
      const impact = await impactAnalysis(id);
      if (Array.isArray(impact) && impact.length === 0) return ok("No downstream impact found.");
      return ok(JSON.stringify(impact, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_analyze_cross_wave",
  "Find cross-wave validation issues (e.g. a requirement in wave 2 depending on one in wave 3).",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ project }) => {
    try {
      const violations = await crossWaveValidation(project);
      if (Array.isArray(violations) && violations.length === 0) return ok("No cross-wave violations found.");
      return ok(JSON.stringify(violations, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_analyze_consistency",
  "Detect consistency violations: ACTIVE requirements linked by CONFLICTS_WITH, or SUPERSEDES relations where both sides are ACTIVE.",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ project }) => {
    try {
      const violations = await detectConsistencyViolations(project);
      if (Array.isArray(violations) && violations.length === 0) return ok("No consistency violations found.");
      return ok(JSON.stringify(violations, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Completeness analysis tool
// ==========================================================================

server.tool(
  "gc_analyze_completeness",
  "Analyze overall completeness of requirements: checks for missing fields and status distribution.",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ project }) => {
    try {
      const result = await analyzeCompleteness(project);
      if (result.total === 0) return ok("No requirements found.");
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Dashboard stats tool
// ==========================================================================

server.tool(
  "gc_dashboard_stats",
  "Get aggregate project health dashboard: requirement counts by status and wave, traceability coverage percentages, and recent changes.",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ project }) => {
    try {
      const stats = await getDashboardStats(project);
      return ok(JSON.stringify(stats, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Work order tool
// ==========================================================================

server.tool(
  "gc_get_work_order",
  "Get a topologically-sorted work order derived from the requirements DAG. Shows what is unblocked and ready to work on, grouped by wave, sorted by dependency order and MoSCoW priority.",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ project }) => {
    try {
      const result = await getWorkOrder(project);
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Graph tools
// ==========================================================================

server.tool(
  "gc_materialize_graph",
  "Materialize the mixed-entity graph in Apache AGE. Run this after bulk changes to graph-participating entities and relations.",
  {},
  async () => {
    try {
      await materializeGraph();
      return ok("Graph materialized successfully.");
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_ancestors",
  "Get all ancestor requirement UIDs for a given requirement in the dependency graph.",
  {
    uid: z.string().describe("Requirement UID (e.g. 'GC-A001')"),
    depth: z.number().int().optional().default(10).describe("Maximum traversal depth (default 10)"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ uid, depth, project }) => {
    try {
      const ancestors = await getAncestors(uid, depth, project);
      if (Array.isArray(ancestors) && ancestors.length === 0) return ok("No ancestors found.");
      return ok(JSON.stringify(ancestors, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_descendants",
  "Get all descendant requirement UIDs for a given requirement in the dependency graph.",
  {
    uid: z.string().describe("Requirement UID (e.g. 'GC-A001')"),
    depth: z.number().int().optional().default(10).describe("Maximum traversal depth (default 10)"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ uid, depth, project }) => {
    try {
      const descendants = await getDescendants(uid, depth, project);
      if (Array.isArray(descendants) && descendants.length === 0) return ok("No descendants found.");
      return ok(JSON.stringify(descendants, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_find_paths",
  "Find all paths between two requirements in the dependency graph. Returns nodes (requirement UIDs) and edges (with source, target, and relation type) for each path.",
  {
    source: z.string().describe("Source requirement UID"),
    target: z.string().describe("Target requirement UID"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ source, target, project }) => {
    try {
      const paths = await findPaths(source, target, project);
      if (Array.isArray(paths) && paths.length === 0) return ok("No paths found.");
      return ok(JSON.stringify(paths, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_graph_visualization",
  "Get the full mixed-entity graph visualization data (nodes and edges with metadata) for a project. Supports filtering by entity type.",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
    entity_types: z.array(z.string()).optional().describe("Filter by entity types (e.g. ['REQUIREMENT']). Omit to include all types."),
  },
  async ({ project, entity_types }) => {
    try {
      const data = await getGraphVisualization(project, entity_types);
      return ok(JSON.stringify(data, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_extract_subgraph",
  "Extract a mixed-entity subgraph starting from one or more root graph node IDs. Returns all reachable nodes and edges. Supports filtering by entity type.",
  {
    roots: z.array(z.string()).describe("Root graph node IDs to start traversal from (e.g. ['REQUIREMENT:<uuid>', 'RISK_SCENARIO:<uuid>'])"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
    entity_types: z.array(z.string()).optional().describe("Filter by entity types (e.g. ['REQUIREMENT']). Omit to include all types."),
    max_depth: z.number().int().positive().optional().describe("Optional traversal depth limit"),
  },
  async ({ roots, project, entity_types, max_depth }) => {
    try {
      const data = await extractSubgraph(roots, project, entity_types, max_depth);
      return ok(JSON.stringify(data, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_traverse_graph",
  "Traverse the mixed-entity graph outward from one or more root graph node IDs. Returns the visited neighborhood as nodes and edges.",
  {
    root_node_ids: z.array(z.string()).min(1).describe("Root graph node IDs to traverse from"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
    entity_types: z.array(z.string()).optional().describe("Optional entity-type filter for the traversal result"),
    max_depth: z.number().int().positive().optional().describe("Optional traversal depth limit"),
  },
  async ({ root_node_ids, project, entity_types, max_depth }) => {
    try {
      const data = await traverseGraph(root_node_ids, project, entity_types, max_depth);
      return ok(JSON.stringify(data, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_find_graph_paths",
  "Find paths in the mixed-entity graph between two graph node IDs. Returns mixed-node path results, not requirement-DAG-only paths.",
  {
    source_node_id: z.string().describe("Source graph node ID"),
    target_node_id: z.string().describe("Target graph node ID"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
    entity_types: z.array(z.string()).optional().describe("Optional entity-type filter to constrain returned paths"),
    max_depth: z.number().int().positive().optional().describe("Optional path length limit"),
  },
  async ({ source_node_id, target_node_id, project, entity_types, max_depth }) => {
    try {
      const paths = await findGraphPaths(
        source_node_id,
        target_node_id,
        project,
        entity_types,
        max_depth,
      );
      if (Array.isArray(paths) && paths.length === 0) return ok("No paths found.");
      return ok(JSON.stringify(paths, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Baseline tools
// ==========================================================================

server.tool(
  "gc_create_baseline",
  "Create a named baseline — a point-in-time snapshot of the current requirement set. Captures the current Envers revision number.",
  {
    name: z.string().max(100).describe("Baseline name (e.g. 'v1.0', 'Sprint-3 freeze')"),
    description: z.string().optional().describe("Description of what this baseline represents"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ name, description, project }) => {
    try {
      const data = { name };
      if (description !== undefined) data.description = description;
      return ok(JSON.stringify(await createBaseline(data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_baselines",
  "List all baselines for a project, ordered by creation date (newest first).",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ project }) => {
    try {
      const baselines = await listBaselines(project);
      if (Array.isArray(baselines) && baselines.length === 0) return ok("No baselines found.");
      return ok(JSON.stringify(baselines, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_baseline",
  "Get a baseline by its UUID.",
  {
    id: z.string().uuid().describe("Baseline UUID"),
  },
  async ({ id }) => {
    try {
      return ok(JSON.stringify(await getBaseline(id), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_baseline_snapshot",
  "Get the requirement snapshot for a baseline — reconstructs the full requirement set as it existed at that point in time.",
  {
    id: z.string().uuid().describe("Baseline UUID"),
  },
  async ({ id }) => {
    try {
      return ok(JSON.stringify(await getBaselineSnapshot(id), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_compare_baselines",
  "Compare two baselines to see what requirements were added, removed, or modified between them.",
  {
    baseline_id: z.string().uuid().describe("First baseline UUID (the 'before')"),
    other_baseline_id: z.string().uuid().describe("Second baseline UUID (the 'after')"),
  },
  async ({ baseline_id, other_baseline_id }) => {
    try {
      return ok(JSON.stringify(await compareBaselines(baseline_id, other_baseline_id), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_baseline",
  "Delete a baseline. This does not affect the underlying requirement history.",
  {
    id: z.string().uuid().describe("Baseline UUID"),
  },
  async ({ id }) => {
    try {
      await deleteBaseline(id);
      return ok("Baseline deleted successfully.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Admin tools
// ==========================================================================

server.tool(
  "gc_import_strictdoc",
  "Import a StrictDoc (.sdoc) file, creating documents, sections, text blocks, requirements, and relations preserving the source hierarchy. Idempotent: re-importing updates existing requirements by UID and skips existing documents/sections.",
  {
    file_path: z.string().describe("Absolute path to the .sdoc file"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ file_path, project }) => {
    try {
      return ok(JSON.stringify(await importStrictdoc(file_path, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_import_reqif",
  "Import requirements from a ReqIF 1.2 (.reqif) file. Idempotent: re-importing updates existing requirements by IDENTIFIER.",
  {
    file_path: z.string().describe("Absolute path to the .reqif file"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ file_path, project }) => {
    try {
      return ok(JSON.stringify(await importReqif(file_path, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_sync_github",
  "Sync GitHub issues for a repository. Creates traceability links between issues and requirements.",
  {
    owner: z.string().regex(/^[a-zA-Z0-9][a-zA-Z0-9._-]*$/).describe("GitHub repository owner"),
    repo: z.string().regex(/^[a-zA-Z0-9][a-zA-Z0-9._-]*$/).describe("GitHub repository name"),
  },
  async ({ owner, repo }) => {
    try {
      return ok(JSON.stringify(await syncGithub(owner, repo), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_sync_github_prs",
  "Sync GitHub pull requests for a repository. Updates traceability links with PR state (open, closed, merged).",
  {
    owner: z.string().regex(/^[a-zA-Z0-9][a-zA-Z0-9._-]*$/).describe("GitHub repository owner"),
    repo: z.string().regex(/^[a-zA-Z0-9][a-zA-Z0-9._-]*$/).describe("GitHub repository name"),
  },
  async ({ owner, repo }) => {
    try {
      return ok(JSON.stringify(await syncGithubPrs(owner, repo), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Analysis sweep tools
// ==========================================================================

server.tool(
  "gc_run_sweep",
  "Run a full analysis sweep (orphans, coverage gaps, cross-wave, cycles, consistency) on a project. Returns a report of all detected problems. Optionally triggers configured notifications (GitHub issues, webhooks).",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ project }) => {
    try {
      const result = await runSweep(project);
      if (!result.has_problems) return ok("Sweep complete. No problems detected.");
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_run_sweep_all",
  "Run a full analysis sweep across ALL projects. Returns a list of reports, one per project.",
  {},
  async () => {
    try {
      const results = await runSweepAll();
      if (Array.isArray(results) && results.length === 0) return ok("No projects to sweep.");
      return ok(JSON.stringify(results, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Embedding tools
// ==========================================================================

server.tool(
  "gc_embed_requirement",
  "Generate a vector embedding for a requirement's text content (title, statement, rationale). Skips if embedding is already up-to-date. Returns status: 'embedded', 'up_to_date', or 'provider_unavailable'.",
  {
    requirement_id: z.string().uuid().describe("Requirement UUID"),
  },
  async ({ requirement_id }) => {
    try {
      const result = await embedRequirement(requirement_id);
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_embedding_status",
  "Check the embedding status of a requirement: whether it has an embedding, if it's stale (text changed), or if the model has changed since embedding.",
  {
    requirement_id: z.string().uuid().describe("Requirement UUID"),
  },
  async ({ requirement_id }) => {
    try {
      const result = await getEmbeddingStatus(requirement_id);
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_embed_project",
  "Batch-embed all requirements in a project. Only embeds requirements missing embeddings or with stale content. Use force=true to re-embed everything (e.g., after model migration).",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
    force: z.boolean().optional().describe("Force re-embedding all requirements (default false)"),
  },
  async ({ project, force }) => {
    try {
      const result = await embedProject(project, force);
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Semantic analysis tools
// ==========================================================================

server.tool(
  "gc_analyze_similarity",
  "Find semantically similar requirement pairs by computing cosine similarity across requirement embeddings. Returns pairs exceeding the threshold, sorted by similarity score descending. Requires embeddings to exist (run gc_embed_project first).",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
    threshold: z
      .number()
      .min(0)
      .max(1)
      .optional()
      .describe("Minimum similarity score (0-1). Defaults to server-configured threshold (0.85)"),
  },
  async ({ project, threshold }) => {
    try {
      const result = await analyzeSemanticSimilarity(project, threshold);
      if (result.pairs && result.pairs.length === 0) {
        return ok(
          `No similar requirement pairs found above threshold ${result.threshold}. ` +
            `(${result.embedded_count}/${result.total_requirements} requirements embedded, ${result.pairs_analyzed} pairs analyzed)`,
        );
      }
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Quality Gate tools
// ==========================================================================

server.tool(
  "gc_create_quality_gate",
  "Create a quality gate rule for a project. Quality gates define pass/fail thresholds for CI/CD integration (e.g. 'minimum 80% of ACTIVE requirements must have a TESTS link').",
  {
    name: z.string().max(100).describe("Quality gate name (e.g. 'Test Coverage Gate')"),
    description: z.string().optional().describe("Description of what this gate checks"),
    metric_type: z.enum(METRIC_TYPES).describe("Metric to evaluate: COVERAGE (% with link type), ORPHAN_COUNT, COMPLETENESS (issue count)"),
    metric_param: z.string().optional().describe("Parameter for the metric. Required for COVERAGE: a LinkType (IMPLEMENTS, TESTS, DOCUMENTS, CONSTRAINS, VERIFIES)"),
    scope_status: z.enum(STATUSES).optional().describe("Filter requirements by status (e.g. ACTIVE). Omit to check all non-archived requirements"),
    operator: z.enum(COMPARISON_OPERATORS).describe("Comparison operator: GTE (>=), LTE (<=), EQ (==), GT (>), LT (<)"),
    threshold: z.number().describe("Threshold value to compare against (e.g. 80 for 80% coverage)"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ name, description, metric_type, metric_param, scope_status, operator, threshold, project }) => {
    try {
      const data = { name, metric_type, operator, threshold };
      if (description !== undefined) data.description = description;
      if (metric_param !== undefined) data.metric_param = metric_param;
      if (scope_status !== undefined) data.scope_status = scope_status;
      return ok(JSON.stringify(await createQualityGate(data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_quality_gates",
  "List all quality gates for a project.",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ project }) => {
    try {
      const gates = await listQualityGates(project);
      if (Array.isArray(gates) && gates.length === 0) return ok("No quality gates found.");
      return ok(JSON.stringify(gates, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_quality_gate",
  "Get a quality gate by its UUID.",
  {
    id: z.string().uuid().describe("Quality gate UUID"),
  },
  async ({ id }) => {
    try {
      return ok(JSON.stringify(await getQualityGate(id), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_update_quality_gate",
  "Update a quality gate. Only specified fields are changed.",
  {
    id: z.string().uuid().describe("Quality gate UUID"),
    name: z.string().max(100).optional().describe("New name"),
    description: z.string().optional().describe("New description"),
    metric_type: z.enum(METRIC_TYPES).optional().describe("New metric type"),
    metric_param: z.string().optional().describe("New metric parameter"),
    scope_status: z.enum(STATUSES).optional().describe("New scope status filter"),
    operator: z.enum(COMPARISON_OPERATORS).optional().describe("New comparison operator"),
    threshold: z.number().optional().describe("New threshold value"),
    enabled: z.boolean().optional().describe("Enable or disable the gate"),
  },
  async ({ id, ...fields }) => {
    try {
      const data = {};
      for (const [k, v] of Object.entries(fields)) {
        if (v !== undefined) data[k] = v;
      }
      return ok(JSON.stringify(await updateQualityGate(id, data), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_quality_gate",
  "Delete a quality gate.",
  {
    id: z.string().uuid().describe("Quality gate UUID"),
  },
  async ({ id }) => {
    try {
      await deleteQualityGate(id);
      return ok("Quality gate deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_evaluate_quality_gates",
  "Evaluate all enabled quality gates for a project. Returns overall pass/fail plus per-gate results. Designed for CI/CD integration.",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ project }) => {
    try {
      const result = await evaluateQualityGates(project);
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Document tools
// ==========================================================================

server.tool(
  "gc_create_document",
  "Create a document — a top-level container for organizing requirements into coherent specifications.",
  {
    title: z.string().max(200).describe("Document title"),
    version: z.string().max(50).describe("Document version (e.g. '1.0.0')"),
    description: z.string().optional().describe("Document description"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ title, version, description, project }) => {
    try {
      const data = { title, version };
      if (description !== undefined) data.description = description;
      return ok(JSON.stringify(await createDocument(data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_documents",
  "List all documents for a project, ordered by creation date (newest first).",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ project }) => {
    try {
      const documents = await listDocuments(project);
      if (Array.isArray(documents) && documents.length === 0) return ok("No documents found.");
      return ok(JSON.stringify(documents, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_document",
  "Get a document by its UUID.",
  {
    id: z.string().uuid().describe("Document UUID"),
  },
  async ({ id }) => {
    try {
      return ok(JSON.stringify(await getDocument(id), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_update_document",
  "Update a document. Only specified fields are changed.",
  {
    id: z.string().uuid().describe("Document UUID"),
    title: z.string().max(200).optional().describe("New title"),
    version: z.string().max(50).optional().describe("New version"),
    description: z.string().optional().describe("New description"),
  },
  async ({ id, ...fields }) => {
    try {
      const data = {};
      for (const [k, v] of Object.entries(fields)) {
        if (v !== undefined) data[k] = v;
      }
      return ok(JSON.stringify(await updateDocument(id, data), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_document",
  "Delete a document.",
  {
    id: z.string().uuid().describe("Document UUID"),
  },
  async ({ id }) => {
    try {
      await deleteDocument(id);
      return ok("Document deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Section tools
// ==========================================================================

server.tool(
  "gc_create_section",
  "Create a section within a document. Sections support arbitrary nesting via optional parentId.",
  {
    document_id: z.string().uuid().describe("Document UUID"),
    parent_id: z.string().uuid().optional().describe("Parent section UUID (omit for root section)"),
    title: z.string().max(200).describe("Section title"),
    description: z.string().optional().describe("Section description"),
    sort_order: z.number().int().optional().describe("Sort order among siblings (default 0)"),
  },
  async ({ document_id, parent_id, title, description, sort_order }) => {
    try {
      const data = { title };
      if (parent_id !== undefined) data.parent_id = parent_id;
      if (description !== undefined) data.description = description;
      if (sort_order !== undefined) data.sort_order = sort_order;
      return ok(JSON.stringify(await createSection(document_id, data), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_sections",
  "List all sections in a document (flat, ordered by sort order).",
  {
    document_id: z.string().uuid().describe("Document UUID"),
  },
  async ({ document_id }) => {
    try {
      const sections = await listSections(document_id);
      if (Array.isArray(sections) && sections.length === 0) return ok("No sections found.");
      return ok(JSON.stringify(sections, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_section_tree",
  "Get sections as a nested tree structure for a document.",
  {
    document_id: z.string().uuid().describe("Document UUID"),
  },
  async ({ document_id }) => {
    try {
      const tree = await getSectionTree(document_id);
      if (Array.isArray(tree) && tree.length === 0) return ok("No sections found.");
      return ok(JSON.stringify(tree, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_section",
  "Get a section by its UUID.",
  {
    id: z.string().uuid().describe("Section UUID"),
  },
  async ({ id }) => {
    try {
      return ok(JSON.stringify(await getSection(id), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_update_section",
  "Update a section. Only specified fields are changed.",
  {
    id: z.string().uuid().describe("Section UUID"),
    title: z.string().max(200).optional().describe("New title"),
    description: z.string().optional().describe("New description"),
    sort_order: z.number().int().optional().describe("New sort order"),
  },
  async ({ id, ...fields }) => {
    try {
      const data = {};
      for (const [k, v] of Object.entries(fields)) {
        if (v !== undefined) data[k] = v;
      }
      return ok(JSON.stringify(await updateSection(id, data), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_section",
  "Delete a section and all its children (cascading).",
  {
    id: z.string().uuid().describe("Section UUID"),
  },
  async ({ id }) => {
    try {
      await deleteSection(id);
      return ok("Section deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Section Content tools
// ==========================================================================

server.tool(
  "gc_add_section_content",
  "Add a content item to a section — either a requirement reference or a text block. Items are ordered by sort_order for rendering.",
  {
    section_id: z.string().uuid().describe("Section UUID"),
    content_type: z.enum(["REQUIREMENT", "TEXT_BLOCK"]).describe("Content type"),
    requirement_id: z.string().uuid().optional().describe("Requirement UUID (required for REQUIREMENT type)"),
    text_content: z.string().optional().describe("Text content (required for TEXT_BLOCK type)"),
    sort_order: z.number().int().optional().describe("Sort order for rendering sequence (default 0)"),
  },
  async ({ section_id, content_type, requirement_id, text_content, sort_order }) => {
    try {
      const data = { content_type };
      if (requirement_id !== undefined) data.requirement_id = requirement_id;
      if (text_content !== undefined) data.text_content = text_content;
      if (sort_order !== undefined) data.sort_order = sort_order;
      return ok(JSON.stringify(await addSectionContent(section_id, data), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_section_content",
  "List content items in a section, ordered by sort_order for rendering in reading order.",
  {
    section_id: z.string().uuid().describe("Section UUID"),
  },
  async ({ section_id }) => {
    try {
      const items = await listSectionContent(section_id);
      if (Array.isArray(items) && items.length === 0) return ok("No content items found.");
      return ok(JSON.stringify(items, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_update_section_content",
  "Update a section content item. Only text_content (for TEXT_BLOCK) and sort_order can be changed.",
  {
    id: z.string().uuid().describe("Section content UUID"),
    text_content: z.string().optional().describe("New text content (TEXT_BLOCK only)"),
    sort_order: z.number().int().optional().describe("New sort order"),
  },
  async ({ id, ...fields }) => {
    try {
      const data = {};
      for (const [k, v] of Object.entries(fields)) {
        if (v !== undefined) data[k] = v;
      }
      return ok(JSON.stringify(await updateSectionContent(id, data), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_section_content",
  "Delete a content item from a section.",
  {
    id: z.string().uuid().describe("Section content UUID"),
  },
  async ({ id }) => {
    try {
      await deleteSectionContent(id);
      return ok("Section content deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Document Reading Order tools
// ==========================================================================

server.tool(
  "gc_get_document_reading_order",
  "Get a document rendered in reading order: sections with their content (text blocks and requirement references) nested in authored sequence.",
  {
    document_id: z.string().uuid().describe("Document UUID"),
  },
  async ({ document_id }) => {
    try {
      const result = await getDocumentReadingOrder(document_id);
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Document Grammar tools
// ==========================================================================

server.tool(
  "gc_set_document_grammar",
  "Set or replace the grammar for a document. Defines custom fields, allowed requirement types, and allowed relation types.",
  {
    document_id: z.string().uuid().describe("Document UUID"),
    fields: z.array(z.object({
      name: z.string().describe("Field name"),
      type: z.enum(["STRING", "INTEGER", "BOOLEAN", "ENUM"]).describe("Field data type"),
      required: z.boolean().describe("Whether the field is required"),
      enum_values: z.array(z.string()).optional().describe("Valid values for ENUM type"),
    })).optional().describe("Custom field definitions"),
    allowed_requirement_types: z.array(z.string()).optional().describe("Allowed requirement types (e.g. FUNCTIONAL, NON_FUNCTIONAL)"),
    allowed_relation_types: z.array(z.string()).optional().describe("Allowed relation types (e.g. PARENT, DEPENDS_ON)"),
  },
  async ({ document_id, fields, allowed_requirement_types, allowed_relation_types }) => {
    try {
      const grammar = {};
      if (fields !== undefined) grammar.fields = fields;
      if (allowed_requirement_types !== undefined) grammar.allowedRequirementTypes = allowed_requirement_types;
      if (allowed_relation_types !== undefined) grammar.allowedRelationTypes = allowed_relation_types;
      return ok(JSON.stringify(await setDocumentGrammar(document_id, grammar), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_document_grammar",
  "Get the grammar for a document.",
  {
    document_id: z.string().uuid().describe("Document UUID"),
  },
  async ({ document_id }) => {
    try {
      const grammar = await getDocumentGrammar(document_id);
      if (!grammar) return ok("No grammar defined for this document.");
      return ok(JSON.stringify(grammar, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_document_grammar",
  "Remove the grammar from a document.",
  {
    document_id: z.string().uuid().describe("Document UUID"),
  },
  async ({ document_id }) => {
    try {
      await deleteDocumentGrammar(document_id);
      return ok("Document grammar deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Architecture Decision Records
// ==========================================================================

server.tool(
  "gc_create_adr",
  "Create an architecture decision record (ADR) — a first-class entity for tracking architectural decisions linked to requirements.",
  {
    uid: z.string().max(20).describe("ADR UID (e.g. 'ADR-018')"),
    title: z.string().max(200).describe("ADR title"),
    decision_date: z.string().describe("Decision date (YYYY-MM-DD)"),
    context: z.string().optional().describe("Context — what motivated this decision"),
    decision: z.string().optional().describe("The decision made"),
    consequences: z.string().optional().describe("Consequences — positive, negative, risks"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ uid, title, decision_date, context, decision, consequences, project }) => {
    try {
      const data = { uid, title, decision_date };
      if (context !== undefined) data.context = context;
      if (decision !== undefined) data.decision = decision;
      if (consequences !== undefined) data.consequences = consequences;
      return ok(JSON.stringify(await createAdr(data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_adrs",
  "List all ADRs for a project, ordered by decision date (newest first).",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ project }) => {
    try {
      const adrs = await listAdrs(project);
      if (Array.isArray(adrs) && adrs.length === 0) return ok("No ADRs found.");
      return ok(JSON.stringify(adrs, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_adr",
  "Get an ADR by its UUID or by its UID (e.g. 'ADR-018'). Provide either id or uid.",
  {
    id: z.string().uuid().optional().describe("ADR UUID"),
    uid: z.string().optional().describe("ADR UID (e.g. 'ADR-018')"),
    project: z.string().optional().describe("Project identifier (required when looking up by uid)"),
  },
  async ({ id, uid, project }) => {
    try {
      if (id) {
        return ok(JSON.stringify(await getAdr(id), null, 2));
      }
      if (uid) {
        return ok(JSON.stringify(await getAdrByUid(uid, project), null, 2));
      }
      return err(new Error("Provide either 'id' or 'uid'."));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_update_adr",
  "Update an ADR. Only specified fields are changed.",
  {
    id: z.string().uuid().describe("ADR UUID"),
    title: z.string().max(200).optional().describe("New title"),
    decision_date: z.string().optional().describe("New decision date (YYYY-MM-DD)"),
    context: z.string().optional().describe("Updated context"),
    decision: z.string().optional().describe("Updated decision"),
    consequences: z.string().optional().describe("Updated consequences"),
    superseded_by: z.string().max(20).optional().describe("UID of the superseding ADR"),
  },
  async ({ id, title, decision_date, context, decision, consequences, superseded_by }) => {
    try {
      const data = {};
      if (title !== undefined) data.title = title;
      if (decision_date !== undefined) data.decision_date = decision_date;
      if (context !== undefined) data.context = context;
      if (decision !== undefined) data.decision = decision;
      if (consequences !== undefined) data.consequences = consequences;
      if (superseded_by !== undefined) data.superseded_by = superseded_by;
      return ok(JSON.stringify(await updateAdr(id, data), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_adr",
  "Delete an ADR by its UUID.",
  {
    id: z.string().uuid().describe("ADR UUID"),
  },
  async ({ id }) => {
    try {
      await deleteAdr(id);
      return ok("ADR deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_transition_adr_status",
  `Transition an ADR's status. Valid transitions: PROPOSED→ACCEPTED, ACCEPTED→DEPRECATED, ACCEPTED→SUPERSEDED.`,
  {
    id: z.string().uuid().describe("ADR UUID"),
    status: z.enum(ADR_STATUSES).describe("Target status"),
  },
  async ({ id, status }) => {
    try {
      return ok(JSON.stringify(await transitionAdrStatus(id, status), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_adr_requirements",
  "Get all requirements linked to an ADR via traceability links (reverse traceability).",
  {
    id: z.string().uuid().describe("ADR UUID"),
  },
  async ({ id }) => {
    try {
      const reqs = await getAdrRequirements(id);
      if (Array.isArray(reqs) && reqs.length === 0) return ok("No linked requirements found.");
      return ok(JSON.stringify(reqs, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Operational Asset tools
// ==========================================================================

server.tool(
  "gc_create_asset",
  "Create an operational asset in a project.",
  {
    uid: z.string().max(50).describe("Unique asset identifier (e.g. 'ASSET-001')"),
    name: z.string().max(200).describe("Asset name"),
    description: z.string().optional().describe("Asset description"),
    asset_type: z.enum(ASSET_TYPES).optional().describe("Asset type (default: OTHER)"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ uid, name, description, asset_type, project }) => {
    try {
      const data = { uid, name };
      if (description !== undefined) data.description = description;
      if (asset_type !== undefined) data.asset_type = asset_type;
      return ok(JSON.stringify(await createAsset(data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_assets",
  "List operational assets in a project, optionally filtered by type.",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
    type: z.enum(ASSET_TYPES).optional().describe("Filter by asset type"),
  },
  async ({ project, type }) => {
    try {
      const assets = await listAssets({ project, type });
      if (Array.isArray(assets) && assets.length === 0) return ok("No assets found.");
      return ok(JSON.stringify(assets, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_asset",
  "Get an operational asset by its UUID.",
  {
    id: z.string().uuid().describe("Asset UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, project }) => {
    try {
      return ok(JSON.stringify(await getAsset(id, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_asset_by_uid",
  "Get an operational asset by its human-readable UID (e.g. 'ASSET-001').",
  {
    uid: z.string().describe("Asset UID"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ uid, project }) => {
    try {
      return ok(JSON.stringify(await getAssetByUid(uid, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_update_asset",
  "Update an operational asset.",
  {
    id: z.string().uuid().describe("Asset UUID"),
    name: z.string().max(200).optional().describe("New name"),
    description: z.string().optional().describe("New description"),
    asset_type: z.enum(ASSET_TYPES).optional().describe("New asset type"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, name, description, asset_type, project }) => {
    try {
      const data = {};
      if (name !== undefined) data.name = name;
      if (description !== undefined) data.description = description;
      if (asset_type !== undefined) data.asset_type = asset_type;
      return ok(JSON.stringify(await updateAsset(id, data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_asset",
  "Delete an operational asset by its UUID. Cascade-deletes all its relations.",
  {
    id: z.string().uuid().describe("Asset UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, project }) => {
    try {
      await deleteAsset(id, project);
      return ok("Asset deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_archive_asset",
  "Archive (soft-delete) an operational asset.",
  {
    id: z.string().uuid().describe("Asset UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, project }) => {
    try {
      return ok(JSON.stringify(await archiveAsset(id, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_create_asset_relation",
  `Create a typed relationship between two operational assets. Relation types: ${ASSET_RELATION_TYPES.join(", ")}. Optional provenance: source_system, external_source_id, collected_at (ISO-8601), confidence.`,
  {
    source_id: z.string().uuid().describe("Source asset UUID"),
    target_id: z.string().uuid().describe("Target asset UUID"),
    relation_type: z.enum(ASSET_RELATION_TYPES).describe("Relationship type"),
    source_system: z.string().max(100).optional().describe("Source system that asserted this fact (e.g. AWS_CONFIG)"),
    external_source_id: z.string().max(500).optional().describe("Identifier for this fact in the source system"),
    collected_at: z.string().optional().describe("ISO-8601 timestamp when fact was collected/asserted"),
    confidence: z.string().max(50).optional().describe("Confidence or quality metadata"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ source_id, target_id, relation_type, source_system, external_source_id, collected_at, confidence, project }) => {
    try {
      const data = { target_id, relation_type };
      if (source_system !== undefined) data.source_system = source_system;
      if (external_source_id !== undefined) data.external_source_id = external_source_id;
      if (collected_at !== undefined) data.collected_at = collected_at;
      if (confidence !== undefined) data.confidence = confidence;
      return ok(JSON.stringify(await createAssetRelation(source_id, data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_asset_relations",
  "Get all incoming and outgoing relations for an operational asset.",
  {
    id: z.string().uuid().describe("Asset UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, project }) => {
    try {
      const rels = await getAssetRelations(id, project);
      if (Array.isArray(rels) && rels.length === 0) return ok("No relations found.");
      return ok(JSON.stringify(rels, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_asset_relation",
  "Delete a relation from an operational asset.",
  {
    asset_id: z.string().uuid().describe("Asset UUID (source or target of the relation)"),
    relation_id: z.string().uuid().describe("Relation UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ asset_id, relation_id, project }) => {
    try {
      await deleteAssetRelation(asset_id, relation_id, project);
      return ok("Asset relation deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_detect_asset_cycles",
  "Detect cycles in the asset topology graph for a project.",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ project }) => {
    try {
      const cycles = await detectAssetCycles(project);
      if (Array.isArray(cycles) && cycles.length === 0) return ok("No cycles detected.");
      return ok(JSON.stringify(cycles, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_asset_impact_analysis",
  "Multi-hop impact analysis: find all assets transitively affected by a given asset.",
  {
    id: z.string().uuid().describe("Asset UUID to analyze impact from"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, project }) => {
    try {
      const result = await assetImpactAnalysis(id, project);
      if (Array.isArray(result) && result.length === 0) return ok("No impacted assets found.");
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_extract_asset_subgraph",
  "Extract a connected subgraph of assets starting from one or more root UIDs.",
  {
    root_uids: z.array(z.string()).min(1).describe("List of root asset UIDs to start from"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ root_uids, project }) => {
    try {
      return ok(JSON.stringify(await extractAssetSubgraph({ root_uids }, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Asset Links (cross-entity linking)
// ==========================================================================

server.tool(
  "gc_create_asset_link",
  `Link an operational asset to a requirement, control, risk scenario, or other entity. Target types: ${ASSET_LINK_TARGET_TYPES.join(", ")}. Link types: ${ASSET_LINK_TYPES.join(", ")}.`,
  {
    asset_id: z.string().uuid().describe("UUID of the operational asset"),
    target_type: z.enum(ASSET_LINK_TARGET_TYPES).describe("Type of the target entity"),
    target_entity_id: z.string().uuid().optional().describe("UUID of the modeled target entity when linking to a first-class internal node"),
    target_identifier: z.string().max(500).optional().describe("Identifier of the external or unmodeled target (e.g. requirement UID, URL)"),
    link_type: z.enum(ASSET_LINK_TYPES).describe("Nature of the relationship"),
    target_url: z.string().max(2000).optional().describe("Optional URL for the target"),
    target_title: z.string().max(255).optional().describe("Optional display title for the target"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ asset_id, target_type, target_entity_id, target_identifier, link_type, target_url, target_title, project }) => {
    try {
      const data = { target_type, link_type };
      if (target_entity_id !== undefined) data.target_entity_id = target_entity_id;
      if (target_identifier !== undefined) data.target_identifier = target_identifier;
      if (target_url !== undefined) data.target_url = target_url;
      if (target_title !== undefined) data.target_title = target_title;
      return ok(JSON.stringify(await createAssetLink(asset_id, data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_asset_links",
  "Get all cross-entity links for an operational asset, optionally filtered by target type.",
  {
    asset_id: z.string().uuid().describe("UUID of the operational asset"),
    target_type: z.enum(ASSET_LINK_TARGET_TYPES).optional().describe("Filter by target type"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ asset_id, target_type, project }) => {
    try {
      return ok(JSON.stringify(await getAssetLinks(asset_id, target_type, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_asset_link",
  "Delete a cross-entity link from an operational asset.",
  {
    asset_id: z.string().uuid().describe("UUID of the operational asset"),
    link_id: z.string().uuid().describe("UUID of the link to delete"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ asset_id, link_id, project }) => {
    try {
      await deleteAssetLink(asset_id, link_id, project);
      return ok("Link deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_asset_links_by_target",
  "Find all assets linked to a specific target (e.g. all assets linked to a requirement).",
  {
    target_type: z.enum(ASSET_LINK_TARGET_TYPES).describe("Type of the target entity"),
    target_entity_id: z.string().uuid().optional().describe("UUID of the target entity"),
    target_identifier: z.string().optional().describe("Identifier of the target"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ target_type, target_entity_id, target_identifier, project }) => {
    try {
      if (!target_entity_id && !target_identifier) {
        return err(new Error("Provide either 'target_entity_id' or 'target_identifier'."));
      }
      return ok(
        JSON.stringify(
          await getAssetLinksByTarget(target_type, target_entity_id, target_identifier, project),
          null,
          2,
        ),
      );
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// External Identifiers (source provenance)
// ==========================================================================

server.tool(
  "gc_create_asset_external_id",
  "Register an external identifier for an operational asset, mapping it to a source system (e.g. AWS ARN, Terraform resource ID, ServiceNow CI). Supports multiple overlapping sources per asset.",
  {
    asset_id: z.string().uuid().describe("UUID of the operational asset"),
    source_system: z.string().max(100).describe("Name of the source system (e.g. AWS, TERRAFORM, SERVICENOW)"),
    source_id: z.string().max(500).describe("Identifier in the source system (e.g. ARN, resource ID)"),
    collected_at: z.string().optional().describe("ISO-8601 timestamp when this identifier was collected/asserted"),
    confidence: z.string().max(50).optional().describe("Confidence or quality metadata (e.g. HIGH, 0.95)"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ asset_id, source_system, source_id, collected_at, confidence, project }) => {
    try {
      const data = { source_system, source_id };
      if (collected_at !== undefined) data.collected_at = collected_at;
      if (confidence !== undefined) data.confidence = confidence;
      return ok(JSON.stringify(await createAssetExternalId(asset_id, data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_asset_external_ids",
  "List all external identifiers for an operational asset, optionally filtered by source system.",
  {
    asset_id: z.string().uuid().describe("UUID of the operational asset"),
    source_system: z.string().optional().describe("Filter by source system name"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ asset_id, source_system, project }) => {
    try {
      const result = await getAssetExternalIds(asset_id, source_system, project);
      if (Array.isArray(result) && result.length === 0) return ok("No external identifiers found.");
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_update_asset_external_id",
  "Update the collection timestamp or confidence metadata of an external identifier. Source system and source ID are immutable.",
  {
    asset_id: z.string().uuid().describe("UUID of the operational asset"),
    external_id_id: z.string().uuid().describe("UUID of the external identifier to update"),
    collected_at: z.string().optional().describe("ISO-8601 timestamp when this identifier was collected/asserted"),
    confidence: z.string().max(50).optional().describe("Confidence or quality metadata"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ asset_id, external_id_id, collected_at, confidence, project }) => {
    try {
      const data = {};
      if (collected_at !== undefined) data.collected_at = collected_at;
      if (confidence !== undefined) data.confidence = confidence;
      return ok(JSON.stringify(await updateAssetExternalId(asset_id, external_id_id, data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_asset_external_id",
  "Delete an external identifier from an operational asset.",
  {
    asset_id: z.string().uuid().describe("UUID of the operational asset"),
    external_id_id: z.string().uuid().describe("UUID of the external identifier to delete"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ asset_id, external_id_id, project }) => {
    try {
      await deleteAssetExternalId(asset_id, external_id_id, project);
      return ok("External identifier deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_find_asset_by_external_id",
  "Find operational assets by their external identifier in a specific source system. Reverse lookup from source system + source ID to Ground Control assets.",
  {
    source_system: z.string().describe("Source system name (e.g. AWS, TERRAFORM)"),
    source_id: z.string().describe("Identifier in the source system"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ source_system, source_id, project }) => {
    try {
      const result = await findAssetByExternalId(source_system, source_id, project);
      if (Array.isArray(result) && result.length === 0) return ok("No assets found with that external identifier.");
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Observations (time-bounded state facts)
// ==========================================================================

server.tool(
  "gc_create_observation",
  "Record a time-bounded state fact (observation) about an operational asset, such as a configuration value, exposure status, identity assignment, deployment attribute, patch state, or discovered relationship.",
  {
    asset_id: z.string().uuid().describe("UUID of the operational asset"),
    category: z.enum(OBSERVATION_CATEGORIES).describe("Category of observation"),
    observation_key: z.string().max(200).describe("What is being observed (e.g. 'os_version', 'cve_exposure', 'ip_address')"),
    observation_value: z.string().describe("The observed value"),
    source: z.string().max(200).describe("Who or what produced this observation (e.g. 'nessus-scanner', 'aws-config')"),
    observed_at: z.string().describe("ISO-8601 timestamp when the fact was observed"),
    expires_at: z.string().optional().describe("ISO-8601 timestamp when this observation becomes stale/invalid"),
    confidence: z.string().max(50).optional().describe("Confidence level (e.g. HIGH, MEDIUM, 0.95)"),
    evidence_ref: z.string().max(2000).optional().describe("URL or reference to supporting evidence"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ asset_id, category, observation_key, observation_value, source, observed_at, expires_at, confidence, evidence_ref, project }) => {
    try {
      const data = { category, observation_key, observation_value, source, observed_at };
      if (expires_at !== undefined) data.expires_at = expires_at;
      if (confidence !== undefined) data.confidence = confidence;
      if (evidence_ref !== undefined) data.evidence_ref = evidence_ref;
      return ok(JSON.stringify(await createObservation(asset_id, data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_observations",
  "List observations for an operational asset, optionally filtered by category and/or key.",
  {
    asset_id: z.string().uuid().describe("UUID of the operational asset"),
    category: z.enum(OBSERVATION_CATEGORIES).optional().describe("Filter by observation category"),
    key: z.string().optional().describe("Filter by observation key"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ asset_id, category, key, project }) => {
    try {
      const result = await listObservations(asset_id, { category, key, project });
      if (Array.isArray(result) && result.length === 0) return ok("No observations found.");
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_observation",
  "Get a specific observation by ID.",
  {
    asset_id: z.string().uuid().describe("UUID of the operational asset"),
    observation_id: z.string().uuid().describe("UUID of the observation"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ asset_id, observation_id, project }) => {
    try {
      return ok(JSON.stringify(await getObservation(asset_id, observation_id, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_update_observation",
  "Update a mutable field of an observation (value, expiry, confidence, or evidence reference). Category, key, source, and observed-at are immutable.",
  {
    asset_id: z.string().uuid().describe("UUID of the operational asset"),
    observation_id: z.string().uuid().describe("UUID of the observation to update"),
    observation_value: z.string().optional().describe("Updated observed value"),
    expires_at: z.string().optional().describe("Updated expiry timestamp"),
    confidence: z.string().max(50).optional().describe("Updated confidence level"),
    evidence_ref: z.string().max(2000).optional().describe("Updated evidence reference"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ asset_id, observation_id, observation_value, expires_at, confidence, evidence_ref, project }) => {
    try {
      const data = {};
      if (observation_value !== undefined) data.observation_value = observation_value;
      if (expires_at !== undefined) data.expires_at = expires_at;
      if (confidence !== undefined) data.confidence = confidence;
      if (evidence_ref !== undefined) data.evidence_ref = evidence_ref;
      return ok(JSON.stringify(await updateObservation(asset_id, observation_id, data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_observation",
  "Delete an observation from an operational asset.",
  {
    asset_id: z.string().uuid().describe("UUID of the operational asset"),
    observation_id: z.string().uuid().describe("UUID of the observation to delete"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ asset_id, observation_id, project }) => {
    try {
      await deleteObservation(asset_id, observation_id, project);
      return ok("Observation deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_latest_observations",
  "Get the most recent non-expired observation for each unique (category, key) pair on an operational asset. Useful for building a current-state snapshot. Expired observations (past their expiresAt timestamp) are excluded.",
  {
    asset_id: z.string().uuid().describe("UUID of the operational asset"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ asset_id, project }) => {
    try {
      const result = await listLatestObservations(asset_id, project);
      if (Array.isArray(result) && result.length === 0) return ok("No observations found.");
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Risk Scenario tools
// ==========================================================================

server.tool(
  "gc_create_risk_scenario",
  "Create a risk scenario — a scoped statement of potential future loss tied to operational assets within a defined time horizon.",
  {
    uid: z.string().max(20).describe("Risk scenario UID (e.g. 'RS-001')"),
    title: z.string().max(200).describe("Risk scenario title"),
    threat_source: z.string().describe("Threat source or actor"),
    threat_event: z.string().describe("Threat event or method"),
    affected_object: z.string().describe("Affected object, asset, boundary, process, system, objective, or third party"),
    vulnerability: z.string().optional().describe("Vulnerability, exposure, or resistance condition (when applicable)"),
    consequence: z.string().describe("Effect or consequence description"),
    time_horizon: z.string().max(100).describe("Defined time horizon (e.g. '12 months', 'Q2 2026')"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ uid, title, threat_source, threat_event, affected_object, vulnerability, consequence, time_horizon, project }) => {
    try {
      const data = { uid, title, threat_source, threat_event, affected_object, consequence, time_horizon };
      if (vulnerability !== undefined) data.vulnerability = vulnerability;
      return ok(JSON.stringify(await createRiskScenario(data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_risk_scenarios",
  "List all risk scenarios for a project, ordered by creation date (newest first).",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ project }) => {
    try {
      const scenarios = await listRiskScenarios(project);
      if (Array.isArray(scenarios) && scenarios.length === 0) return ok("No risk scenarios found.");
      return ok(JSON.stringify(scenarios, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_risk_scenario",
  "Get a risk scenario by UUID or UID.",
  {
    id: z.string().uuid().optional().describe("Risk scenario UUID"),
    uid: z.string().optional().describe("Risk scenario UID (e.g. 'RS-001')"),
    project: z.string().optional().describe("Project identifier (required when looking up by UID with multiple projects)"),
  },
  async ({ id, uid, project }) => {
    try {
      if (id) {
        return ok(JSON.stringify(await getRiskScenario(id, project), null, 2));
      }
      if (uid) {
        return ok(JSON.stringify(await getRiskScenarioByUid(uid, project), null, 2));
      }
      return err(new Error("Provide either 'id' (UUID) or 'uid'"));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_update_risk_scenario",
  "Update mutable fields of a risk scenario. Only provided fields are updated.",
  {
    id: z.string().uuid().describe("Risk scenario UUID"),
    title: z.string().max(200).optional().describe("Updated title"),
    threat_source: z.string().optional().describe("Updated threat source"),
    threat_event: z.string().optional().describe("Updated threat event"),
    affected_object: z.string().optional().describe("Updated affected object"),
    vulnerability: z.string().optional().describe("Updated vulnerability"),
    consequence: z.string().optional().describe("Updated consequence"),
    time_horizon: z.string().max(100).optional().describe("Updated time horizon"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, title, threat_source, threat_event, affected_object, vulnerability, consequence, time_horizon, project }) => {
    try {
      const data = {};
      if (title !== undefined) data.title = title;
      if (threat_source !== undefined) data.threat_source = threat_source;
      if (threat_event !== undefined) data.threat_event = threat_event;
      if (affected_object !== undefined) data.affected_object = affected_object;
      if (vulnerability !== undefined) data.vulnerability = vulnerability;
      if (consequence !== undefined) data.consequence = consequence;
      if (time_horizon !== undefined) data.time_horizon = time_horizon;
      return ok(JSON.stringify(await updateRiskScenario(id, data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_risk_scenario",
  "Delete a risk scenario.",
  {
    id: z.string().uuid().describe("Risk scenario UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, project }) => {
    try {
      await deleteRiskScenario(id, project);
      return ok("Risk scenario deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_transition_risk_scenario_status",
  "Transition a risk scenario content lifecycle status. Valid transitions: DRAFT→ACTIVE|ARCHIVED, ACTIVE→ARCHIVED.",
  {
    id: z.string().uuid().describe("Risk scenario UUID"),
    status: z.enum(RISK_SCENARIO_STATUSES).describe("Target status"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, status, project }) => {
    try {
      return ok(JSON.stringify(await transitionRiskScenarioStatus(id, status, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_risk_scenario_requirements",
  "Get requirements linked to a risk scenario.",
  {
    id: z.string().uuid().describe("Risk scenario UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, project }) => {
    try {
      const result = await getRiskScenarioRequirements(id, project);
      if (Array.isArray(result) && result.length === 0) return ok("No linked requirements found.");
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_create_risk_scenario_link",
  "Link a risk scenario to a threat model, vulnerability, control, finding, evidence, audit record, risk register entry, observation, asset, requirement, or external artifact.",
  {
    risk_scenario_id: z.string().uuid().describe("Risk scenario UUID"),
    target_type: z.enum(RISK_SCENARIO_LINK_TARGET_TYPES).describe("Type of the linked artifact"),
    target_entity_id: z.string().uuid().optional().describe("UUID of the modeled internal target entity"),
    target_identifier: z.string().max(500).optional().describe("Identifier of the linked artifact (UID, URL, or external ID)"),
    link_type: z.enum(RISK_SCENARIO_LINK_TYPES).describe("Nature of the relationship"),
    target_url: z.string().max(2000).optional().describe("URL of the linked artifact"),
    target_title: z.string().max(255).optional().describe("Human-readable title of the linked artifact"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ risk_scenario_id, target_type, target_entity_id, target_identifier, link_type, target_url, target_title, project }) => {
    try {
      const data = { target_type, link_type };
      if (target_entity_id !== undefined) data.target_entity_id = target_entity_id;
      if (target_identifier !== undefined) data.target_identifier = target_identifier;
      if (target_url !== undefined) data.target_url = target_url;
      if (target_title !== undefined) data.target_title = target_title;
      return ok(JSON.stringify(await createRiskScenarioLink(risk_scenario_id, data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_risk_scenario_links",
  "List all links from a risk scenario, optionally filtered by target type.",
  {
    risk_scenario_id: z.string().uuid().describe("Risk scenario UUID"),
    target_type: z.enum(RISK_SCENARIO_LINK_TARGET_TYPES).optional().describe("Filter by target type"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ risk_scenario_id, target_type, project }) => {
    try {
      const result = await listRiskScenarioLinks(risk_scenario_id, { targetType: target_type, project });
      if (Array.isArray(result) && result.length === 0) return ok("No links found.");
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_risk_scenario_link",
  "Delete a link from a risk scenario.",
  {
    risk_scenario_id: z.string().uuid().describe("Risk scenario UUID"),
    link_id: z.string().uuid().describe("Link UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ risk_scenario_id, link_id, project }) => {
    try {
      await deleteRiskScenarioLink(risk_scenario_id, link_id, project);
      return ok("Risk scenario link deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Control tools
// ==========================================================================

server.tool(
  "gc_create_control",
  "Create a control — a security or risk control with definitions, objectives, ownership, and methodology factor mappings.",
  {
    uid: z.string().max(50).describe("Control UID (e.g. 'CTRL-001', 'ISO-27001-A.8.1')"),
    title: z.string().max(200).describe("Control title"),
    control_function: z.enum(CONTROL_FUNCTIONS).describe("Control role: PREVENTIVE, DETECTIVE, CORRECTIVE, COMPENSATING"),
    description: z.string().optional().describe("Control description"),
    objective: z.string().optional().describe("Control objective"),
    owner: z.string().max(200).optional().describe("Control owner"),
    implementation_scope: z.string().optional().describe("Where/how the control is implemented"),
    methodology_factors: z.record(z.string(), z.unknown()).optional().describe("Methodology-aware factor mappings (e.g. FAIR-CAM strength/coverage)"),
    effectiveness: z.record(z.string(), z.unknown()).optional().describe("Effectiveness metrics"),
    category: z.string().max(100).optional().describe("Control category (e.g. Access Control, Encryption)"),
    source: z.string().max(200).optional().describe("Framework source (e.g. ISO 27001 A.9, NIST CSF PR.AC-1)"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ uid, title, control_function, description, objective, owner, implementation_scope, methodology_factors, effectiveness, category, source, project }) => {
    try {
      const data = { uid, title, control_function };
      if (description !== undefined) data.description = description;
      if (objective !== undefined) data.objective = objective;
      if (owner !== undefined) data.owner = owner;
      if (implementation_scope !== undefined) data.implementation_scope = implementation_scope;
      if (methodology_factors !== undefined) data.methodology_factors = methodology_factors;
      if (effectiveness !== undefined) data.effectiveness = effectiveness;
      if (category !== undefined) data.category = category;
      if (source !== undefined) data.source = source;
      return ok(JSON.stringify(await createControl(data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_controls",
  "List controls for a project.",
  {
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ project }) => {
    try {
      const result = await listControls(project);
      if (Array.isArray(result) && result.length === 0) return ok("No controls found.");
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_control",
  "Get a control by UUID or UID.",
  {
    id: z.string().optional().describe("Control UUID"),
    uid: z.string().optional().describe("Control UID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, uid, project }) => {
    try {
      const result = id ? await getControl(id, project) : await getControlByUid(uid, project);
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_update_control",
  "Update a control.",
  {
    id: z.string().uuid().describe("Control UUID"),
    title: z.string().max(200).optional().describe("Updated title"),
    control_function: z.enum(CONTROL_FUNCTIONS).optional().describe("Updated control function"),
    description: z.string().optional().describe("Updated description"),
    objective: z.string().optional().describe("Updated objective"),
    owner: z.string().max(200).optional().describe("Updated owner"),
    implementation_scope: z.string().optional().describe("Updated implementation scope"),
    methodology_factors: z.record(z.string(), z.unknown()).optional().describe("Updated methodology factors"),
    effectiveness: z.record(z.string(), z.unknown()).optional().describe("Updated effectiveness"),
    category: z.string().max(100).optional().describe("Updated category"),
    source: z.string().max(200).optional().describe("Updated source"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, title, control_function, description, objective, owner, implementation_scope, methodology_factors, effectiveness, category, source, project }) => {
    try {
      const data = {};
      if (title !== undefined) data.title = title;
      if (control_function !== undefined) data.control_function = control_function;
      if (description !== undefined) data.description = description;
      if (objective !== undefined) data.objective = objective;
      if (owner !== undefined) data.owner = owner;
      if (implementation_scope !== undefined) data.implementation_scope = implementation_scope;
      if (methodology_factors !== undefined) data.methodology_factors = methodology_factors;
      if (effectiveness !== undefined) data.effectiveness = effectiveness;
      if (category !== undefined) data.category = category;
      if (source !== undefined) data.source = source;
      return ok(JSON.stringify(await updateControl(id, data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_transition_control_status",
  "Transition a control's lifecycle status.",
  {
    id: z.string().uuid().describe("Control UUID"),
    status: z.enum(CONTROL_STATUSES).describe("Target status"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, status, project }) => {
    try {
      return ok(JSON.stringify(await transitionControlStatus(id, status, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_control",
  "Delete a control.",
  {
    id: z.string().uuid().describe("Control UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, project }) => {
    try {
      await deleteControl(id, project);
      return ok("Control deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_create_control_link",
  "Link a control to an asset, risk scenario, evidence, code artifact, or other entity.",
  {
    control_id: z.string().uuid().describe("Control UUID"),
    target_type: z.enum(CONTROL_LINK_TARGET_TYPES).describe("Target entity type"),
    target_entity_id: z.string().uuid().optional().describe("Target entity UUID (for internal entities)"),
    target_identifier: z.string().max(500).optional().describe("Target identifier (for external references)"),
    link_type: z.enum(CONTROL_LINK_TYPES).describe("Relationship type"),
    target_url: z.string().max(2000).optional().describe("URL to the target"),
    target_title: z.string().max(255).optional().describe("Display title for the target"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ control_id, target_type, target_entity_id, target_identifier, link_type, target_url, target_title, project }) => {
    try {
      const data = { target_type, link_type };
      if (target_entity_id !== undefined) data.target_entity_id = target_entity_id;
      if (target_identifier !== undefined) data.target_identifier = target_identifier;
      if (target_url !== undefined) data.target_url = target_url;
      if (target_title !== undefined) data.target_title = target_title;
      return ok(JSON.stringify(await createControlLink(control_id, data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_control_links",
  "List links from a control, optionally filtered by target type.",
  {
    control_id: z.string().uuid().describe("Control UUID"),
    target_type: z.enum(CONTROL_LINK_TARGET_TYPES).optional().describe("Filter by target type"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ control_id, target_type, project }) => {
    try {
      const result = await listControlLinks(control_id, { targetType: target_type, project });
      if (Array.isArray(result) && result.length === 0) return ok("No control links found.");
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_control_link",
  "Delete a control link.",
  {
    control_id: z.string().uuid().describe("Control UUID"),
    link_id: z.string().uuid().describe("Link UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ control_id, link_id, project }) => {
    try {
      await deleteControlLink(control_id, link_id, project);
      return ok("Control link deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Methodology Profile tools
// ==========================================================================

server.tool(
  "gc_create_methodology_profile",
  "Create a methodology profile that defines a risk assessment method and schema.",
  {
    profile_key: z.string().max(100).describe("Stable methodology profile key"),
    name: z.string().max(200).describe("Methodology profile name"),
    version: z.string().max(50).describe("Profile version"),
    family: z.enum(METHODOLOGY_FAMILIES).describe("Methodology family"),
    description: z.string().optional().describe("Profile description"),
    input_schema: z.record(z.string(), z.unknown()).optional().describe("Input schema metadata"),
    output_schema: z.record(z.string(), z.unknown()).optional().describe("Output schema metadata"),
    status: z.enum(METHODOLOGY_PROFILE_STATUSES).optional().describe("Lifecycle status"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ profile_key, name, version, family, description, input_schema, output_schema, status, project }) => {
    try {
      const data = { profile_key, name, version, family };
      if (description !== undefined) data.description = description;
      if (input_schema !== undefined) data.input_schema = input_schema;
      if (output_schema !== undefined) data.output_schema = output_schema;
      if (status !== undefined) data.status = status;
      return ok(JSON.stringify(await createMethodologyProfile(data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_methodology_profiles",
  "List methodology profiles for a project.",
  {
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ project }) => {
    try {
      const result = await listMethodologyProfiles(project);
      if (Array.isArray(result) && result.length === 0) return ok("No methodology profiles found.");
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_methodology_profile",
  "Get a methodology profile by UUID.",
  {
    id: z.string().uuid().describe("Methodology profile UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, project }) => {
    try {
      return ok(JSON.stringify(await getMethodologyProfile(id, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_update_methodology_profile",
  "Update a methodology profile.",
  {
    id: z.string().uuid().describe("Methodology profile UUID"),
    name: z.string().max(200).optional().describe("Updated name"),
    version: z.string().max(50).optional().describe("Updated version"),
    family: z.enum(METHODOLOGY_FAMILIES).optional().describe("Updated family"),
    description: z.string().optional().describe("Updated description"),
    input_schema: z.record(z.string(), z.unknown()).optional().describe("Updated input schema metadata"),
    output_schema: z.record(z.string(), z.unknown()).optional().describe("Updated output schema metadata"),
    status: z.enum(METHODOLOGY_PROFILE_STATUSES).optional().describe("Updated lifecycle status"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, name, version, family, description, input_schema, output_schema, status, project }) => {
    try {
      const data = {};
      if (name !== undefined) data.name = name;
      if (version !== undefined) data.version = version;
      if (family !== undefined) data.family = family;
      if (description !== undefined) data.description = description;
      if (input_schema !== undefined) data.input_schema = input_schema;
      if (output_schema !== undefined) data.output_schema = output_schema;
      if (status !== undefined) data.status = status;
      return ok(JSON.stringify(await updateMethodologyProfile(id, data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_methodology_profile",
  "Delete a methodology profile.",
  {
    id: z.string().uuid().describe("Methodology profile UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, project }) => {
    try {
      await deleteMethodologyProfile(id, project);
      return ok("Methodology profile deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Risk Register Record tools
// ==========================================================================

server.tool(
  "gc_create_risk_register_record",
  "Create a risk register record that governs one or more risk scenarios.",
  {
    uid: z.string().max(50).describe("Risk register record UID"),
    title: z.string().max(200).describe("Risk register record title"),
    owner: z.string().max(200).optional().describe("Owner"),
    review_cadence: z.string().max(100).optional().describe("Review cadence"),
    next_review_at: z.string().optional().describe("Next review timestamp"),
    category_tags: z.array(z.string()).optional().describe("Category tags"),
    decision_metadata: z.record(z.string(), z.unknown()).optional().describe("Decision metadata"),
    asset_scope_summary: z.string().optional().describe("Asset scope summary"),
    risk_scenario_ids: z.array(z.string().uuid()).optional().describe("Linked risk scenario UUIDs"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ uid, title, owner, review_cadence, next_review_at, category_tags, decision_metadata, asset_scope_summary, risk_scenario_ids, project }) => {
    try {
      const data = { uid, title };
      if (owner !== undefined) data.owner = owner;
      if (review_cadence !== undefined) data.review_cadence = review_cadence;
      if (next_review_at !== undefined) data.next_review_at = next_review_at;
      if (category_tags !== undefined) data.category_tags = category_tags;
      if (decision_metadata !== undefined) data.decision_metadata = decision_metadata;
      if (asset_scope_summary !== undefined) data.asset_scope_summary = asset_scope_summary;
      if (risk_scenario_ids !== undefined) data.risk_scenario_ids = risk_scenario_ids;
      return ok(JSON.stringify(await createRiskRegisterRecord(data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_risk_register_records",
  "List risk register records for a project.",
  {
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ project }) => {
    try {
      const result = await listRiskRegisterRecords(project);
      if (Array.isArray(result) && result.length === 0) return ok("No risk register records found.");
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_risk_register_record",
  "Get a risk register record by UUID.",
  {
    id: z.string().uuid().describe("Risk register record UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, project }) => {
    try {
      return ok(JSON.stringify(await getRiskRegisterRecord(id, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_update_risk_register_record",
  "Update a risk register record.",
  {
    id: z.string().uuid().describe("Risk register record UUID"),
    title: z.string().max(200).optional().describe("Updated title"),
    owner: z.string().max(200).optional().describe("Updated owner"),
    review_cadence: z.string().max(100).optional().describe("Updated review cadence"),
    next_review_at: z.string().optional().describe("Updated next review timestamp"),
    category_tags: z.array(z.string()).optional().describe("Updated category tags"),
    decision_metadata: z.record(z.string(), z.unknown()).optional().describe("Updated decision metadata"),
    asset_scope_summary: z.string().optional().describe("Updated asset scope summary"),
    risk_scenario_ids: z.array(z.string().uuid()).optional().describe("Updated risk scenario UUID list"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, title, owner, review_cadence, next_review_at, category_tags, decision_metadata, asset_scope_summary, risk_scenario_ids, project }) => {
    try {
      const data = {};
      if (title !== undefined) data.title = title;
      if (owner !== undefined) data.owner = owner;
      if (review_cadence !== undefined) data.review_cadence = review_cadence;
      if (next_review_at !== undefined) data.next_review_at = next_review_at;
      if (category_tags !== undefined) data.category_tags = category_tags;
      if (decision_metadata !== undefined) data.decision_metadata = decision_metadata;
      if (asset_scope_summary !== undefined) data.asset_scope_summary = asset_scope_summary;
      if (risk_scenario_ids !== undefined) data.risk_scenario_ids = risk_scenario_ids;
      return ok(JSON.stringify(await updateRiskRegisterRecord(id, data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_transition_risk_register_record_status",
  "Transition a risk register record status through the governance workflow.",
  {
    id: z.string().uuid().describe("Risk register record UUID"),
    status: z.enum(RISK_REGISTER_STATUSES).describe("Target status"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, status, project }) => {
    try {
      return ok(JSON.stringify(await transitionRiskRegisterRecordStatus(id, status, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_risk_register_record",
  "Delete a risk register record.",
  {
    id: z.string().uuid().describe("Risk register record UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, project }) => {
    try {
      await deleteRiskRegisterRecord(id, project);
      return ok("Risk register record deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Risk Assessment Result tools
// ==========================================================================

server.tool(
  "gc_create_risk_assessment_result",
  "Create a versioned risk assessment result for a risk scenario.",
  {
    risk_scenario_id: z.string().uuid().describe("Risk scenario UUID"),
    risk_register_record_id: z.string().uuid().optional().describe("Risk register record UUID"),
    methodology_profile_id: z.string().uuid().describe("Methodology profile UUID"),
    analyst_identity: z.string().optional().describe("Analyst or agent identity"),
    assumptions: z.string().optional().describe("Assessment assumptions"),
    input_factors: z.record(z.string(), z.unknown()).optional().describe("Structured input factors"),
    observation_date: z.string().optional().describe("Observation date/time"),
    assessment_at: z.string().optional().describe("Assessment timestamp"),
    time_horizon: z.string().optional().describe("Assessment time horizon"),
    confidence: z.string().optional().describe("Confidence metadata"),
    uncertainty_metadata: z.record(z.string(), z.unknown()).optional().describe("Structured uncertainty metadata"),
    computed_outputs: z.record(z.string(), z.unknown()).optional().describe("Structured computed outputs"),
    evidence_refs: z.array(z.string()).optional().describe("Evidence references"),
    notes: z.string().optional().describe("Additional notes"),
    observation_ids: z.array(z.string().uuid()).optional().describe("Observation UUIDs used in the assessment"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ risk_scenario_id, risk_register_record_id, methodology_profile_id, analyst_identity, assumptions, input_factors, observation_date, assessment_at, time_horizon, confidence, uncertainty_metadata, computed_outputs, evidence_refs, notes, observation_ids, project }) => {
    try {
      const data = { risk_scenario_id, methodology_profile_id };
      if (risk_register_record_id !== undefined) data.risk_register_record_id = risk_register_record_id;
      if (analyst_identity !== undefined) data.analyst_identity = analyst_identity;
      if (assumptions !== undefined) data.assumptions = assumptions;
      if (input_factors !== undefined) data.input_factors = input_factors;
      if (observation_date !== undefined) data.observation_date = observation_date;
      if (assessment_at !== undefined) data.assessment_at = assessment_at;
      if (time_horizon !== undefined) data.time_horizon = time_horizon;
      if (confidence !== undefined) data.confidence = confidence;
      if (uncertainty_metadata !== undefined) data.uncertainty_metadata = uncertainty_metadata;
      if (computed_outputs !== undefined) data.computed_outputs = computed_outputs;
      if (evidence_refs !== undefined) data.evidence_refs = evidence_refs;
      if (notes !== undefined) data.notes = notes;
      if (observation_ids !== undefined) data.observation_ids = observation_ids;
      return ok(JSON.stringify(await createRiskAssessmentResult(data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_risk_assessment_results",
  "List risk assessment results, optionally filtered by scenario or risk register record.",
  {
    risk_scenario_id: z.string().uuid().optional().describe("Risk scenario UUID"),
    risk_register_record_id: z.string().uuid().optional().describe("Risk register record UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ risk_scenario_id, risk_register_record_id, project }) => {
    try {
      const result = await listRiskAssessmentResults({
        riskScenarioId: risk_scenario_id,
        riskRegisterRecordId: risk_register_record_id,
        project,
      });
      if (Array.isArray(result) && result.length === 0) return ok("No risk assessment results found.");
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_risk_assessment_result",
  "Get a risk assessment result by UUID.",
  {
    id: z.string().uuid().describe("Risk assessment result UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, project }) => {
    try {
      return ok(JSON.stringify(await getRiskAssessmentResult(id, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_update_risk_assessment_result",
  "Update a risk assessment result.",
  {
    id: z.string().uuid().describe("Risk assessment result UUID"),
    risk_register_record_id: z.string().uuid().optional().describe("Updated risk register record UUID"),
    methodology_profile_id: z.string().uuid().optional().describe("Updated methodology profile UUID"),
    analyst_identity: z.string().optional().describe("Updated analyst identity"),
    assumptions: z.string().optional().describe("Updated assumptions"),
    input_factors: z.record(z.string(), z.unknown()).optional().describe("Updated input factors"),
    observation_date: z.string().optional().describe("Updated observation date"),
    assessment_at: z.string().optional().describe("Updated assessment timestamp"),
    time_horizon: z.string().optional().describe("Updated time horizon"),
    confidence: z.string().optional().describe("Updated confidence"),
    uncertainty_metadata: z.record(z.string(), z.unknown()).optional().describe("Updated uncertainty metadata"),
    computed_outputs: z.record(z.string(), z.unknown()).optional().describe("Updated computed outputs"),
    evidence_refs: z.array(z.string()).optional().describe("Updated evidence references"),
    notes: z.string().optional().describe("Updated notes"),
    observation_ids: z.array(z.string().uuid()).optional().describe("Updated observation UUIDs"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, risk_register_record_id, methodology_profile_id, analyst_identity, assumptions, input_factors, observation_date, assessment_at, time_horizon, confidence, uncertainty_metadata, computed_outputs, evidence_refs, notes, observation_ids, project }) => {
    try {
      const data = {};
      if (risk_register_record_id !== undefined) data.risk_register_record_id = risk_register_record_id;
      if (methodology_profile_id !== undefined) data.methodology_profile_id = methodology_profile_id;
      if (analyst_identity !== undefined) data.analyst_identity = analyst_identity;
      if (assumptions !== undefined) data.assumptions = assumptions;
      if (input_factors !== undefined) data.input_factors = input_factors;
      if (observation_date !== undefined) data.observation_date = observation_date;
      if (assessment_at !== undefined) data.assessment_at = assessment_at;
      if (time_horizon !== undefined) data.time_horizon = time_horizon;
      if (confidence !== undefined) data.confidence = confidence;
      if (uncertainty_metadata !== undefined) data.uncertainty_metadata = uncertainty_metadata;
      if (computed_outputs !== undefined) data.computed_outputs = computed_outputs;
      if (evidence_refs !== undefined) data.evidence_refs = evidence_refs;
      if (notes !== undefined) data.notes = notes;
      if (observation_ids !== undefined) data.observation_ids = observation_ids;
      return ok(JSON.stringify(await updateRiskAssessmentResult(id, data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_transition_risk_assessment_approval_state",
  "Transition a risk assessment result approval state.",
  {
    id: z.string().uuid().describe("Risk assessment result UUID"),
    approval_state: z.enum(RISK_ASSESSMENT_APPROVAL_STATUSES).describe("Target approval state"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, approval_state, project }) => {
    try {
      return ok(JSON.stringify(await transitionRiskAssessmentApprovalState(id, approval_state, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_risk_assessment_result",
  "Delete a risk assessment result.",
  {
    id: z.string().uuid().describe("Risk assessment result UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, project }) => {
    try {
      await deleteRiskAssessmentResult(id, project);
      return ok("Risk assessment result deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Treatment Plan tools
// ==========================================================================

server.tool(
  "gc_create_treatment_plan",
  "Create a treatment plan linked to a risk register record.",
  {
    uid: z.string().max(50).describe("Treatment plan UID"),
    title: z.string().max(200).describe("Treatment plan title"),
    risk_register_record_id: z.string().uuid().describe("Risk register record UUID"),
    risk_scenario_id: z.string().uuid().optional().describe("Optional risk scenario UUID"),
    strategy: z.enum(TREATMENT_STRATEGIES).describe("Treatment strategy"),
    owner: z.string().max(200).optional().describe("Owner"),
    rationale: z.string().optional().describe("Rationale"),
    due_date: z.string().optional().describe("Due date"),
    status: z.enum(TREATMENT_PLAN_STATUSES).optional().describe("Initial status"),
    action_items: z.array(z.record(z.string(), z.unknown())).optional().describe("Structured action items"),
    reassessment_triggers: z.array(z.string()).optional().describe("Reassessment triggers"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ uid, title, risk_register_record_id, risk_scenario_id, strategy, owner, rationale, due_date, status, action_items, reassessment_triggers, project }) => {
    try {
      const data = { uid, title, risk_register_record_id, strategy };
      if (risk_scenario_id !== undefined) data.risk_scenario_id = risk_scenario_id;
      if (owner !== undefined) data.owner = owner;
      if (rationale !== undefined) data.rationale = rationale;
      if (due_date !== undefined) data.due_date = due_date;
      if (status !== undefined) data.status = status;
      if (action_items !== undefined) data.action_items = action_items;
      if (reassessment_triggers !== undefined) data.reassessment_triggers = reassessment_triggers;
      return ok(JSON.stringify(await createTreatmentPlan(data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_treatment_plans",
  "List treatment plans, optionally filtered by risk register record.",
  {
    risk_register_record_id: z.string().uuid().optional().describe("Risk register record UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ risk_register_record_id, project }) => {
    try {
      const result = await listTreatmentPlans({ riskRegisterRecordId: risk_register_record_id, project });
      if (Array.isArray(result) && result.length === 0) return ok("No treatment plans found.");
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_treatment_plan",
  "Get a treatment plan by UUID.",
  {
    id: z.string().uuid().describe("Treatment plan UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, project }) => {
    try {
      return ok(JSON.stringify(await getTreatmentPlan(id, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_update_treatment_plan",
  "Update a treatment plan.",
  {
    id: z.string().uuid().describe("Treatment plan UUID"),
    title: z.string().max(200).optional().describe("Updated title"),
    risk_scenario_id: z.string().uuid().optional().describe("Updated risk scenario UUID"),
    strategy: z.enum(TREATMENT_STRATEGIES).optional().describe("Updated strategy"),
    owner: z.string().max(200).optional().describe("Updated owner"),
    rationale: z.string().optional().describe("Updated rationale"),
    due_date: z.string().optional().describe("Updated due date"),
    action_items: z.array(z.record(z.string(), z.unknown())).optional().describe("Updated action items"),
    reassessment_triggers: z.array(z.string()).optional().describe("Updated reassessment triggers"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, title, risk_scenario_id, strategy, owner, rationale, due_date, action_items, reassessment_triggers, project }) => {
    try {
      const data = {};
      if (title !== undefined) data.title = title;
      if (risk_scenario_id !== undefined) data.risk_scenario_id = risk_scenario_id;
      if (strategy !== undefined) data.strategy = strategy;
      if (owner !== undefined) data.owner = owner;
      if (rationale !== undefined) data.rationale = rationale;
      if (due_date !== undefined) data.due_date = due_date;
      if (action_items !== undefined) data.action_items = action_items;
      if (reassessment_triggers !== undefined) data.reassessment_triggers = reassessment_triggers;
      return ok(JSON.stringify(await updateTreatmentPlan(id, data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_transition_treatment_plan_status",
  "Transition a treatment plan status.",
  {
    id: z.string().uuid().describe("Treatment plan UUID"),
    status: z.enum(TREATMENT_PLAN_STATUSES).describe("Target status"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, status, project }) => {
    try {
      return ok(JSON.stringify(await transitionTreatmentPlanStatus(id, status, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_treatment_plan",
  "Delete a treatment plan.",
  {
    id: z.string().uuid().describe("Treatment plan UUID"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ id, project }) => {
    try {
      await deleteTreatmentPlan(id, project);
      return ok("Treatment plan deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Verification Results
// ==========================================================================

server.tool(
  "gc_create_verification_result",
  "Store a verification result from any prover or verifier.",
  {
    target_id: z.string().uuid().optional().describe("Traceability link UUID (what was verified)"),
    requirement_id: z.string().uuid().optional().describe("Requirement UUID driving the verification"),
    prover: z.string().max(100).describe("Verifier tool identifier (e.g. 'openjml-esc', 'tlaplus-tlc', 'opa', 'manual-review')"),
    property: z.string().optional().describe("The formal property checked (human-readable or formal notation)"),
    result: z.enum(VERIFICATION_STATUSES).describe("Verification outcome"),
    assurance_level: z.enum(ASSURANCE_LEVELS).describe("Assurance level (L0-L3)"),
    evidence: z.record(z.unknown()).optional().describe("Prover-specific output (proof artifacts, counterexamples, logs)"),
    verified_at: z.string().datetime().describe("ISO 8601 timestamp of when the verification ran"),
    expires_at: z.string().datetime().optional().describe("ISO 8601 timestamp for re-verification trigger"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ target_id, requirement_id, prover, property, result, assurance_level, evidence, verified_at, expires_at, project }) => {
    try {
      const data = { prover, result, assurance_level, verified_at };
      if (target_id !== undefined) data.target_id = target_id;
      if (requirement_id !== undefined) data.requirement_id = requirement_id;
      if (property !== undefined) data.property = property;
      if (evidence !== undefined) data.evidence = evidence;
      if (expires_at !== undefined) data.expires_at = expires_at;
      return ok(JSON.stringify(await createVerificationResult(data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_verification_results",
  "List verification results with optional filters.",
  {
    requirement_id: z.string().uuid().optional().describe("Filter by requirement UUID"),
    prover: z.string().optional().describe("Filter by prover identifier"),
    result: z.enum(VERIFICATION_STATUSES).optional().describe("Filter by verification outcome"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ requirement_id, prover, result, project }) => {
    try {
      return ok(JSON.stringify(await listVerificationResults({ requirementId: requirement_id, prover, result, project }), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_verification_result",
  "Get a verification result by UUID.",
  {
    id: z.string().uuid().describe("Verification result UUID"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ id, project }) => {
    try {
      return ok(JSON.stringify(await getVerificationResult(id, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_update_verification_result",
  "Update a verification result.",
  {
    id: z.string().uuid().describe("Verification result UUID"),
    target_id: z.string().uuid().optional().describe("Traceability link UUID"),
    requirement_id: z.string().uuid().optional().describe("Requirement UUID"),
    prover: z.string().max(100).optional().describe("Verifier tool identifier"),
    property: z.string().optional().describe("The formal property checked"),
    result: z.enum(VERIFICATION_STATUSES).optional().describe("Verification outcome"),
    assurance_level: z.enum(ASSURANCE_LEVELS).optional().describe("Assurance level (L0-L3)"),
    evidence: z.record(z.unknown()).optional().describe("Prover-specific output"),
    verified_at: z.string().datetime().optional().describe("ISO 8601 timestamp of when the verification ran"),
    expires_at: z.string().datetime().optional().describe("ISO 8601 timestamp for re-verification trigger"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ id, target_id, requirement_id, prover, property, result, assurance_level, evidence, verified_at, expires_at, project }) => {
    try {
      const data = {};
      if (target_id !== undefined) data.target_id = target_id;
      if (requirement_id !== undefined) data.requirement_id = requirement_id;
      if (prover !== undefined) data.prover = prover;
      if (property !== undefined) data.property = property;
      if (result !== undefined) data.result = result;
      if (assurance_level !== undefined) data.assurance_level = assurance_level;
      if (evidence !== undefined) data.evidence = evidence;
      if (verified_at !== undefined) data.verified_at = verified_at;
      if (expires_at !== undefined) data.expires_at = expires_at;
      return ok(JSON.stringify(await updateVerificationResult(id, data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_verification_result",
  "Delete a verification result.",
  {
    id: z.string().uuid().describe("Verification result UUID"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ id, project }) => {
    try {
      await deleteVerificationResult(id, project);
      return ok("Verification result deleted.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Plugins (GC-P005)
// ==========================================================================

server.tool(
  "gc_list_plugins",
  "List all registered plugins with their metadata, type, state, and capabilities. Returns both built-in (classpath) and dynamic (DB-persisted) plugins.",
  {
    type: z.enum(PLUGIN_TYPES).optional().describe("Filter by plugin type"),
    capability: z.string().optional().describe("Filter by capability tag"),
    project: z.string().optional().describe("Project identifier to scope dynamic plugins"),
  },
  async ({ type, capability, project }) => {
    try {
      return ok(JSON.stringify(await listPlugins({ type, capability, project }), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_plugin",
  "Get a registered plugin by its canonical name.",
  {
    name: z.string().describe("The plugin's canonical name"),
  },
  async ({ name }) => {
    try {
      return ok(JSON.stringify(await getPlugin(name), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_register_plugin",
  "Register a dynamic plugin. Persisted to the database and survives restarts.",
  {
    name: z.string().max(100).describe("Unique plugin name"),
    version: z.string().max(50).describe("Semantic version"),
    type: z.enum(PLUGIN_TYPES).describe("Plugin type/category"),
    description: z.string().optional().describe("Human-readable description"),
    capabilities: z.array(z.string()).optional().describe("Capability tags"),
    metadata: z.record(z.unknown()).optional().describe("Plugin-specific metadata"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ name, version, type, description, capabilities, metadata, project }) => {
    try {
      const data = { name, version, type };
      if (description !== undefined) data.description = description;
      if (capabilities !== undefined) data.capabilities = capabilities;
      if (metadata !== undefined) data.metadata = metadata;
      return ok(JSON.stringify(await registerPlugin(data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_unregister_plugin",
  "Unregister a dynamic plugin. Removes it from the database.",
  {
    name: z.string().describe("The plugin's canonical name"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ name, project }) => {
    try {
      await unregisterPlugin(name, project);
      return ok("Plugin unregistered.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Control Pack tools
// ==========================================================================

server.tool(
  "gc_install_control_pack",
  "Install a versioned control pack into a project. Creates control records, framework mapping links, and pack entries idempotently.",
  {
    pack_id: z.string().max(200).describe("Stable pack identity (e.g. 'nist-sp800-53-rev5', 'iso-27001-annex-a')"),
    version: z.string().max(50).describe("Semantic version of the pack (e.g. '1.0.0')"),
    publisher: z.string().max(200).optional().describe("Organization or author who published the pack"),
    description: z.string().optional().describe("Human-readable pack description"),
    source_url: z.string().max(2000).optional().describe("Origin URL (registry, git repo, etc.)"),
    checksum: z.string().max(128).optional().describe("SHA-256 content hash for integrity verification"),
    compatibility: z.record(z.string(), z.unknown()).optional().describe("Compatibility constraints (e.g. min GC version)"),
    pack_metadata: z.record(z.string(), z.unknown()).optional().describe("Arbitrary pack-level metadata"),
    entries: z.array(z.object({
      uid: z.string().max(50).describe("Control UID within the pack"),
      title: z.string().max(200).describe("Control title"),
      control_function: z.enum(CONTROL_FUNCTIONS).describe("Control function"),
      description: z.string().optional().describe("Control description"),
      objective: z.string().optional().describe("Control objective"),
      owner: z.string().max(200).optional().describe("Control owner"),
      implementation_scope: z.string().optional().describe("Implementation scope"),
      methodology_factors: z.record(z.string(), z.unknown()).optional().describe("Methodology factor mappings"),
      effectiveness: z.record(z.string(), z.unknown()).optional().describe("Effectiveness metrics"),
      category: z.string().max(100).optional().describe("Control category"),
      source: z.string().max(200).optional().describe("Framework source"),
      implementation_guidance: z.string().optional().describe("How to implement this control"),
      expected_evidence: z.array(z.record(z.string(), z.unknown())).optional().describe("Evidence pattern templates (not real evidence)"),
      framework_mappings: z.array(z.object({
        framework: z.string().optional().describe("Framework name (e.g. 'NIST CSF')"),
        identifier: z.string().describe("Framework control identifier (e.g. 'PR.AC-1')"),
        title: z.string().optional().describe("Mapping title"),
        url: z.string().optional().describe("Reference URL"),
      })).optional().describe("Framework mappings (materialized as MAPS_TO control links)"),
    })).min(1).describe("Control definitions to install"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ pack_id, version, publisher, description, source_url, checksum, compatibility, pack_metadata, entries, project }) => {
    try {
      const body = {
        packId: pack_id,
        version,
        entries: entries.map(e => ({
          uid: e.uid,
          title: e.title,
          controlFunction: e.control_function,
          description: e.description,
          objective: e.objective,
          owner: e.owner,
          implementationScope: e.implementation_scope,
          methodologyFactors: e.methodology_factors,
          effectiveness: e.effectiveness,
          category: e.category,
          source: e.source,
          implementationGuidance: e.implementation_guidance,
          expectedEvidence: e.expected_evidence,
          frameworkMappings: e.framework_mappings,
        })),
      };
      if (publisher !== undefined) body.publisher = publisher;
      if (description !== undefined) body.description = description;
      if (source_url !== undefined) body.sourceUrl = source_url;
      if (checksum !== undefined) body.checksum = checksum;
      if (compatibility !== undefined) body.compatibility = compatibility;
      if (pack_metadata !== undefined) body.packMetadata = pack_metadata;
      return ok(JSON.stringify(await installControlPack(body, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_upgrade_control_pack",
  "Upgrade an installed control pack to a new version. Applies upstream changes to non-overridden fields and preserves local tailoring.",
  {
    pack_id: z.string().max(200).describe("Pack identity to upgrade"),
    new_version: z.string().max(50).describe("New semantic version"),
    publisher: z.string().max(200).optional().describe("Publisher"),
    description: z.string().optional().describe("Updated description"),
    source_url: z.string().max(2000).optional().describe("Updated origin URL"),
    checksum: z.string().max(128).optional().describe("Updated content hash"),
    compatibility: z.record(z.string(), z.unknown()).optional().describe("Updated compatibility constraints"),
    pack_metadata: z.record(z.string(), z.unknown()).optional().describe("Updated pack metadata"),
    entries: z.array(z.object({
      uid: z.string().max(50).describe("Control UID within the pack"),
      title: z.string().max(200).describe("Control title"),
      control_function: z.enum(CONTROL_FUNCTIONS).describe("Control function"),
      description: z.string().optional().describe("Control description"),
      objective: z.string().optional().describe("Control objective"),
      owner: z.string().max(200).optional().describe("Control owner"),
      implementation_scope: z.string().optional().describe("Implementation scope"),
      methodology_factors: z.record(z.string(), z.unknown()).optional().describe("Methodology factor mappings"),
      effectiveness: z.record(z.string(), z.unknown()).optional().describe("Effectiveness metrics"),
      category: z.string().max(100).optional().describe("Control category"),
      source: z.string().max(200).optional().describe("Framework source"),
      implementation_guidance: z.string().optional().describe("Implementation guidance"),
      expected_evidence: z.array(z.record(z.string(), z.unknown())).optional().describe("Evidence pattern templates"),
      framework_mappings: z.array(z.object({
        framework: z.string().optional().describe("Framework name"),
        identifier: z.string().describe("Framework control identifier"),
        title: z.string().optional().describe("Mapping title"),
        url: z.string().optional().describe("Reference URL"),
      })).optional().describe("Framework mappings"),
    })).min(1).describe("Updated control definitions"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ pack_id, new_version, publisher, description, source_url, checksum, compatibility, pack_metadata, entries, project }) => {
    try {
      const body = {
        packId: pack_id,
        newVersion: new_version,
        entries: entries.map(e => ({
          uid: e.uid,
          title: e.title,
          controlFunction: e.control_function,
          description: e.description,
          objective: e.objective,
          owner: e.owner,
          implementationScope: e.implementation_scope,
          methodologyFactors: e.methodology_factors,
          effectiveness: e.effectiveness,
          category: e.category,
          source: e.source,
          implementationGuidance: e.implementation_guidance,
          expectedEvidence: e.expected_evidence,
          frameworkMappings: e.framework_mappings,
        })),
      };
      if (publisher !== undefined) body.publisher = publisher;
      if (description !== undefined) body.description = description;
      if (source_url !== undefined) body.sourceUrl = source_url;
      if (checksum !== undefined) body.checksum = checksum;
      if (compatibility !== undefined) body.compatibility = compatibility;
      if (pack_metadata !== undefined) body.packMetadata = pack_metadata;
      return ok(JSON.stringify(await upgradeControlPack(body, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_control_packs",
  "List installed control packs for a project.",
  {
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ project }) => {
    try {
      const result = await listControlPacks(project);
      if (Array.isArray(result) && result.length === 0) return ok("No control packs found.");
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_control_pack",
  "Get a control pack by its pack identifier.",
  {
    pack_id: z.string().describe("Pack identity (e.g. 'nist-sp800-53-rev5')"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ pack_id, project }) => {
    try {
      return ok(JSON.stringify(await getControlPack(pack_id, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_deprecate_control_pack",
  "Deprecate an installed control pack.",
  {
    pack_id: z.string().describe("Pack identity to deprecate"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ pack_id, project }) => {
    try {
      return ok(JSON.stringify(await deprecateControlPack(pack_id, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_remove_control_pack",
  "Remove an installed control pack (terminal state, irreversible). Controls remain but provenance link is severed.",
  {
    pack_id: z.string().describe("Pack identity to remove"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ pack_id, project }) => {
    try {
      await removeControlPack(pack_id, project);
      return ok("Control pack removed.");
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_control_pack_entries",
  "List entries (control definitions) within an installed control pack.",
  {
    pack_id: z.string().describe("Pack identity"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ pack_id, project }) => {
    try {
      const result = await listControlPackEntries(pack_id, project);
      if (Array.isArray(result) && result.length === 0) return ok("No entries found.");
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_get_control_pack_entry",
  "Get a specific entry from a control pack by its entry UID.",
  {
    pack_id: z.string().describe("Pack identity"),
    entry_uid: z.string().describe("Entry UID within the pack"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ pack_id, entry_uid, project }) => {
    try {
      return ok(JSON.stringify(await getControlPackEntry(pack_id, entry_uid, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_create_control_pack_override",
  "Create or update a field-level override on a pack entry for local tailoring.",
  {
    pack_id: z.string().describe("Pack identity"),
    entry_uid: z.string().describe("Entry UID within the pack"),
    field_name: z.string().max(100).describe("Field to override (title, description, objective, controlFunction, owner, implementationScope, category)"),
    override_value: z.string().optional().describe("Override value"),
    reason: z.string().max(500).optional().describe("Reason for the override"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ pack_id, entry_uid, field_name, override_value, reason, project }) => {
    try {
      const data = { fieldName: field_name };
      if (override_value !== undefined) data.overrideValue = override_value;
      if (reason !== undefined) data.reason = reason;
      return ok(JSON.stringify(await createControlPackOverride(pack_id, entry_uid, data, project), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_control_pack_overrides",
  "List field-level overrides for a pack entry.",
  {
    pack_id: z.string().describe("Pack identity"),
    entry_uid: z.string().describe("Entry UID within the pack"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ pack_id, entry_uid, project }) => {
    try {
      const result = await listControlPackOverrides(pack_id, entry_uid, project);
      if (Array.isArray(result) && result.length === 0) return ok("No overrides found.");
      return ok(JSON.stringify(result, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_delete_control_pack_override",
  "Delete a field-level override, restoring the original pack value on the materialized control.",
  {
    pack_id: z.string().describe("Pack identity"),
    entry_uid: z.string().describe("Entry UID within the pack"),
    override_id: z.string().uuid().describe("Override UUID to delete"),
    project: z.string().optional().describe("Project identifier"),
  },
  async ({ pack_id, entry_uid, override_id, project }) => {
    try {
      await deleteControlPackOverride(pack_id, entry_uid, override_id, project);
      return ok("Override deleted and original value restored.");
    } catch (e) {
      return err(e);
    }
  },
);

// ==========================================================================
// Start
// ==========================================================================

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
