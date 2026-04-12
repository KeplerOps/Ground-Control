package com.keplerops.groundcontrol.domain.graph.service;

import com.keplerops.groundcontrol.domain.graph.model.GraphEdge;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.graph.model.GraphNode;
import com.keplerops.groundcontrol.domain.threatmodels.repository.ThreatModelLinkRepository;
import com.keplerops.groundcontrol.domain.threatmodels.repository.ThreatModelRepository;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelLinkTargetType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ThreatModelGraphProjectionContributor implements GraphProjectionContributor {

    private final ThreatModelRepository threatModelRepository;
    private final ThreatModelLinkRepository threatModelLinkRepository;

    public ThreatModelGraphProjectionContributor(
            ThreatModelRepository threatModelRepository, ThreatModelLinkRepository threatModelLinkRepository) {
        this.threatModelRepository = threatModelRepository;
        this.threatModelLinkRepository = threatModelLinkRepository;
    }

    @Override
    public List<GraphNode> contributeNodes(UUID projectId) {
        // Include all threat models regardless of status. Archived threat models
        // remain in the graph as historical evidence with `status=ARCHIVED` in
        // their node properties — frontends can filter visually. Filtering
        // archived nodes here would create dangling edges from
        // AssetGraphProjectionContributor and RiskGraphProjectionContributor,
        // which both project incoming THREAT_MODEL edges without status checks.
        return threatModelRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(tm -> {
                    Map<String, Object> properties = new LinkedHashMap<>();
                    properties.put("title", tm.getTitle());
                    properties.put("status", tm.getStatus().name());
                    properties.put("threatSource", tm.getThreatSource());
                    properties.put("threatEvent", tm.getThreatEvent());
                    properties.put("effect", tm.getEffect());
                    if (tm.getStride() != null) {
                        properties.put("stride", tm.getStride().name());
                    }
                    properties.put("narrative", tm.getNarrative());
                    properties.put("createdBy", tm.getCreatedBy());
                    return new GraphNode(
                            GraphIds.nodeId(GraphEntityType.THREAT_MODEL, tm.getId()),
                            tm.getId().toString(),
                            GraphEntityType.THREAT_MODEL,
                            tm.getProject().getIdentifier(),
                            tm.getUid(),
                            tm.getUid(),
                            properties);
                })
                .toList();
    }

    @Override
    public List<GraphEdge> contributeEdges(UUID projectId) {
        return threatModelLinkRepository.findByProjectId(projectId).stream()
                .map(link -> toThreatModelLinkEdge(
                        link.getId(),
                        link.getThreatModel().getId(),
                        link.getTargetType(),
                        link.getTargetEntityId(),
                        link.getLinkType().name()))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private GraphEdge toThreatModelLinkEdge(
            UUID linkId,
            UUID threatModelId,
            ThreatModelLinkTargetType targetType,
            UUID targetEntityId,
            String edgeType) {
        if (targetEntityId == null) {
            return null;
        }
        var targetEntityType =
                switch (targetType) {
                    case ASSET -> GraphEntityType.OPERATIONAL_ASSET;
                    case REQUIREMENT -> GraphEntityType.REQUIREMENT;
                    case CONTROL -> GraphEntityType.CONTROL;
                    case RISK_SCENARIO -> GraphEntityType.RISK_SCENARIO;
                    case OBSERVATION -> GraphEntityType.OBSERVATION;
                    case RISK_ASSESSMENT_RESULT -> GraphEntityType.RISK_ASSESSMENT_RESULT;
                    case VERIFICATION_RESULT -> GraphEntityType.VERIFICATION_RESULT;
                    case ARCHITECTURE_MODEL, CODE, ISSUE, EVIDENCE, EXTERNAL -> null;
                };
        if (targetEntityType == null) {
            return null;
        }
        return new GraphEdge(
                linkId.toString(),
                edgeType,
                GraphIds.nodeId(GraphEntityType.THREAT_MODEL, threatModelId),
                GraphIds.nodeId(targetEntityType, targetEntityId),
                GraphEntityType.THREAT_MODEL,
                targetEntityType,
                Map.of());
    }
}
