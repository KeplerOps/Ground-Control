package com.keplerops.groundcontrol.domain.graph.service;

import com.keplerops.groundcontrol.domain.assets.repository.AssetLinkRepository;
import com.keplerops.groundcontrol.domain.assets.repository.AssetRelationRepository;
import com.keplerops.groundcontrol.domain.assets.repository.ObservationRepository;
import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
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
public class AssetGraphProjectionContributor implements GraphProjectionContributor {

    private final OperationalAssetRepository assetRepository;
    private final ObservationRepository observationRepository;
    private final AssetRelationRepository assetRelationRepository;
    private final AssetLinkRepository assetLinkRepository;

    public AssetGraphProjectionContributor(
            OperationalAssetRepository assetRepository,
            ObservationRepository observationRepository,
            AssetRelationRepository assetRelationRepository,
            AssetLinkRepository assetLinkRepository) {
        this.assetRepository = assetRepository;
        this.observationRepository = observationRepository;
        this.assetRelationRepository = assetRelationRepository;
        this.assetLinkRepository = assetLinkRepository;
    }

    @Override
    public List<GraphNode> contributeNodes(UUID projectId) {
        var assetNodes = assetRepository.findByProjectIdAndArchivedAtIsNull(projectId).stream()
                .map(asset -> {
                    Map<String, Object> properties = new LinkedHashMap<>();
                    properties.put("title", asset.getName());
                    properties.put("name", asset.getName());
                    properties.put("description", asset.getDescription());
                    properties.put("assetType", asset.getAssetType().name());
                    properties.put("archivedAt", asset.getArchivedAt());
                    return new GraphNode(
                            GraphIds.nodeId(GraphEntityType.OPERATIONAL_ASSET, asset.getId()),
                            asset.getId().toString(),
                            GraphEntityType.OPERATIONAL_ASSET,
                            asset.getProject().getIdentifier(),
                            asset.getUid(),
                            asset.getUid(),
                            properties);
                })
                .toList();
        var observationNodes = observationRepository.findByProjectId(projectId).stream()
                .map(observation -> {
                    Map<String, Object> properties = new LinkedHashMap<>();
                    properties.put("title", observation.getObservationKey());
                    properties.put("category", observation.getCategory().name());
                    properties.put("observationKey", observation.getObservationKey());
                    properties.put("observationValue", observation.getObservationValue());
                    properties.put("source", observation.getSource());
                    properties.put("observedAt", observation.getObservedAt());
                    properties.put("expiresAt", observation.getExpiresAt());
                    properties.put("confidence", observation.getConfidence());
                    properties.put("evidenceRef", observation.getEvidenceRef());
                    return new GraphNode(
                            GraphIds.nodeId(GraphEntityType.OBSERVATION, observation.getId()),
                            observation.getId().toString(),
                            GraphEntityType.OBSERVATION,
                            observation.getAsset().getProject().getIdentifier(),
                            null,
                            observation.getObservationKey(),
                            properties);
                })
                .toList();
        return java.util.stream.Stream.concat(assetNodes.stream(), observationNodes.stream())
                .toList();
    }

    @Override
    public List<GraphEdge> contributeEdges(UUID projectId) {
        var relationEdges = assetRelationRepository.findActiveByProjectId(projectId).stream()
                .map(relation -> new GraphEdge(
                        relation.getId().toString(),
                        relation.getRelationType().name(),
                        GraphIds.nodeId(
                                GraphEntityType.OPERATIONAL_ASSET,
                                relation.getSource().getId()),
                        GraphIds.nodeId(
                                GraphEntityType.OPERATIONAL_ASSET,
                                relation.getTarget().getId()),
                        GraphEntityType.OPERATIONAL_ASSET,
                        GraphEntityType.OPERATIONAL_ASSET,
                        Map.of("createdAt", relation.getCreatedAt())))
                .toList();

        var observationEdges = observationRepository.findByProjectId(projectId).stream()
                .map(observation -> new GraphEdge(
                        "OBSERVED_ON:" + observation.getId() + ":"
                                + observation.getAsset().getId(),
                        "OBSERVED_ON",
                        GraphIds.nodeId(GraphEntityType.OBSERVATION, observation.getId()),
                        GraphIds.nodeId(
                                GraphEntityType.OPERATIONAL_ASSET,
                                observation.getAsset().getId()),
                        GraphEntityType.OBSERVATION,
                        GraphEntityType.OPERATIONAL_ASSET,
                        Map.of("observedAt", observation.getObservedAt())))
                .toList();

        var internalLinkEdges = assetLinkRepository.findByProjectId(projectId).stream()
                .map(link -> toInternalLinkEdge(
                        link.getId(),
                        link.getAsset().getId(),
                        link.getTargetType(),
                        link.getTargetEntityId(),
                        link.getLinkType().name()))
                .filter(java.util.Objects::nonNull)
                .toList();

        return java.util.stream.Stream.of(relationEdges, observationEdges, internalLinkEdges)
                .flatMap(List::stream)
                .toList();
    }

    private GraphEdge toInternalLinkEdge(
            UUID linkId, UUID assetId, AssetLinkTargetType targetType, UUID targetEntityId, String edgeType) {
        if (targetEntityId == null) {
            return null;
        }
        var targetEntityType =
                switch (targetType) {
                    case REQUIREMENT -> GraphEntityType.REQUIREMENT;
                    case RISK_SCENARIO -> GraphEntityType.RISK_SCENARIO;
                    case RISK_REGISTER_RECORD -> GraphEntityType.RISK_REGISTER_RECORD;
                    case RISK_ASSESSMENT_RESULT -> GraphEntityType.RISK_ASSESSMENT_RESULT;
                    case TREATMENT_PLAN -> GraphEntityType.TREATMENT_PLAN;
                    case METHODOLOGY_PROFILE -> GraphEntityType.METHODOLOGY_PROFILE;
                    case CONTROL -> GraphEntityType.CONTROL;
                    case THREAT_MODEL_ENTRY, FINDING, EVIDENCE, AUDIT, ISSUE, CODE, CONFIGURATION, EXTERNAL -> null;
                };
        if (targetEntityType == null) {
            return null;
        }
        return new GraphEdge(
                linkId.toString(),
                edgeType,
                GraphIds.nodeId(GraphEntityType.OPERATIONAL_ASSET, assetId),
                GraphIds.nodeId(targetEntityType, targetEntityId),
                GraphEntityType.OPERATIONAL_ASSET,
                targetEntityType,
                Map.of());
    }
}
