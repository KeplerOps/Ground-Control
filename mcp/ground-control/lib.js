import { mkdtempSync, readFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { basename, isAbsolute, join } from "node:path";
import { execFile as execFileCb } from "node:child_process";
import { promisify } from "node:util";
import { load as parseYaml } from "js-yaml";

const execFile = promisify(execFileCb);
const GROUND_CONTROL_PROJECT_RE = /^[a-z0-9][a-z0-9-]*$/;

function formatCommandFailure(command, error) {
  const details = [];
  if (error.code === "ENOENT") {
    details.push(`${command} is not installed or not available on PATH`);
  } else if (error.message) {
    details.push(error.message);
  }

  const stderr = error.stderr?.trim();
  const stdout = error.stdout?.trim();
  if (stderr) {
    details.push(`stderr: ${stderr}`);
  } else if (stdout) {
    details.push(`stdout: ${stdout}`);
  }

  return details.join(" | ");
}

export function buildGroundControlContextSnippet(project = "your-project-id") {
  return [
    "## Ground Control Context",
    "",
    "```yaml",
    "ground_control:",
    `  project: ${project}`,
    "```",
  ].join("\n");
}

async function execFileWithInput(file, args, { input, ...options } = {}) {
  return await new Promise((resolve, reject) => {
    const child = execFileCb(file, args, options, (error, stdout, stderr) => {
      if (error) {
        error.stdout = stdout;
        error.stderr = stderr;
        reject(error);
        return;
      }

      resolve({ stdout, stderr });
    });

    if (input != null) {
      child.stdin.end(input);
    }
  });
}

// ---------------------------------------------------------------------------
// Constants (matching Java enums)
// ---------------------------------------------------------------------------

export const STATUSES = ["DRAFT", "ACTIVE", "DEPRECATED", "ARCHIVED"];
export const REQUIREMENT_TYPES = ["FUNCTIONAL", "NON_FUNCTIONAL", "CONSTRAINT", "INTERFACE"];
export const PRIORITIES = ["MUST", "SHOULD", "COULD", "WONT"];
export const RELATION_TYPES = ["PARENT", "DEPENDS_ON", "CONFLICTS_WITH", "REFINES", "SUPERSEDES", "RELATED"];
export const ARTIFACT_TYPES = [
  "GITHUB_ISSUE",
  "PULL_REQUEST",
  "CODE_FILE",
  "ADR",
  "CONFIG",
  "POLICY",
  "TEST",
  "SPEC",
  "PROOF",
  "DOCUMENTATION",
  "RISK_SCENARIO",
  "CONTROL",
];
export const LINK_TYPES = ["IMPLEMENTS", "TESTS", "DOCUMENTS", "CONSTRAINS", "VERIFIES"];
export const METRIC_TYPES = ["COVERAGE", "ORPHAN_COUNT", "COMPLETENESS"];
export const COMPARISON_OPERATORS = ["GTE", "LTE", "EQ", "GT", "LT"];
export const ADR_STATUSES = ["PROPOSED", "ACCEPTED", "DEPRECATED", "SUPERSEDED"];
export const ASSET_TYPES = [
  "APPLICATION",
  "SERVICE",
  "SYSTEM",
  "DATABASE",
  "NETWORK",
  "HOST",
  "CONTAINER",
  "IDENTITY",
  "DATA_STORE",
  "ENDPOINT",
  "INTEGRATION",
  "WORKLOAD",
  "THIRD_PARTY",
  "BOUNDARY",
  "OTHER",
];
export const ASSET_RELATION_TYPES = [
  "CONTAINS",
  "DEPENDS_ON",
  "COMMUNICATES_WITH",
  "TRUST_BOUNDARY",
  "SUPPORTS",
  "ACCESSES",
  "DATA_FLOW",
];
export const ASSET_LINK_TARGET_TYPES = [
  "REQUIREMENT",
  "CONTROL",
  "RISK_SCENARIO",
  "RISK_REGISTER_RECORD",
  "RISK_ASSESSMENT_RESULT",
  "TREATMENT_PLAN",
  "METHODOLOGY_PROFILE",
  "THREAT_MODEL_ENTRY",
  "FINDING",
  "EVIDENCE",
  "AUDIT",
  "EXTERNAL",
];
export const ASSET_LINK_TYPES = [
  "IMPLEMENTS",
  "MITIGATES",
  "SUBJECT_OF",
  "EVIDENCED_BY",
  "GOVERNED_BY",
  "DEPENDS_ON",
  "ASSOCIATED",
];
export const OBSERVATION_CATEGORIES = [
  "CONFIGURATION",
  "EXPOSURE",
  "IDENTITY",
  "DEPLOYMENT",
  "PATCH_STATE",
  "RELATIONSHIP",
  "OTHER",
];
export const RISK_SCENARIO_STATUSES = ["DRAFT", "ACTIVE", "ARCHIVED"];
export const METHODOLOGY_FAMILIES = ["FAIR", "NIST_SP800_30_R1", "ISO_27005", "CUSTOM"];
export const METHODOLOGY_PROFILE_STATUSES = ["ACTIVE", "DEPRECATED"];
export const RISK_REGISTER_STATUSES = [
  "IDENTIFIED",
  "ANALYZING",
  "ASSESSED",
  "TREATING",
  "MONITORING",
  "ACCEPTED",
  "CLOSED",
];
export const RISK_ASSESSMENT_APPROVAL_STATUSES = ["DRAFT", "SUBMITTED", "APPROVED", "REJECTED"];
export const TREATMENT_PLAN_STATUSES = ["PLANNED", "IN_PROGRESS", "BLOCKED", "COMPLETED", "CANCELED"];
export const TREATMENT_STRATEGIES = ["MITIGATE", "ACCEPT", "TRANSFER", "SHARE", "AVOID", "OTHER"];
export const RISK_SCENARIO_LINK_TARGET_TYPES = [
  "THREAT_MODEL",
  "VULNERABILITY",
  "CONTROL",
  "FINDING",
  "EVIDENCE",
  "AUDIT_RECORD",
  "RISK_REGISTER_RECORD",
  "RISK_ASSESSMENT_RESULT",
  "TREATMENT_PLAN",
  "METHODOLOGY_PROFILE",
  "OBSERVATION",
  "ASSET",
  "REQUIREMENT",
  "EXTERNAL",
];
export const RISK_SCENARIO_LINK_TYPES = [
  "MITIGATED_BY",
  "EXPLOITS",
  "AFFECTS",
  "EVIDENCED_BY",
  "GOVERNED_BY",
  "ASSESSED_IN",
  "REGISTERED_IN",
  "OBSERVED_IN",
  "ASSOCIATED",
];

// ---------------------------------------------------------------------------
// Field name mapping (snake_case MCP <-> camelCase API)
// ---------------------------------------------------------------------------

const TO_CAMEL = {
  requirement_type: "requirementType",
  artifact_type: "artifactType",
  artifact_identifier: "artifactIdentifier",
  artifact_url: "artifactUrl",
  artifact_title: "artifactTitle",
  link_type: "linkType",
  relation_type: "relationType",
  target_id: "targetId",
  new_uid: "newUid",
  copy_relations: "copyRelations",
  revision_number: "revisionNumber",
  revision_type: "revisionType",
  sync_status: "syncStatus",
  last_synced_at: "lastSyncedAt",
  source_uid: "sourceUid",
  target_uid: "targetUid",
  source_wave: "sourceWave",
  target_wave: "targetWave",
  requirement_uid: "requirementUid",
  extra_body: "extraBody",
  project_identifier: "projectIdentifier",
  blocking_status: "blockingStatus",
  blocked_by: "blockedBy",
  content_hash: "contentHash",
  model_id: "modelId",
  has_embedding: "hasEmbedding",
  is_stale: "isStale",
  model_mismatch: "modelMismatch",
  current_model_id: "currentModelId",
  embedding_model_id: "embeddingModelId",
  embedded_at: "embeddedAt",
  pairs_analyzed: "pairsAnalyzed",
  embedded_count: "embeddedCount",
  similarity_threshold: "similarityThreshold",
  total_unblocked: "totalUnblocked",
  total_blocked: "totalBlocked",
  total_unconstrained: "totalUnconstrained",
  total_requirements: "totalRequirements",
  created_by: "createdBy",
  baseline_id: "baselineId",
  baseline_name: "baselineName",
  other_baseline_id: "otherBaselineId",
  other_baseline_name: "otherBaselineName",
  added_count: "addedCount",
  removed_count: "removedCount",
  modified_count: "modifiedCount",
  requirement_count: "requirementCount",
  requirement_id: "requirementId",
  from_revision: "fromRevision",
  to_revision: "toRevision",
  field_changes: "fieldChanges",
  relation_changes: "relationChanges",
  traceability_link_changes: "traceabilityLinkChanges",
  change_type: "changeType",
  old_value: "oldValue",
  new_value: "newValue",
  relation_id: "relationId",
  link_id: "linkId",
  metric_type: "metricType",
  metric_param: "metricParam",
  scope_status: "scopeStatus",
  total_gates: "totalGates",
  passed_count: "passedCount",
  failed_count: "failedCount",
  gate_id: "gateId",
  gate_name: "gateName",
  actual_value: "actualValue",
  created_by: "createdBy",
  document_id: "documentId",
  parent_id: "parentId",
  sort_order: "sortOrder",
  section_id: "sectionId",
  content_type: "contentType",
  text_content: "textContent",
  entity_type: "entityType",
  entity_types: "entityTypes",
  edge_type: "edgeType",
  domain_id: "domainId",
  graph_node_id: "graphNodeId",
  requirement_uid: "requirementUid",
  decision_date: "decisionDate",
  superseded_by: "supersededBy",
  asset_type: "assetType",
  asset_id: "assetId",
  asset_uid: "assetUid",
  archived_at: "archivedAt",
  member_uids: "memberUids",
  root_uids: "rootUids",
  target_type: "targetType",
  target_entity_id: "targetEntityId",
  target_identifier: "targetIdentifier",
  target_url: "targetUrl",
  target_title: "targetTitle",
  source_system: "sourceSystem",
  source_id: "sourceId",
  collected_at: "collectedAt",
  external_source_id: "externalSourceId",
  external_id_id: "externalIdId",
  observation_key: "observationKey",
  observation_value: "observationValue",
  observed_at: "observedAt",
  expires_at: "expiresAt",
  verified_at: "verifiedAt",
  assurance_level: "assuranceLevel",
  evidence_ref: "evidenceRef",
  observation_id: "observationId",
  threat_source: "threatSource",
  threat_event: "threatEvent",
  affected_object: "affectedObject",
  time_horizon: "timeHorizon",
  observation_refs: "observationRefs",
  topology_context: "topologyContext",
  risk_scenario_id: "riskScenarioId",
  risk_register_record_id: "riskRegisterRecordId",
  methodology_profile_id: "methodologyProfileId",
  profile_key: "profileKey",
  input_schema: "inputSchema",
  output_schema: "outputSchema",
  review_cadence: "reviewCadence",
  next_review_at: "nextReviewAt",
  category_tags: "categoryTags",
  decision_metadata: "decisionMetadata",
  asset_scope_summary: "assetScopeSummary",
  risk_scenario_ids: "riskScenarioIds",
  control_function: "controlFunction",
  control_id: "controlId",
  implementation_scope: "implementationScope",
  methodology_factors: "methodologyFactors",
  analyst_identity: "analystIdentity",
  input_factors: "inputFactors",
  observation_date: "observationDate",
  assessment_at: "assessmentAt",
  uncertainty_metadata: "uncertaintyMetadata",
  computed_outputs: "computedOutputs",
  evidence_refs: "evidenceRefs",
  observation_ids: "observationIds",
  approval_state: "approvalState",
  due_date: "dueDate",
  action_items: "actionItems",
  reassessment_triggers: "reassessmentTriggers",
  root_node_ids: "rootNodeIds",
  max_depth: "maxDepth",
  source_node_id: "sourceNodeId",
  target_node_id: "targetNodeId",
};

const TO_SNAKE = Object.fromEntries(Object.entries(TO_CAMEL).map(([k, v]) => [v, k]));

function toCamelCase(obj) {
  if (obj === null || obj === undefined || typeof obj !== "object") return obj;
  if (Array.isArray(obj)) return obj.map(toCamelCase);
  const out = {};
  for (const [k, v] of Object.entries(obj)) {
    out[TO_CAMEL[k] || k] = toCamelCase(v);
  }
  return out;
}

function toSnakeCase(obj) {
  if (obj === null || obj === undefined || typeof obj !== "object") return obj;
  if (Array.isArray(obj)) return obj.map(toSnakeCase);
  const out = {};
  for (const [k, v] of Object.entries(obj)) {
    out[TO_SNAKE[k] || k] = toSnakeCase(v);
  }
  return out;
}

// ---------------------------------------------------------------------------
// HTTP client
// ---------------------------------------------------------------------------

const BASE_URL = process.env.GC_BASE_URL || "http://localhost:8000";

export function buildUrl(path, params) {
  const url = new URL(path, BASE_URL);
  if (params) {
    for (const [k, v] of Object.entries(params)) {
      if (v !== undefined && v !== null && v !== "") {
        url.searchParams.set(k, String(v));
      }
    }
  }
  return url.toString();
}

export function parseErrorBody(text) {
  try {
    const body = JSON.parse(text);
    if (body.error && body.error.message) {
      return body.error.message;
    }
    return text;
  } catch {
    return text;
  }
}

async function request(method, path, { body, params, formData } = {}) {
  const url = buildUrl(path, params);
  const options = { method };

  if (formData) {
    options.headers = { "X-Actor": "mcp-server" };
    options.body = formData;
    // Let fetch set Content-Type with boundary for multipart
  } else if (body !== undefined) {
    options.headers = { "Content-Type": "application/json", "X-Actor": "mcp-server" };
    options.body = JSON.stringify(toCamelCase(body));
  } else {
    options.headers = { "X-Actor": "mcp-server" };
  }

  const res = await fetch(url, options);

  if (res.status === 204) return null;

  const text = await res.text();

  if (!res.ok) {
    const msg = parseErrorBody(text);
    throw new Error(`${res.status}: ${msg}`);
  }

  const data = text ? JSON.parse(text) : null;
  return toSnakeCase(data);
}

// ---------------------------------------------------------------------------
// Project API functions
// ---------------------------------------------------------------------------

export async function listProjects() {
  return request("GET", "/api/v1/projects");
}

export async function getProject(identifier) {
  return request("GET", `/api/v1/projects/${encodeURIComponent(identifier)}`);
}

export async function createProject(data) {
  return request("POST", "/api/v1/projects", { body: data });
}

export async function updateProject(identifier, data) {
  return request("PUT", `/api/v1/projects/${encodeURIComponent(identifier)}`, { body: data });
}

// ---------------------------------------------------------------------------
// Requirement API functions
// ---------------------------------------------------------------------------

export async function getRequirementByUid(uid, project) {
  return request("GET", `/api/v1/requirements/uid/${encodeURIComponent(uid)}`, {
    params: { project },
  });
}

export async function getRequirement(id) {
  return request("GET", `/api/v1/requirements/${encodeURIComponent(id)}`);
}

export async function listRequirements({ status, type, priority, wave, search, page, size, sort, project } = {}) {
  return request("GET", "/api/v1/requirements", {
    params: { status, type, priority, wave, search, page, size, sort, project },
  });
}

export async function createRequirement(data, project) {
  return request("POST", "/api/v1/requirements", { body: data, params: { project } });
}

export async function updateRequirement(id, data) {
  return request("PUT", `/api/v1/requirements/${encodeURIComponent(id)}`, { body: data });
}

export async function transitionStatus(id, status, reason) {
  const body = { status };
  if (reason) body.reason = reason;
  return request("POST", `/api/v1/requirements/${encodeURIComponent(id)}/transition`, { body });
}

export async function archiveRequirement(id) {
  return request("POST", `/api/v1/requirements/${encodeURIComponent(id)}/archive`);
}

export async function bulkTransitionStatus(ids, status, reason) {
  const body = { ids, status };
  if (reason) body.reason = reason;
  return request("POST", "/api/v1/requirements/bulk/transition", { body });
}

export async function cloneRequirement(id, newUid, copyRelations) {
  return request("POST", `/api/v1/requirements/${encodeURIComponent(id)}/clone`, {
    body: { new_uid: newUid, copy_relations: copyRelations },
  });
}

export async function getRelations(id) {
  return request("GET", `/api/v1/requirements/${encodeURIComponent(id)}/relations`);
}

export async function createRelation(sourceId, targetId, relationType) {
  return request("POST", `/api/v1/requirements/${encodeURIComponent(sourceId)}/relations`, {
    body: { target_id: targetId, relation_type: relationType },
  });
}

export async function getTraceabilityLinks(id) {
  return request("GET", `/api/v1/requirements/${encodeURIComponent(id)}/traceability`);
}

export async function createTraceabilityLink(requirementId, data) {
  return request(
    "POST",
    `/api/v1/requirements/${encodeURIComponent(requirementId)}/traceability`,
    { body: data },
  );
}

export async function detectCycles(project) {
  return request("GET", "/api/v1/analysis/cycles", { params: { project } });
}

export async function findOrphans(project) {
  return request("GET", "/api/v1/analysis/orphans", { params: { project } });
}

export async function findCoverageGaps(linkType, project) {
  return request("GET", "/api/v1/analysis/coverage-gaps", {
    params: { linkType, project },
  });
}

export async function impactAnalysis(id) {
  return request("GET", `/api/v1/analysis/impact/${encodeURIComponent(id)}`);
}

export async function crossWaveValidation(project) {
  return request("GET", "/api/v1/analysis/cross-wave", { params: { project } });
}

export async function detectConsistencyViolations(project) {
  return request("GET", "/api/v1/analysis/consistency-violations", { params: { project } });
}

export async function analyzeCompleteness(project) {
  return request("GET", "/api/v1/analysis/completeness", { params: { project } });
}

export async function getDashboardStats(project) {
  return request("GET", "/api/v1/analysis/dashboard-stats", { params: { project } });
}

export async function getWorkOrder(project) {
  return request("GET", "/api/v1/analysis/work-order", { params: { project } });
}

function readOperatorSuppliedFile(filePath) {
  if (!filePath || !isAbsolute(filePath)) {
    throw new Error("file_path must be an absolute path");
  }

  // eslint-disable-next-line security/detect-non-literal-fs-filename -- file_path is validated operator input
  return readFileSync(filePath);
}

function readAbsoluteTextFile(filePath) {
  if (!filePath || !isAbsolute(filePath)) {
    throw new Error("file_path must be an absolute path");
  }

  // eslint-disable-next-line security/detect-non-literal-fs-filename -- file_path is validated absolute input
  return readFileSync(filePath, "utf8");
}

export async function importStrictdoc(filePath, project) {
  const content = readOperatorSuppliedFile(filePath);
  const form = new FormData();
  form.append("file", new Blob([content]), basename(filePath));
  const params = {};
  if (project) params.project = project;
  return request("POST", "/api/v1/admin/import/strictdoc", { formData: form, params });
}

export async function importReqif(filePath, project) {
  const content = readOperatorSuppliedFile(filePath);
  const form = new FormData();
  form.append("file", new Blob([content]), basename(filePath));
  const params = {};
  if (project) params.project = project;
  return request("POST", "/api/v1/admin/import/reqif", { formData: form, params });
}

export async function syncGithub(owner, repo) {
  return request("POST", "/api/v1/admin/sync/github", {
    params: { owner, repo },
  });
}

export async function syncGithubPrs(owner, repo) {
  return request("POST", "/api/v1/admin/sync/github/prs", {
    params: { owner, repo },
  });
}

// ---------------------------------------------------------------------------
// History functions
// ---------------------------------------------------------------------------

export async function getRequirementHistory(id) {
  return request("GET", `/api/v1/requirements/${encodeURIComponent(id)}/history`);
}

export async function getRelationHistory(reqId, relId) {
  return request("GET", `/api/v1/requirements/${encodeURIComponent(reqId)}/relations/${encodeURIComponent(relId)}/history`);
}

export async function getTraceabilityLinkHistory(reqId, linkId) {
  return request("GET", `/api/v1/requirements/${encodeURIComponent(reqId)}/traceability/${encodeURIComponent(linkId)}/history`);
}

export async function getRequirementTimeline(id, changeCategory, actor, from, to, limit, offset) {
  const params = new URLSearchParams();
  if (changeCategory) params.set("changeCategory", changeCategory);
  if (actor) params.set("actor", actor);
  if (from) params.set("from", from);
  if (to) params.set("to", to);
  if (limit != null) params.set("limit", String(limit));
  if (offset != null) params.set("offset", String(offset));
  const qs = params.toString();
  return request("GET", `/api/v1/requirements/${encodeURIComponent(id)}/timeline${qs ? `?${qs}` : ""}`);
}

export async function getRequirementDiff(id, fromRevision, toRevision) {
  const params = new URLSearchParams();
  params.set("fromRevision", String(fromRevision));
  params.set("toRevision", String(toRevision));
  return request("GET", `/api/v1/requirements/${encodeURIComponent(id)}/diff?${params.toString()}`);
}

export async function getProjectTimeline(project, changeCategory, actor, from, to, limit, offset) {
  const params = new URLSearchParams();
  if (project) params.set("project", project);
  if (changeCategory) params.set("changeCategory", changeCategory);
  if (actor) params.set("actor", actor);
  if (from) params.set("from", from);
  if (to) params.set("to", to);
  if (limit != null) params.set("limit", String(limit));
  if (offset != null) params.set("offset", String(offset));
  const qs = params.toString();
  return request("GET", `/api/v1/audit/timeline${qs ? `?${qs}` : ""}`);
}

export async function exportAuditTimeline(project, changeCategory, actor, from, to, limit) {
  const url = buildUrl("/api/v1/audit/timeline/export", {
    project, changeCategory, actor, from, to, limit,
  });
  const resp = await fetch(url);
  if (!resp.ok) throw new Error(`${resp.status}: ${await resp.text()}`);
  return resp.text();
}

export async function exportRequirements(project, format) {
  const url = buildUrl("/api/v1/export/requirements", { project, format });
  const resp = await fetch(url);
  if (!resp.ok) throw new Error(`${resp.status}: ${await resp.text()}`);
  if (!format || format === "csv") return resp.text();
  const buf = await resp.arrayBuffer();
  return Buffer.from(buf).toString("base64");
}

export async function exportSweepReport(project, format) {
  const url = buildUrl("/api/v1/export/sweep", { project, format });
  const resp = await fetch(url, { method: "POST" });
  if (!resp.ok) throw new Error(`${resp.status}: ${await resp.text()}`);
  if (!format || format === "csv") return resp.text();
  const buf = await resp.arrayBuffer();
  return Buffer.from(buf).toString("base64");
}

export async function exportDocument(documentId, format) {
  const url = buildUrl(`/api/v1/export/document/${encodeURIComponent(documentId)}`, { format });
  const resp = await fetch(url);
  if (!resp.ok) throw new Error(`${resp.status}: ${await resp.text()}`);
  // PDF is binary (base64); sdoc, html, reqif are text
  if (format === "pdf") {
    const buf = await resp.arrayBuffer();
    return Buffer.from(buf).toString("base64");
  }
  return resp.text();
}

// ---------------------------------------------------------------------------
// Delete functions
// ---------------------------------------------------------------------------

export async function deleteRelation(reqId, relId) {
  await request("DELETE", `/api/v1/requirements/${encodeURIComponent(reqId)}/relations/${encodeURIComponent(relId)}`);
}

export async function deleteTraceabilityLink(reqId, linkId) {
  await request("DELETE", `/api/v1/requirements/${encodeURIComponent(reqId)}/traceability/${encodeURIComponent(linkId)}`);
}

// ---------------------------------------------------------------------------
// Graph functions
// ---------------------------------------------------------------------------

export async function materializeGraph() {
  return request("POST", "/api/v1/admin/graph/materialize");
}

export async function getAncestors(uid, depth, project) {
  return request("GET", `/api/v1/requirements/graph/ancestors/${encodeURIComponent(uid)}`, {
    params: { depth, project },
  });
}

export async function getDescendants(uid, depth, project) {
  return request("GET", `/api/v1/requirements/graph/descendants/${encodeURIComponent(uid)}`, {
    params: { depth, project },
  });
}

export async function findPaths(source, target, project) {
  return request("GET", "/api/v1/requirements/graph/paths", {
    params: { source, target, project },
  });
}

export async function getGraphVisualization(project, entityTypes) {
  return request("GET", "/api/v1/graph/visualization", {
    params: { project, entityTypes: entityTypes ? entityTypes.join(",") : undefined },
  });
}

export async function extractSubgraph(rootNodeIds, project, entityTypes, maxDepth) {
  return request("POST", "/api/v1/graph/subgraph/query", {
    params: { project },
    body: { root_node_ids: rootNodeIds, entity_types: entityTypes, max_depth: maxDepth },
  });
}

export async function traverseGraph(rootNodeIds, project, entityTypes, maxDepth) {
  return request("POST", "/api/v1/graph/traversal/query", {
    params: { project },
    body: { root_node_ids: rootNodeIds, entity_types: entityTypes, max_depth: maxDepth },
  });
}

export async function findGraphPaths(sourceNodeId, targetNodeId, project, entityTypes, maxDepth) {
  return request("POST", "/api/v1/graph/paths/query", {
    params: { project },
    body: {
      source_node_id: sourceNodeId,
      target_node_id: targetNodeId,
      entity_types: entityTypes,
      max_depth: maxDepth,
    },
  });
}

// ---------------------------------------------------------------------------
// Baseline functions
// ---------------------------------------------------------------------------

export async function createBaseline(data, project) {
  return request("POST", "/api/v1/baselines", { body: data, params: { project } });
}

export async function listBaselines(project) {
  return request("GET", "/api/v1/baselines", { params: { project } });
}

export async function getBaseline(id) {
  return request("GET", `/api/v1/baselines/${encodeURIComponent(id)}`);
}

export async function getBaselineSnapshot(id) {
  return request("GET", `/api/v1/baselines/${encodeURIComponent(id)}/snapshot`);
}

export async function compareBaselines(id, otherId) {
  return request("GET", `/api/v1/baselines/${encodeURIComponent(id)}/compare/${encodeURIComponent(otherId)}`);
}

export async function deleteBaseline(id) {
  await request("DELETE", `/api/v1/baselines/${encodeURIComponent(id)}`);
}

// ---------------------------------------------------------------------------
// GitHub issue creation
// ---------------------------------------------------------------------------

export function formatIssueBody(req, extraBody) {
  const headerParts = [
    `**${req.uid}**`,
    req.requirement_type || "FUNCTIONAL",
    req.priority || "SHOULD",
  ];
  if (req.wave != null) {
    headerParts.push(`Wave ${req.wave}`);
  }
  headerParts.push(req.status || "DRAFT");

  let body = `> ${headerParts.join(" | ")}\n\n## Statement\n\n${req.statement}`;

  if (req.rationale) {
    body += `\n\n## Rationale\n\n${req.rationale}`;
  }

  body += `\n\n---\n*Created from Ground Control requirement ${req.uid}*`;

  if (extraBody) {
    body += `\n\n${extraBody}`;
  }

  return body;
}

const GITHUB_REPO_RE = /^[a-zA-Z0-9][a-zA-Z0-9._-]*\/[a-zA-Z0-9][a-zA-Z0-9._-]*$/;

export async function createGitHubIssue({ title, body, labels, repo }) {
  const targetRepo = repo || process.env.GH_REPO;
  if (targetRepo && !GITHUB_REPO_RE.test(targetRepo)) {
    throw new Error(`Invalid GitHub repo format: expected 'owner/repo', got '${targetRepo}'`);
  }
  const args = ["issue", "create", "--title", title, "--body", body];
  if (targetRepo) {
    args.push("--repo", targetRepo);
  }
  if (labels && labels.length > 0) {
    args.push("--label", labels.join(","));
  }

  const { stdout } = await execFile("gh", args);
  const url = stdout.trim();
  const match = url.match(/\/issues\/(\d+)$/);
  if (!match) {
    throw new Error(`Could not parse issue number from gh output: ${url}`);
  }
  const number = parseInt(match[1], 10);
  return { url, number };
}

export async function createGitHubIssueViaApi(data, project) {
  return request("POST", "/api/v1/admin/github/issues", { body: data, params: { project } });
}

// ---------------------------------------------------------------------------
// Repository context helpers
// ---------------------------------------------------------------------------

export function parseRepoGroundControlContext(agentsMarkdown) {
  const snippet = buildGroundControlContextSnippet();
  const headingMatch = agentsMarkdown.match(/^#{1,6}\s+Ground Control Context\s*$/m);
  if (!headingMatch || headingMatch.index == null) {
    return {
      status: "missing_ground_control_context",
      project: null,
      errors: [
        "AGENTS.md must include a 'Ground Control Context' section with a fenced YAML block.",
      ],
      suggested_agents_snippet: snippet,
    };
  }

  const sectionStart = headingMatch.index + headingMatch[0].length;
  const afterHeading = agentsMarkdown.slice(sectionStart);
  const nextHeadingMatch = afterHeading.match(/^#{1,6}\s+\S.*$/m);
  const sectionBody = nextHeadingMatch ? afterHeading.slice(0, nextHeadingMatch.index) : afterHeading;
  const yamlBlockMatch = sectionBody.match(/```(?:yaml|yml)\s*\n([\s\S]*?)```/m);
  if (!yamlBlockMatch) {
    return {
      status: "invalid_ground_control_context",
      project: null,
      errors: [
        "The 'Ground Control Context' section must contain a fenced YAML block.",
      ],
      suggested_agents_snippet: snippet,
    };
  }

  let parsed;
  try {
    parsed = parseYaml(yamlBlockMatch[1]);
  } catch (error) {
    return {
      status: "invalid_ground_control_context",
      project: null,
      errors: [`Could not parse Ground Control context YAML: ${error.message}`],
      suggested_agents_snippet: snippet,
    };
  }

  const project = parsed?.ground_control?.project;
  if (typeof project !== "string" || project.trim() === "") {
    return {
      status: "invalid_ground_control_context",
      project: null,
      errors: [
        "Ground Control context must define ground_control.project as a non-empty string.",
      ],
      suggested_agents_snippet: snippet,
    };
  }

  if (!GROUND_CONTROL_PROJECT_RE.test(project)) {
    return {
      status: "invalid_ground_control_context",
      project: null,
      errors: [
        "ground_control.project must be a lowercase identifier using letters, numbers, and hyphens only.",
      ],
      suggested_agents_snippet: snippet,
    };
  }

  return {
    status: "ok",
    project,
    errors: [],
    suggested_agents_snippet: snippet,
  };
}

export async function getRepoGroundControlContext(repoPath) {
  const repoRoot = await ensureGitRepo(repoPath);
  const agentsPath = join(repoRoot, "AGENTS.md");
  const snippet = buildGroundControlContextSnippet();

  let agentsMarkdown;
  try {
    agentsMarkdown = readAbsoluteTextFile(agentsPath);
  } catch (error) {
    if (error.code === "ENOENT") {
      return {
        repo_path: repoRoot,
        agents_path: agentsPath,
        status: "missing_agents_md",
        project: null,
        errors: [
          "AGENTS.md was not found at the repository root.",
        ],
        suggested_agents_snippet: snippet,
      };
    }

    throw error;
  }

  return {
    repo_path: repoRoot,
    agents_path: agentsPath,
    ...parseRepoGroundControlContext(agentsMarkdown),
  };
}

// ---------------------------------------------------------------------------
// Codex workflow helpers
// ---------------------------------------------------------------------------

async function ensureGitRepo(repoPath) {
  if (!repoPath || !isAbsolute(repoPath)) {
    throw new Error("repo_path must be an absolute path to a Git repository");
  }

  try {
    const { stdout } = await execFile("git", ["-C", repoPath, "rev-parse", "--show-toplevel"]);
    return stdout.trim();
  } catch (error) {
    throw new Error(`repo_path is not a valid Git repository: ${formatCommandFailure("git", error)}`);
  }
}

async function getIssueContext(issueNumber, repo) {
  if (issueNumber == null) return null;

  const args = ["issue", "view", String(issueNumber), "--json", "number,title,body"];
  const targetRepo = repo || process.env.GH_REPO;
  if (targetRepo) {
    args.push("--repo", targetRepo);
  }

  try {
    const { stdout } = await execFile("gh", args);
    return JSON.parse(stdout);
  } catch (error) {
    return {
      number: issueNumber,
      warning: `Failed to fetch GitHub issue context: ${error.message}`,
    };
  }
}

async function listWorkingTreeChanges(repoPath) {
  const [tracked, untracked] = await Promise.all([
    execFile("git", ["-C", repoPath, "diff", "--name-only", "HEAD"]),
    execFile("git", ["-C", repoPath, "ls-files", "--others", "--exclude-standard"]),
  ]);

  const files = new Set();
  for (const output of [tracked.stdout, untracked.stdout]) {
    for (const line of output.split("\n")) {
      const trimmed = line.trim();
      if (trimmed) files.add(trimmed);
    }
  }
  return Array.from(files).sort();
}

function findNewWorkingTreeChanges(beforeFiles, afterFiles) {
  const before = new Set(beforeFiles);
  return afterFiles.filter((file) => !before.has(file));
}

function summarizeTraceabilityLinks(traceabilityLinks = []) {
  return traceabilityLinks.map((link) => ({
    artifact_type: link.artifact_type,
    artifact_identifier: link.artifact_identifier,
    artifact_title: link.artifact_title,
    link_type: link.link_type,
  }));
}

function readGeneratedCodexSummary(outputPath) {
  try {
    // eslint-disable-next-line security/detect-non-literal-fs-filename -- outputPath is created by mkdtemp within the local temp directory
    return readFileSync(outputPath, "utf8").trim();
  } catch (error) {
    if (error.code === "ENOENT") {
      return "";
    }
    throw error;
  }
}

export function buildCodexArchitecturePreflightPrompt({ requirement, traceabilityLinks = [], issueContext = null }) {
  const traceabilitySummary = summarizeTraceabilityLinks(traceabilityLinks);

  return [
    "You are Codex performing an architecture preflight before implementation.",
    "",
    "Your job is to set the implementation on the right road before coding starts.",
    "",
    "Hard constraints:",
    "- Do not implement the requirement itself.",
    "- You may add or update ADRs, design notes, workflow notes, or other guidance docs when they materially reduce design risk.",
    "- Keep guidance minimal but sufficient. Do not write an implementation plan.",
    "- Do not invent new abstractions if existing cross-cutting concerns, schemas, error handling, validation, logging, or workflow patterns already cover the need.",
    "",
    "Quality bar:",
    "- Hold the upcoming implementation to a top-tier production engineering bar for maintainability, reliability, security, consistency, reuse of existing cross-cutting concerns, clear boundaries, and avoidance of abstraction or concept confusion.",
    "- Call out all gotchas and guardrails up front. Do not silently omit concerns because they seem low priority.",
    "",
    "Requirement payload:",
    JSON.stringify(requirement, null, 2),
    "",
    "Existing traceability summary:",
    JSON.stringify(traceabilitySummary, null, 2),
    "",
    "GitHub issue context:",
    JSON.stringify(issueContext, null, 2),
    "",
    "Required focus:",
    "- Identify existing cross-cutting concerns, schemas, validation layers, exception handling, logging/observability, security patterns, persistence patterns, and workflow conventions that implementation must reuse.",
    "- Identify risks of concept conflation, leaky abstractions, duplicate schemas, duplicate validation, duplicate exception hierarchies, or duplicate workflow logic.",
    "- Identify where existing contracts, schemas, controllers, DTOs, services, repositories, exception handling, logging, or testing patterns already solve part of the problem.",
    "- Add or update ADRs/design docs only if needed to lock in guardrails or clarify boundaries.",
    "- State explicit non-goals and anti-patterns to avoid.",
    "",
    "Final response requirements:",
    "- List files changed.",
    "- Summarize architecture decisions and guardrails.",
    "- Summarize required cross-cutting concerns to reuse.",
    "- Summarize gotchas and anti-patterns to avoid.",
    "- Summarize non-goals and implementation boundaries.",
    "",
    "Do not spend time re-fetching requirement details if the provided payload is sufficient.",
  ].join("\n");
}

export function buildCodexArchitectureExecArgs({ repoPath, outputPath }) {
  return [
    "exec",
    "--ephemeral",
    "--sandbox",
    "workspace-write",
    "-C",
    repoPath,
    "--output-last-message",
    outputPath,
    "-",
  ];
}

export async function runCodexArchitecturePreflight({
  requirementUid,
  project,
  repoPath,
  issueNumber,
  repo,
}) {
  const repoRoot = await ensureGitRepo(repoPath);
  const requirement = await getRequirementByUid(requirementUid, project);
  const traceabilityLinks = await getTraceabilityLinks(requirement.id);
  const issueContext = await getIssueContext(issueNumber, repo);
  const preexistingChangedFiles = await listWorkingTreeChanges(repoRoot);

  const tempDir = mkdtempSync(join(tmpdir(), "gc-codex-preflight-"));
  const outputPath = join(tempDir, "codex-last-message.txt");
  const prompt = buildCodexArchitecturePreflightPrompt({
    requirement,
    traceabilityLinks,
    issueContext,
  });

  try {
    await execFileWithInput(
      "codex",
      buildCodexArchitectureExecArgs({ repoPath: repoRoot, outputPath }),
      {
        input: prompt,
        cwd: repoRoot,
        maxBuffer: 10 * 1024 * 1024,
        env: { ...process.env, NO_COLOR: "1" },
      },
    );

    const summary = readGeneratedCodexSummary(outputPath);
    const changedFiles = findNewWorkingTreeChanges(preexistingChangedFiles, await listWorkingTreeChanges(repoRoot));
    return {
      requirement_uid: requirementUid,
      repo_path: repoRoot,
      preexisting_changed_files: preexistingChangedFiles,
      changed_files: changedFiles,
      summary,
    };
  } catch (error) {
    throw new Error(`Codex architecture preflight failed: ${formatCommandFailure("codex", error)}`);
  } finally {
    rmSync(tempDir, { recursive: true, force: true });
  }
}

export function buildCodexReviewPrompt(baseBranch) {
  return [
    `Review the changes against ${baseBranch}.`,
    "",
    "Hold the code to a top-tier production engineering bar for maintainability, reliability, security, consistency, validation, logging, exception handling, schema reuse, reuse of existing cross-cutting concerns, and avoidance of abstraction or concept confusion.",
    "",
    "Important review rules:",
    "- Enumerate all material issues you can find. Do not stop after a small handful of findings.",
    "- Do not prioritize, bucket, or silently omit issues because they appear low priority. The caller intends to fix everything now.",
    "- Call out cases where the change reinvents existing infrastructure, bypasses existing validation or error handling, duplicates schemas or DTOs, weakens observability, or introduces brittle abstractions.",
    "- Include precise file and line references for every finding.",
    "- If there are no findings, say 'No findings' explicitly and mention any residual test or coverage risks.",
  ].join("\n");
}

export function buildCodexReviewArgs({ baseBranch, uncommitted }) {
  const args = ["review", "--base", baseBranch];
  if (uncommitted) {
    args.push("--uncommitted");
  }
  args.push("-");
  return args;
}

export async function runCodexReview({ repoPath, baseBranch = "dev", uncommitted = false }) {
  const repoRoot = await ensureGitRepo(repoPath);
  const prompt = buildCodexReviewPrompt(baseBranch);
  const args = buildCodexReviewArgs({ baseBranch, uncommitted });
  let stdout;

  try {
    ({ stdout } = await execFileWithInput("codex", args, {
      input: prompt,
      cwd: repoRoot,
      maxBuffer: 10 * 1024 * 1024,
      env: { ...process.env, NO_COLOR: "1" },
    }));
  } catch (error) {
    throw new Error(`Codex review failed: ${formatCommandFailure("codex", error)}`);
  }

  return {
    repo_path: repoRoot,
    base_branch: baseBranch,
    uncommitted,
    review: stdout.trim(),
  };
}

// ---------------------------------------------------------------------------
// Analysis sweep API functions
// ---------------------------------------------------------------------------

export async function runSweep(project) {
  return request("POST", "/api/v1/analysis/sweep", { params: { project } });
}

export async function runSweepAll() {
  return request("POST", "/api/v1/analysis/sweep/all");
}

// ---------------------------------------------------------------------------
// Embedding API functions
// ---------------------------------------------------------------------------

export async function embedRequirement(requirementId) {
  return request("POST", `/api/v1/embeddings/${encodeURIComponent(requirementId)}`);
}

export async function getEmbeddingStatus(requirementId) {
  return request("GET", `/api/v1/embeddings/${encodeURIComponent(requirementId)}/status`);
}

export async function embedProject(project, force) {
  return request("POST", "/api/v1/embeddings/batch", {
    params: { project, force: force ? "true" : undefined },
  });
}

export async function deleteEmbedding(requirementId) {
  await request("DELETE", `/api/v1/embeddings/${encodeURIComponent(requirementId)}`);
}

export async function analyzeSemanticSimilarity(project, threshold) {
  return request("GET", "/api/v1/analysis/semantic-similarity", {
    params: { project, threshold },
  });
}

// ---------------------------------------------------------------------------
// Quality Gate API functions
// ---------------------------------------------------------------------------

export async function createQualityGate(data, project) {
  return request("POST", "/api/v1/quality-gates", { body: data, params: { project } });
}

export async function listQualityGates(project) {
  return request("GET", "/api/v1/quality-gates", { params: { project } });
}

export async function getQualityGate(id) {
  return request("GET", `/api/v1/quality-gates/${encodeURIComponent(id)}`);
}

export async function updateQualityGate(id, data) {
  return request("PUT", `/api/v1/quality-gates/${encodeURIComponent(id)}`, { body: data });
}

export async function deleteQualityGate(id) {
  await request("DELETE", `/api/v1/quality-gates/${encodeURIComponent(id)}`);
}

export async function evaluateQualityGates(project) {
  return request("POST", "/api/v1/quality-gates/evaluate", { params: { project } });
}

// ---------------------------------------------------------------------------
// Document API functions
// ---------------------------------------------------------------------------

export async function createDocument(data, project) {
  return request("POST", "/api/v1/documents", { body: data, params: { project } });
}

export async function listDocuments(project) {
  return request("GET", "/api/v1/documents", { params: { project } });
}

export async function getDocument(id) {
  return request("GET", `/api/v1/documents/${encodeURIComponent(id)}`);
}

export async function updateDocument(id, data) {
  return request("PUT", `/api/v1/documents/${encodeURIComponent(id)}`, { body: data });
}

export async function deleteDocument(id) {
  await request("DELETE", `/api/v1/documents/${encodeURIComponent(id)}`);
}

// ---------------------------------------------------------------------------
// Section API functions
// ---------------------------------------------------------------------------

export async function createSection(documentId, data) {
  return request("POST", `/api/v1/documents/${encodeURIComponent(documentId)}/sections`, { body: data });
}

export async function listSections(documentId) {
  return request("GET", `/api/v1/documents/${encodeURIComponent(documentId)}/sections`);
}

export async function getSectionTree(documentId) {
  return request("GET", `/api/v1/documents/${encodeURIComponent(documentId)}/sections/tree`);
}

export async function getSection(id) {
  return request("GET", `/api/v1/sections/${encodeURIComponent(id)}`);
}

export async function updateSection(id, data) {
  return request("PUT", `/api/v1/sections/${encodeURIComponent(id)}`, { body: data });
}

export async function deleteSection(id) {
  await request("DELETE", `/api/v1/sections/${encodeURIComponent(id)}`);
}

// ---------------------------------------------------------------------------
// Section Content API functions
// ---------------------------------------------------------------------------

export async function addSectionContent(sectionId, data) {
  return request("POST", `/api/v1/sections/${encodeURIComponent(sectionId)}/content`, { body: data });
}

export async function listSectionContent(sectionId) {
  return request("GET", `/api/v1/sections/${encodeURIComponent(sectionId)}/content`);
}

export async function updateSectionContent(id, data) {
  return request("PUT", `/api/v1/sections/content/${encodeURIComponent(id)}`, { body: data });
}

export async function deleteSectionContent(id) {
  await request("DELETE", `/api/v1/sections/content/${encodeURIComponent(id)}`);
}

// ---------------------------------------------------------------------------
// Document Reading Order API functions
// ---------------------------------------------------------------------------

export async function getDocumentReadingOrder(documentId) {
  return request("GET", `/api/v1/documents/${encodeURIComponent(documentId)}/reading-order`);
}

// ---------------------------------------------------------------------------
// Document Grammar API functions
// ---------------------------------------------------------------------------

export async function setDocumentGrammar(documentId, grammar) {
  return request("PUT", `/api/v1/documents/${encodeURIComponent(documentId)}/grammar`, { body: grammar });
}

export async function getDocumentGrammar(documentId) {
  return request("GET", `/api/v1/documents/${encodeURIComponent(documentId)}/grammar`);
}

export async function deleteDocumentGrammar(documentId) {
  await request("DELETE", `/api/v1/documents/${encodeURIComponent(documentId)}/grammar`);
}

// ---------------------------------------------------------------------------
// Architecture Decision Record API functions
// ---------------------------------------------------------------------------

export async function createAdr(data, project) {
  return request("POST", "/api/v1/adrs", { body: data, params: { project } });
}

export async function listAdrs(project) {
  return request("GET", "/api/v1/adrs", { params: { project } });
}

export async function getAdr(id) {
  return request("GET", `/api/v1/adrs/${encodeURIComponent(id)}`);
}

export async function getAdrByUid(uid, project) {
  return request("GET", `/api/v1/adrs/uid/${encodeURIComponent(uid)}`, { params: { project } });
}

export async function updateAdr(id, data) {
  return request("PUT", `/api/v1/adrs/${encodeURIComponent(id)}`, { body: data });
}

export async function deleteAdr(id) {
  await request("DELETE", `/api/v1/adrs/${encodeURIComponent(id)}`);
}

export async function transitionAdrStatus(id, status) {
  return request("PUT", `/api/v1/adrs/${encodeURIComponent(id)}/status`, { body: { status } });
}

export async function getAdrRequirements(id) {
  return request("GET", `/api/v1/adrs/${encodeURIComponent(id)}/requirements`);
}

// ---------------------------------------------------------------------------
// Operational Asset API functions
// ---------------------------------------------------------------------------

export async function createAsset(data, project) {
  return request("POST", "/api/v1/assets", { body: data, params: { project } });
}

export async function listAssets({ project, type } = {}) {
  return request("GET", "/api/v1/assets", { params: { project, type } });
}

export async function getAsset(id, project) {
  return request("GET", `/api/v1/assets/${encodeURIComponent(id)}`, { params: { project } });
}

export async function getAssetByUid(uid, project) {
  return request("GET", `/api/v1/assets/uid/${encodeURIComponent(uid)}`, { params: { project } });
}

export async function updateAsset(id, data, project) {
  return request("PUT", `/api/v1/assets/${encodeURIComponent(id)}`, { body: data, params: { project } });
}

export async function deleteAsset(id, project) {
  await request("DELETE", `/api/v1/assets/${encodeURIComponent(id)}`, { params: { project } });
}

export async function archiveAsset(id, project) {
  return request("POST", `/api/v1/assets/${encodeURIComponent(id)}/archive`, { params: { project } });
}

export async function createAssetRelation(assetId, data, project) {
  return request("POST", `/api/v1/assets/${encodeURIComponent(assetId)}/relations`, {
    body: data,
    params: { project },
  });
}

export async function getAssetRelations(assetId, project) {
  return request("GET", `/api/v1/assets/${encodeURIComponent(assetId)}/relations`, { params: { project } });
}

export async function deleteAssetRelation(assetId, relationId, project) {
  await request(
    "DELETE",
    `/api/v1/assets/${encodeURIComponent(assetId)}/relations/${encodeURIComponent(relationId)}`,
    { params: { project } },
  );
}

export async function detectAssetCycles(project) {
  return request("GET", "/api/v1/assets/topology/cycles", { params: { project } });
}

export async function assetImpactAnalysis(assetId, project) {
  return request("GET", `/api/v1/assets/${encodeURIComponent(assetId)}/topology/impact`, { params: { project } });
}

export async function extractAssetSubgraph(data, project) {
  return request("POST", "/api/v1/assets/topology/subgraph", { body: data, params: { project } });
}

// --- Asset Links (cross-entity linking) ---

export async function createAssetLink(assetId, data, project) {
  return request("POST", `/api/v1/assets/${encodeURIComponent(assetId)}/links`, {
    body: data,
    params: { project },
  });
}

export async function getAssetLinks(assetId, targetType, project) {
  return request("GET", `/api/v1/assets/${encodeURIComponent(assetId)}/links`, {
    params: { target_type: targetType, project },
  });
}

export async function deleteAssetLink(assetId, linkId, project) {
  await request("DELETE", `/api/v1/assets/${encodeURIComponent(assetId)}/links/${encodeURIComponent(linkId)}`, {
    params: { project },
  });
}

export async function getAssetLinksByTarget(targetType, targetEntityId, targetIdentifier, project) {
  return request("GET", "/api/v1/assets/links/by-target", {
    params: { target_type: targetType, target_entity_id: targetEntityId, target_identifier: targetIdentifier, project },
  });
}

// --- External Identifiers (source provenance) ---

export async function createAssetExternalId(assetId, data, project) {
  return request("POST", `/api/v1/assets/${encodeURIComponent(assetId)}/external-ids`, {
    body: data,
    params: { project },
  });
}

export async function getAssetExternalIds(assetId, sourceSystem, project) {
  return request("GET", `/api/v1/assets/${encodeURIComponent(assetId)}/external-ids`, {
    params: { source_system: sourceSystem, project },
  });
}

export async function updateAssetExternalId(assetId, extIdId, data, project) {
  return request(
    "PUT",
    `/api/v1/assets/${encodeURIComponent(assetId)}/external-ids/${encodeURIComponent(extIdId)}`,
    { body: data, params: { project } },
  );
}

export async function deleteAssetExternalId(assetId, extIdId, project) {
  await request(
    "DELETE",
    `/api/v1/assets/${encodeURIComponent(assetId)}/external-ids/${encodeURIComponent(extIdId)}`,
    { params: { project } },
  );
}

export async function findAssetByExternalId(sourceSystem, sourceId, project) {
  return request("GET", "/api/v1/assets/external-ids/by-source", {
    params: { source_system: sourceSystem, source_id: sourceId, project },
  });
}

// ---------------------------------------------------------------------------
// Observation API functions
// ---------------------------------------------------------------------------

export async function createObservation(assetId, data, project) {
  return request("POST", `/api/v1/assets/${encodeURIComponent(assetId)}/observations`, {
    body: data,
    params: { project },
  });
}

export async function listObservations(assetId, { category, key, project } = {}) {
  return request("GET", `/api/v1/assets/${encodeURIComponent(assetId)}/observations`, {
    params: { category, key, project },
  });
}

export async function getObservation(assetId, observationId, project) {
  return request(
    "GET",
    `/api/v1/assets/${encodeURIComponent(assetId)}/observations/${encodeURIComponent(observationId)}`,
    { params: { project } },
  );
}

export async function updateObservation(assetId, observationId, data, project) {
  return request(
    "PUT",
    `/api/v1/assets/${encodeURIComponent(assetId)}/observations/${encodeURIComponent(observationId)}`,
    { body: data, params: { project } },
  );
}

export async function deleteObservation(assetId, observationId, project) {
  await request(
    "DELETE",
    `/api/v1/assets/${encodeURIComponent(assetId)}/observations/${encodeURIComponent(observationId)}`,
    { params: { project } },
  );
}

export async function listLatestObservations(assetId, project) {
  return request("GET", `/api/v1/assets/${encodeURIComponent(assetId)}/observations/latest`, {
    params: { project },
  });
}

// ---------------------------------------------------------------------------
// Risk Scenario API functions
// ---------------------------------------------------------------------------

export async function createRiskScenario(data, project) {
  return request("POST", "/api/v1/risk-scenarios", { body: data, params: { project } });
}

export async function listRiskScenarios(project) {
  return request("GET", "/api/v1/risk-scenarios", { params: { project } });
}

export async function getRiskScenario(id, project) {
  return request("GET", `/api/v1/risk-scenarios/${encodeURIComponent(id)}`, { params: { project } });
}

export async function getRiskScenarioByUid(uid, project) {
  return request("GET", `/api/v1/risk-scenarios/uid/${encodeURIComponent(uid)}`, { params: { project } });
}

export async function updateRiskScenario(id, data, project) {
  return request("PUT", `/api/v1/risk-scenarios/${encodeURIComponent(id)}`, { body: data, params: { project } });
}

export async function deleteRiskScenario(id, project) {
  await request("DELETE", `/api/v1/risk-scenarios/${encodeURIComponent(id)}`, { params: { project } });
}

export async function transitionRiskScenarioStatus(id, status, project) {
  return request("PUT", `/api/v1/risk-scenarios/${encodeURIComponent(id)}/status`, {
    body: { status },
    params: { project },
  });
}

export async function getRiskScenarioRequirements(id, project) {
  return request("GET", `/api/v1/risk-scenarios/${encodeURIComponent(id)}/requirements`, { params: { project } });
}

// ---------------------------------------------------------------------------
// Risk Scenario Link API functions
// ---------------------------------------------------------------------------

export async function createRiskScenarioLink(riskScenarioId, data, project) {
  return request("POST", `/api/v1/risk-scenarios/${encodeURIComponent(riskScenarioId)}/links`, {
    body: data,
    params: { project },
  });
}

export async function listRiskScenarioLinks(riskScenarioId, { targetType, project } = {}) {
  return request("GET", `/api/v1/risk-scenarios/${encodeURIComponent(riskScenarioId)}/links`, {
    params: { target_type: targetType, project },
  });
}

export async function deleteRiskScenarioLink(riskScenarioId, linkId, project) {
  await request(
    "DELETE",
    `/api/v1/risk-scenarios/${encodeURIComponent(riskScenarioId)}/links/${encodeURIComponent(linkId)}`,
    { params: { project } },
  );
}

// ---------------------------------------------------------------------------
// Control API functions
// ---------------------------------------------------------------------------

export const CONTROL_STATUSES = ["DRAFT", "PROPOSED", "IMPLEMENTED", "OPERATIONAL", "DEPRECATED", "RETIRED"];
export const CONTROL_FUNCTIONS = ["PREVENTIVE", "DETECTIVE", "CORRECTIVE", "COMPENSATING"];
export const CONTROL_LINK_TARGET_TYPES = [
  "ASSET", "RISK_SCENARIO", "RISK_REGISTER_RECORD", "RISK_ASSESSMENT_RESULT",
  "TREATMENT_PLAN", "METHODOLOGY_PROFILE", "OBSERVATION", "REQUIREMENT",
  "EVIDENCE", "FINDING", "CODE", "CONFIGURATION", "OPERATIONAL_ARTIFACT", "EXTERNAL",
];
export const CONTROL_LINK_TYPES = [
  "PROTECTS", "IMPLEMENTS", "EVIDENCED_BY", "OBSERVED_IN", "MITIGATES", "MAPS_TO", "ASSOCIATED",
];

export async function createControl(data, project) {
  return request("POST", "/api/v1/controls", { body: data, params: { project } });
}

export async function listControls(project) {
  return request("GET", "/api/v1/controls", { params: { project } });
}

export async function getControl(id, project) {
  return request("GET", `/api/v1/controls/${encodeURIComponent(id)}`, { params: { project } });
}

export async function getControlByUid(uid, project) {
  return request("GET", `/api/v1/controls/uid/${encodeURIComponent(uid)}`, { params: { project } });
}

export async function updateControl(id, data, project) {
  return request("PUT", `/api/v1/controls/${encodeURIComponent(id)}`, { body: data, params: { project } });
}

export async function deleteControl(id, project) {
  await request("DELETE", `/api/v1/controls/${encodeURIComponent(id)}`, { params: { project } });
}

export async function transitionControlStatus(id, status, project) {
  return request("PUT", `/api/v1/controls/${encodeURIComponent(id)}/status`, {
    body: { status },
    params: { project },
  });
}

export async function createControlLink(controlId, data, project) {
  return request("POST", `/api/v1/controls/${encodeURIComponent(controlId)}/links`, {
    body: data,
    params: { project },
  });
}

export async function listControlLinks(controlId, { targetType, project } = {}) {
  return request("GET", `/api/v1/controls/${encodeURIComponent(controlId)}/links`, {
    params: { target_type: targetType, project },
  });
}

export async function deleteControlLink(controlId, linkId, project) {
  await request(
    "DELETE",
    `/api/v1/controls/${encodeURIComponent(controlId)}/links/${encodeURIComponent(linkId)}`,
    { params: { project } },
  );
}

// ---------------------------------------------------------------------------
// Methodology Profile API functions
// ---------------------------------------------------------------------------

export async function createMethodologyProfile(data, project) {
  return request("POST", "/api/v1/methodology-profiles", { body: data, params: { project } });
}

export async function listMethodologyProfiles(project) {
  return request("GET", "/api/v1/methodology-profiles", { params: { project } });
}

export async function getMethodologyProfile(id, project) {
  return request("GET", `/api/v1/methodology-profiles/${encodeURIComponent(id)}`, { params: { project } });
}

export async function updateMethodologyProfile(id, data, project) {
  return request("PUT", `/api/v1/methodology-profiles/${encodeURIComponent(id)}`, {
    body: data,
    params: { project },
  });
}

export async function deleteMethodologyProfile(id, project) {
  await request("DELETE", `/api/v1/methodology-profiles/${encodeURIComponent(id)}`, { params: { project } });
}

// ---------------------------------------------------------------------------
// Risk Register Record API functions
// ---------------------------------------------------------------------------

export async function createRiskRegisterRecord(data, project) {
  return request("POST", "/api/v1/risk-register-records", { body: data, params: { project } });
}

export async function listRiskRegisterRecords(project) {
  return request("GET", "/api/v1/risk-register-records", { params: { project } });
}

export async function getRiskRegisterRecord(id, project) {
  return request("GET", `/api/v1/risk-register-records/${encodeURIComponent(id)}`, { params: { project } });
}

export async function updateRiskRegisterRecord(id, data, project) {
  return request("PUT", `/api/v1/risk-register-records/${encodeURIComponent(id)}`, {
    body: data,
    params: { project },
  });
}

export async function transitionRiskRegisterRecordStatus(id, status, project) {
  return request("PUT", `/api/v1/risk-register-records/${encodeURIComponent(id)}/status`, {
    body: { status },
    params: { project },
  });
}

export async function deleteRiskRegisterRecord(id, project) {
  await request("DELETE", `/api/v1/risk-register-records/${encodeURIComponent(id)}`, { params: { project } });
}

// ---------------------------------------------------------------------------
// Risk Assessment Result API functions
// ---------------------------------------------------------------------------

export async function createRiskAssessmentResult(data, project) {
  return request("POST", "/api/v1/risk-assessment-results", { body: data, params: { project } });
}

export async function listRiskAssessmentResults({ riskScenarioId, riskRegisterRecordId, project } = {}) {
  return request("GET", "/api/v1/risk-assessment-results", {
    params: { riskScenarioId, riskRegisterRecordId, project },
  });
}

export async function getRiskAssessmentResult(id, project) {
  return request("GET", `/api/v1/risk-assessment-results/${encodeURIComponent(id)}`, { params: { project } });
}

export async function updateRiskAssessmentResult(id, data, project) {
  return request("PUT", `/api/v1/risk-assessment-results/${encodeURIComponent(id)}`, {
    body: data,
    params: { project },
  });
}

export async function transitionRiskAssessmentApprovalState(id, approvalState, project) {
  return request("PUT", `/api/v1/risk-assessment-results/${encodeURIComponent(id)}/approval-state`, {
    body: { approval_state: approvalState },
    params: { project },
  });
}

export async function deleteRiskAssessmentResult(id, project) {
  await request("DELETE", `/api/v1/risk-assessment-results/${encodeURIComponent(id)}`, { params: { project } });
}

// ---------------------------------------------------------------------------
// Treatment Plan API functions
// ---------------------------------------------------------------------------

export async function createTreatmentPlan(data, project) {
  return request("POST", "/api/v1/treatment-plans", { body: data, params: { project } });
}

export async function listTreatmentPlans({ riskRegisterRecordId, project } = {}) {
  return request("GET", "/api/v1/treatment-plans", { params: { riskRegisterRecordId, project } });
}

export async function getTreatmentPlan(id, project) {
  return request("GET", `/api/v1/treatment-plans/${encodeURIComponent(id)}`, { params: { project } });
}

export async function updateTreatmentPlan(id, data, project) {
  return request("PUT", `/api/v1/treatment-plans/${encodeURIComponent(id)}`, {
    body: data,
    params: { project },
  });
}

export async function transitionTreatmentPlanStatus(id, status, project) {
  return request("PUT", `/api/v1/treatment-plans/${encodeURIComponent(id)}/status`, {
    body: { status },
    params: { project },
  });
}

export async function deleteTreatmentPlan(id, project) {
  await request("DELETE", `/api/v1/treatment-plans/${encodeURIComponent(id)}`, { params: { project } });
}

// ---------------------------------------------------------------------------
// Verification Result constants & API functions
// ---------------------------------------------------------------------------

export const VERIFICATION_STATUSES = ["PROVEN", "REFUTED", "TIMEOUT", "UNKNOWN", "ERROR"];
export const ASSURANCE_LEVELS = ["L0", "L1", "L2", "L3"];

export async function createVerificationResult(data, project) {
  return request("POST", "/api/v1/verification-results", { body: data, params: { project } });
}

export async function listVerificationResults({ requirementId, prover, result, project } = {}) {
  return request("GET", "/api/v1/verification-results", {
    params: { requirement_id: requirementId, prover, result, project },
  });
}

export async function getVerificationResult(id, project) {
  return request("GET", `/api/v1/verification-results/${encodeURIComponent(id)}`, { params: { project } });
}

export async function updateVerificationResult(id, data, project) {
  return request("PUT", `/api/v1/verification-results/${encodeURIComponent(id)}`, {
    body: data,
    params: { project },
  });
}

export async function deleteVerificationResult(id, project) {
  await request("DELETE", `/api/v1/verification-results/${encodeURIComponent(id)}`, { params: { project } });
}
