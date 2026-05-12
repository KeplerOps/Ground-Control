package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.graph.GraphTraversalLimits;
import com.keplerops.groundcontrol.domain.graph.model.GraphEdge;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphNode;
import com.keplerops.groundcontrol.domain.graph.model.GraphProjection;
import com.keplerops.groundcontrol.domain.graph.service.MixedGraphClient;
import com.keplerops.groundcontrol.domain.graph.service.MixedGraphService;
import java.util.ArrayList;
import java.util.Collections;
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
    void getVisualizationForwardsEntityTypesToClient() {
        // The service parses entityType names and forwards the parsed set to the client; the
        // client (e.g. AGE adapter) does the actual filter so the projection cap can apply to the
        // filtered set rather than the full project. Return only the REQUIREMENT node here so the
        // assertion verifies the service round-trips the client's filtered response unchanged.
        var requirement = node("REQUIREMENT:req-1", GraphEntityType.REQUIREMENT, "REQ-1");
        when(mixedGraphClient.getVisualization(eq(projectId), eq(java.util.Set.of(GraphEntityType.REQUIREMENT))))
                .thenReturn(new GraphProjection(List.of(requirement), List.of()));

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
        when(mixedGraphClient.getVisualization(eq(projectId), any())).thenReturn(projection);

        var subgraph = mixedGraphService.extractSubgraph(projectId, List.of(a.id()), 1, null);

        assertThat(subgraph.nodes()).containsExactlyInAnyOrder(a, b);
        assertThat(subgraph.edges()).extracting(GraphEdge::id).containsExactly("ab");
    }

    @Test
    void traverseThrowsWhenRootNodeIsMissing() {
        when(mixedGraphClient.getVisualization(eq(projectId), any()))
                .thenReturn(new GraphProjection(List.of(), List.of()));

        assertThatThrownBy(() -> mixedGraphService.traverse(projectId, List.of("missing"), 2, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void findPathsReturnsSingleBreadthFirstPathWithEdgeTypes() {
        var a = node("REQUIREMENT:a", GraphEntityType.REQUIREMENT, "REQ-A");
        var b = node("OPERATIONAL_ASSET:b", GraphEntityType.OPERATIONAL_ASSET, "ASSET-B");
        var c = node("RISK_SCENARIO:c", GraphEntityType.RISK_SCENARIO, "RS-C");
        when(mixedGraphClient.getVisualization(eq(projectId), any()))
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
        when(mixedGraphClient.getVisualization(eq(projectId), any()))
                .thenReturn(new GraphProjection(List.of(a, b), List.of()));

        var paths = mixedGraphService.findPaths(projectId, a.id(), b.id(), 2, null);

        assertThat(paths).isEmpty();
    }

    @Test
    void findPathsThrowsWhenTargetNodeIsMissingAfterFiltering() {
        // With the filter pushed into the client, the mock simulates the AGE behavior of returning
        // only nodes that match the filter. The service then can't find the asset target and 404s.
        var requirement = node("REQUIREMENT:req-1", GraphEntityType.REQUIREMENT, "REQ-1");
        var asset = node("OPERATIONAL_ASSET:asset-1", GraphEntityType.OPERATIONAL_ASSET, "ASSET-1");
        when(mixedGraphClient.getVisualization(eq(projectId), eq(java.util.Set.of(GraphEntityType.REQUIREMENT))))
                .thenReturn(new GraphProjection(List.of(requirement), List.of()));

        assertThatThrownBy(() ->
                        mixedGraphService.findPaths(projectId, requirement.id(), asset.id(), 2, List.of("REQUIREMENT")))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(asset.id());
    }

    // --- Bound enforcement (ADR-032: service-level defense in depth) ---

    @Test
    void extractSubgraphRejectsDepthAboveMax() {
        assertThatThrownBy(() -> mixedGraphService.extractSubgraph(
                        projectId, List.of("root"), GraphTraversalLimits.MAX_DEPTH + 1, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("depth");
    }

    @Test
    void extractSubgraphRejectsDepthBelowOne() {
        assertThatThrownBy(() -> mixedGraphService.extractSubgraph(projectId, List.of("root"), 0, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("depth");
    }

    @Test
    void traverseRejectsDepthAboveMax() {
        assertThatThrownBy(() -> mixedGraphService.traverse(
                        projectId, List.of("root"), GraphTraversalLimits.MAX_DEPTH + 1, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("depth");
    }

    @Test
    void findPathsRejectsDepthAboveMax() {
        assertThatThrownBy(() ->
                        mixedGraphService.findPaths(projectId, "src", "tgt", GraphTraversalLimits.MAX_DEPTH + 1, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("depth");
    }

    @Test
    void findPathsRejectsDepthBelowOne() {
        assertThatThrownBy(() -> mixedGraphService.findPaths(projectId, "src", "tgt", 0, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("depth");
    }

    @Test
    void extractSubgraphRejectsTooManyRootNodes() {
        List<String> oversize = new ArrayList<>(GraphTraversalLimits.MAX_ROOT_NODES + 1);
        for (int i = 0; i <= GraphTraversalLimits.MAX_ROOT_NODES; i++) {
            oversize.add("REQUIREMENT:r-" + i);
        }
        assertThatThrownBy(() -> mixedGraphService.extractSubgraph(projectId, oversize, 1, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("rootNodeIds");
    }

    @Test
    void traverseRejectsEmptyRootNodes() {
        // @NotEmpty on the DTO covers HTTP, but the service must also refuse direct calls.
        assertThatThrownBy(() -> mixedGraphService.traverse(projectId, List.of(), 1, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("rootNodeIds");
    }

    @Test
    void extractSubgraphRejectsOversizeEntityTypesFilter() {
        List<String> filter = Collections.nCopies(GraphTraversalLimits.MAX_ENTITY_TYPE_FILTER + 1, "REQUIREMENT");
        assertThatThrownBy(() -> mixedGraphService.extractSubgraph(projectId, List.of("root"), 1, filter))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("entityTypes");
    }

    @Test
    void extractSubgraphRejectsUnknownEntityType() {
        assertThatThrownBy(() ->
                        mixedGraphService.extractSubgraph(projectId, List.of("root"), 1, List.of("NOT_AN_ENTITY_TYPE")))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("entityType");
    }

    @Test
    void getVisualizationRejectsUnknownEntityType() {
        assertThatThrownBy(() -> mixedGraphService.getVisualization(projectId, List.of("NOT_AN_ENTITY_TYPE")))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("entityType");
    }

    @Test
    void findPathsHonoursMaxDepthEdgeCount() {
        // Codex review cycle 1 finding #2: A→B→C with maxDepth=1 must not return the 2-edge
        // path because the caller asked for at most 1 edge.
        var a = node("REQUIREMENT:a", GraphEntityType.REQUIREMENT, "REQ-A");
        var b = node("OPERATIONAL_ASSET:b", GraphEntityType.OPERATIONAL_ASSET, "ASSET-B");
        var c = node("RISK_SCENARIO:c", GraphEntityType.RISK_SCENARIO, "RS-C");
        when(mixedGraphClient.getVisualization(eq(projectId), any()))
                .thenReturn(new GraphProjection(
                        List.of(a, b, c),
                        List.of(
                                edge("ab", "ASSOCIATED", a.id(), b.id(), a.entityType(), b.entityType()),
                                edge("bc", "AFFECTS", b.id(), c.id(), b.entityType(), c.entityType()))));

        var paths = mixedGraphService.findPaths(projectId, a.id(), c.id(), 1, null);

        assertThat(paths).isEmpty();
    }

    @Test
    void parseEntityTypesRejectsBlankValue() {
        // Codex review cycle 1 finding #3: a blank entry must not silently degrade to "no filter".
        assertThatThrownBy(() -> mixedGraphService.extractSubgraph(projectId, List.of("root"), 1, List.of(" ")))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void getVisualizationRejectsProjectionExceedingNodeCap() {
        // Build a synthetic projection that exceeds MAX_PROJECTION_NODES so the service rejects
        // the response rather than serializing a multi-megabyte payload.
        int oversize = GraphTraversalLimits.MAX_PROJECTION_NODES + 1;
        List<GraphNode> nodes = new ArrayList<>(oversize);
        for (int i = 0; i < oversize; i++) {
            nodes.add(node("REQUIREMENT:req-" + i, GraphEntityType.REQUIREMENT, "REQ-" + i));
        }
        when(mixedGraphClient.getVisualization(eq(projectId), any())).thenReturn(new GraphProjection(nodes, List.of()));

        assertThatThrownBy(() -> mixedGraphService.getVisualization(projectId, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("projection");
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
