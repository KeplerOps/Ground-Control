#!/usr/bin/env node

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
  getRequirementHistory,
  getRelationHistory,
  getTraceabilityLinkHistory,
  getRequirementTimeline,
  getRequirementDiff,
  getProjectTimeline,
  exportAuditTimeline,
  deleteRelation,
  deleteTraceabilityLink,
  materializeGraph,
  getAncestors,
  getDescendants,
  findPaths,
  getGraphVisualization,
  extractSubgraph,
  createGitHubIssue,
  formatIssueBody,
  runSweep,
  runSweepAll,
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
  STATUSES,
  REQUIREMENT_TYPES,
  PRIORITIES,
  RELATION_TYPES,
  ARTIFACT_TYPES,
  LINK_TYPES,
  METRIC_TYPES,
  COMPARISON_OPERATORS,
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
    repo: z.string().optional().describe("GitHub repo as 'owner/repo' (defaults to GH_REPO env var)"),
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
  "Materialize the requirements dependency graph in Apache AGE. Run this after bulk changes to relations.",
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
  "Get the full graph visualization data (all requirement nodes and relation edges with metadata) for a project. Returns data suitable for rendering dependency diagrams.",
  {
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ project }) => {
    try {
      const data = await getGraphVisualization(project);
      return ok(JSON.stringify(data, null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_extract_subgraph",
  "Extract a subgraph starting from one or more root requirements. Returns all transitively reachable requirements and their relations as a self-contained graph.",
  {
    roots: z.array(z.string()).describe("Root requirement UIDs to start traversal from (e.g. ['GC-G001', 'GC-G002'])"),
    project: z.string().optional().describe("Project identifier (auto-resolved if only one project exists)"),
  },
  async ({ roots, project }) => {
    try {
      const data = await extractSubgraph(roots, project);
      return ok(JSON.stringify(data, null, 2));
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
  "Import requirements from a StrictDoc (.sdoc) file. Idempotent: re-importing updates existing requirements by UID.",
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
    owner: z.string().describe("GitHub repository owner"),
    repo: z.string().describe("GitHub repository name"),
  },
  async ({ owner, repo }) => {
    try {
      return ok(JSON.stringify(await syncGithub(owner, repo), null, 2));
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
