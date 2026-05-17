// Enums
//
// The requirement/traceability enum vocabularies below mirror the backend Java
// enums under `domain/requirements/state/`, which are the single source of truth
// (ADR-034). `tools/policy/checks.py::run_enum_contract_check` (run by
// `bin/policy` in CI) fails the build on any drift; `enum-contract.test.ts` is
// the developer-local mirror of that gate.
export type Status = "DRAFT" | "ACTIVE" | "DEPRECATED" | "ARCHIVED";
export const STATUSES: Status[] = ["DRAFT", "ACTIVE", "DEPRECATED", "ARCHIVED"];
export type Priority = "MUST" | "SHOULD" | "COULD" | "WONT";
export const PRIORITIES: Priority[] = ["MUST", "SHOULD", "COULD", "WONT"];
export type ControlFunction =
  | "PREVENTIVE"
  | "DETECTIVE"
  | "CORRECTIVE"
  | "COMPENSATING";
export const CONTROL_FUNCTIONS: ControlFunction[] = [
  "PREVENTIVE",
  "DETECTIVE",
  "CORRECTIVE",
  "COMPENSATING",
];
export type RequirementType =
  | "FUNCTIONAL"
  | "NON_FUNCTIONAL"
  | "CONSTRAINT"
  | "INTERFACE";
export const REQUIREMENT_TYPES: RequirementType[] = [
  "FUNCTIONAL",
  "NON_FUNCTIONAL",
  "CONSTRAINT",
  "INTERFACE",
];
export type RelationType =
  | "PARENT"
  | "DEPENDS_ON"
  | "CONFLICTS_WITH"
  | "REFINES"
  | "SUPERSEDES"
  | "RELATED";
export const RELATION_TYPES: RelationType[] = [
  "PARENT",
  "DEPENDS_ON",
  "CONFLICTS_WITH",
  "REFINES",
  "SUPERSEDES",
  "RELATED",
];
export type ArtifactType =
  | "GITHUB_ISSUE"
  | "PULL_REQUEST"
  | "CODE_FILE"
  | "ADR"
  | "CONFIG"
  | "POLICY"
  | "TEST"
  | "SPEC"
  | "PROOF"
  | "DOCUMENTATION"
  | "RISK_SCENARIO"
  | "CONTROL";
export const ARTIFACT_TYPES: ArtifactType[] = [
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
export type LinkType =
  | "IMPLEMENTS"
  | "TESTS"
  | "DOCUMENTS"
  | "CONSTRAINS"
  | "VERIFIES";
export const LINK_TYPES: LinkType[] = [
  "IMPLEMENTS",
  "TESTS",
  "DOCUMENTS",
  "CONSTRAINS",
  "VERIFIES",
];
export type SyncStatus = "SYNCED" | "STALE" | "BROKEN";
export type RevisionType = "ADD" | "MOD" | "DEL";
export type PackType = "CONTROL_PACK" | "REQUIREMENTS_PACK" | "CUSTOM";
export type CatalogStatus = "AVAILABLE" | "WITHDRAWN" | "SUPERSEDED";
export type PackRegistryImportFormat = "AUTO" | "OSCAL_JSON" | "GC_MANIFEST";
export const PACK_REGISTRY_IMPORT_FORMATS: PackRegistryImportFormat[] = [
  "AUTO",
  "OSCAL_JSON",
  "GC_MANIFEST",
];
export type TestCaseStatus =
  | "DRAFT"
  | "APPROVED"
  | "DEPRECATED"
  | "ARCHIVED";
export const TEST_CASE_STATUSES: TestCaseStatus[] = [
  "DRAFT",
  "APPROVED",
  "DEPRECATED",
  "ARCHIVED",
];
export type TestCaseType = "MANUAL" | "AUTOMATED" | "HYBRID";
export const TEST_CASE_TYPES: TestCaseType[] = ["MANUAL", "AUTOMATED", "HYBRID"];
export type TestCasePriority = "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";
export const TEST_CASE_PRIORITIES: TestCasePriority[] = [
  "CRITICAL",
  "HIGH",
  "MEDIUM",
  "LOW",
];

// TC-002 / ADR-041 — step-based test case format. Rich-text fields hold
// CommonMark Markdown by convention (inline images via `![alt](url)`).
// Mirrors backend TestCaseStepResponse / TestCaseStepRequest field-for-field.
export interface TestCaseStepResponse {
  id: string;
  testCaseId: string;
  stepNumber: number;
  action: string;
  expectedResult: string;
  actualResult: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TestCaseStepRequest {
  stepNumber: number;
  action: string;
  expectedResult: string;
  actualResult?: string | null;
}

export interface UpdateTestCaseStepRequest {
  stepNumber?: number;
  action?: string;
  expectedResult?: string;
  actualResult?: string | null;
  clearActualResult?: boolean;
}

// TC-004 / ADR-042 — authored test-case format axis. Mirrors backend
// TestCaseFormat.java. Set on test-case create and immutable thereafter.
export type TestCaseFormat = "STEP_BASED" | "GHERKIN";
export const TEST_CASE_FORMATS: TestCaseFormat[] = ["STEP_BASED", "GHERKIN"];

// Mirrors backend TestCaseGherkinResponse / TestCaseGherkinRequest. The
// backend stores .feature source verbatim; clients render with a Gherkin-aware
// syntax highlighter at their discretion (no HTML sink is wired through this
// surface, per ADR-042 §Rendering).
export interface TestCaseGherkinResponse {
  id: string;
  testCaseId: string;
  source: string;
  createdAt: string;
  updatedAt: string;
}

export interface TestCaseGherkinRequest {
  source: string;
}

export interface UpdateTestCaseGherkinRequest {
  source: string;
}
export type GraphEntityType =
  | "REQUIREMENT"
  | "OPERATIONAL_ASSET"
  | "OBSERVATION"
  | "RISK_SCENARIO"
  | "RISK_REGISTER_RECORD"
  | "RISK_ASSESSMENT_RESULT"
  | "TREATMENT_PLAN"
  | "METHODOLOGY_PROFILE";

// GC-M012 asset ownership / criticality / scope vocabularies. Mirrors the
// backend `AssetCriticality`, `AssetEnvironment`, and `AssetScope` enums; ADR-012
// records them as L0 pure value enums. Distinct from `quality_gate.scopeStatus`,
// control `implementationScope`, and risk `assetScopeSummary`.
export type AssetCriticality = "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";
export const ASSET_CRITICALITIES: AssetCriticality[] = [
  "CRITICAL",
  "HIGH",
  "MEDIUM",
  "LOW",
];

export type AssetEnvironment =
  | "PRODUCTION"
  | "STAGING"
  | "DEVELOPMENT"
  | "TEST"
  | "NON_PRODUCTION"
  | "OTHER";
export const ASSET_ENVIRONMENTS: AssetEnvironment[] = [
  "PRODUCTION",
  "STAGING",
  "DEVELOPMENT",
  "TEST",
  "NON_PRODUCTION",
  "OTHER",
];

export type AssetScope = "IN_SCOPE" | "OUT_OF_SCOPE";
export const ASSET_SCOPES: AssetScope[] = ["IN_SCOPE", "OUT_OF_SCOPE"];

// Responses
export interface ProjectResponse {
  id: string;
  identifier: string;
  name: string;
  description: string;
  createdAt: string;
  updatedAt: string;
}

export interface RequirementResponse {
  id: string;
  graphNodeId: string;
  uid: string;
  projectIdentifier: string;
  title: string;
  statement: string;
  rationale: string;
  requirementType: RequirementType;
  priority: Priority;
  status: Status;
  wave: number;
  createdAt: string;
  updatedAt: string;
  archivedAt: string | null;
}

export interface RelationResponse {
  id: string;
  sourceId: string;
  sourceUid: string;
  targetId: string;
  targetUid: string;
  relationType: RelationType;
  createdAt: string;
}

export interface GraphVisualizationNodeResponse {
  id: string;
  domainId: string;
  entityType: GraphEntityType | string;
  projectIdentifier: string;
  uid: string | null;
  label: string;
  properties: Record<string, unknown>;
}

export interface GraphEdgeResponse {
  id: string;
  edgeType: string;
  sourceId: string;
  targetId: string;
  sourceEntityType: string;
  targetEntityType: string;
  properties: Record<string, unknown>;
}

export interface GraphVisualizationResponse {
  nodes: GraphVisualizationNodeResponse[];
  edges: GraphEdgeResponse[];
  totalNodes: number;
  totalEdges: number;
}

export interface GraphNeighborhoodResponse extends GraphVisualizationResponse {
  rootNodeIds: string[];
}

export interface GraphPathResponse {
  nodeIds: string[];
  edgeTypes: string[];
}

export interface PackDependencyResponse {
  packId: string;
  versionConstraint: string | null;
}

export interface RegisteredControlPackEntryResponse {
  uid: string;
  title: string;
  controlFunction: ControlFunction;
  description?: string | null;
  objective?: string | null;
  owner?: string | null;
  implementationScope?: string | null;
  methodologyFactors?: Record<string, unknown> | null;
  effectiveness?: Record<string, unknown> | null;
  category?: string | null;
  source?: string | null;
  implementationGuidance?: string | null;
  expectedEvidence?: Record<string, unknown>[] | null;
  frameworkMappings?: Record<string, unknown>[] | null;
}

export interface PackRegistryEntryResponse {
  id: string;
  projectIdentifier: string;
  packId: string;
  packType: PackType;
  publisher: string | null;
  version: string;
  description: string | null;
  sourceUrl: string | null;
  checksum: string | null;
  signatureInfo: Record<string, unknown> | null;
  compatibility: Record<string, unknown> | null;
  dependencies: PackDependencyResponse[] | null;
  controlPackEntries: RegisteredControlPackEntryResponse[] | null;
  provenance: Record<string, unknown> | null;
  registryMetadata: Record<string, unknown> | null;
  catalogStatus: CatalogStatus;
  registeredAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface TraceabilityLinkResponse {
  id: string;
  requirementId: string;
  artifactType: ArtifactType;
  artifactIdentifier: string;
  artifactUrl: string;
  artifactTitle: string;
  linkType: LinkType;
  syncStatus: SyncStatus;
  lastSyncedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RequirementHistoryResponse {
  revisionNumber: number;
  revisionType: RevisionType;
  timestamp: string;
  actor: string;
  snapshot: RequirementResponse;
}

export interface RelationHistoryResponse {
  revisionNumber: number;
  revisionType: RevisionType;
  timestamp: string;
  actor: string;
  snapshot: {
    id: string;
    sourceId: string;
    targetId: string;
    relationType: RelationType;
    description: string;
    createdAt: string;
  };
}

export interface TraceabilityLinkHistoryResponse {
  revisionNumber: number;
  revisionType: RevisionType;
  timestamp: string;
  actor: string;
  snapshot: TraceabilityLinkResponse;
}

export type ChangeCategory = "REQUIREMENT" | "RELATION" | "TRACEABILITY_LINK";
export const CHANGE_CATEGORIES: ChangeCategory[] = [
  "REQUIREMENT",
  "RELATION",
  "TRACEABILITY_LINK",
];

export interface FieldChangeResponse {
  oldValue: unknown;
  newValue: unknown;
}

export interface TimelineEntryResponse {
  revisionNumber: number;
  revisionType: RevisionType;
  timestamp: string;
  actor: string;
  changeCategory: ChangeCategory;
  entityId: string;
  snapshot: Record<string, unknown>;
  changes: Record<string, FieldChangeResponse>;
}

export interface RequirementSummaryResponse {
  id: string;
  uid: string;
  title: string;
  status: Status;
  wave: number;
}

export interface CycleEdgeResponse {
  sourceUid: string;
  targetUid: string;
  relationType: RelationType;
}

export interface CycleResponse {
  members: string[];
  edges: CycleEdgeResponse[];
}

export interface RelationValidationResponse {
  id: string;
  sourceId: string;
  sourceUid: string;
  sourceWave: number;
  targetId: string;
  targetUid: string;
  targetWave: number;
  relationType: RelationType;
}

export interface ConsistencyViolationResponse {
  relationId: string;
  sourceId: string;
  sourceUid: string;
  sourceStatus: Status;
  targetId: string;
  targetUid: string;
  targetStatus: Status;
  relationType: RelationType;
  violationType: string;
}

export interface CompletenessIssueResponse {
  uid: string;
  issue: string;
}

export interface CompletenessResponse {
  total: number;
  byStatus: Record<string, number>;
  issues: CompletenessIssueResponse[];
}

export interface WaveStatsResponse {
  wave: number | null;
  total: number;
  byStatus: Record<string, number>;
}

export interface CoverageStatsResponse {
  total: number;
  covered: number;
  percentage: number;
}

export interface RecentChangeResponse {
  uid: string;
  title: string;
  revisionType: RevisionType;
  timestamp: string;
  actor: string;
}

export interface DashboardStatsResponse {
  totalRequirements: number;
  byStatus: Record<string, number>;
  byWave: WaveStatsResponse[];
  coverageByLinkType: Record<string, CoverageStatsResponse>;
  recentChanges: RecentChangeResponse[];
}

export interface BulkStatusTransitionResponse {
  succeeded: RequirementResponse[];
  failed: Array<{ id: string; error: string }>;
  totalRequested: number;
  totalSucceeded: number;
  totalFailed: number;
}

export interface SyncResultResponse {
  syncId: string;
  syncedAt: string;
  issuesFetched: number;
  issuesCreated: number;
  issuesUpdated: number;
  linksUpdated: number;
  errors: string[];
}

export interface GitHubIssueResponse {
  issueUrl: string;
  issueNumber: number;
  traceabilityLinkId: string;
  warning: string | null;
}

export interface ImportResultResponse {
  importId: string;
  importedAt: string;
  requirementsParsed: number;
  requirementsCreated: number;
  requirementsUpdated: number;
  relationsCreated: number;
  relationsSkipped: number;
  traceabilityLinksCreated: number;
  traceabilityLinksSkipped: number;
  errors: string[];
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// Requests
export interface ProjectRequest {
  identifier: string;
  name: string;
  description?: string;
}

export interface UpdateProjectRequest {
  name: string;
  description?: string;
}

export interface RequirementRequest {
  uid: string;
  title: string;
  // Backend RequirementRequest annotates `statement` @NotBlank — required on create.
  statement: string;
  rationale?: string;
  requirementType?: RequirementType;
  priority?: Priority;
  wave?: number;
}

export interface UpdateRequirementRequest {
  title: string;
  statement: string;
  rationale?: string;
  requirementType?: RequirementType;
  priority?: Priority;
  wave?: number;
}

export interface RelationRequest {
  targetId: string;
  relationType: RelationType;
}

export interface TraceabilityLinkRequest {
  artifactType: ArtifactType;
  artifactIdentifier: string;
  artifactUrl?: string;
  artifactTitle?: string;
  linkType: LinkType;
}

export interface CloneRequirementRequest {
  newUid: string;
  copyRelations?: boolean;
}

export interface GitHubIssueRequest {
  requirementUid: string;
  repo?: string;
  extraBody?: string;
  labels?: string[];
}
