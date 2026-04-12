package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.graph.service.VerificationResultGraphProjectionContributor;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.verification.model.VerificationResult;
import com.keplerops.groundcontrol.domain.verification.repository.VerificationResultRepository;
import com.keplerops.groundcontrol.domain.verification.state.AssuranceLevel;
import com.keplerops.groundcontrol.domain.verification.state.VerificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerificationResultGraphProjectionContributorTest {

    @Mock
    private VerificationResultRepository verificationResultRepository;

    @InjectMocks
    private VerificationResultGraphProjectionContributor contributor;

    private static final Instant NOW = Instant.parse("2026-04-11T12:00:00Z");

    private Project makeProject(UUID projectId) {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", projectId);
        return project;
    }

    private VerificationResult makeResult(Project project, Requirement requirement) {
        var result = new VerificationResult(project, "openjml", VerificationStatus.PROVEN, AssuranceLevel.L2, NOW);
        setField(result, "id", UUID.randomUUID());
        setField(result, "createdAt", NOW);
        setField(result, "updatedAt", NOW);
        result.setRequirement(requirement);
        result.setProperty("status_machine_invariant_holds");
        return result;
    }

    @Test
    void contributesNodesForEachVerificationResult() {
        var projectId = UUID.randomUUID();
        var project = makeProject(projectId);
        var resultWithRequirement = makeResult(project, null);
        var resultStandalone = makeResult(project, null);

        when(verificationResultRepository.findByProjectIdOrderByVerifiedAtDesc(projectId))
                .thenReturn(List.of(resultWithRequirement, resultStandalone));

        var nodes = contributor.contributeNodes(projectId);

        assertThat(nodes).hasSize(2);
        assertThat(nodes).allMatch(node -> node.entityType() == GraphEntityType.VERIFICATION_RESULT);
        assertThat(nodes.get(0).id())
                .isEqualTo(GraphIds.nodeId(GraphEntityType.VERIFICATION_RESULT, resultWithRequirement.getId()));
        assertThat(nodes.get(0).properties().get("prover")).isEqualTo("openjml");
        assertThat(nodes.get(0).properties().get("result")).isEqualTo("PROVEN");
        assertThat(nodes.get(0).properties().get("assuranceLevel")).isEqualTo("L2");
    }

    @Test
    void emitsVerifiesEdgeOnlyWhenRequirementIsSet() {
        var projectId = UUID.randomUUID();
        var project = makeProject(projectId);

        var requirement = new Requirement(
                project, "REQ-001", "Status machine invariant", "Status transitions follow the documented graph.");
        setField(requirement, "id", UUID.randomUUID());

        var resultWithRequirement = makeResult(project, requirement);
        var resultStandalone = makeResult(project, null);

        when(verificationResultRepository.findByProjectIdOrderByVerifiedAtDesc(projectId))
                .thenReturn(List.of(resultWithRequirement, resultStandalone));

        var edges = contributor.contributeEdges(projectId);

        assertThat(edges).hasSize(1);
        assertThat(edges.get(0).edgeType()).isEqualTo("VERIFIES");
        assertThat(edges.get(0).sourceEntityType()).isEqualTo(GraphEntityType.VERIFICATION_RESULT);
        assertThat(edges.get(0).targetEntityType()).isEqualTo(GraphEntityType.REQUIREMENT);
        assertThat(edges.get(0).sourceId())
                .isEqualTo(GraphIds.nodeId(GraphEntityType.VERIFICATION_RESULT, resultWithRequirement.getId()));
        assertThat(edges.get(0).targetId())
                .isEqualTo(GraphIds.nodeId(GraphEntityType.REQUIREMENT, requirement.getId()));
    }
}
