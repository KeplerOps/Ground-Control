package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.controls.model.Control;
import com.keplerops.groundcontrol.domain.controls.model.ControlTest;
import com.keplerops.groundcontrol.domain.controls.repository.ControlTestRepository;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.controls.state.ControlTestConclusion;
import com.keplerops.groundcontrol.domain.controls.state.ControlTestMethodology;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.graph.service.ControlTestGraphProjectionContributor;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ControlTestGraphProjectionContributorTest {

    @Mock
    private ControlTestRepository controlTestRepository;

    @InjectMocks
    private ControlTestGraphProjectionContributor contributor;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CONTROL_ID = UUID.fromString("00000000-0000-0000-0000-000000000500");

    private ControlTest makeTest(String uid, UUID id) {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var control = new Control(project, "CTRL-001", "Access Control", ControlFunction.PREVENTIVE);
        setField(control, "id", CONTROL_ID);
        var ct = new ControlTest(
                project,
                control,
                uid,
                ControlTestMethodology.INSPECTION,
                ControlTestConclusion.EFFECTIVE,
                "auditor@example.com",
                LocalDate.of(2026, 5, 1));
        ct.setTestSteps("Inspect.");
        ct.setExpectedResults("None.");
        ct.setActualResults("None.");
        setField(ct, "id", id);
        return ct;
    }

    @Test
    void contributesOneNodePerControlTest() {
        var testA = makeTest("CT-001", UUID.fromString("00000000-0000-0000-0000-000000000600"));
        var testB = makeTest("CT-002", UUID.fromString("00000000-0000-0000-0000-000000000601"));
        when(controlTestRepository.findByProjectIdOrderByTestDateDesc(PROJECT_ID))
                .thenReturn(List.of(testA, testB));

        var nodes = contributor.contributeNodes(PROJECT_ID);

        assertThat(nodes).hasSize(2);
        assertThat(nodes.get(0).entityType()).isEqualTo(GraphEntityType.CONTROL_TEST);
        assertThat(nodes.get(0).properties())
                .containsEntry("uid", "CT-001")
                .containsEntry("methodology", "INSPECTION")
                .containsEntry("conclusion", "EFFECTIVE")
                .containsEntry("testerIdentity", "auditor@example.com")
                .containsEntry("testDate", "2026-05-01")
                .containsEntry("controlUid", "CTRL-001");
        assertThat(nodes.get(0).id()).isEqualTo(GraphIds.nodeId(GraphEntityType.CONTROL_TEST, testA.getId()));
    }

    @Test
    void contributesOfControlEdgePerTest() {
        var testA = makeTest("CT-001", UUID.fromString("00000000-0000-0000-0000-000000000600"));
        when(controlTestRepository.findByProjectIdOrderByTestDateDesc(PROJECT_ID))
                .thenReturn(List.of(testA));

        var edges = contributor.contributeEdges(PROJECT_ID);

        assertThat(edges).hasSize(1);
        var edge = edges.get(0);
        assertThat(edge.edgeType()).isEqualTo("OF_CONTROL");
        assertThat(edge.sourceEntityType()).isEqualTo(GraphEntityType.CONTROL_TEST);
        assertThat(edge.targetEntityType()).isEqualTo(GraphEntityType.CONTROL);
        assertThat(edge.sourceId()).isEqualTo(GraphIds.nodeId(GraphEntityType.CONTROL_TEST, testA.getId()));
        assertThat(edge.targetId()).isEqualTo(GraphIds.nodeId(GraphEntityType.CONTROL, CONTROL_ID));
    }

    @Test
    void emptyProjectYieldsEmptyProjection() {
        when(controlTestRepository.findByProjectIdOrderByTestDateDesc(PROJECT_ID))
                .thenReturn(List.of());

        assertThat(contributor.contributeNodes(PROJECT_ID)).isEmpty();
        assertThat(contributor.contributeEdges(PROJECT_ID)).isEmpty();
    }
}
