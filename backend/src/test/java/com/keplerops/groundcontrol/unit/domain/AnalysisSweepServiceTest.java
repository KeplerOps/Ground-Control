package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.service.AnalysisService;
import com.keplerops.groundcontrol.domain.requirements.service.AnalysisSweepService;
import com.keplerops.groundcontrol.domain.requirements.service.CompletenessResult;
import com.keplerops.groundcontrol.domain.requirements.service.ConsistencyViolation;
import com.keplerops.groundcontrol.domain.requirements.service.CycleEdge;
import com.keplerops.groundcontrol.domain.requirements.service.CycleResult;
import com.keplerops.groundcontrol.domain.requirements.service.SweepNotifier;
import com.keplerops.groundcontrol.domain.requirements.service.SweepReport;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalysisSweepServiceTest {

    @Mock
    private AnalysisService analysisService;

    @Mock
    private ProjectService projectService;

    @Mock
    private SweepNotifier notifier;

    private AnalysisSweepService sweepService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Project TEST_PROJECT = createTestProject();

    private static Project createTestProject() {
        var project = new Project("test-project", "Test Project");
        try {
            var field = Project.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(project, PROJECT_ID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return project;
    }

    @BeforeEach
    void setUp() {
        sweepService = new AnalysisSweepService(analysisService, projectService, List.of(notifier));
    }

    private void stubCleanProject() {
        when(projectService.resolveProjectId("test-project")).thenReturn(PROJECT_ID);
        when(projectService.getById(PROJECT_ID)).thenReturn(TEST_PROJECT);
        when(analysisService.detectCycles(PROJECT_ID)).thenReturn(List.of());
        when(analysisService.findOrphans(PROJECT_ID)).thenReturn(List.of());
        for (LinkType lt : LinkType.values()) {
            when(analysisService.findCoverageGaps(PROJECT_ID, lt)).thenReturn(List.of());
        }
        when(analysisService.crossWaveValidation(PROJECT_ID)).thenReturn(List.of());
        when(analysisService.detectConsistencyViolations(PROJECT_ID)).thenReturn(List.of());
        when(analysisService.analyzeCompleteness(PROJECT_ID))
                .thenReturn(new CompletenessResult(0, Map.of(), List.of()));
    }

    @Nested
    class Sweep {

        @Test
        void runsAllAnalyses() {
            stubCleanProject();

            var report = sweepService.sweep("test-project");

            verify(analysisService).detectCycles(PROJECT_ID);
            verify(analysisService).findOrphans(PROJECT_ID);
            for (LinkType lt : LinkType.values()) {
                verify(analysisService).findCoverageGaps(PROJECT_ID, lt);
            }
            verify(analysisService).crossWaveValidation(PROJECT_ID);
            verify(analysisService).detectConsistencyViolations(PROJECT_ID);
            verify(analysisService).analyzeCompleteness(PROJECT_ID);

            assertThat(report.projectIdentifier()).isEqualTo("test-project");
            assertThat(report.hasProblems()).isFalse();
        }

        @Test
        void doesNotNotifyWhenNoProblems() {
            stubCleanProject();

            sweepService.sweep("test-project");

            verify(notifier, never()).notify(any());
        }

        @Test
        void notifiesWhenProblemsDetected() {
            when(projectService.resolveProjectId("test-project")).thenReturn(PROJECT_ID);
            when(projectService.getById(PROJECT_ID)).thenReturn(TEST_PROJECT);

            var orphan = makeRequirement("GC-ORPH1", UUID.randomUUID());
            when(analysisService.detectCycles(PROJECT_ID)).thenReturn(List.of());
            when(analysisService.findOrphans(PROJECT_ID)).thenReturn(List.of(orphan));
            for (LinkType lt : LinkType.values()) {
                when(analysisService.findCoverageGaps(PROJECT_ID, lt)).thenReturn(List.of());
            }
            when(analysisService.crossWaveValidation(PROJECT_ID)).thenReturn(List.of());
            when(analysisService.detectConsistencyViolations(PROJECT_ID)).thenReturn(List.of());
            when(analysisService.analyzeCompleteness(PROJECT_ID))
                    .thenReturn(new CompletenessResult(1, Map.of("DRAFT", 1), List.of()));

            var report = sweepService.sweep("test-project");

            assertThat(report.hasProblems()).isTrue();
            assertThat(report.orphans()).hasSize(1);
            assertThat(report.orphans().getFirst().uid()).isEqualTo("GC-ORPH1");

            var captor = ArgumentCaptor.forClass(SweepReport.class);
            verify(notifier).notify(captor.capture());
            assertThat(captor.getValue().hasProblems()).isTrue();
        }

        @Test
        void includesCyclesInReport() {
            when(projectService.resolveProjectId("test-project")).thenReturn(PROJECT_ID);
            when(projectService.getById(PROJECT_ID)).thenReturn(TEST_PROJECT);

            var cycle = new CycleResult(
                    List.of("GC-A", "GC-B"), List.of(new CycleEdge("GC-A", "GC-B", RelationType.DEPENDS_ON)));
            when(analysisService.detectCycles(PROJECT_ID)).thenReturn(List.of(cycle));
            when(analysisService.findOrphans(PROJECT_ID)).thenReturn(List.of());
            for (LinkType lt : LinkType.values()) {
                when(analysisService.findCoverageGaps(PROJECT_ID, lt)).thenReturn(List.of());
            }
            when(analysisService.crossWaveValidation(PROJECT_ID)).thenReturn(List.of());
            when(analysisService.detectConsistencyViolations(PROJECT_ID)).thenReturn(List.of());
            when(analysisService.analyzeCompleteness(PROJECT_ID))
                    .thenReturn(new CompletenessResult(2, Map.of(), List.of()));

            var report = sweepService.sweep("test-project");

            assertThat(report.hasProblems()).isTrue();
            assertThat(report.cycles()).hasSize(1);
            assertThat(report.totalProblems()).isEqualTo(1);
        }

        @Test
        void continuesOnNotifierFailure() {
            when(projectService.resolveProjectId("test-project")).thenReturn(PROJECT_ID);
            when(projectService.getById(PROJECT_ID)).thenReturn(TEST_PROJECT);

            var orphan = makeRequirement("GC-ORPH1", UUID.randomUUID());
            when(analysisService.detectCycles(PROJECT_ID)).thenReturn(List.of());
            when(analysisService.findOrphans(PROJECT_ID)).thenReturn(List.of(orphan));
            for (LinkType lt : LinkType.values()) {
                when(analysisService.findCoverageGaps(PROJECT_ID, lt)).thenReturn(List.of());
            }
            when(analysisService.crossWaveValidation(PROJECT_ID)).thenReturn(List.of());
            when(analysisService.detectConsistencyViolations(PROJECT_ID)).thenReturn(List.of());
            when(analysisService.analyzeCompleteness(PROJECT_ID))
                    .thenReturn(new CompletenessResult(1, Map.of(), List.of()));

            org.mockito.Mockito.doThrow(new RuntimeException("webhook down"))
                    .when(notifier)
                    .notify(any());

            // Should not throw
            var report = sweepService.sweep("test-project");
            assertThat(report.hasProblems()).isTrue();
        }

        @Test
        void includesCrossWaveViolationsInReport() {
            when(projectService.resolveProjectId("test-project")).thenReturn(PROJECT_ID);
            when(projectService.getById(PROJECT_ID)).thenReturn(TEST_PROJECT);

            var early = makeRequirement("GC-EARLY", UUID.randomUUID());
            early.setWave(1);
            var late = makeRequirement("GC-LATE", UUID.randomUUID());
            late.setWave(3);
            var violation = new RequirementRelation(early, late, RelationType.DEPENDS_ON);

            when(analysisService.detectCycles(PROJECT_ID)).thenReturn(List.of());
            when(analysisService.findOrphans(PROJECT_ID)).thenReturn(List.of());
            for (LinkType lt : LinkType.values()) {
                when(analysisService.findCoverageGaps(PROJECT_ID, lt)).thenReturn(List.of());
            }
            when(analysisService.crossWaveValidation(PROJECT_ID)).thenReturn(List.of(violation));
            when(analysisService.detectConsistencyViolations(PROJECT_ID)).thenReturn(List.of());
            when(analysisService.analyzeCompleteness(PROJECT_ID))
                    .thenReturn(new CompletenessResult(2, Map.of(), List.of()));

            var report = sweepService.sweep("test-project");

            assertThat(report.hasProblems()).isTrue();
            assertThat(report.crossWaveViolations()).hasSize(1);
            SweepReport.CrossWaveViolationSummary summary =
                    report.crossWaveViolations().get(0);
            assertThat(summary.sourceUid()).isEqualTo("GC-EARLY");
            assertThat(summary.sourceWave()).isEqualTo(1);
            assertThat(summary.targetUid()).isEqualTo("GC-LATE");
            assertThat(summary.targetWave()).isEqualTo(3);
            assertThat(summary.relationType()).isEqualTo(RelationType.DEPENDS_ON.name());
        }

        @Test
        void includesConsistencyViolationsInReport() {
            when(projectService.resolveProjectId("test-project")).thenReturn(PROJECT_ID);
            when(projectService.getById(PROJECT_ID)).thenReturn(TEST_PROJECT);

            var source = makeRequirement("GC-SRC", UUID.randomUUID());
            setField(source, "status", Status.ACTIVE);
            var target = makeRequirement("GC-TGT", UUID.randomUUID());
            setField(target, "status", Status.ACTIVE);
            var rel = new RequirementRelation(source, target, RelationType.CONFLICTS_WITH);
            var violation = new ConsistencyViolation(rel, "ACTIVE_CONFLICT");

            when(analysisService.detectCycles(PROJECT_ID)).thenReturn(List.of());
            when(analysisService.findOrphans(PROJECT_ID)).thenReturn(List.of());
            for (LinkType lt : LinkType.values()) {
                when(analysisService.findCoverageGaps(PROJECT_ID, lt)).thenReturn(List.of());
            }
            when(analysisService.crossWaveValidation(PROJECT_ID)).thenReturn(List.of());
            when(analysisService.detectConsistencyViolations(PROJECT_ID)).thenReturn(List.of(violation));
            when(analysisService.analyzeCompleteness(PROJECT_ID))
                    .thenReturn(new CompletenessResult(2, Map.of(), List.of()));

            var report = sweepService.sweep("test-project");

            assertThat(report.hasProblems()).isTrue();
            assertThat(report.consistencyViolations()).hasSize(1);
            SweepReport.ConsistencyViolationSummary summary =
                    report.consistencyViolations().get(0);
            assertThat(summary.sourceUid()).isEqualTo("GC-SRC");
            assertThat(summary.sourceStatus()).isEqualTo("ACTIVE");
            assertThat(summary.targetUid()).isEqualTo("GC-TGT");
            assertThat(summary.targetStatus()).isEqualTo("ACTIVE");
            assertThat(summary.violationType()).isEqualTo("ACTIVE_CONFLICT");
        }
    }

    @Nested
    class SweepAll {

        @Test
        void sweepsAllProjects() {
            when(projectService.list()).thenReturn(List.of(TEST_PROJECT));
            stubCleanProject();

            var reports = sweepService.sweepAll();

            assertThat(reports).hasSize(1);
            assertThat(reports.getFirst().projectIdentifier()).isEqualTo("test-project");
        }

        @Test
        void continuesOnProjectFailure() {
            var project2 = new Project("project-2", "Project 2");
            setField(project2, "id", UUID.fromString("00000000-0000-0000-0000-000000000002"));

            when(projectService.list()).thenReturn(List.of(TEST_PROJECT, project2));

            // First project throws
            when(projectService.resolveProjectId("test-project")).thenThrow(new RuntimeException("db error"));

            // Second project succeeds
            var p2Id = UUID.fromString("00000000-0000-0000-0000-000000000002");
            when(projectService.resolveProjectId("project-2")).thenReturn(p2Id);
            when(projectService.getById(p2Id)).thenReturn(project2);
            when(analysisService.detectCycles(p2Id)).thenReturn(List.of());
            when(analysisService.findOrphans(p2Id)).thenReturn(List.of());
            for (LinkType lt : LinkType.values()) {
                when(analysisService.findCoverageGaps(p2Id, lt)).thenReturn(List.of());
            }
            when(analysisService.crossWaveValidation(p2Id)).thenReturn(List.of());
            when(analysisService.detectConsistencyViolations(p2Id)).thenReturn(List.of());
            when(analysisService.analyzeCompleteness(p2Id)).thenReturn(new CompletenessResult(0, Map.of(), List.of()));

            var reports = sweepService.sweepAll();

            assertThat(reports).hasSize(1);
            assertThat(reports.getFirst().projectIdentifier()).isEqualTo("project-2");
        }
    }

    private static Requirement makeRequirement(String uid, UUID id) {
        var req = new Requirement(TEST_PROJECT, uid, "Title for " + uid, "Statement for " + uid);
        setField(req, "id", id);
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
