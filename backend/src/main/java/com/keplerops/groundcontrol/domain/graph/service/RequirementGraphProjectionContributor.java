package com.keplerops.groundcontrol.domain.graph.service;

import com.keplerops.groundcontrol.domain.graph.model.GraphEdge;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.graph.model.GraphNode;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RequirementGraphProjectionContributor implements GraphProjectionContributor {

    private final RequirementRepository requirementRepository;
    private final RequirementRelationRepository relationRepository;

    public RequirementGraphProjectionContributor(
            RequirementRepository requirementRepository, RequirementRelationRepository relationRepository) {
        this.requirementRepository = requirementRepository;
        this.relationRepository = relationRepository;
    }

    @Override
    public List<GraphNode> contributeNodes(UUID projectId) {
        return requirementRepository.findByProjectIdAndArchivedAtIsNull(projectId).stream()
                .map(requirement -> {
                    Map<String, Object> properties = new LinkedHashMap<>();
                    properties.put("title", requirement.getTitle());
                    properties.put("statement", requirement.getStatement());
                    properties.put("priority", requirement.getPriority().name());
                    properties.put("status", requirement.getStatus().name());
                    properties.put(
                            "requirementType", requirement.getRequirementType().name());
                    properties.put("wave", requirement.getWave());
                    return new GraphNode(
                            GraphIds.nodeId(GraphEntityType.REQUIREMENT, requirement.getId()),
                            requirement.getId().toString(),
                            GraphEntityType.REQUIREMENT,
                            requirement.getProject().getIdentifier(),
                            requirement.getUid(),
                            requirement.getUid(),
                            properties);
                })
                .toList();
    }

    @Override
    public List<GraphEdge> contributeEdges(UUID projectId) {
        return relationRepository.findActiveWithSourceAndTargetByProjectId(projectId).stream()
                .map(relation -> {
                    Map<String, Object> properties = new LinkedHashMap<>();
                    properties.put("createdAt", relation.getCreatedAt());
                    properties.put("sourceUid", relation.getSource().getUid());
                    properties.put("targetUid", relation.getTarget().getUid());
                    return new GraphEdge(
                            relation.getId().toString(),
                            relation.getRelationType().name(),
                            GraphIds.nodeId(
                                    GraphEntityType.REQUIREMENT,
                                    relation.getSource().getId()),
                            GraphIds.nodeId(
                                    GraphEntityType.REQUIREMENT,
                                    relation.getTarget().getId()),
                            GraphEntityType.REQUIREMENT,
                            GraphEntityType.REQUIREMENT,
                            properties);
                })
                .toList();
    }
}
