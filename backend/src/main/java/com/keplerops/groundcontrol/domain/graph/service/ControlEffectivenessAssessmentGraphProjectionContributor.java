package com.keplerops.groundcontrol.domain.graph.service;

import com.keplerops.groundcontrol.domain.controls.repository.ControlEffectivenessAssessmentRepository;
import com.keplerops.groundcontrol.domain.graph.model.GraphEdge;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.graph.model.GraphNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Projects {@link com.keplerops.groundcontrol.domain.controls.model.ControlEffectivenessAssessment}
 * rows into the graph for GC-I013. Each assessment is a node carrying both effectiveness ratings,
 * and the sole outgoing edge points at the control it rated (edge type {@code OF_CONTROL}).
 *
 * <p>The {@code operatingEffectiveness} property is the read target for future GC-T003
 * risk-scoring code that traverses Control → ControlEffectivenessAssessment to derive
 * residual-risk influence. No separate link table exists; the parent {@code control_id} FK is
 * authoritative.
 */
@Component
public class ControlEffectivenessAssessmentGraphProjectionContributor implements GraphProjectionContributor {

    private static final String EDGE_OF_CONTROL = "OF_CONTROL";
    private static final String EDGE_SUPPORTED_BY = "SUPPORTED_BY";

    private final ControlEffectivenessAssessmentRepository repository;
    private final com.keplerops.groundcontrol.domain.controls.repository.ControlTestRepository controlTestRepository;

    public ControlEffectivenessAssessmentGraphProjectionContributor(
            ControlEffectivenessAssessmentRepository repository,
            com.keplerops.groundcontrol.domain.controls.repository.ControlTestRepository controlTestRepository) {
        this.repository = repository;
        this.controlTestRepository = controlTestRepository;
    }

    @Override
    public List<GraphNode> contributeNodes(UUID projectId) {
        return repository.findByProjectIdOrderByAssessedAtDesc(projectId).stream()
                .map(assessment -> {
                    Map<String, Object> properties = new LinkedHashMap<>();
                    properties.put("uid", assessment.getUid());
                    properties.put(
                            "designEffectiveness",
                            assessment.getDesignEffectiveness().name());
                    properties.put(
                            "operatingEffectiveness",
                            assessment.getOperatingEffectiveness().name());
                    properties.put("assessor", assessment.getAssessor());
                    properties.put("assessedAt", assessment.getAssessedAt().toString());
                    properties.put("controlUid", assessment.getControl().getUid());
                    return new GraphNode(
                            GraphIds.nodeId(GraphEntityType.CONTROL_EFFECTIVENESS_ASSESSMENT, assessment.getId()),
                            assessment.getId().toString(),
                            GraphEntityType.CONTROL_EFFECTIVENESS_ASSESSMENT,
                            assessment.getProject().getIdentifier(),
                            assessment.getUid(),
                            assessment.getUid(),
                            properties);
                })
                .toList();
    }

    @Override
    public List<GraphEdge> contributeEdges(UUID projectId) {
        var edges = new ArrayList<GraphEdge>();
        for (var assessment : repository.findByProjectIdOrderByAssessedAtDesc(projectId)) {
            edges.add(new GraphEdge(
                    assessment.getId().toString(),
                    EDGE_OF_CONTROL,
                    GraphIds.nodeId(GraphEntityType.CONTROL_EFFECTIVENESS_ASSESSMENT, assessment.getId()),
                    GraphIds.nodeId(
                            GraphEntityType.CONTROL, assessment.getControl().getId()),
                    GraphEntityType.CONTROL_EFFECTIVENESS_ASSESSMENT,
                    GraphEntityType.CONTROL,
                    Map.of()));
            // SUPPORTED_BY edges: one per supporting ControlTest. Skip IDs that don't parse or
            // no longer resolve in the same project so the graph never carries dangling edges
            // (AGE materialization would reject edges whose target nodes don't exist). Edge ID
            // is composite to stay unique across multiple assessments referencing the same test.
            if (assessment.getSupportingTestIds() != null) {
                for (String rawTestId : assessment.getSupportingTestIds()) {
                    UUID testId = tryParseUuid(rawTestId);
                    boolean resolved = testId != null
                            && controlTestRepository
                                    .findByIdAndProjectId(testId, projectId)
                                    .isPresent();
                    if (resolved) {
                        edges.add(new GraphEdge(
                                assessment.getId() + ":supports:" + testId,
                                EDGE_SUPPORTED_BY,
                                GraphIds.nodeId(GraphEntityType.CONTROL_EFFECTIVENESS_ASSESSMENT, assessment.getId()),
                                GraphIds.nodeId(GraphEntityType.CONTROL_TEST, testId),
                                GraphEntityType.CONTROL_EFFECTIVENESS_ASSESSMENT,
                                GraphEntityType.CONTROL_TEST,
                                Map.of()));
                    }
                }
            }
        }
        return edges;
    }

    private static UUID tryParseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
