package com.keplerops.groundcontrol.domain.graph.service;

import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.findings.repository.FindingLinkRepository;
import com.keplerops.groundcontrol.domain.findings.repository.FindingRepository;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkTargetType;
import com.keplerops.groundcontrol.domain.graph.model.GraphEdge;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.graph.model.GraphNode;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class FindingGraphProjectionContributor implements GraphProjectionContributor {

    private final FindingRepository findingRepository;
    private final FindingLinkRepository findingLinkRepository;
    private final OperationalAssetRepository operationalAssetRepository;
    private final RiskScenarioRepository riskScenarioRepository;

    public FindingGraphProjectionContributor(
            FindingRepository findingRepository,
            FindingLinkRepository findingLinkRepository,
            OperationalAssetRepository operationalAssetRepository,
            RiskScenarioRepository riskScenarioRepository) {
        this.findingRepository = findingRepository;
        this.findingLinkRepository = findingLinkRepository;
        this.operationalAssetRepository = operationalAssetRepository;
        this.riskScenarioRepository = riskScenarioRepository;
    }

    @Override
    public List<GraphNode> contributeNodes(UUID projectId) {
        // Include all findings regardless of status so VERIFIED_CLOSED findings stay
        // in the graph as historical evidence (per the same convention used for
        // ThreatModel: status is on the node, not a node-existence filter). Filtering
        // here would create dangling edges from contributors that project inbound
        // FINDING edges without status checks.
        return findingRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(f -> {
                    Map<String, Object> properties = new LinkedHashMap<>();
                    properties.put("title", f.getTitle());
                    properties.put("status", f.getStatus().name());
                    properties.put("findingType", f.getFindingType().name());
                    properties.put("severity", f.getSeverity().name());
                    properties.put("description", f.getDescription());
                    if (f.getRootCauseAnalysis() != null) {
                        properties.put("rootCauseAnalysis", f.getRootCauseAnalysis());
                    }
                    if (f.getOwner() != null) {
                        properties.put("owner", f.getOwner());
                    }
                    if (f.getDueDate() != null) {
                        properties.put("dueDate", f.getDueDate().toString());
                    }
                    if (f.getCreatedBy() != null) {
                        properties.put("createdBy", f.getCreatedBy());
                    }
                    return new GraphNode(
                            GraphIds.nodeId(GraphEntityType.FINDING, f.getId()),
                            f.getId().toString(),
                            GraphEntityType.FINDING,
                            f.getProject().getIdentifier(),
                            f.getUid(),
                            f.getUid(),
                            properties);
                })
                .toList();
    }

    @Override
    public List<GraphEdge> contributeEdges(UUID projectId) {
        // Skip edges to archived targets so the projection never produces dangling
        // edges. Mirrors ThreatModelGraphProjectionContributor's archival contract:
        // AssetGraphProjectionContributor omits archived assets; the risk-scenario
        // contributor omits ARCHIVED scenarios; CONTROL and OBSERVATION are not
        // archived in their own contributors so they don't need a live filter.
        Set<UUID> liveAssetIds =
                Set.copyOf(operationalAssetRepository.findIdsByProjectIdAndArchivedAtIsNull(projectId));
        Set<UUID> liveRiskScenarioIds = Set.copyOf(
                riskScenarioRepository.findIdsByProjectIdAndStatusNot(projectId, RiskScenarioStatus.ARCHIVED));

        return findingLinkRepository.findByProjectId(projectId).stream()
                .map(link -> toFindingLinkEdge(
                        link.getId(),
                        link.getFinding().getId(),
                        link.getTargetType(),
                        link.getTargetEntityId(),
                        link.getLinkType().name(),
                        liveAssetIds,
                        liveRiskScenarioIds))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private GraphEdge toFindingLinkEdge(
            UUID linkId,
            UUID findingId,
            FindingLinkTargetType targetType,
            UUID targetEntityId,
            String edgeType,
            Set<UUID> liveAssetIds,
            Set<UUID> liveRiskScenarioIds) {
        if (targetEntityId == null) {
            return null;
        }
        var targetEntityType =
                switch (targetType) {
                    case CONTROL -> GraphEntityType.CONTROL;
                    case RISK_SCENARIO -> GraphEntityType.RISK_SCENARIO;
                    case ASSET -> GraphEntityType.OPERATIONAL_ASSET;
                    case OBSERVATION -> GraphEntityType.OBSERVATION;
                    case OPERATIONAL_ARTIFACT, EVIDENCE, AUDIT, REMEDIATION_PLAN, EXTERNAL -> null;
                };
        if (targetEntityType == null) {
            return null;
        }
        boolean targetIsLive =
                switch (targetType) {
                    case ASSET -> liveAssetIds.contains(targetEntityId);
                    case RISK_SCENARIO -> liveRiskScenarioIds.contains(targetEntityId);
                    default -> true;
                };
        if (!targetIsLive) {
            return null;
        }
        return new GraphEdge(
                linkId.toString(),
                edgeType,
                GraphIds.nodeId(GraphEntityType.FINDING, findingId),
                GraphIds.nodeId(targetEntityType, targetEntityId),
                GraphEntityType.FINDING,
                targetEntityType,
                Map.of());
    }
}
