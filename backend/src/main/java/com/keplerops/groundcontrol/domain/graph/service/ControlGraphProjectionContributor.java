package com.keplerops.groundcontrol.domain.graph.service;

import com.keplerops.groundcontrol.domain.controls.repository.ControlLinkRepository;
import com.keplerops.groundcontrol.domain.controls.repository.ControlRepository;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
import com.keplerops.groundcontrol.domain.graph.model.GraphEdge;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.graph.model.GraphNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ControlGraphProjectionContributor implements GraphProjectionContributor {

    private final ControlRepository controlRepository;
    private final ControlLinkRepository controlLinkRepository;

    public ControlGraphProjectionContributor(
            ControlRepository controlRepository, ControlLinkRepository controlLinkRepository) {
        this.controlRepository = controlRepository;
        this.controlLinkRepository = controlLinkRepository;
    }

    @Override
    public List<GraphNode> contributeNodes(UUID projectId) {
        return controlRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(control -> {
                    Map<String, Object> properties = new LinkedHashMap<>();
                    properties.put("title", control.getTitle());
                    properties.put("uid", control.getUid());
                    properties.put("description", control.getDescription());
                    properties.put("status", control.getStatus().name());
                    properties.put(
                            "controlFunction", control.getControlFunction().name());
                    properties.put("owner", control.getOwner());
                    properties.put("category", control.getCategory());
                    properties.put("source", control.getSource());
                    return new GraphNode(
                            GraphIds.nodeId(GraphEntityType.CONTROL, control.getId()),
                            control.getId().toString(),
                            GraphEntityType.CONTROL,
                            control.getProject().getIdentifier(),
                            control.getUid(),
                            control.getTitle(),
                            properties);
                })
                .toList();
    }

    @Override
    public List<GraphEdge> contributeEdges(UUID projectId) {
        return controlLinkRepository.findByProjectId(projectId).stream()
                .map(link -> toControlLinkEdge(
                        link.getId(),
                        link.getControl().getId(),
                        link.getTargetType(),
                        link.getTargetEntityId(),
                        link.getLinkType().name()))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private GraphEdge toControlLinkEdge(
            UUID linkId, UUID controlId, ControlLinkTargetType targetType, UUID targetEntityId, String edgeType) {
        if (targetEntityId == null) {
            return null;
        }
        var targetEntityType =
                switch (targetType) {
                    case ASSET -> GraphEntityType.OPERATIONAL_ASSET;
                    case REQUIREMENT -> GraphEntityType.REQUIREMENT;
                    case RISK_SCENARIO -> GraphEntityType.RISK_SCENARIO;
                    case RISK_REGISTER_RECORD -> GraphEntityType.RISK_REGISTER_RECORD;
                    case RISK_ASSESSMENT_RESULT -> GraphEntityType.RISK_ASSESSMENT_RESULT;
                    case TREATMENT_PLAN -> GraphEntityType.TREATMENT_PLAN;
                    case METHODOLOGY_PROFILE -> GraphEntityType.METHODOLOGY_PROFILE;
                    case OBSERVATION -> GraphEntityType.OBSERVATION;
                    case EVIDENCE, FINDING, CODE, CONFIGURATION, OPERATIONAL_ARTIFACT, EXTERNAL -> null;
                };
        if (targetEntityType == null) {
            return null;
        }
        return new GraphEdge(
                linkId.toString(),
                edgeType,
                GraphIds.nodeId(GraphEntityType.CONTROL, controlId),
                GraphIds.nodeId(targetEntityType, targetEntityId),
                GraphEntityType.CONTROL,
                targetEntityType,
                Map.of());
    }
}
