// Enums
export type Status = "DRAFT" | "ACTIVE" | "DEPRECATED" | "ARCHIVED";
export type Priority = "MUST" | "SHOULD" | "COULD" | "WONT";
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
  | "DEPENDS_ON"
  | "PARENT"
  | "REFINES"
  | "CONFLICTS_WITH"
  | "SUPERSEDES"
  | "RELATED";
export type ArtifactType =
  | "GITHUB_ISSUE"
  | "CODE_FILE"
  | "ADR"
  | "CONFIG"
  | "POLICY"
  | "TEST"
  | "SPEC"
  | "PROOF"
  | "DOCUMENTATION";
export const ARTIFACT_TYPES: ArtifactType[] = [
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
export type SyncStatus = "SYNCED" | "NOT_SYNCED" | "ERROR";
export type RevisionType = "ADD" | "MOD" | "DEL";

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
  uid: string;
  title: string;
  statement: string;
  priority: string;
  status: string;
  requirementType: string;
  wave: number;
}

export interface GraphVisualizationResponse {
  nodes: GraphVisualizationNodeResponse[];
  edges: RelationResponse[];
  totalNodes: number;
  totalEdges: number;
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
  statement?: string;
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
