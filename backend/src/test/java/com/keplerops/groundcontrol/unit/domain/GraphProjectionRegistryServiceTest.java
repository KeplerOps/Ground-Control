package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.graph.model.GraphEdge;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphNode;
import com.keplerops.groundcontrol.domain.graph.service.GraphProjectionContributor;
import com.keplerops.groundcontrol.domain.graph.service.GraphProjectionRegistryService;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GraphProjectionRegistryServiceTest {

    @Test
    void buildProjectionAggregatesAllProjectsAcrossContributors() {
        var projectRepository = mock(ProjectRepository.class);
        var contributor = mock(GraphProjectionContributor.class);
        var service = new GraphProjectionRegistryService(projectRepository, List.of(contributor));

        var alpha = new Project("alpha", "Alpha");
        var beta = new Project("beta", "Beta");
        setField(alpha, "id", UUID.randomUUID());
        setField(beta, "id", UUID.randomUUID());

        when(projectRepository.findAll()).thenReturn(List.of(alpha, beta));
        when(contributor.contributeNodes(alpha.getId()))
                .thenReturn(List.of(new GraphNode(
                        "REQUIREMENT:1", "1", GraphEntityType.REQUIREMENT, "alpha", "REQ-1", "REQ-1", Map.of())));
        when(contributor.contributeEdges(alpha.getId()))
                .thenReturn(List.of(new GraphEdge(
                        "edge-1",
                        "DEPENDS_ON",
                        "REQUIREMENT:1",
                        "REQUIREMENT:2",
                        GraphEntityType.REQUIREMENT,
                        GraphEntityType.REQUIREMENT,
                        Map.of())));
        when(contributor.contributeNodes(beta.getId()))
                .thenReturn(List.of(new GraphNode(
                        "OPERATIONAL_ASSET:2",
                        "2",
                        GraphEntityType.OPERATIONAL_ASSET,
                        "beta",
                        "ASSET-2",
                        "ASSET-2",
                        Map.of())));
        when(contributor.contributeEdges(beta.getId())).thenReturn(List.of());

        var projection = service.buildProjection();

        assertThat(projection.nodes()).hasSize(2);
        assertThat(projection.edges()).hasSize(1);
        verify(contributor).contributeNodes(alpha.getId());
        verify(contributor).contributeEdges(beta.getId());
    }
}
