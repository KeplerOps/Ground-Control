package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.qualitygates.model.QualityGate;
import com.keplerops.groundcontrol.domain.qualitygates.repository.QualityGateRepository;
import com.keplerops.groundcontrol.domain.qualitygates.service.CreateQualityGateCommand;
import com.keplerops.groundcontrol.domain.qualitygates.service.QualityGateService;
import com.keplerops.groundcontrol.domain.qualitygates.state.ComparisonOperator;
import com.keplerops.groundcontrol.domain.qualitygates.state.MetricType;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.requirements.service.AnalysisService;
import com.keplerops.groundcontrol.domain.requirements.service.CompletenessIssue;
import com.keplerops.groundcontrol.domain.requirements.service.CompletenessResult;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QualityGateServiceTest {

    @Mock
    private QualityGateRepository qualityGateRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private TraceabilityLinkRepository traceabilityLinkRepository;

    @Mock
    private AnalysisService analysisService;

    private QualityGateService service;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Project TEST_PROJECT = createTestProject();

    private static Project createTestProject() {
        var project = new Project("test-project", "Test Project");
        setField(project, "id", PROJECT_ID);
        return project;
    }

    @BeforeEach
    void setUp() {
        service = new QualityGateService(
                qualityGateRepository,
                projectRepository,
                projectService,
                requirementRepository,
                traceabilityLinkRepository,
                analysisService);
    }

    @Nested
    class Create {

        @Test
        void createsGateSuccessfully() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(TEST_PROJECT));
            when(qualityGateRepository.existsByProjectIdAndName(PROJECT_ID, "Coverage Gate"))
                    .thenReturn(false);
            when(qualityGateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateQualityGateCommand(
                    PROJECT_ID,
                    "Coverage Gate",
                    "Checks test coverage",
                    MetricType.COVERAGE,
                    "TESTS",
                    Status.ACTIVE,
                    ComparisonOperator.GTE,
                    80.0);

            var result = service.create(command);

            assertThat(result.getName()).isEqualTo("Coverage Gate");
            assertThat(result.getMetricType()).isEqualTo(MetricType.COVERAGE);
            verify(qualityGateRepository).save(any(QualityGate.class));
        }

        @Test
        void throwsConflictOnDuplicateName() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(TEST_PROJECT));
            when(qualityGateRepository.existsByProjectIdAndName(PROJECT_ID, "Coverage Gate"))
                    .thenReturn(true);

            var command = new CreateQualityGateCommand(
                    PROJECT_ID,
                    "Coverage Gate",
                    null,
                    MetricType.COVERAGE,
                    "TESTS",
                    null,
                    ComparisonOperator.GTE,
                    80.0);

            assertThatThrownBy(() -> service.create(command)).isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void updatesFieldsSelectively() {
            var gate = makeGate("Old Name", MetricType.COVERAGE, "TESTS", Status.ACTIVE, ComparisonOperator.GTE, 80.0);
            when(qualityGateRepository.findById(gate.getId())).thenReturn(Optional.of(gate));
            when(qualityGateRepository.existsByProjectIdAndName(PROJECT_ID, "New Name"))
                    .thenReturn(false);
            when(qualityGateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new com.keplerops.groundcontrol.domain.qualitygates.service.UpdateQualityGateCommand(
                    "New Name", null, null, null, null, null, 90.0, false);

            var result = service.update(gate.getId(), command);

            assertThat(result.getName()).isEqualTo("New Name");
            assertThat(result.getThreshold()).isEqualTo(90.0);
            assertThat(result.isEnabled()).isFalse();
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesGate() {
            var gate = makeGate("Gate", MetricType.ORPHAN_COUNT, null, null, ComparisonOperator.LTE, 0.0);
            when(qualityGateRepository.findById(gate.getId())).thenReturn(Optional.of(gate));

            service.delete(gate.getId());

            verify(qualityGateRepository).delete(gate);
        }
    }

    @Nested
    class Evaluate {

        @BeforeEach
        void stubProject() {
            when(projectService.resolveProjectId("test-project")).thenReturn(PROJECT_ID);
            when(projectService.getById(PROJECT_ID)).thenReturn(TEST_PROJECT);
        }

        @Test
        void coverageGatePassesWhenAboveThreshold() {
            var req1 = makeRequirement("GC-001", UUID.randomUUID(), Status.ACTIVE);
            var req2 = makeRequirement("GC-002", UUID.randomUUID(), Status.ACTIVE);
            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(req1, req2));
            when(traceabilityLinkRepository.existsByRequirementIdAndLinkType(req1.getId(), LinkType.TESTS))
                    .thenReturn(true);
            when(traceabilityLinkRepository.existsByRequirementIdAndLinkType(req2.getId(), LinkType.TESTS))
                    .thenReturn(true);

            var gate = makeGate(
                    "Test Coverage", MetricType.COVERAGE, "TESTS", Status.ACTIVE, ComparisonOperator.GTE, 80.0);
            when(qualityGateRepository.findByProjectIdAndEnabledTrueOrderByNameAsc(PROJECT_ID))
                    .thenReturn(List.of(gate));

            var result = service.evaluate("test-project");

            assertThat(result.passed()).isTrue();
            assertThat(result.passedCount()).isEqualTo(1);
            assertThat(result.gates().getFirst().actualValue()).isEqualTo(100.0);
        }

        @Test
        void coverageGateFailsWhenBelowThreshold() {
            var req1 = makeRequirement("GC-001", UUID.randomUUID(), Status.ACTIVE);
            var req2 = makeRequirement("GC-002", UUID.randomUUID(), Status.ACTIVE);
            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(req1, req2));
            when(traceabilityLinkRepository.existsByRequirementIdAndLinkType(req1.getId(), LinkType.TESTS))
                    .thenReturn(true);
            when(traceabilityLinkRepository.existsByRequirementIdAndLinkType(req2.getId(), LinkType.TESTS))
                    .thenReturn(false);

            var gate = makeGate(
                    "Test Coverage", MetricType.COVERAGE, "TESTS", Status.ACTIVE, ComparisonOperator.GTE, 80.0);
            when(qualityGateRepository.findByProjectIdAndEnabledTrueOrderByNameAsc(PROJECT_ID))
                    .thenReturn(List.of(gate));

            var result = service.evaluate("test-project");

            assertThat(result.passed()).isFalse();
            assertThat(result.failedCount()).isEqualTo(1);
            assertThat(result.gates().getFirst().actualValue()).isEqualTo(50.0);
        }

        @Test
        void scopeStatusFiltersRequirements() {
            var activeReq = makeRequirement("GC-001", UUID.randomUUID(), Status.ACTIVE);
            var draftReq = makeRequirement("GC-002", UUID.randomUUID(), Status.DRAFT);
            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(activeReq, draftReq));
            when(traceabilityLinkRepository.existsByRequirementIdAndLinkType(activeReq.getId(), LinkType.TESTS))
                    .thenReturn(true);

            var gate = makeGate(
                    "Active Coverage", MetricType.COVERAGE, "TESTS", Status.ACTIVE, ComparisonOperator.GTE, 100.0);
            when(qualityGateRepository.findByProjectIdAndEnabledTrueOrderByNameAsc(PROJECT_ID))
                    .thenReturn(List.of(gate));

            var result = service.evaluate("test-project");

            assertThat(result.passed()).isTrue();
            assertThat(result.gates().getFirst().actualValue()).isEqualTo(100.0);
        }

        @Test
        void noGatesReturnsPassedTrue() {
            when(qualityGateRepository.findByProjectIdAndEnabledTrueOrderByNameAsc(PROJECT_ID))
                    .thenReturn(List.of());

            var result = service.evaluate("test-project");

            assertThat(result.passed()).isTrue();
            assertThat(result.totalGates()).isZero();
        }

        @Test
        void orphanCountGateEvaluates() {
            var orphan = makeRequirement("GC-ORPH", UUID.randomUUID(), Status.DRAFT);
            when(analysisService.findOrphans(PROJECT_ID)).thenReturn(List.of(orphan));

            var gate = makeGate("No Orphans", MetricType.ORPHAN_COUNT, null, null, ComparisonOperator.LTE, 0.0);
            when(qualityGateRepository.findByProjectIdAndEnabledTrueOrderByNameAsc(PROJECT_ID))
                    .thenReturn(List.of(gate));

            var result = service.evaluate("test-project");

            assertThat(result.passed()).isFalse();
            assertThat(result.gates().getFirst().actualValue()).isEqualTo(1.0);
        }

        @Test
        void completenessGateEvaluates() {
            var issues = List.of(new CompletenessIssue("GC-001", "Missing statement"));
            when(analysisService.analyzeCompleteness(PROJECT_ID))
                    .thenReturn(new CompletenessResult(1, Map.of("DRAFT", 1), issues));

            var gate = makeGate("Completeness", MetricType.COMPLETENESS, null, null, ComparisonOperator.EQ, 0.0);
            when(qualityGateRepository.findByProjectIdAndEnabledTrueOrderByNameAsc(PROJECT_ID))
                    .thenReturn(List.of(gate));

            var result = service.evaluate("test-project");

            assertThat(result.passed()).isFalse();
            assertThat(result.gates().getFirst().actualValue()).isEqualTo(1.0);
        }
    }

    private static QualityGate makeGate(
            String name,
            MetricType metricType,
            String metricParam,
            Status scopeStatus,
            ComparisonOperator operator,
            double threshold) {
        var gate = new QualityGate(TEST_PROJECT, name, null, metricType, metricParam, scopeStatus, operator, threshold);
        setField(gate, "id", UUID.randomUUID());
        return gate;
    }

    private static Requirement makeRequirement(String uid, UUID id, Status status) {
        var req = new Requirement(TEST_PROJECT, uid, "Title for " + uid, "Statement for " + uid);
        setField(req, "id", id);
        setField(req, "status", status);
        return req;
    }

    private static void setField(Object obj, String fieldName, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
