package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.requirements.service.AnalysisService;
import com.keplerops.groundcontrol.domain.requirements.service.AuditService;
import com.keplerops.groundcontrol.domain.requirements.service.BlockingStatus;
import com.keplerops.groundcontrol.domain.requirements.service.CompletenessResult;
import com.keplerops.groundcontrol.domain.requirements.service.ConsistencyViolation;
import com.keplerops.groundcontrol.domain.requirements.service.CoverageStats;
import com.keplerops.groundcontrol.domain.requirements.service.CycleEdge;
import com.keplerops.groundcontrol.domain.requirements.service.CycleResult;
import com.keplerops.groundcontrol.domain.requirements.service.DashboardStats;
import com.keplerops.groundcontrol.domain.requirements.service.RecentChange;
import com.keplerops.groundcontrol.domain.requirements.service.WorkOrderItem;
import com.keplerops.groundcontrol.domain.requirements.service.WorkOrderResult;
import com.keplerops.groundcontrol.domain.requirements.service.WorkOrderWave;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.Priority;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private RequirementRelationRepository relationRepository;

    @Mock
    private TraceabilityLinkRepository traceabilityLinkRepository;

    @Mock
    private AuditService auditService;

    private AnalysisService service;

    private static final List<RelationType> DAG_TYPES =
            List.of(RelationType.PARENT, RelationType.DEPENDS_ON, RelationType.REFINES);

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
        service = new AnalysisService(
                requirementRepository, relationRepository, traceabilityLinkRepository, auditService);
    }

    private static Requirement makeRequirement(String uid, UUID id) {
        var req = new Requirement(TEST_PROJECT, uid, "Title for " + uid, "Statement for " + uid);
        setField(req, "id", id);
        return req;
    }

    private static Requirement makeRequirement(String uid, UUID id, Integer wave) {
        var req = makeRequirement(uid, id);
        req.setWave(wave);
        return req;
    }

    private static void setField(Object obj, String fieldName, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class DetectCycles {

        @Test
        void emptyGraph_returnsEmpty() {
            when(relationRepository.findActiveByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
                    .thenReturn(List.of());

            var result = service.detectCycles(PROJECT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void dagWithNoCycles_returnsEmpty() {
            UUID aId = UUID.randomUUID();
            UUID bId = UUID.randomUUID();
            var a = makeRequirement("REQ-A", aId);
            var b = makeRequirement("REQ-B", bId);
            var rel = new RequirementRelation(a, b, RelationType.PARENT);

            when(relationRepository.findActiveByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
                    .thenReturn(List.of(rel));

            var result = service.detectCycles(PROJECT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void triangleCycle_detected() {
            UUID aId = UUID.randomUUID();
            UUID bId = UUID.randomUUID();
            UUID cId = UUID.randomUUID();
            var a = makeRequirement("REQ-A", aId);
            var b = makeRequirement("REQ-B", bId);
            var c = makeRequirement("REQ-C", cId);

            var ab = new RequirementRelation(a, b, RelationType.DEPENDS_ON);
            var bc = new RequirementRelation(b, c, RelationType.DEPENDS_ON);
            var ca = new RequirementRelation(c, a, RelationType.PARENT);

            when(relationRepository.findActiveByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
                    .thenReturn(List.of(ab, bc, ca));

            var result = service.detectCycles(PROJECT_ID);

            assertThat(result).hasSize(1);
            CycleResult cycle = result.get(0);
            assertThat(cycle.members()).hasSize(4); // A, B, C, A (cycle closes)
            assertThat(cycle.edges()).hasSize(3);

            // Verify edges carry the correct relation types
            assertThat(cycle.edges())
                    .extracting(CycleEdge::relationType)
                    .containsExactlyInAnyOrder(RelationType.DEPENDS_ON, RelationType.DEPENDS_ON, RelationType.PARENT);
        }
    }

    @Nested
    class FindOrphans {

        @Test
        void noRelationsNoLinks_isOrphan() {
            UUID reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-ORPHAN", reqId);

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(req));
            when(relationRepository.findBySourceId(reqId)).thenReturn(List.of());
            when(relationRepository.findByTargetId(reqId)).thenReturn(List.of());
            when(traceabilityLinkRepository.existsByRequirementId(reqId)).thenReturn(false);

            var result = service.findOrphans(PROJECT_ID);

            assertThat(result).containsExactly(req);
        }

        @Test
        void withRelation_notOrphan() {
            UUID reqId = UUID.randomUUID();
            UUID otherId = UUID.randomUUID();
            var req = makeRequirement("REQ-LINKED", reqId);
            var other = makeRequirement("REQ-OTHER", otherId);
            var rel = new RequirementRelation(req, other, RelationType.PARENT);

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(req));
            when(relationRepository.findBySourceId(reqId)).thenReturn(List.of(rel));

            var result = service.findOrphans(PROJECT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void withLink_notOrphan() {
            UUID reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-TRACED", reqId);

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(req));
            when(relationRepository.findBySourceId(reqId)).thenReturn(List.of());
            when(relationRepository.findByTargetId(reqId)).thenReturn(List.of());
            when(traceabilityLinkRepository.existsByRequirementId(reqId)).thenReturn(true);

            var result = service.findOrphans(PROJECT_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindCoverageGaps {

        @Test
        void withoutLinkType_isGap() {
            UUID reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-GAP", reqId);

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(req));
            when(traceabilityLinkRepository.existsByRequirementIdAndLinkType(reqId, LinkType.TESTS))
                    .thenReturn(false);

            var result = service.findCoverageGaps(PROJECT_ID, LinkType.TESTS);

            assertThat(result).containsExactly(req);
        }

        @Test
        void withLinkType_notGap() {
            UUID reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-COVERED", reqId);

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(req));
            when(traceabilityLinkRepository.existsByRequirementIdAndLinkType(reqId, LinkType.TESTS))
                    .thenReturn(true);

            var result = service.findCoverageGaps(PROJECT_ID, LinkType.TESTS);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class ImpactAnalysis {

        @Test
        void noDownstream_returnsSeedOnly() {
            UUID seedId = UUID.randomUUID();
            var seed = makeRequirement("REQ-SEED", seedId);

            when(requirementRepository.findById(seedId)).thenReturn(Optional.of(seed));
            when(relationRepository.findActiveByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
                    .thenReturn(List.of());

            var result = service.impactAnalysis(seedId);

            assertThat(result).containsExactly(seed);
        }

        @Test
        void directDependents_returned() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            var parent = makeRequirement("REQ-PARENT", parentId);
            var child = makeRequirement("REQ-CHILD", childId);

            // child --PARENT--> parent (child depends on parent)
            var rel = new RequirementRelation(child, parent, RelationType.PARENT);

            when(requirementRepository.findById(parentId)).thenReturn(Optional.of(parent));
            when(requirementRepository.findById(childId)).thenReturn(Optional.of(child));
            when(relationRepository.findActiveByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
                    .thenReturn(List.of(rel));

            var result = service.impactAnalysis(parentId);

            assertThat(result).containsExactlyInAnyOrder(parent, child);
        }

        @Test
        void transitiveDependents_returned() {
            UUID aId = UUID.randomUUID();
            UUID bId = UUID.randomUUID();
            UUID cId = UUID.randomUUID();
            var a = makeRequirement("REQ-A", aId);
            var b = makeRequirement("REQ-B", bId);
            var c = makeRequirement("REQ-C", cId);

            // c --PARENT--> b --PARENT--> a
            var relBA = new RequirementRelation(b, a, RelationType.PARENT);
            var relCB = new RequirementRelation(c, b, RelationType.PARENT);

            when(requirementRepository.findById(aId)).thenReturn(Optional.of(a));
            when(requirementRepository.findById(bId)).thenReturn(Optional.of(b));
            when(requirementRepository.findById(cId)).thenReturn(Optional.of(c));
            when(relationRepository.findActiveByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
                    .thenReturn(List.of(relBA, relCB));

            var result = service.impactAnalysis(aId);

            assertThat(result).containsExactlyInAnyOrder(a, b, c);
        }

        @Test
        void diamond_noDuplicates() {
            UUID aId = UUID.randomUUID();
            UUID bId = UUID.randomUUID();
            UUID cId = UUID.randomUUID();
            UUID dId = UUID.randomUUID();
            var a = makeRequirement("REQ-A", aId);
            var b = makeRequirement("REQ-B", bId);
            var c = makeRequirement("REQ-C", cId);
            var d = makeRequirement("REQ-D", dId);

            // Diamond: b-->a, c-->a, d-->b, d-->c
            var relBA = new RequirementRelation(b, a, RelationType.PARENT);
            var relCA = new RequirementRelation(c, a, RelationType.PARENT);
            var relDB = new RequirementRelation(d, b, RelationType.DEPENDS_ON);
            var relDC = new RequirementRelation(d, c, RelationType.DEPENDS_ON);

            when(requirementRepository.findById(aId)).thenReturn(Optional.of(a));
            when(requirementRepository.findById(bId)).thenReturn(Optional.of(b));
            when(requirementRepository.findById(cId)).thenReturn(Optional.of(c));
            when(requirementRepository.findById(dId)).thenReturn(Optional.of(d));
            when(relationRepository.findActiveByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
                    .thenReturn(List.of(relBA, relCA, relDB, relDC));

            var result = service.impactAnalysis(aId);

            assertThat(result).hasSize(4);
            assertThat(result).containsExactlyInAnyOrder(a, b, c, d);
        }

        @Test
        void missingId_throwsNotFoundException() {
            UUID missingId = UUID.randomUUID();
            when(requirementRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.impactAnalysis(missingId)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class DetectConsistencyViolations {

        @Test
        void noViolations_returnsEmpty() {
            when(relationRepository.findActiveWithSourceAndTargetByProjectId(PROJECT_ID))
                    .thenReturn(List.of());

            var result = service.detectConsistencyViolations(PROJECT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void activeConflict_detected() {
            UUID aId = UUID.randomUUID();
            UUID bId = UUID.randomUUID();
            var a = makeRequirement("REQ-A", aId);
            var b = makeRequirement("REQ-B", bId);
            setField(a, "status", Status.ACTIVE);
            setField(b, "status", Status.ACTIVE);

            var rel = new RequirementRelation(a, b, RelationType.CONFLICTS_WITH);

            when(relationRepository.findActiveWithSourceAndTargetByProjectId(PROJECT_ID))
                    .thenReturn(List.of(rel));

            var result = service.detectConsistencyViolations(PROJECT_ID);

            assertThat(result).hasSize(1);
            ConsistencyViolation violation = result.get(0);
            assertThat(violation.violationType()).isEqualTo("ACTIVE_CONFLICT");
            assertThat(violation.relation()).isSameAs(rel);
        }

        @Test
        void activeSupersedes_detected() {
            UUID aId = UUID.randomUUID();
            UUID bId = UUID.randomUUID();
            var a = makeRequirement("REQ-A", aId);
            var b = makeRequirement("REQ-B", bId);
            setField(a, "status", Status.ACTIVE);
            setField(b, "status", Status.ACTIVE);

            var rel = new RequirementRelation(a, b, RelationType.SUPERSEDES);

            when(relationRepository.findActiveWithSourceAndTargetByProjectId(PROJECT_ID))
                    .thenReturn(List.of(rel));

            var result = service.detectConsistencyViolations(PROJECT_ID);

            assertThat(result).hasSize(1);
            ConsistencyViolation violation = result.get(0);
            assertThat(violation.violationType()).isEqualTo("ACTIVE_SUPERSEDES");
            assertThat(violation.relation()).isSameAs(rel);
        }

        @Test
        void draftConflict_notDetected() {
            UUID aId = UUID.randomUUID();
            UUID bId = UUID.randomUUID();
            var a = makeRequirement("REQ-A", aId);
            var b = makeRequirement("REQ-B", bId);
            setField(a, "status", Status.ACTIVE);
            // b remains DRAFT (default)

            var rel = new RequirementRelation(a, b, RelationType.CONFLICTS_WITH);

            when(relationRepository.findActiveWithSourceAndTargetByProjectId(PROJECT_ID))
                    .thenReturn(List.of(rel));

            var result = service.detectConsistencyViolations(PROJECT_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class CrossWaveValidation {

        @Test
        void correctOrder_returnsEmpty() {
            UUID aId = UUID.randomUUID();
            UUID bId = UUID.randomUUID();
            var a = makeRequirement("REQ-A", aId, 2);
            var b = makeRequirement("REQ-B", bId, 1);

            // Source wave 2, target wave 1 — later depends on earlier, correct order
            var rel = new RequirementRelation(a, b, RelationType.DEPENDS_ON);

            when(relationRepository.findActiveWithSourceAndTargetByProjectId(PROJECT_ID))
                    .thenReturn(List.of(rel));

            var result = service.crossWaveValidation(PROJECT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void forwardDependency_detected() {
            UUID aId = UUID.randomUUID();
            UUID bId = UUID.randomUUID();
            var a = makeRequirement("REQ-A", aId, 1);
            var b = makeRequirement("REQ-B", bId, 3);

            // Source wave 1 < target wave 3 — earlier depends on later, forward dependency violation
            var rel = new RequirementRelation(a, b, RelationType.DEPENDS_ON);

            when(relationRepository.findActiveWithSourceAndTargetByProjectId(PROJECT_ID))
                    .thenReturn(List.of(rel));

            var result = service.crossWaveValidation(PROJECT_ID);

            assertThat(result).containsExactly(rel);
        }

        @Test
        void nullWaves_skipped() {
            UUID aId = UUID.randomUUID();
            UUID bId = UUID.randomUUID();
            var a = makeRequirement("REQ-A", aId);
            var b = makeRequirement("REQ-B", bId);

            var rel = new RequirementRelation(a, b, RelationType.PARENT);

            when(relationRepository.findActiveWithSourceAndTargetByProjectId(PROJECT_ID))
                    .thenReturn(List.of(rel));

            var result = service.crossWaveValidation(PROJECT_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class AnalyzeCompleteness {

        @Test
        void emptyProject_returnsZero() {
            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of());

            CompletenessResult result = service.analyzeCompleteness(PROJECT_ID);

            assertThat(result.total()).isZero();
            assertThat(result.byStatus()).isEmpty();
            assertThat(result.issues()).isEmpty();
        }

        @Test
        void countsStatuses() {
            var draft = makeRequirement("REQ-D", UUID.randomUUID());
            var active = makeRequirement("REQ-A", UUID.randomUUID());
            setField(active, "status", Status.ACTIVE);
            var active2 = makeRequirement("REQ-A2", UUID.randomUUID());
            setField(active2, "status", Status.ACTIVE);

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(draft, active, active2));

            CompletenessResult result = service.analyzeCompleteness(PROJECT_ID);

            assertThat(result.total()).isEqualTo(3);
            assertThat(result.byStatus()).containsEntry("DRAFT", 1);
            assertThat(result.byStatus()).containsEntry("ACTIVE", 2);
            assertThat(result.issues()).isEmpty();
        }

        @Test
        void detectsMissingStatement() {
            var req = makeRequirement("REQ-BLANK", UUID.randomUUID());
            setField(req, "statement", "");

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(req));

            CompletenessResult result = service.analyzeCompleteness(PROJECT_ID);

            assertThat(result.issues()).hasSize(1);
            assertThat(result.issues().get(0).uid()).isEqualTo("REQ-BLANK");
            assertThat(result.issues().get(0).issue()).isEqualTo("missing statement");
        }

        @Test
        void detectsMissingTitle() {
            var req = makeRequirement("REQ-NOTITLE", UUID.randomUUID());
            setField(req, "title", "");

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(req));

            CompletenessResult result = service.analyzeCompleteness(PROJECT_ID);

            assertThat(result.issues()).hasSize(1);
            assertThat(result.issues().get(0).uid()).isEqualTo("REQ-NOTITLE");
            assertThat(result.issues().get(0).issue()).isEqualTo("missing title");
        }
    }

    @Nested
    class GetDashboardStats {

        @Test
        void emptyProject_returnsZeroCounts() {
            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of());
            when(auditService.getRecentRequirementChanges(Set.of(), 10)).thenReturn(List.of());

            DashboardStats result = service.getDashboardStats(PROJECT_ID);

            assertThat(result.totalRequirements()).isZero();
            assertThat(result.byStatus()).isEmpty();
            assertThat(result.byWave()).isEmpty();
            assertThat(result.coverageByLinkType()).hasSize(LinkType.values().length);
            assertThat(result.recentChanges()).isEmpty();
        }

        @Test
        void aggregatesByStatusAndWave() {
            UUID aId = UUID.randomUUID();
            UUID bId = UUID.randomUUID();
            UUID cId = UUID.randomUUID();
            var draft1 = makeRequirement("REQ-D1", aId, 1);
            var active1 = makeRequirement("REQ-A1", bId, 1);
            setField(active1, "status", Status.ACTIVE);
            var active2 = makeRequirement("REQ-A2", cId, 2);
            setField(active2, "status", Status.ACTIVE);

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(draft1, active1, active2));
            when(auditService.getRecentRequirementChanges(Set.of(aId, bId, cId), 10))
                    .thenReturn(List.of());

            DashboardStats result = service.getDashboardStats(PROJECT_ID);

            assertThat(result.totalRequirements()).isEqualTo(3);
            assertThat(result.byStatus()).containsEntry("DRAFT", 1);
            assertThat(result.byStatus()).containsEntry("ACTIVE", 2);

            // Wave 1: 2 reqs (1 DRAFT, 1 ACTIVE); Wave 2: 1 req (1 ACTIVE)
            assertThat(result.byWave()).hasSize(2);
            assertThat(result.byWave().get(0).wave()).isEqualTo(1);
            assertThat(result.byWave().get(0).total()).isEqualTo(2);
            assertThat(result.byWave().get(1).wave()).isEqualTo(2);
            assertThat(result.byWave().get(1).total()).isEqualTo(1);
        }

        @Test
        void coverageComputed() {
            UUID reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-COV", reqId);

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(req));
            when(traceabilityLinkRepository.existsByRequirementIdAndLinkType(reqId, LinkType.IMPLEMENTS))
                    .thenReturn(true);
            when(traceabilityLinkRepository.existsByRequirementIdAndLinkType(reqId, LinkType.TESTS))
                    .thenReturn(false);
            when(traceabilityLinkRepository.existsByRequirementIdAndLinkType(reqId, LinkType.DOCUMENTS))
                    .thenReturn(false);
            when(traceabilityLinkRepository.existsByRequirementIdAndLinkType(reqId, LinkType.CONSTRAINS))
                    .thenReturn(false);
            when(traceabilityLinkRepository.existsByRequirementIdAndLinkType(reqId, LinkType.VERIFIES))
                    .thenReturn(false);
            when(auditService.getRecentRequirementChanges(Set.of(reqId), 10))
                    .thenReturn(List.of(new RecentChange("REQ-COV", "Title", "ADD", Instant.now(), "test-actor")));

            DashboardStats result = service.getDashboardStats(PROJECT_ID);

            CoverageStats implCoverage = result.coverageByLinkType().get("IMPLEMENTS");
            assertThat(implCoverage.total()).isEqualTo(1);
            assertThat(implCoverage.covered()).isEqualTo(1);
            assertThat(implCoverage.percentage()).isEqualTo(100.0);

            CoverageStats testsCoverage = result.coverageByLinkType().get("TESTS");
            assertThat(testsCoverage.covered()).isZero();
            assertThat(testsCoverage.percentage()).isEqualTo(0.0);

            assertThat(result.recentChanges()).hasSize(1);
        }
    }

    @Nested
    class GetWorkOrder {

        @Test
        void emptyProject_returnsEmptyResult() {
            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of());
            when(relationRepository.findActiveByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
                    .thenReturn(List.of());

            WorkOrderResult result = service.getWorkOrder(PROJECT_ID);

            assertThat(result.totalRequirements()).isZero();
            assertThat(result.totalUnblocked()).isZero();
            assertThat(result.totalBlocked()).isZero();
            assertThat(result.totalUnconstrained()).isZero();
            assertThat(result.waves()).isEmpty();
        }

        @Test
        void blockedRequirement_identifiedWithBlockers() {
            UUID aId = UUID.randomUUID();
            UUID bId = UUID.randomUUID();
            var a = makeRequirement("REQ-A", aId, 1);
            var b = makeRequirement("REQ-B", bId, 1);
            // b is DRAFT (default), a depends on b -> a is BLOCKED
            var rel = new RequirementRelation(a, b, RelationType.DEPENDS_ON);

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(a, b));
            when(relationRepository.findActiveByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
                    .thenReturn(List.of(rel));

            WorkOrderResult result = service.getWorkOrder(PROJECT_ID);

            assertThat(result.totalRequirements()).isEqualTo(2);
            assertThat(result.totalBlocked()).isEqualTo(1);
            assertThat(result.totalUnconstrained()).isEqualTo(1);

            WorkOrderWave wave = result.waves().get(0);
            WorkOrderItem blockedItem = wave.items().stream()
                    .filter(i -> i.uid().equals("REQ-A"))
                    .findFirst()
                    .orElseThrow();
            assertThat(blockedItem.blockingStatus()).isEqualTo(BlockingStatus.BLOCKED);
            assertThat(blockedItem.blockedBy()).containsExactly("REQ-B");

            WorkOrderItem unconstrainedItem = wave.items().stream()
                    .filter(i -> i.uid().equals("REQ-B"))
                    .findFirst()
                    .orElseThrow();
            assertThat(unconstrainedItem.blockingStatus()).isEqualTo(BlockingStatus.UNCONSTRAINED);
        }

        @Test
        void satisfiedDependency_isUnblocked() {
            UUID aId = UUID.randomUUID();
            UUID bId = UUID.randomUUID();
            var a = makeRequirement("REQ-A", aId, 1);
            var b = makeRequirement("REQ-B", bId, 1);
            setField(b, "status", Status.ACTIVE);
            // a depends on b, b is ACTIVE -> a is UNBLOCKED
            var rel = new RequirementRelation(a, b, RelationType.DEPENDS_ON);

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(a, b));
            when(relationRepository.findActiveByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
                    .thenReturn(List.of(rel));

            WorkOrderResult result = service.getWorkOrder(PROJECT_ID);

            assertThat(result.totalUnblocked()).isEqualTo(1);

            WorkOrderItem unblockedItem = result.waves().get(0).items().stream()
                    .filter(i -> i.uid().equals("REQ-A"))
                    .findFirst()
                    .orElseThrow();
            assertThat(unblockedItem.blockingStatus()).isEqualTo(BlockingStatus.UNBLOCKED);
            assertThat(unblockedItem.blockedBy()).isEmpty();
        }

        @Test
        void singleWave_sortedByDependencyThenPriority() {
            UUID aId = UUID.randomUUID();
            UUID bId = UUID.randomUUID();
            UUID cId = UUID.randomUUID();
            // c (COULD) depends on a (MUST). b (SHOULD) is independent.
            var a = makeRequirement("REQ-A", aId, 1);
            a.setPriority(Priority.MUST);
            var b = makeRequirement("REQ-B", bId, 1);
            b.setPriority(Priority.SHOULD);
            var c = makeRequirement("REQ-C", cId, 1);
            c.setPriority(Priority.COULD);
            setField(a, "status", Status.ACTIVE);

            var rel = new RequirementRelation(c, a, RelationType.DEPENDS_ON);

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(a, b, c));
            when(relationRepository.findActiveByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
                    .thenReturn(List.of(rel));

            WorkOrderResult result = service.getWorkOrder(PROJECT_ID);

            List<String> uids = result.waves().get(0).items().stream()
                    .map(WorkOrderItem::uid)
                    .toList();
            // a comes before c (dependency). b is independent with SHOULD priority.
            // Topo sort: a (MUST, no deps) and b (SHOULD, no deps) first, then c (COULD, depends on a)
            // Priority tie-breaking: a (MUST=0) before b (SHOULD=1)
            assertThat(uids).containsExactly("REQ-A", "REQ-B", "REQ-C");
        }

        @Test
        void multipleWaves_groupedSeparately() {
            UUID aId = UUID.randomUUID();
            UUID bId = UUID.randomUUID();
            var a = makeRequirement("REQ-A", aId, 1);
            var b = makeRequirement("REQ-B", bId, 2);

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(a, b));
            when(relationRepository.findActiveByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
                    .thenReturn(List.of());

            WorkOrderResult result = service.getWorkOrder(PROJECT_ID);

            assertThat(result.waves()).hasSize(2);
            assertThat(result.waves().get(0).wave()).isEqualTo(1);
            assertThat(result.waves().get(0).items()).hasSize(1);
            assertThat(result.waves().get(0).items().get(0).uid()).isEqualTo("REQ-A");
            assertThat(result.waves().get(1).wave()).isEqualTo(2);
            assertThat(result.waves().get(1).items()).hasSize(1);
            assertThat(result.waves().get(1).items().get(0).uid()).isEqualTo("REQ-B");
        }

        @Test
        void nullWave_sortedLast() {
            UUID aId = UUID.randomUUID();
            UUID bId = UUID.randomUUID();
            var a = makeRequirement("REQ-A", aId, 1);
            var b = makeRequirement("REQ-B", bId); // null wave

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(a, b));
            when(relationRepository.findActiveByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
                    .thenReturn(List.of());

            WorkOrderResult result = service.getWorkOrder(PROJECT_ID);

            assertThat(result.waves()).hasSize(2);
            assertThat(result.waves().get(0).wave()).isEqualTo(1);
            assertThat(result.waves().get(1).wave()).isNull();
        }
    }
}
