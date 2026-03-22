package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.*;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.state.*;
import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S2187") // Tests are in @Nested inner classes
class RequirementTest {

    private static final Project TEST_PROJECT = createTestProject();

    private static Project createTestProject() {
        var project = new Project("test-project", "Test Project");
        try {
            var field = Project.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(project, UUID.fromString("00000000-0000-0000-0000-000000000001"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return project;
    }

    private static Requirement createRequirement(String uid) {
        return new Requirement(TEST_PROJECT, uid, "Title for " + uid, "Statement for " + uid);
    }

    private static void setId(Requirement req, UUID id) {
        try {
            Field f = Requirement.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(req, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class Defaults {

        @Test
        void newRequirementHasDraftStatus() {
            var req = createRequirement("REQ-001");
            assertThat(req.getStatus()).isEqualTo(Status.DRAFT);
        }

        @Test
        void newRequirementHasFunctionalType() {
            var req = createRequirement("REQ-001");
            assertThat(req.getRequirementType()).isEqualTo(RequirementType.FUNCTIONAL);
        }

        @Test
        void newRequirementHasMustPriority() {
            var req = createRequirement("REQ-001");
            assertThat(req.getPriority()).isEqualTo(Priority.MUST);
        }

        @Test
        void newRequirementHasNullArchivedAt() {
            var req = createRequirement("REQ-001");
            assertThat(req.getArchivedAt()).isNull();
        }
    }

    @Nested
    class StatusTransitions {

        @Test
        void draftToActiveSucceeds() {
            var req = createRequirement("REQ-001");
            req.transitionStatus(Status.ACTIVE);
            assertThat(req.getStatus()).isEqualTo(Status.ACTIVE);
        }

        @Test
        void activeToDeprecatedSucceeds() {
            var req = createRequirement("REQ-001");
            req.transitionStatus(Status.ACTIVE);
            req.transitionStatus(Status.DEPRECATED);
            assertThat(req.getStatus()).isEqualTo(Status.DEPRECATED);
        }

        @Test
        void activeToArchivedSucceeds() {
            var req = createRequirement("REQ-001");
            req.transitionStatus(Status.ACTIVE);
            req.transitionStatus(Status.ARCHIVED);
            assertThat(req.getStatus()).isEqualTo(Status.ARCHIVED);
        }

        @Test
        void deprecatedToArchivedSucceeds() {
            var req = createRequirement("REQ-001");
            req.transitionStatus(Status.ACTIVE);
            req.transitionStatus(Status.DEPRECATED);
            req.transitionStatus(Status.ARCHIVED);
            assertThat(req.getStatus()).isEqualTo(Status.ARCHIVED);
        }

        @Test
        void draftToArchivedFails() {
            var req = createRequirement("REQ-001");
            assertThatThrownBy(() -> req.transitionStatus(Status.ARCHIVED))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("DRAFT")
                    .hasMessageContaining("ARCHIVED");
        }

        @Test
        void archivedToAnyFails() {
            var req = createRequirement("REQ-001");
            req.transitionStatus(Status.ACTIVE);
            req.transitionStatus(Status.ARCHIVED);
            for (Status target : Status.values()) {
                if (target == Status.ARCHIVED) continue;
                assertThatThrownBy(() -> req.transitionStatus(target)).isInstanceOf(DomainValidationException.class);
            }
        }

        @Test
        void invalidTransitionContainsErrorCodeAndDetail() {
            var req = createRequirement("REQ-001");
            assertThatThrownBy(() -> req.transitionStatus(Status.DEPRECATED))
                    .isInstanceOf(DomainValidationException.class)
                    .satisfies(ex -> {
                        var dve = (DomainValidationException) ex;
                        assertThat(dve.getErrorCode()).isEqualTo("invalid_status_transition");
                        assertThat(dve.getDetail()).containsEntry("current_status", "DRAFT");
                        assertThat(dve.getDetail()).containsEntry("target_status", "DEPRECATED");
                    });
        }
    }

    @Nested
    class Archive {

        @Test
        void archiveFromActiveSetsStatusAndTimestamp() {
            var req = createRequirement("REQ-001");
            req.transitionStatus(Status.ACTIVE);
            req.archive();
            assertThat(req.getStatus()).isEqualTo(Status.ARCHIVED);
            assertThat(req.getArchivedAt()).isNotNull();
        }

        @Test
        void archiveIsIdempotent() {
            var req = createRequirement("REQ-001");
            req.transitionStatus(Status.ACTIVE);
            req.archive();
            var firstArchivedAt = req.getArchivedAt();
            req.archive(); // should not throw
            assertThat(req.getArchivedAt()).isEqualTo(firstArchivedAt);
        }

        @Test
        void archiveFromDraftFails() {
            var req = createRequirement("REQ-001");
            assertThatThrownBy(req::archive).isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    class Accessors {

        @Test
        void settersAndGettersCoverAllFields() {
            var req = createRequirement("REQ-001");
            req.setStatement("new statement");
            assertThat(req.getStatement()).isEqualTo("new statement");
            req.setRationale("some rationale");
            assertThat(req.getRationale()).isEqualTo("some rationale");
            req.setWave(3);
            assertThat(req.getWave()).isEqualTo(3);
            assertThat(req.getCreatedAt()).isNull(); // not persisted
            assertThat(req.getUpdatedAt()).isNull();
            assertThat(req.toString()).isEqualTo("REQ-001");
        }
    }

    @Nested
    class Equality {

        @Test
        void sameInstanceIsEqual() {
            var req = createRequirement("REQ-001");
            assertThat(req).isEqualTo(req);
        }

        @Test
        void twoInstancesWithSameUidAreEqual() {
            var req1 = createRequirement("REQ-001");
            var req2 = createRequirement("REQ-001");
            assertThat(req1).isEqualTo(req2);
        }

        @Test
        void differentUidsAreNotEqual() {
            var req1 = createRequirement("REQ-001");
            var req2 = createRequirement("REQ-002");
            assertThat(req1).isNotEqualTo(req2);
        }

        @Test
        void equalRequirementsHaveSameHashCode() {
            var req1 = createRequirement("REQ-001");
            var req2 = createRequirement("REQ-001");
            assertThat(req1.hashCode()).isEqualTo(req2.hashCode());
        }

        @Test
        void requirementIsNotEqualToNull() {
            var req = createRequirement("REQ-001");
            assertThat(req).isNotEqualTo(null);
        }

        @Test
        void requirementIsNotEqualToDifferentType() {
            var req = createRequirement("REQ-001");
            assertThat(req).isNotEqualTo("REQ-001");
        }

        @Test
        void sameUidInSetDeduplicates() {
            var req1 = createRequirement("REQ-001");
            var req2 = createRequirement("REQ-001");
            var set = new java.util.HashSet<Requirement>();
            set.add(req1);
            set.add(req2);
            assertThat(set).hasSize(1);
        }
    }

    @Nested
    class RelationEquality {

        @Test
        void sameSourceTargetTypeIsEqual() {
            var source = createRequirement("REQ-001");
            var target = createRequirement("REQ-002");
            setId(source, UUID.fromString("00000000-0000-0000-0000-000000000010"));
            setId(target, UUID.fromString("00000000-0000-0000-0000-000000000020"));
            var rel1 = new RequirementRelation(source, target, RelationType.DEPENDS_ON);
            var rel2 = new RequirementRelation(source, target, RelationType.DEPENDS_ON);
            assertThat(rel1).isEqualTo(rel2);
        }

        @Test
        void differentRelationTypeIsNotEqual() {
            var source = createRequirement("REQ-001");
            var target = createRequirement("REQ-002");
            setId(source, UUID.fromString("00000000-0000-0000-0000-000000000010"));
            setId(target, UUID.fromString("00000000-0000-0000-0000-000000000020"));
            var rel1 = new RequirementRelation(source, target, RelationType.DEPENDS_ON);
            var rel2 = new RequirementRelation(source, target, RelationType.REFINES);
            assertThat(rel1).isNotEqualTo(rel2);
        }

        @Test
        void equalRelationsHaveSameHashCode() {
            var source = createRequirement("REQ-001");
            var target = createRequirement("REQ-002");
            setId(source, UUID.fromString("00000000-0000-0000-0000-000000000010"));
            setId(target, UUID.fromString("00000000-0000-0000-0000-000000000020"));
            var rel1 = new RequirementRelation(source, target, RelationType.DEPENDS_ON);
            var rel2 = new RequirementRelation(source, target, RelationType.DEPENDS_ON);
            assertThat(rel1.hashCode()).isEqualTo(rel2.hashCode());
        }
    }

    @Nested
    class Relations {

        @Test
        void selfLoopThrows() {
            var req = createRequirement("REQ-001");
            setId(req, UUID.randomUUID());
            assertThatThrownBy(() -> new RequirementRelation(req, req, RelationType.DEPENDS_ON))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("itself");
        }

        @Test
        void validRelationCreates() {
            var source = createRequirement("REQ-001");
            var target = createRequirement("REQ-002");
            setId(source, UUID.randomUUID());
            setId(target, UUID.randomUUID());
            var rel = new RequirementRelation(source, target, RelationType.DEPENDS_ON);
            assertThat(rel.getSource()).isEqualTo(source);
            assertThat(rel.getTarget()).isEqualTo(target);
            assertThat(rel.getRelationType()).isEqualTo(RelationType.DEPENDS_ON);
        }

        @Test
        void relationAccessorsCoverAllFields() {
            var source = createRequirement("REQ-001");
            var target = createRequirement("REQ-002");
            setId(source, UUID.randomUUID());
            setId(target, UUID.randomUUID());
            var rel = new RequirementRelation(source, target, RelationType.DEPENDS_ON);
            assertThat(rel.getId()).isNull(); // not persisted
            assertThat(rel.getCreatedAt()).isNull();
            assertThat(rel.getDescription()).isEmpty();
            rel.setDescription("some desc");
            assertThat(rel.getDescription()).isEqualTo("some desc");
            assertThat(rel.toString()).contains("REQ-001").contains("REQ-002").contains("DEPENDS_ON");
        }
    }
}
