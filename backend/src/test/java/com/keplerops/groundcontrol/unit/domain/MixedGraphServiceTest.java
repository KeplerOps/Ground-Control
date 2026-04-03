package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.graph.model.GraphEdge;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphNode;
import com.keplerops.groundcontrol.domain.graph.model.GraphProjection;
import com.keplerops.groundcontrol.domain.graph.service.MixedGraphClient;
import com.keplerops.groundcontrol.domain.graph.service.MixedGraphService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MixedGraphServiceTest {

    @Mock
    private MixedGraphClient mixedGraphClient;

    @InjectMocks
    private MixedGraphService mixedGraphService;

    private final UUID projectId = UUID.randomUUID();

    @Test
    void getVisualizationFiltersNodesAndPrunesEdges() {
        var requirement = node("REQUIREMENT:req-1", GraphEntityType.REQUIREMENT, "REQ-1");
        var asset = node("OPERATIONAL_ASSET:asset-1", GraphEntityType.OPERATIONAL_ASSET, "ASSET-1");
        var projection = new GraphProjection(
                List.of(requirement, asset),
                List.of(edge(
                        "edge-1",
                        "ASSOCIATED",
                        requirement.id(),
                        asset.id(),
                        GraphEntityType.REQUIREMENT,
                        GraphEntityType.OPERATIONAL_ASSET)));
        when(mixedGraphClient.getVisualization(projectId)).thenReturn(projection);

        var filtered = mixedGraphService.getVisualization(projectId, List.of("REQUIREMENT"));

        assertThat(filtered.nodes()).containsExactly(requirement);
        assertThat(filtered.edges()).isEmpty();
    }

    @Test
    void extractSubgraphReturnsNeighborhoodUpToRequestedDepth() {
        var a = node("REQUIREMENT:a", GraphEntityType.REQUIREMENT, "REQ-A");
        var b = node("OPERATIONAL_ASSET:b", GraphEntityType.OPERATIONAL_ASSET, "ASSET-B");
        var c = node("RISK_SCENARIO:c", GraphEntityType.RISK_SCENARIO, "RS-C");
        var projection = new GraphProjection(
                List.of(a, b, c),
                List.of(
                        edge("ab", "ASSOCIATED", a.id(), b.id(), a.entityType(), b.entityType()),
                        edge("bc", "AFFECTS", b.id(), c.id(), b.entityType(), c.entityType())));
        when(mixedGraphClient.getVisualization(projectId)).thenReturn(projection);

        var subgraph = mixedGraphService.extractSubgraph(projectId, List.of(a.id()), 1, null);

        assertThat(subgraph.nodes()).containsExactlyInAnyOrder(a, b);
        assertThat(subgraph.edges()).extracting(GraphEdge::id).containsExactly("ab");
    }

    @Test
    void traverseThrowsWhenRootNodeIsMissing() {
        when(mixedGraphClient.getVisualization(projectId)).thenReturn(new GraphProjection(List.of(), List.of()));

        assertThatThrownBy(() -> mixedGraphService.traverse(projectId, List.of("missing"), 2, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void findPathsReturnsSingleBreadthFirstPathWithEdgeTypes() {
        var a = node("REQUIREMENT:a", GraphEntityType.REQUIREMENT, "REQ-A");
        var b = node("OPERATIONAL_ASSET:b", GraphEntityType.OPERATIONAL_ASSET, "ASSET-B");
        var c = node("RISK_SCENARIO:c", GraphEntityType.RISK_SCENARIO, "RS-C");
        when(mixedGraphClient.getVisualization(projectId))
                .thenReturn(new GraphProjection(
                        List.of(a, b, c),
                        List.of(
                                edge("ab", "ASSOCIATED", a.id(), b.id(), a.entityType(), b.entityType()),
                                edge("bc", "AFFECTS", b.id(), c.id(), b.entityType(), c.entityType()))));

        var paths = mixedGraphService.findPaths(
                projectId, a.id(), c.id(), 3, List.of("REQUIREMENT", "OPERATIONAL_ASSET", "RISK_SCENARIO"));

        assertThat(paths).hasSize(1);
        assertThat(paths.getFirst().nodeIds()).containsExactly(a.id(), b.id(), c.id());
        assertThat(paths.getFirst().edgeTypes()).containsExactly("ASSOCIATED", "AFFECTS");
    }

    @Test
    void findPathsReturnsEmptyWhenNoPathExists() {
        var a = node("REQUIREMENT:a", GraphEntityType.REQUIREMENT, "REQ-A");
        var b = node("OPERATIONAL_ASSET:b", GraphEntityType.OPERATIONAL_ASSET, "ASSET-B");
        when(mixedGraphClient.getVisualization(projectId)).thenReturn(new GraphProjection(List.of(a, b), List.of()));

        var paths = mixedGraphService.findPaths(projectId, a.id(), b.id(), 2, null);

        assertThat(paths).isEmpty();
    }

    @Test
    void findPathsThrowsWhenTargetNodeIsMissingAfterFiltering() {
        var requirement = node("REQUIREMENT:req-1", GraphEntityType.REQUIREMENT, "REQ-1");
        var asset = node("OPERATIONAL_ASSET:asset-1", GraphEntityType.OPERATIONAL_ASSET, "ASSET-1");
        when(mixedGraphClient.getVisualization(projectId))
                .thenReturn(new GraphProjection(
                        List.of(requirement, asset),
                        List.of(edge(
                                "edge-1",
                                "ASSOCIATED",
                                requirement.id(),
                                asset.id(),
                                requirement.entityType(),
                                asset.entityType()))));

        assertThatThrownBy(() ->
                        mixedGraphService.findPaths(projectId, requirement.id(), asset.id(), 2, List.of("REQUIREMENT")))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(asset.id());
    }

    private GraphNode node(String id, GraphEntityType entityType, String label) {
        return new GraphNode(
                id,
                id.substring(id.indexOf(':') + 1),
                entityType,
                "ground-control",
                label,
                label,
                Map.of("title", label));
    }

    private GraphEdge edge(
            String id,
            String edgeType,
            String sourceId,
            String targetId,
            GraphEntityType sourceType,
            GraphEntityType targetType) {
        return new GraphEdge(id, edgeType, sourceId, targetId, sourceType, targetType, Map.of());
    }
}
