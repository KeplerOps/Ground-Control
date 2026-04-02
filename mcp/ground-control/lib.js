import { readFileSync } from "node:fs";
import { basename } from "node:path";
import { execFile as execFileCb } from "node:child_process";
import { promisify } from "node:util";

const execFile = promisify(execFileCb);

// ---------------------------------------------------------------------------
// Constants (matching Java enums)
// ---------------------------------------------------------------------------

export const STATUSES = ["DRAFT", "ACTIVE", "DEPRECATED", "ARCHIVED"];
export const REQUIREMENT_TYPES = ["FUNCTIONAL", "NON_FUNCTIONAL", "CONSTRAINT", "INTERFACE"];
export const PRIORITIES = ["MUST", "SHOULD", "COULD", "WONT"];
export const RELATION_TYPES = ["PARENT", "DEPENDS_ON", "CONFLICTS_WITH", "REFINES", "SUPERSEDES", "RELATED"];
export const ARTIFACT_TYPES = [
  "GITHUB_ISSUE",
  "CODE_FILE",
  "ADR",
  "CONFIG",
  "POLICY",
  "TEST",
  "SPEC",
  "PROOF",
  "DOCUMENTATION",
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
  evidence_ref: "evidenceRef",
  observation_id: "observationId",
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

export async function importStrictdoc(filePath, project) {
  const content = readFileSync(filePath);
  const form = new FormData();
  form.append("file", new Blob([content]), basename(filePath));
  const params = {};
  if (project) params.project = project;
  return request("POST", "/api/v1/admin/import/strictdoc", { formData: form, params });
}

export async function importReqif(filePath, project) {
  const content = readFileSync(filePath);
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
  return request("GET", `/api/v1/graph/ancestors/${encodeURIComponent(uid)}`, {
    params: { depth, project },
  });
}

export async function getDescendants(uid, depth, project) {
  return request("GET", `/api/v1/graph/descendants/${encodeURIComponent(uid)}`, {
    params: { depth, project },
  });
}

export async function findPaths(source, target, project) {
  return request("GET", "/api/v1/graph/paths", {
    params: { source, target, project },
  });
}

export async function getGraphVisualization(project, entityTypes) {
  return request("GET", "/api/v1/graph/visualization", {
    params: { project, entityTypes: entityTypes ? entityTypes.join(",") : undefined },
  });
}

export async function extractSubgraph(roots, project, entityTypes) {
  return request("GET", "/api/v1/graph/subgraph", {
    params: { roots: roots.join(","), project, entityTypes: entityTypes ? entityTypes.join(",") : undefined },
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

export async function createGitHubIssue({ title, body, labels, repo }) {
  const targetRepo = repo || process.env.GH_REPO;
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

export async function getAsset(id) {
  return request("GET", `/api/v1/assets/${encodeURIComponent(id)}`);
}

export async function getAssetByUid(uid, project) {
  return request("GET", `/api/v1/assets/uid/${encodeURIComponent(uid)}`, { params: { project } });
}

export async function updateAsset(id, data) {
  return request("PUT", `/api/v1/assets/${encodeURIComponent(id)}`, { body: data });
}

export async function deleteAsset(id) {
  await request("DELETE", `/api/v1/assets/${encodeURIComponent(id)}`);
}

export async function archiveAsset(id) {
  return request("POST", `/api/v1/assets/${encodeURIComponent(id)}/archive`);
}

export async function createAssetRelation(assetId, data) {
  return request("POST", `/api/v1/assets/${encodeURIComponent(assetId)}/relations`, { body: data });
}

export async function getAssetRelations(assetId) {
  return request("GET", `/api/v1/assets/${encodeURIComponent(assetId)}/relations`);
}

export async function deleteAssetRelation(assetId, relationId) {
  await request(
    "DELETE",
    `/api/v1/assets/${encodeURIComponent(assetId)}/relations/${encodeURIComponent(relationId)}`,
  );
}

export async function detectAssetCycles(project) {
  return request("GET", "/api/v1/assets/topology/cycles", { params: { project } });
}

export async function assetImpactAnalysis(assetId) {
  return request("GET", `/api/v1/assets/${encodeURIComponent(assetId)}/topology/impact`);
}

export async function extractAssetSubgraph(data, project) {
  return request("POST", "/api/v1/assets/topology/subgraph", { body: data, params: { project } });
}

// --- Asset Links (cross-entity linking) ---

export async function createAssetLink(assetId, data) {
  return request("POST", `/api/v1/assets/${encodeURIComponent(assetId)}/links`, { body: data });
}

export async function getAssetLinks(assetId, targetType) {
  return request("GET", `/api/v1/assets/${encodeURIComponent(assetId)}/links`, {
    params: { target_type: targetType },
  });
}

export async function deleteAssetLink(assetId, linkId) {
  await request("DELETE", `/api/v1/assets/${encodeURIComponent(assetId)}/links/${encodeURIComponent(linkId)}`);
}

export async function getAssetLinksByTarget(targetType, targetIdentifier, project) {
  return request("GET", "/api/v1/assets/links/by-target", {
    params: { target_type: targetType, target_identifier: targetIdentifier, project },
  });
}

// --- External Identifiers (source provenance) ---

export async function createAssetExternalId(assetId, data) {
  return request("POST", `/api/v1/assets/${encodeURIComponent(assetId)}/external-ids`, { body: data });
}

export async function getAssetExternalIds(assetId, sourceSystem) {
  return request("GET", `/api/v1/assets/${encodeURIComponent(assetId)}/external-ids`, {
    params: { source_system: sourceSystem },
  });
}

export async function updateAssetExternalId(assetId, extIdId, data) {
  return request(
    "PUT",
    `/api/v1/assets/${encodeURIComponent(assetId)}/external-ids/${encodeURIComponent(extIdId)}`,
    { body: data },
  );
}

export async function deleteAssetExternalId(assetId, extIdId) {
  await request(
    "DELETE",
    `/api/v1/assets/${encodeURIComponent(assetId)}/external-ids/${encodeURIComponent(extIdId)}`,
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

export async function createObservation(assetId, data) {
  return request("POST", `/api/v1/assets/${encodeURIComponent(assetId)}/observations`, { body: data });
}

export async function listObservations(assetId, { category, key } = {}) {
  return request("GET", `/api/v1/assets/${encodeURIComponent(assetId)}/observations`, {
    params: { category, key },
  });
}

export async function getObservation(assetId, observationId) {
  return request(
    "GET",
    `/api/v1/assets/${encodeURIComponent(assetId)}/observations/${encodeURIComponent(observationId)}`,
  );
}

export async function updateObservation(assetId, observationId, data) {
  return request(
    "PUT",
    `/api/v1/assets/${encodeURIComponent(assetId)}/observations/${encodeURIComponent(observationId)}`,
    { body: data },
  );
}

export async function deleteObservation(assetId, observationId) {
  await request(
    "DELETE",
    `/api/v1/assets/${encodeURIComponent(assetId)}/observations/${encodeURIComponent(observationId)}`,
  );
}

export async function listLatestObservations(assetId) {
  return request("GET", `/api/v1/assets/${encodeURIComponent(assetId)}/observations/latest`);
}
