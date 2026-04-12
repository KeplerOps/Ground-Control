package com.keplerops.groundcontrol.domain.graph.service;

import com.keplerops.groundcontrol.domain.graph.model.GraphEdge;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.graph.model.GraphNode;
import com.keplerops.groundcontrol.domain.verification.repository.VerificationResultRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Materialises {@code VerificationResult} entities into the mixed graph as
 * {@link GraphEntityType#VERIFICATION_RESULT} nodes, plus a {@code VERIFIES}
 * edge to the linked requirement when set.
 *
 * <p>This contributor exists so that other contributors (for example
 * {@code ThreatModelGraphProjectionContributor}) can safely emit edges that
 * land on {@code VERIFICATION_RESULT} nodes without leaving dangling
 * references in the graph.
 */
@Component
public class VerificationResultGraphProjectionContributor implements GraphProjectionContributor {

    private final VerificationResultRepository verificationResultRepository;

    public VerificationResultGraphProjectionContributor(VerificationResultRepository verificationResultRepository) {
        this.verificationResultRepository = verificationResultRepository;
    }

    @Override
    public List<GraphNode> contributeNodes(UUID projectId) {
        return verificationResultRepository.findByProjectIdOrderByVerifiedAtDesc(projectId).stream()
                .map(result -> {
                    Map<String, Object> properties = new LinkedHashMap<>();
                    properties.put("prover", result.getProver());
                    properties.put("property", result.getProperty());
                    properties.put("result", result.getResult().name());
                    properties.put("assuranceLevel", result.getAssuranceLevel().name());
                    properties.put("verifiedAt", result.getVerifiedAt());
                    properties.put("expiresAt", result.getExpiresAt());
                    return new GraphNode(
                            GraphIds.nodeId(GraphEntityType.VERIFICATION_RESULT, result.getId()),
                            result.getId().toString(),
                            GraphEntityType.VERIFICATION_RESULT,
                            result.getProject().getIdentifier(),
                            null,
                            result.getProver(),
                            properties);
                })
                .toList();
    }

    @Override
    public List<GraphEdge> contributeEdges(UUID projectId) {
        // Skip edges to archived requirements: RequirementGraphProjectionContributor
        // omits archived requirement nodes (`findByProjectIdAndArchivedAtIsNull`),
        // so emitting an edge here would either dangle (projection-only mode) or
        // be silently dropped by AGE materialization. Behaviour must stay
        // consistent across both modes.
        return verificationResultRepository.findByProjectIdOrderByVerifiedAtDesc(projectId).stream()
                .filter(result -> result.getRequirement() != null
                        && result.getRequirement().getArchivedAt() == null)
                .map(result -> new GraphEdge(
                        "VERIFIES:" + result.getId() + ":"
                                + result.getRequirement().getId(),
                        "VERIFIES",
                        GraphIds.nodeId(GraphEntityType.VERIFICATION_RESULT, result.getId()),
                        GraphIds.nodeId(
                                GraphEntityType.REQUIREMENT,
                                result.getRequirement().getId()),
                        GraphEntityType.VERIFICATION_RESULT,
                        GraphEntityType.REQUIREMENT,
                        Map.of()))
                .toList();
    }
}
