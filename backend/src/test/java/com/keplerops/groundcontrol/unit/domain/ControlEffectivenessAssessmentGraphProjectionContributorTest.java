package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.controls.model.Control;
import com.keplerops.groundcontrol.domain.controls.model.ControlEffectivenessAssessment;
import com.keplerops.groundcontrol.domain.controls.model.ControlTest;
import com.keplerops.groundcontrol.domain.controls.repository.ControlEffectivenessAssessmentRepository;
import com.keplerops.groundcontrol.domain.controls.repository.ControlTestRepository;
import com.keplerops.groundcontrol.domain.controls.state.ControlEffectivenessRating;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.controls.state.ControlTestConclusion;
import com.keplerops.groundcontrol.domain.controls.state.ControlTestMethodology;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.graph.service.ControlEffectivenessAssessmentGraphProjectionContributor;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ControlEffectivenessAssessmentGraphProjectionContributorTest {

    @Mock
    private ControlEffectivenessAssessmentRepository repository;

    @Mock
    private ControlTestRepository controlTestRepository;

    @InjectMocks
    private ControlEffectivenessAssessmentGraphProjectionContributor contributor;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CONTROL_ID = UUID.fromString("00000000-0000-0000-0000-000000000500");

    private Project project;
    private Control control;

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        control = new Control(project, "CTRL-001", "Access Control", ControlFunction.PREVENTIVE);
        setField(control, "id", CONTROL_ID);
    }

    private ControlEffectivenessAssessment makeAssessment(String uid, UUID id) {
        var assessment = new ControlEffectivenessAssessment(
                project,
                control,
                uid,
                ControlEffectivenessRating.EFFECTIVE,
                ControlEffectivenessRating.PARTIALLY_EFFECTIVE,
                LocalDate.of(2026, 5, 1),
                "auditor@example.com");
        setField(assessment, "id", id);
        return assessment;
    }

    private ControlTest makeTest(UUID id) {
        var ct = new ControlTest(
                project,
                control,
                "CT-" + id.toString().substring(0, 8),
                ControlTestMethodology.INSPECTION,
                ControlTestConclusion.EFFECTIVE,
                "auditor@example.com",
                LocalDate.of(2026, 5, 1));
        setField(ct, "id", id);
        return ct;
    }

    @Test
    void contributesOneNodePerAssessment() {
        var assessment = makeAssessment("CEA-001", UUID.fromString("00000000-0000-0000-0000-000000000700"));
        when(repository.findByProjectIdOrderByAssessedAtDesc(PROJECT_ID)).thenReturn(List.of(assessment));

        var nodes = contributor.contributeNodes(PROJECT_ID);

        assertThat(nodes).hasSize(1);
        assertThat(nodes.get(0).entityType()).isEqualTo(GraphEntityType.CONTROL_EFFECTIVENESS_ASSESSMENT);
        assertThat(nodes.get(0).properties())
                .containsEntry("uid", "CEA-001")
                .containsEntry("designEffectiveness", "EFFECTIVE")
                .containsEntry("operatingEffectiveness", "PARTIALLY_EFFECTIVE")
                .containsEntry("assessor", "auditor@example.com")
                .containsEntry("assessedAt", "2026-05-01")
                .containsEntry("controlUid", "CTRL-001");
    }

    @Test
    void contributesOfControlEdgeAndSupportedByEdgesForResolvedTests() {
        var testIdA = UUID.fromString("00000000-0000-0000-0000-000000000600");
        var testIdB = UUID.fromString("00000000-0000-0000-0000-000000000601");
        var assessment = makeAssessment("CEA-001", UUID.fromString("00000000-0000-0000-0000-000000000700"));
        assessment.setSupportingTestIds(List.of(testIdA.toString(), testIdB.toString()));
        when(repository.findByProjectIdOrderByAssessedAtDesc(PROJECT_ID)).thenReturn(List.of(assessment));
        when(controlTestRepository.findByIdAndProjectId(testIdA, PROJECT_ID))
                .thenReturn(Optional.of(makeTest(testIdA)));
        when(controlTestRepository.findByIdAndProjectId(testIdB, PROJECT_ID))
                .thenReturn(Optional.of(makeTest(testIdB)));

        var edges = contributor.contributeEdges(PROJECT_ID);

        // 1 OF_CONTROL + 2 SUPPORTED_BY edges, asserted in one chain to keep AssertJ's
        // failure message coherent if the contributor regresses.
        assertThat(edges)
                .hasSize(3)
                .anyMatch(e -> "OF_CONTROL".equals(e.edgeType()) && e.targetEntityType() == GraphEntityType.CONTROL)
                .anyMatch(e -> "SUPPORTED_BY".equals(e.edgeType())
                        && e.targetEntityType() == GraphEntityType.CONTROL_TEST
                        && e.targetId().equals(GraphIds.nodeId(GraphEntityType.CONTROL_TEST, testIdA)))
                .anyMatch(e -> "SUPPORTED_BY".equals(e.edgeType())
                        && e.targetId().equals(GraphIds.nodeId(GraphEntityType.CONTROL_TEST, testIdB)));
    }

    @Test
    void skipsSupportedByEdgesWhenTestIdMalformed() {
        var assessment = makeAssessment("CEA-001", UUID.fromString("00000000-0000-0000-0000-000000000700"));
        assessment.setSupportingTestIds(List.of("not-a-uuid"));
        when(repository.findByProjectIdOrderByAssessedAtDesc(PROJECT_ID)).thenReturn(List.of(assessment));

        var edges = contributor.contributeEdges(PROJECT_ID);

        assertThat(edges).hasSize(1); // Only OF_CONTROL, no SUPPORTED_BY for the bogus UUID
        assertThat(edges.get(0).edgeType()).isEqualTo("OF_CONTROL");
    }

    @Test
    void skipsSupportedByEdgesWhenTestDoesNotResolve() {
        var deletedTestId = UUID.fromString("00000000-0000-0000-0000-000000000888");
        var assessment = makeAssessment("CEA-001", UUID.fromString("00000000-0000-0000-0000-000000000700"));
        assessment.setSupportingTestIds(List.of(deletedTestId.toString()));
        when(repository.findByProjectIdOrderByAssessedAtDesc(PROJECT_ID)).thenReturn(List.of(assessment));
        when(controlTestRepository.findByIdAndProjectId(deletedTestId, PROJECT_ID))
                .thenReturn(Optional.empty());

        var edges = contributor.contributeEdges(PROJECT_ID);

        // SUPPORTED_BY edge silently dropped to keep AGE materialization safe — no dangling edges.
        assertThat(edges).hasSize(1);
        assertThat(edges.get(0).edgeType()).isEqualTo("OF_CONTROL");
    }

    @Test
    void emptyProjectYieldsEmptyProjection() {
        when(repository.findByProjectIdOrderByAssessedAtDesc(PROJECT_ID)).thenReturn(List.of());

        assertThat(contributor.contributeNodes(PROJECT_ID)).isEmpty();
        assertThat(contributor.contributeEdges(PROJECT_ID)).isEmpty();
    }

    @Test
    void nullSupportingTestIdsEmitsOnlyOfControlEdge() {
        var assessment = makeAssessment("CEA-001", UUID.fromString("00000000-0000-0000-0000-000000000700"));
        // supportingTestIds left as null (no setter call)
        when(repository.findByProjectIdOrderByAssessedAtDesc(PROJECT_ID)).thenReturn(List.of(assessment));

        var edges = contributor.contributeEdges(PROJECT_ID);

        assertThat(edges).hasSize(1);
        assertThat(edges.get(0).edgeType()).isEqualTo("OF_CONTROL");
    }
}
