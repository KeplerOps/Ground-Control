#!/usr/bin/env node

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import {
  getRequirementByUid,
  listRequirements,
  createRequirement,
  updateRequirement,
  transitionStatus,
  bulkTransitionStatus,
  archiveRequirement,
  createRelation,
  getRelations,
  getTraceabilityLinks,
  createTraceabilityLink,
  detectCycles,
  findOrphans,
  findCoverageGaps,
  impactAnalysis,
  crossWaveValidation,
  importStrictdoc,
  syncGithub,
  formatIssueBody,
  createGitHubIssue,
  STATUSES,
  REQUIREMENT_TYPES,
  PRIORITIES,
  RELATION_TYPES,
  ARTIFACT_TYPES,
  LINK_TYPES,
} from "./lib.js";

function ok(text) {
  return { content: [{ type: "text", text }] };
}

function err(e) {
  return { content: [{ type: "text", text: `Error: ${e.message}` }], isError: true };
}

const server = new McpServer({ name: "ground-control", version: "1.0.0" });

// ==========================================================================
// Requirement tools
// ==========================================================================

server.tool(
  "gc_get_requirement",
  "Get a requirement by its human-readable UID (e.g. 'REQ-001').",
  {
    uid: z.string().describe("Requirement UID (e.g. 'REQ-001')"),
  },
  async ({ uid }) => {
    try {
      return ok(JSON.stringify(await getRequirementByUid(uid), null, 2));
    } catch (e) {
      return err(e);
    }
  },
);

server.tool(
  "gc_list_requirements",
  "List requirements with optional filtering by status, type, wave, or free-text search. Returns paginated results.",
  {
    status: z.enum(STATUSES).optional().describe("Filter by status"),
    type: z.enum(REQUIREMENT_TYPES).optional().describe("Filter by requirement type"),
    wave: z.number().int().optional().describe("Filter by wave number"),
    search: z.string().optional().describe("Free-text search in title and statement"),
    page: z.number().int().optional().describe("Page number (0-based)"),
    size: z.number().int().optional().describe("Page size (default 20)"),
  },
  async ({ status, type, wave, search, page, size }) => {
    try {
      return ok(JSON.stringify(await listRequirements({ status, type, wave, search, page, size }), null, 2));
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
  },
  async ({ uid, title, statement, rationale, requirement_type, priority, wave }) => {
    try {
      const data = { uid, title, statement };
      if (rationale !== undefined) data.rationale = rationale;
      if (requirement_type !== undefined) data.requirement_type = requirement_type;
      if (priority !== undefined) data.priority = priority;
      if (wave !== undefined) data.wave = wave;
      return ok(JSON.stringify(await createRequirement(data), null, 2));
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
    uid: z.string().optional().describe("New UID"),
    title: z.string().optional().describe("New title"),
    statement: z.string().optional().describe("New statement"),
    rationale: z.string().optional().describe("New rationale"),
    requirement_type: z.enum(REQUIREMENT_TYPES).optional().describe("New type"),
    priority: z.enum(PRIORITIES).optional().describe("New MoSCoW priority"),
    wave: z.number().int().optional().describe("New wave number"),
  },
  async ({ id, uid, title, statement, rationale, requirement_type, priority, wave }) => {
    try {
      const data = {};
      if (uid !== undefined) data.uid = uid;
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
  },
  async ({ id, status }) => {
    try {
      return ok(JSON.stringify(await transitionStatus(id, status), null, 2));
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
  },
  async ({ uids, status }) => {
    try {
      const ids = [];
      const resolutionFailures = [];
      for (const uid of uids) {
        try {
          const req = await getRequirementByUid(uid);
          ids.push(req.id);
        } catch (e) {
          resolutionFailures.push({ id: uid, error: `UID resolution failed: ${e.message}` });
        }
      }
      if (ids.length === 0) {
        return ok(JSON.stringify({ succeeded: [], failed: resolutionFailures, total_requested: uids.length, total_succeeded: 0, total_failed: resolutionFailures.length }, null, 2));
      }
      const result = await bulkTransitionStatus(ids, status);
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

// ==========================================================================
// GitHub integration tools
// ==========================================================================

server.tool(
  "gc_create_github_issue",
  "Create a GitHub issue from a requirement and auto-link it via traceability. Shells out to `gh` CLI.",
  {
    uid: z.string().describe("Requirement UID (e.g. 'GC-D007')"),
    extra_body: z.string().optional().describe("Additional markdown to append to the issue body"),
    labels: z.array(z.string()).optional().describe("GitHub labels to apply"),
    repo: z.string().optional().describe("GitHub repo as 'owner/repo' (defaults to GH_REPO env var)"),
  },
  async ({ uid, extra_body, labels, repo }) => {
    try {
      const req = await getRequirementByUid(uid);
      const title = `${req.uid}: ${req.title}`;
      const body = formatIssueBody(req, extra_body);
      const { url, number } = await createGitHubIssue({ title, body, labels, repo });

      const result = { url, number };

      try {
        const link = await createTraceabilityLink(req.id, {
          artifact_type: "GITHUB_ISSUE",
          artifact_identifier: `#${number}`,
          link_type: "IMPLEMENTS",
          artifact_url: url,
          artifact_title: title,
        });
        result.traceability_link = link;
      } catch (linkErr) {
        result.warning = `Issue created but traceability link failed: ${linkErr.message}`;
      }

      return ok(JSON.stringify(result, null, 2));
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
  {},
  async () => {
    try {
      const cycles = await detectCycles();
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
  {},
  async () => {
    try {
      const orphans = await findOrphans();
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
  },
  async ({ link_type }) => {
    try {
      const gaps = await findCoverageGaps(link_type);
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
  {},
  async () => {
    try {
      const violations = await crossWaveValidation();
      if (Array.isArray(violations) && violations.length === 0) return ok("No cross-wave violations found.");
      return ok(JSON.stringify(violations, null, 2));
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
  },
  async ({ file_path }) => {
    try {
      return ok(JSON.stringify(await importStrictdoc(file_path), null, 2));
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
