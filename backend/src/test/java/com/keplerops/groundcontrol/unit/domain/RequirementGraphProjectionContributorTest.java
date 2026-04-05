package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.graph.service.RequirementGraphProjectionContributor;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequirementGraphProjectionContributorTest {

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private RequirementRelationRepository relationRepository;

    @InjectMocks
    private RequirementGraphProjectionContributor contributor;

    @Test
    void contributesRequirementNodesAndEdges() {
        var project = new Project("ground-control", "Ground Control");
        var projectId = UUID.randomUUID();
        setField(project, "id", projectId);

        var source = new Requirement(project, "REQ-1", "Identity", "Every request must be authenticated");
        setField(source, "id", UUID.randomUUID());
        source.setWave(2);
        var target = new Requirement(project, "REQ-2", "Audit", "Every change must be audited");
        setField(target, "id", UUID.randomUUID());
        var relation = new RequirementRelation(source, target, RelationType.DEPENDS_ON);
        setField(relation, "id", UUID.randomUUID());
        setField(relation, "createdAt", Instant.parse("2026-04-02T12:00:00Z"));

        when(requirementRepository.findByProjectIdAndArchivedAtIsNull(projectId))
                .thenReturn(List.of(source, target));
        when(relationRepository.findActiveWithSourceAndTargetByProjectId(projectId))
                .thenReturn(List.of(relation));

        var nodes = contributor.contributeNodes(projectId);
        var edges = contributor.contributeEdges(projectId);

        assertThat(nodes).hasSize(2);
        assertThat(nodes.getFirst().id()).isEqualTo(GraphIds.nodeId(GraphEntityType.REQUIREMENT, source.getId()));
        assertThat(nodes.getFirst().properties())
                .containsEntry("title", "Identity")
                .containsEntry("wave", 2);
        assertThat(edges).singleElement().satisfies(edge -> {
            assertThat(edge.edgeType()).isEqualTo("DEPENDS_ON");
            assertThat(edge.sourceId()).isEqualTo(GraphIds.nodeId(GraphEntityType.REQUIREMENT, source.getId()));
            assertThat(edge.targetId()).isEqualTo(GraphIds.nodeId(GraphEntityType.REQUIREMENT, target.getId()));
            assertThat(edge.properties()).containsEntry("sourceUid", "REQ-1").containsEntry("targetUid", "REQ-2");
        });
    }
}
