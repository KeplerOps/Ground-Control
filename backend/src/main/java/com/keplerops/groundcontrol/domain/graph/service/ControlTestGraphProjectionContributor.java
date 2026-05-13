package com.keplerops.groundcontrol.domain.graph.service;

import com.keplerops.groundcontrol.domain.controls.repository.ControlTestRepository;
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
 * Projects {@link com.keplerops.groundcontrol.domain.controls.model.ControlTest} rows into the
 * graph for GC-I012. Each control test is a node tagged with its parent control's UID, and the
 * sole outgoing edge points at the control it tested (edge type {@code OF_CONTROL}).
 *
 * <p>No separate link table exists for control tests; the parent {@code control_id} FK is the
 * authoritative relationship. Downstream graph traversals discover test history via the inverse
 * of this edge.
 */
@Component
public class ControlTestGraphProjectionContributor implements GraphProjectionContributor {

    private static final String EDGE_OF_CONTROL = "OF_CONTROL";

    private final ControlTestRepository controlTestRepository;

    public ControlTestGraphProjectionContributor(ControlTestRepository controlTestRepository) {
        this.controlTestRepository = controlTestRepository;
    }

    @Override
    public List<GraphNode> contributeNodes(UUID projectId) {
        return controlTestRepository.findByProjectIdOrderByTestDateDesc(projectId).stream()
                .map(ct -> {
                    Map<String, Object> properties = new LinkedHashMap<>();
                    properties.put("uid", ct.getUid());
                    properties.put("methodology", ct.getMethodology().name());
                    properties.put("conclusion", ct.getConclusion().name());
                    properties.put("testerIdentity", ct.getTesterIdentity());
                    properties.put("testDate", ct.getTestDate().toString());
                    properties.put("controlUid", ct.getControl().getUid());
                    return new GraphNode(
                            GraphIds.nodeId(GraphEntityType.CONTROL_TEST, ct.getId()),
                            ct.getId().toString(),
                            GraphEntityType.CONTROL_TEST,
                            ct.getProject().getIdentifier(),
                            ct.getUid(),
                            ct.getUid(),
                            properties);
                })
                .toList();
    }

    @Override
    public List<GraphEdge> contributeEdges(UUID projectId) {
        var edges = new ArrayList<GraphEdge>();
        for (var ct : controlTestRepository.findByProjectIdOrderByTestDateDesc(projectId)) {
            edges.add(new GraphEdge(
                    ct.getId().toString(),
                    EDGE_OF_CONTROL,
                    GraphIds.nodeId(GraphEntityType.CONTROL_TEST, ct.getId()),
                    GraphIds.nodeId(GraphEntityType.CONTROL, ct.getControl().getId()),
                    GraphEntityType.CONTROL_TEST,
                    GraphEntityType.CONTROL,
                    Map.of()));
        }
        return edges;
    }
}
