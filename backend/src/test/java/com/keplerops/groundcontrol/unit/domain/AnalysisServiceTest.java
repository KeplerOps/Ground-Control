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
import com.keplerops.groundcontrol.domain.requirements.service.CycleEdge;
import com.keplerops.groundcontrol.domain.requirements.service.CycleResult;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
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
        service = new AnalysisService(requirementRepository, relationRepository, traceabilityLinkRepository);
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
            when(relationRepository.findAllByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
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

            when(relationRepository.findAllByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
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

            when(relationRepository.findAllByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
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

            when(requirementRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(req));
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

            when(requirementRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(req));
            when(relationRepository.findBySourceId(reqId)).thenReturn(List.of(rel));

            var result = service.findOrphans(PROJECT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void withLink_notOrphan() {
            UUID reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-TRACED", reqId);

            when(requirementRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(req));
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

            when(requirementRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(req));
            when(traceabilityLinkRepository.existsByRequirementIdAndLinkType(reqId, LinkType.TESTS))
                    .thenReturn(false);

            var result = service.findCoverageGaps(PROJECT_ID, LinkType.TESTS);

            assertThat(result).containsExactly(req);
        }

        @Test
        void withLinkType_notGap() {
            UUID reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-COVERED", reqId);

            when(requirementRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(req));
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
            when(relationRepository.findAllByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
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
            when(relationRepository.findAllByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
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
            when(relationRepository.findAllByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
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
            when(relationRepository.findAllByProjectAndRelationTypeIn(PROJECT_ID, DAG_TYPES))
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
    class CrossWaveValidation {

        @Test
        void correctOrder_returnsEmpty() {
            UUID aId = UUID.randomUUID();
            UUID bId = UUID.randomUUID();
            var a = makeRequirement("REQ-A", aId, 1);
            var b = makeRequirement("REQ-B", bId, 2);

            // Source wave 1, target wave 2 — correct order
            var rel = new RequirementRelation(a, b, RelationType.DEPENDS_ON);

            when(relationRepository.findAllWithSourceAndTargetByProjectId(PROJECT_ID))
                    .thenReturn(List.of(rel));

            var result = service.crossWaveValidation(PROJECT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void backwardDependency_detected() {
            UUID aId = UUID.randomUUID();
            UUID bId = UUID.randomUUID();
            var a = makeRequirement("REQ-A", aId, 3);
            var b = makeRequirement("REQ-B", bId, 1);

            // Source wave 3 > target wave 1 — backward dependency
            var rel = new RequirementRelation(a, b, RelationType.DEPENDS_ON);

            when(relationRepository.findAllWithSourceAndTargetByProjectId(PROJECT_ID))
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

            when(relationRepository.findAllWithSourceAndTargetByProjectId(PROJECT_ID))
                    .thenReturn(List.of(rel));

            var result = service.crossWaveValidation(PROJECT_ID);

            assertThat(result).isEmpty();
        }
    }
}
