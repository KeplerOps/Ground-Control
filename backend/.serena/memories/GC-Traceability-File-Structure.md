# Ground Control Traceability - Complete File Structure

## REQUIREMENT-SIDE TRACEABILITY

### Models (Domain)
- `/src/main/java/com/keplerops/groundcontrol/domain/requirements/model/TraceabilityLink.java` (Lines 18-143)
  - Entities: TraceabilityLink class
  - Key fields: requirement, artifactType, artifactIdentifier, linkType, syncStatus, artifactUrl, artifactTitle, lastSyncedAt
  - Constructors: 2 (no-arg, full)
  - Methods: getters/setters for all fields

- `/src/main/java/com/keplerops/groundcontrol/domain/requirements/model/RequirementRelation.java`
  - Entities: RequirementRelation class
  - Key fields: id (UUID), source (Requirement), target (Requirement), relationType, description, createdAt
  - Validation: Self-relation check in constructor
  - Methods: getters, setDescription

- `/src/main/java/com/keplerops/groundcontrol/domain/requirements/model/Requirement.java`
  - Core requirement entity (referenced by TraceabilityLink)

### Enums (State)
- `/src/main/java/com/keplerops/groundcontrol/domain/requirements/state/ArtifactType.java`
  - Values: GITHUB_ISSUE, PULL_REQUEST, CODE_FILE, ADR, CONFIG, POLICY, TEST, SPEC, PROOF, DOCUMENTATION, RISK_SCENARIO, CONTROL (12 total)

- `/src/main/java/com/keplerops/groundcontrol/domain/requirements/state/LinkType.java`
  - Values: IMPLEMENTS, TESTS, DOCUMENTS, CONSTRAINS, VERIFIES (5 total)

- `/src/main/java/com/keplerops/groundcontrol/domain/requirements/state/SyncStatus.java`
  - Values: SYNCED, STALE, BROKEN (3 total)

- `/src/main/java/com/keplerops/groundcontrol/domain/requirements/state/RelationType.java`
  - Values: PARENT, DEPENDS_ON, CONFLICTS_WITH, REFINES, SUPERSEDES, RELATED (6 total)
  - DAG Types used in impact analysis: PARENT, DEPENDS_ON, REFINES

### Repositories (Data Access)
- `/src/main/java/com/keplerops/groundcontrol/domain/requirements/repository/TraceabilityLinkRepository.java`
  - Extends: JpaRepository<TraceabilityLink, UUID>
  - Key methods:
    * findByRequirementId(UUID) → List
    * findByRequirementIdIn(Collection) → List (bulk)
    * findByArtifactType(ArtifactType) → List
    * existsByRequirementIdAndArtifactTypeAndArtifactIdentifierAndLinkType(...) → boolean
    * existsByRequirementId(UUID) → boolean
    * existsByRequirementIdAndLinkType(UUID, LinkType) → boolean
    * findRequirementIdsWithLinkType(Collection, LinkType) → Set (custom query)
    * findByArtifactTypeAndArtifactIdentifierWithRequirement(...) → List (JOIN FETCH)

- RequirementRelationRepository
  - Not fully explored but used by AnalysisService
  - Key method: findActiveByProjectAndRelationTypeIn(UUID, List<RelationType>) → List<RequirementRelation>

- RequirementRepository
  - Not fully explored but core for accessing requirements

### Services (Business Logic)
- `/src/main/java/com/keplerops/groundcontrol/domain/requirements/service/TraceabilityService.java` (Lines 15-90)
  - Responsibilities: Lifecycle management of traceability links
  - Key methods:
    * createLink(UUID requirementId, CreateTraceabilityLinkCommand) → TraceabilityLink
    * createLinkUnchecked(Requirement, ArtifactType, String, LinkType) → TraceabilityLink
    * createLink(Requirement, ArtifactType, String, LinkType) → TraceabilityLink [overload]
    * getLinksForRequirement(UUID) → List<TraceabilityLink>
    * deleteLink(UUID) → void
  - Fields: requirementRepository, traceabilityLinkRepository
  - Constructor: 2 params (requirementRepository, traceabilityLinkRepository)

- `/src/main/java/com/keplerops/groundcontrol/domain/requirements/service/AnalysisService.java` (Lines 31-518)
  - Responsibilities: Comprehensive requirement analysis (impact, cycles, orphans, etc.)
  - Key methods:
    * impactAnalysis(UUID requirementId) → Set<Requirement> [Lines 103-132]
    * detectCycles(UUID projectId) → List<Cycle>
    * findOrphans(UUID projectId) → List<Requirement>
    * findCoverageGaps(UUID projectId, LinkType) → List<Requirement>
    * detectConsistencyViolations(UUID projectId) → List<ConsistencyViolation>
    * analyzeCompleteness(UUID projectId) → CompletenessResult
    * crossWaveValidation(UUID projectId) → List<RelationValidation>
    * getWorkOrder(UUID projectId) → WorkOrderResult
    * getDashboardStats(UUID projectId) → DashboardStats
    * extractSubgraph(UUID projectId, List<String> rootUids) → Subgraph
    * getGraphVisualization(UUID projectId) → GraphProjection
  - Constants:
    * DAG_TYPES = [PARENT, DEPENDS_ON, REFINES] [Lines 35-36]
    * SATISFIED_STATUSES = [ACTIVE, DEPRECATED, ARCHIVED] [Line 38]
    * PRIORITY_ORDER = [MUST→0, SHOULD→1, COULD→2, WONT→3] [Lines 40-41]
  - Fields: requirementRepository, relationRepository, traceabilityLinkRepository, auditService
  - Helper methods:
    * indexRequirementsById() [Lines 204-210]
    * buildDependencyMap() [Lines 212-223]
    * computeBlockingStatuses() [Lines 225-243]
    * findBlockers() [Lines 245-254]
    * groupByWave() [Lines 256-262]
    * buildWorkOrderResult() [Lines 264-292]
    * topoSortWave() [Lines 294-332]
    * buildWaveItems() [Lines 334-367]

- `/src/main/java/com/keplerops/groundcontrol/domain/requirements/service/GraphAlgorithms.java`
  - Purpose: Pure graph algorithms (no Spring dependencies)
  - Key static methods:
    * findCycles(Map<UUID, List<UUID>>) → List<List<UUID>> [DFS with 3-color marking]
    * findReachable(UUID, Map<UUID, List<UUID>>) → Set<UUID> [BFS from single node]
    * findReachableFromMultiple(Set<UUID>, Map<UUID, List<UUID>>) → Set<UUID> [BFS from multiple roots]
    * topologicalSort(Map<UUID, List<UUID>>, Comparator<UUID>) → List<UUID> [Kahn's algorithm]
  - Helper methods:
    * dfs() [private, recursive DFS]
    * reconstructCycle() [private, cycle reconstruction]
  - Constants: WHITE=0, GRAY=1, BLACK=2 [3-color marking for DFS]

### Commands/DTOs
- `/src/main/java/com/keplerops/groundcontrol/domain/requirements/service/CreateTraceabilityLinkCommand.java`
  - Used by TraceabilityService.createLink()

- `/src/main/java/com/keplerops/groundcontrol/domain/requirements/service/TraceabilityLinkChange.java`
  - Audit trail for link changes

- `/src/main/java/com/keplerops/groundcontrol/domain/requirements/service/TraceabilityLinkRevision.java`
  - Revision tracking for links

### Controllers (REST API)
- `/src/main/java/com/keplerops/groundcontrol/api/admin/AnalysisController.java` (Lines 15-109)
  - Base path: /api/v1/analysis
  - Endpoints:
    * GET /cycles → List<CycleResponse>
    * GET /orphans → List<RequirementSummaryResponse>
    * GET /coverage-gaps?linkType=... → List<RequirementSummaryResponse>
    * GET /impact/{id} → List<RequirementSummaryResponse> [IMPACT ANALYSIS]
    * GET /consistency-violations → List<ConsistencyViolationResponse>
    * GET /completeness → CompletenessResponse
    * GET /cross-wave → List<RelationValidationResponse>
    * GET /work-order → WorkOrderResponse
    * GET /dashboard-stats → DashboardStatsResponse
    * GET /semantic-similarity → SimilarityResultResponse
  - Fields: analysisService, similarityService, projectService, defaultSimilarityThreshold
  - Query param: project (optional, uses projectService.resolveProjectId)

- `/src/main/java/com/keplerops/groundcontrol/api/requirements/RequirementGraphController.java`
  - Not fully explored but handles requirement graph endpoints

### Response DTOs
- `/src/main/java/com/keplerops/groundcontrol/api/requirements/TraceabilityLinkResponse.java`
- `/src/main/java/com/keplerops/groundcontrol/api/requirements/TraceabilityLinkHistoryResponse.java`
- `/src/main/java/com/keplerops/groundcontrol/api/requirements/TraceabilityLinkRequest.java`

---

## ASSET-SIDE TRACEABILITY

### Models (Domain)
- `/src/main/java/com/keplerops/groundcontrol/domain/assets/model/OperationalAsset.java`
  - Core asset entity (referenced by AssetLink and AssetRelation)

- `/src/main/java/com/keplerops/groundcontrol/domain/assets/model/AssetLink.java`
  - Purpose: Links asset to requirement/control/risk/etc
  - Key fields: asset (OperationalAsset), targetType (AssetLinkTargetType), targetEntityId (UUID), targetIdentifier (String), linkType (AssetLinkType), targetUrl, targetTitle
  - Uniqueness constraints (2):
    * (asset_id, target_type, target_identifier, link_type)
    * (asset_id, target_type, target_entity_id, link_type)
  - Methods: getters/setters for all fields

- `/src/main/java/com/keplerops/groundcontrol/domain/assets/model/AssetRelation.java`
  - Purpose: Asset-to-asset topology
  - Key fields: source (OperationalAsset), target (OperationalAsset), relationType (AssetRelationType), description, sourceSystem, externalSourceId, collectedAt, confidence
  - Uniqueness constraint: (source_id, target_id, relation_type)
  - Validation: Self-relation check in constructor
  - Methods: getters/setters

### Enums (State)
- `/src/main/java/com/keplerops/groundcontrol/domain/assets/state/AssetLinkType.java`
  - Values: IMPLEMENTS, MITIGATES, SUBJECT_OF, EVIDENCED_BY, GOVERNED_BY, DEPENDS_ON, ASSOCIATED (7 total)

- `/src/main/java/com/keplerops/groundcontrol/domain/assets/state/AssetLinkTargetType.java`
  - Values: REQUIREMENT, CONTROL, RISK_SCENARIO, RISK_REGISTER_RECORD, RISK_ASSESSMENT_RESULT, TREATMENT_PLAN, METHODOLOGY_PROFILE, THREAT_MODEL_ENTRY, FINDING, EVIDENCE, AUDIT, EXTERNAL (12 total)

- `/src/main/java/com/keplerops/groundcontrol/domain/assets/state/AssetRelationType.java`
  - Values: CONTAINS, DEPENDS_ON, COMMUNICATES_WITH, TRUST_BOUNDARY, SUPPORTS, ACCESSES, DATA_FLOW (7 total)

### Repositories (Data Access)
- `/src/main/java/com/keplerops/groundcontrol/domain/assets/repository/AssetLinkRepository.java`
  - Extends: JpaRepository<AssetLink, UUID>

- `/src/main/java/com/keplerops/groundcontrol/domain/assets/repository/AssetRelationRepository.java`
  - Key method: findActiveByProjectId(UUID projectId) → List<AssetRelation>

- OperationalAssetRepository
  - Key methods:
    * findByIdAndProjectId(UUID, UUID) → Optional<OperationalAsset>
    * findByProjectIdAndUidIgnoreCase(UUID, String) → Optional<OperationalAsset>
    * findAllById(Collection<UUID>) → List<OperationalAsset>

- AssetExternalIdRepository
  - Manages external IDs for assets

### Services (Business Logic)
- `/src/main/java/com/keplerops/groundcontrol/domain/assets/service/AssetService.java`
  - Responsibilities: Asset lifecycle and cross-entity linking
  - Fields: assetRepository, relationRepository, linkRepository, externalIdRepository, projectRepository, graphTargetResolverService
  - Methods: (not fully explored)

- `/src/main/java/com/keplerops/groundcontrol/domain/assets/service/AssetTopologyService.java` (Lines 19-129)
  - Responsibilities: Asset topology analysis (impact, cycles, subgraph)
  - Key methods:
    * detectCycles(UUID projectId) → List<AssetCycleResult> [Lines 32-68]
    * impactAnalysis(UUID projectId, UUID assetId) → Set<OperationalAsset> [Lines 70-75]
    * impactAnalysis(UUID assetId) → Set<OperationalAsset> [Lines 77-83] [DEPRECATED]
    * computeImpactAnalysis(UUID projectId, UUID assetId) → Set<OperationalAsset> [Lines 85-98]
    * extractSubgraph(UUID projectId, List<String> rootUids) → AssetSubgraphResult [Lines 100-128]
  - Impact Analysis Algorithm (same as requirement-side):
    1. Get all active relations
    2. Build reverse adjacency map (target → sources)
    3. BFS from input asset through reverse edges
    4. Return all reachable assets
  - Cycle Detection:
    1. Build adjacency set from relations
    2. Convert to List-based adjacency map
    3. Call GraphAlgorithms.findCycles()
    4. Map cycle UUIDs back to asset UIDs
  - Subgraph Extraction:
    1. Bidirectional adjacency map
    2. BFS from multiple roots
    3. Filter relations to subgraph bounds
  - Fields: assetRepository, relationRepository

### Commands/DTOs
- `/src/main/java/com/keplerops/groundcontrol/domain/assets/service/CreateAssetLinkCommand.java`
  - Used by AssetService for creating links

- `/src/main/java/com/keplerops/groundcontrol/domain/assets/service/AssetSubgraphResult.java`
  - Result of subgraph extraction

- AssetCycleResult
  - Contains cycle members (UIDs) and edges

- AssetCycleEdge
  - Edge info: sourceUid, targetUid, relationType

### Controllers (REST API)
- `/src/main/java/com/keplerops/groundcontrol/api/assets/AssetController.java` (Lines 29-293)
  - Base path: /api/v1/assets
  - Key endpoints:
    * POST /links [Lines 154-171] - Create asset link
    * GET /{id}/links [Lines 173-187] - Get links from asset
    * DELETE /{id}/links/{linkId} [Lines 189-195] - Delete link
    * GET /links/target [Lines 197-207] - Get links by target
    * POST /cycles [Lines 269-277] - Detect cycles
    * GET /impact-analysis/{id} [Lines 279-285] - IMPACT ANALYSIS
    * GET /subgraph [Lines 287-292] - Extract subgraph
  - Full list of methods:
    * CRUD: create, list, getById, getByUid, update, delete, archive (Lines 44-101)
    * Relations: createRelation, updateRelation, getRelations, deleteRelation (Lines 103-152)
    * Links: createLink, getLinks, deleteLink, getLinksByTarget (Lines 154-207)
    * External IDs: createExternalId, getExternalIds, updateExternalId, deleteExternalId, findByExternalId (Lines 209-267)
    * Topology: detectCycles, impactAnalysis, extractSubgraph (Lines 269-292)
  - Fields: assetService, topologyService, projectService

### Response DTOs
- `/src/main/java/com/keplerops/groundcontrol/api/assets/AssetLinkResponse.java`
- `/src/main/java/com/keplerops/groundcontrol/api/assets/AssetLinkRequest.java`
- `/src/main/java/com/keplerops/groundcontrol/api/assets/AssetResponse.java`
- `/src/main/java/com/keplerops/groundcontrol/api/assets/AssetSubgraphResponse.java`
- `/src/main/java/com/keplerops/groundcontrol/api/assets/SubgraphRequest.java`
- ObservationResponse
- ObservationController

---

## MIXED GRAPH (CROSS-ENTITY TRACEABILITY)

### Models
- `/src/main/java/com/keplerops/groundcontrol/domain/graph/model/GraphNode.java`
  - Record class
  - Fields: id, domainId, entityType, projectIdentifier, uid, label, properties (Map)

- `/src/main/java/com/keplerops/groundcontrol/domain/graph/model/GraphEdge.java`
  - Record class
  - Fields: id, edgeType, sourceId, targetId, sourceEntityType, targetEntityType, properties (Map)

- `/src/main/java/com/keplerops/groundcontrol/domain/graph/model/GraphProjection.java`
  - Record class
  - Fields: nodes (List<GraphNode>), edges (List<GraphEdge>)

- `/src/main/java/com/keplerops/groundcontrol/domain/graph/model/GraphEntityType.java`
  - Enum with values: REQUIREMENT, OPERATIONAL_ASSET, OBSERVATION, RISK_SCENARIO, RISK_REGISTER_RECORD, RISK_ASSESSMENT_RESULT, TREATMENT_PLAN, METHODOLOGY_PROFILE, CONTROL, CONTROL_LINK, VERIFICATION_RESULT (11 types)

- `/src/main/java/com/keplerops/groundcontrol/domain/graph/model/GraphIds.java`
  - Graph ID utilities

### Services
- `/src/main/java/com/keplerops/groundcontrol/domain/graph/service/MixedGraphService.java` (Lines 22-179)
  - Responsibilities: Cross-entity graph traversal and path finding
  - Key methods:
    * getVisualization(UUID projectId) → GraphProjection [Lines 32-34]
    * extractSubgraph(UUID projectId, List<String> roots) → GraphProjection [Lines 36-39]
    * traverse(UUID projectId, List<String> roots, int maxDepth, List<String> entityTypeNames) → GraphProjection [Lines 41-44]
    * findPaths(UUID projectId, String sourceNodeId, String targetNodeId, int maxDepth, List<String> entityTypeNames) → List<GraphPathResult> [Lines 46-88]
      - Algorithm: BFS between two nodes
      - Returns single path or empty list
      - Supports entity type filtering
    * neighborhoodProjection(UUID projectId, List<String> rootNodeIds, int depth) → GraphProjection [Lines 90-128]
      - Algorithm: BFS up to max depth
      - Supports entity type filtering
  - Helper methods:
    * filterProjection(GraphProjection, Set<GraphEntityType>) → GraphProjection [Lines 130-142]
    * parseEntityTypes(List<String>) → Set<GraphEntityType> [Lines 144-153]
    * buildAdjacency(List<GraphEdge>) → Map<String, Set<String>> [Lines 155-166]
    * buildEdgeLookup(List<GraphEdge>) → Map<String, GraphEdge> [Lines 168-174]
    * undirectedKey(String, String) → String [Lines 176-178]
  - Field: mixedGraphClient (MixedGraphClient)

- `/src/main/java/com/keplerops/groundcontrol/domain/graph/service/MixedGraphClient.java`
  - Client for accessing mixed graph (not fully explored)
  - Key method: getVisualization(UUID projectId) → GraphProjection

- `/src/main/java/com/keplerops/groundcontrol/domain/graph/service/GraphPathResult.java`
  - Result of path finding
  - Contains: path (List<String> of node IDs), edgeTypes (List<String>)

- `/src/main/java/com/keplerops/groundcontrol/domain/graph/service/GraphProjectionContributor.java`
  - Interface for entities contributing to graph projection

- `/src/main/java/com/keplerops/groundcontrol/domain/graph/service/RequirementGraphProjectionContributor.java`
  - Implementation for requirement graph nodes/edges

- `/src/main/java/com/keplerops/groundcontrol/domain/graph/service/AssetGraphProjectionContributor.java`
  - Implementation for asset graph nodes/edges

- `/src/main/java/com/keplerops/groundcontrol/domain/graph/service/RiskGraphProjectionContributor.java`
  - Implementation for risk entity graph nodes/edges

- `/src/main/java/com/keplerops/groundcontrol/domain/graph/service/GraphProjectionRegistryService.java`
  - Service for managing graph projection contributors

- `/src/main/java/com/keplerops/groundcontrol/domain/graph/service/GraphTargetResolverService.java`
  - Service for resolving link targets to entities

### Controllers
- `/src/main/java/com/keplerops/groundcontrol/api/requirements/RequirementGraphController.java`
  - Not fully explored but handles graph endpoints

---

## SUMMARY STATISTICS

**Total Enums:** 10
- Requirement-side: ArtifactType, LinkType, SyncStatus, RelationType
- Asset-side: AssetLinkType, AssetLinkTargetType, AssetRelationType
- Graph: GraphEntityType

**Total Model Classes:** 6
- Requirement-side: TraceabilityLink, RequirementRelation, Requirement
- Asset-side: AssetLink, AssetRelation, OperationalAsset

**Total Repositories:** 5+
- Requirement-side: TraceabilityLinkRepository, RequirementRelationRepository, RequirementRepository
- Asset-side: AssetLinkRepository, AssetRelationRepository, OperationalAssetRepository, AssetExternalIdRepository

**Total Services:** 8+
- Requirement-side: TraceabilityService, AnalysisService, GraphAlgorithms (utility)
- Asset-side: AssetService, AssetTopologyService
- Graph: MixedGraphService, MixedGraphClient, supporting services

**Total Controllers:** 3
- AnalysisController (requirement analysis endpoints)
- AssetController (asset management and analysis endpoints)
- RequirementGraphController (requirement graph endpoints)

**Total REST Endpoints:** 30+
- Analysis endpoints: 10
- Asset endpoints: 20+
- Graph endpoints: TBD
