package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.service.CloneRequirementCommand;
import com.keplerops.groundcontrol.domain.requirements.service.CreateRequirementCommand;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementFilter;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementService;
import com.keplerops.groundcontrol.domain.requirements.service.UpdateRequirementCommand;
import com.keplerops.groundcontrol.domain.requirements.state.Priority;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import com.keplerops.groundcontrol.domain.requirements.state.RequirementType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class RequirementServiceTest {

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private RequirementRelationRepository relationRepository;

    private RequirementService service;

    @BeforeEach
    void setUp() {
        service = new RequirementService(requirementRepository, relationRepository);
    }

    private static Requirement makeRequirement(String uid) {
        return new Requirement(uid, "Title for " + uid, "Statement for " + uid);
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
    class Create {

        @Test
        void createsRequirementInDraftStatus() {
            var cmd = new CreateRequirementCommand(
                    "REQ-001", "Title", "Statement", "Rationale", RequirementType.FUNCTIONAL, Priority.MUST, 1);

            when(requirementRepository.existsByUid("REQ-001")).thenReturn(false);
            when(requirementRepository.save(any(Requirement.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = service.create(cmd);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(Status.DRAFT);
            assertThat(result.getUid()).isEqualTo("REQ-001");
        }

        @Test
        void throwsConflictOnDuplicateUid() {
            var cmd = new CreateRequirementCommand("REQ-001", "Title", "Statement", null, null, null, null);

            when(requirementRepository.existsByUid("REQ-001")).thenReturn(true);

            assertThatThrownBy(() -> service.create(cmd)).isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class GetById {

        @Test
        void returnsExistingRequirement() {
            var id = UUID.randomUUID();
            var req = makeRequirement("REQ-001");
            when(requirementRepository.findById(id)).thenReturn(Optional.of(req));

            var result = service.getById(id);
            assertThat(result).isNotNull();
            assertThat(result.getUid()).isEqualTo("REQ-001");
        }

        @Test
        void throwsNotFoundForMissingId() {
            var id = UUID.randomUUID();
            when(requirementRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(id)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class GetByUid {

        @Test
        void returnsExistingRequirement() {
            var req = makeRequirement("REQ-001");
            when(requirementRepository.findByUid("REQ-001")).thenReturn(Optional.of(req));

            var result = service.getByUid("REQ-001");
            assertThat(result).isNotNull();
        }

        @Test
        void throwsNotFoundForMissingUid() {
            when(requirementRepository.findByUid("NOPE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByUid("NOPE")).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void updatesFieldsSuccessfully() {
            var id = UUID.randomUUID();
            var req = makeRequirement("REQ-001");
            when(requirementRepository.findById(id)).thenReturn(Optional.of(req));
            when(requirementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var cmd = new UpdateRequirementCommand(
                    "New Title", "New Statement", null, RequirementType.CONSTRAINT, Priority.SHOULD, 2);

            var result = service.update(id, cmd);
            assertThat(result.getTitle()).isEqualTo("New Title");
            assertThat(result.getRequirementType()).isEqualTo(RequirementType.CONSTRAINT);
        }

        @Test
        void throwsNotFoundForMissingId() {
            var id = UUID.randomUUID();
            when(requirementRepository.findById(id)).thenReturn(Optional.empty());

            var cmd = new UpdateRequirementCommand("Title", "Stmt", null, null, null, null);

            assertThatThrownBy(() -> service.update(id, cmd)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class TransitionStatus {

        @Test
        void transitionsSuccessfully() {
            var id = UUID.randomUUID();
            var req = makeRequirement("REQ-001");
            when(requirementRepository.findById(id)).thenReturn(Optional.of(req));
            when(requirementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.transitionStatus(id, Status.ACTIVE);
            assertThat(result.getStatus()).isEqualTo(Status.ACTIVE);
        }

        @Test
        void throwsNotFoundForMissingId() {
            var id = UUID.randomUUID();
            when(requirementRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.transitionStatus(id, Status.ACTIVE))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void throwsDomainValidationForInvalidTransition() {
            var id = UUID.randomUUID();
            var req = makeRequirement("REQ-001");
            when(requirementRepository.findById(id)).thenReturn(Optional.of(req));

            assertThatThrownBy(() -> service.transitionStatus(id, Status.ARCHIVED))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    class Archive {

        @Test
        void archivesSuccessfully() {
            var id = UUID.randomUUID();
            var req = makeRequirement("REQ-001");
            req.transitionStatus(Status.ACTIVE);
            when(requirementRepository.findById(id)).thenReturn(Optional.of(req));
            when(requirementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.archive(id);
            assertThat(result.getStatus()).isEqualTo(Status.ARCHIVED);
            assertThat(result.getArchivedAt()).isNotNull();
        }

        @Test
        void throwsNotFoundForMissingId() {
            var id = UUID.randomUUID();
            when(requirementRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.archive(id)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void throwsDomainValidationFromDraft() {
            var id = UUID.randomUUID();
            var req = makeRequirement("REQ-001");
            when(requirementRepository.findById(id)).thenReturn(Optional.of(req));

            assertThatThrownBy(() -> service.archive(id)).isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    class CreateRelation {

        @Test
        void createsRelationSuccessfully() {
            var sourceId = UUID.randomUUID();
            var targetId = UUID.randomUUID();
            var source = makeRequirement("REQ-001");
            var target = makeRequirement("REQ-002");
            setId(source, sourceId);
            setId(target, targetId);

            when(requirementRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(requirementRepository.findById(targetId)).thenReturn(Optional.of(target));
            when(relationRepository.save(any(RequirementRelation.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = service.createRelation(sourceId, targetId, RelationType.DEPENDS_ON);
            assertThat(result).isNotNull();
            assertThat(result.getRelationType()).isEqualTo(RelationType.DEPENDS_ON);
        }

        @Test
        void createsSupersedingRelation() {
            var sourceId = UUID.randomUUID();
            var targetId = UUID.randomUUID();
            var source = makeRequirement("REQ-001");
            var target = makeRequirement("REQ-002");
            setId(source, sourceId);
            setId(target, targetId);

            when(requirementRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(requirementRepository.findById(targetId)).thenReturn(Optional.of(target));
            when(relationRepository.save(any(RequirementRelation.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = service.createRelation(sourceId, targetId, RelationType.SUPERSEDES);
            assertThat(result).isNotNull();
            assertThat(result.getRelationType()).isEqualTo(RelationType.SUPERSEDES);
        }

        @Test
        void createsRelatedRelation() {
            var sourceId = UUID.randomUUID();
            var targetId = UUID.randomUUID();
            var source = makeRequirement("REQ-001");
            var target = makeRequirement("REQ-002");
            setId(source, sourceId);
            setId(target, targetId);

            when(requirementRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(requirementRepository.findById(targetId)).thenReturn(Optional.of(target));
            when(relationRepository.save(any(RequirementRelation.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = service.createRelation(sourceId, targetId, RelationType.RELATED);
            assertThat(result).isNotNull();
            assertThat(result.getRelationType()).isEqualTo(RelationType.RELATED);
        }

        @Test
        void throwsConflictForDuplicateRelation() {
            var sourceId = UUID.randomUUID();
            var targetId = UUID.randomUUID();

            when(relationRepository.existsBySourceIdAndTargetIdAndRelationType(
                            sourceId, targetId, RelationType.DEPENDS_ON))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.createRelation(sourceId, targetId, RelationType.DEPENDS_ON))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void throwsDomainValidationForSelfLoop() {
            var id = UUID.randomUUID();

            assertThatThrownBy(() -> service.createRelation(id, id, RelationType.DEPENDS_ON))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("itself");
        }

        @Test
        void throwsNotFoundForMissingSource() {
            var sourceId = UUID.randomUUID();
            var targetId = UUID.randomUUID();
            when(requirementRepository.findById(sourceId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createRelation(sourceId, targetId, RelationType.DEPENDS_ON))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class GetRelations {

        @Test
        void returnsRelations() {
            var id = UUID.randomUUID();
            var req = makeRequirement("REQ-001");
            when(requirementRepository.findById(id)).thenReturn(Optional.of(req));
            when(relationRepository.findBySourceIdWithEntities(id)).thenReturn(new java.util.ArrayList<>());
            when(relationRepository.findByTargetIdWithEntities(id)).thenReturn(List.of());

            var result = service.getRelations(id);
            assertThat(result).isNotNull();
        }

        @Test
        void throwsNotFoundForMissingRequirement() {
            var id = UUID.randomUUID();
            when(requirementRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getRelations(id)).isInstanceOf(NotFoundException.class);
        }
    }

    private static void setRelationId(RequirementRelation rel, UUID id) {
        try {
            Field f = RequirementRelation.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(rel, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class BulkTransitionStatus {

        @Test
        void transitionsMultipleSuccessfully() {
            var id1 = UUID.randomUUID();
            var id2 = UUID.randomUUID();
            var req1 = makeRequirement("REQ-001");
            var req2 = makeRequirement("REQ-002");

            when(requirementRepository.findById(id1)).thenReturn(Optional.of(req1));
            when(requirementRepository.findById(id2)).thenReturn(Optional.of(req2));
            when(requirementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.bulkTransitionStatus(List.of(id1, id2), Status.ACTIVE);

            assertThat(result.succeeded()).hasSize(2);
            assertThat(result.failed()).isEmpty();
            assertThat(result.succeeded()).allMatch(r -> r.getStatus() == Status.ACTIVE);
        }

        @Test
        void collectsFailuresAndSuccesses() {
            var validId = UUID.randomUUID();
            var invalidId = UUID.randomUUID();
            var missingId = UUID.randomUUID();

            var validReq = makeRequirement("REQ-001");
            var invalidReq = makeRequirement("REQ-002");
            invalidReq.transitionStatus(Status.ACTIVE);
            invalidReq.transitionStatus(Status.ARCHIVED);

            when(requirementRepository.findById(validId)).thenReturn(Optional.of(validReq));
            when(requirementRepository.findById(invalidId)).thenReturn(Optional.of(invalidReq));
            when(requirementRepository.findById(missingId)).thenReturn(Optional.empty());
            when(requirementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.bulkTransitionStatus(List.of(validId, invalidId, missingId), Status.ACTIVE);

            assertThat(result.succeeded()).hasSize(1);
            assertThat(result.succeeded().get(0).getUid()).isEqualTo("REQ-001");
            assertThat(result.failed()).hasSize(2);
        }

        @Test
        void allFailReturnsEmptySucceeded() {
            var id1 = UUID.randomUUID();
            var id2 = UUID.randomUUID();

            when(requirementRepository.findById(id1)).thenReturn(Optional.empty());
            when(requirementRepository.findById(id2)).thenReturn(Optional.empty());

            var result = service.bulkTransitionStatus(List.of(id1, id2), Status.ACTIVE);

            assertThat(result.succeeded()).isEmpty();
            assertThat(result.failed()).hasSize(2);
        }
    }

    @Nested
    class Clone {

        @Test
        void clonesRequirementWithRelations() {
            var sourceId = UUID.randomUUID();
            var targetId = UUID.randomUUID();
            var source = makeRequirement("REQ-001");
            var target = makeRequirement("REQ-002");
            setId(source, sourceId);
            setId(target, targetId);
            source.setRationale("Important");
            source.setPriority(Priority.SHOULD);
            source.setRequirementType(RequirementType.CONSTRAINT);
            source.setWave(2);

            var outgoingRelation = new RequirementRelation(source, target, RelationType.DEPENDS_ON);

            when(requirementRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(requirementRepository.existsByUid("REQ-001-CLONE")).thenReturn(false);
            when(requirementRepository.save(any(Requirement.class))).thenAnswer(inv -> {
                var r = (Requirement) inv.getArgument(0);
                setId(r, UUID.randomUUID());
                return r;
            });
            when(relationRepository.findBySourceIdWithEntities(sourceId)).thenReturn(List.of(outgoingRelation));
            when(relationRepository.save(any(RequirementRelation.class))).thenAnswer(inv -> inv.getArgument(0));

            var cmd = new CloneRequirementCommand("REQ-001-CLONE", true);
            var result = service.clone(sourceId, cmd);

            assertThat(result.getUid()).isEqualTo("REQ-001-CLONE");
            assertThat(result.getTitle()).isEqualTo("Title for REQ-001");
            assertThat(result.getStatement()).isEqualTo("Statement for REQ-001");
            assertThat(result.getStatus()).isEqualTo(Status.DRAFT);
            assertThat(result.getRationale()).isEqualTo("Important");
            assertThat(result.getPriority()).isEqualTo(Priority.SHOULD);
            assertThat(result.getRequirementType()).isEqualTo(RequirementType.CONSTRAINT);
            assertThat(result.getWave()).isEqualTo(2);

            org.mockito.Mockito.verify(relationRepository).save(any(RequirementRelation.class));
        }

        @Test
        void clonesRequirementWithoutRelations() {
            var sourceId = UUID.randomUUID();
            var source = makeRequirement("REQ-001");
            setId(source, sourceId);
            source.setRationale("Important");
            source.setPriority(Priority.SHOULD);
            source.setRequirementType(RequirementType.CONSTRAINT);
            source.setWave(2);

            when(requirementRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(requirementRepository.existsByUid("REQ-001-CLONE")).thenReturn(false);
            when(requirementRepository.save(any(Requirement.class))).thenAnswer(inv -> {
                var r = (Requirement) inv.getArgument(0);
                setId(r, UUID.randomUUID());
                return r;
            });

            var cmd = new CloneRequirementCommand("REQ-001-CLONE", false);
            var result = service.clone(sourceId, cmd);

            assertThat(result.getUid()).isEqualTo("REQ-001-CLONE");
            assertThat(result.getTitle()).isEqualTo("Title for REQ-001");
            assertThat(result.getStatement()).isEqualTo("Statement for REQ-001");
            assertThat(result.getRationale()).isEqualTo("Important");
            assertThat(result.getPriority()).isEqualTo(Priority.SHOULD);
            assertThat(result.getRequirementType()).isEqualTo(RequirementType.CONSTRAINT);
            assertThat(result.getWave()).isEqualTo(2);

            org.mockito.Mockito.verify(relationRepository, org.mockito.Mockito.never())
                    .findBySourceIdWithEntities(any());
        }

        @Test
        void throwsConflictForDuplicateNewUid() {
            var sourceId = UUID.randomUUID();
            var source = makeRequirement("REQ-001");
            when(requirementRepository.findById(sourceId)).thenReturn(Optional.of(source));
            when(requirementRepository.existsByUid("REQ-EXISTING")).thenReturn(true);

            var cmd = new CloneRequirementCommand("REQ-EXISTING", false);

            assertThatThrownBy(() -> service.clone(sourceId, cmd)).isInstanceOf(ConflictException.class);
        }

        @Test
        void throwsNotFoundForMissingSource() {
            var sourceId = UUID.randomUUID();
            when(requirementRepository.findById(sourceId)).thenReturn(Optional.empty());

            var cmd = new CloneRequirementCommand("REQ-NEW", false);

            assertThatThrownBy(() -> service.clone(sourceId, cmd)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class ListRequirements {

        @SuppressWarnings("unchecked")
        @Test
        void returnsPageWithNullFilter() {
            var page = new PageImpl<>(List.of(makeRequirement("REQ-001")));
            when(requirementRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(page);

            Page<Requirement> result = service.list(Pageable.unpaged(), null);
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @SuppressWarnings("unchecked")
        @Test
        void returnsFilteredPage() {
            var page = new PageImpl<>(List.of(makeRequirement("REQ-001")));
            when(requirementRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(page);

            var filter = new RequirementFilter(Status.DRAFT, null, null, null, null);
            Page<Requirement> result = service.list(Pageable.unpaged(), filter);
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    class DeleteRelation {

        @Test
        void deletesRelationAsSource() {
            var reqId = UUID.randomUUID();
            var relationId = UUID.randomUUID();
            var source = makeRequirement("REQ-001");
            var target = makeRequirement("REQ-002");
            setId(source, reqId);
            setId(target, UUID.randomUUID());
            var relation = new RequirementRelation(source, target, RelationType.DEPENDS_ON);
            setRelationId(relation, relationId);

            when(relationRepository.findById(relationId)).thenReturn(Optional.of(relation));

            service.deleteRelation(reqId, relationId);
            org.mockito.Mockito.verify(relationRepository).delete(relation);
        }

        @Test
        void deletesRelationAsTarget() {
            var reqId = UUID.randomUUID();
            var relationId = UUID.randomUUID();
            var source = makeRequirement("REQ-001");
            var target = makeRequirement("REQ-002");
            setId(source, UUID.randomUUID());
            setId(target, reqId);
            var relation = new RequirementRelation(source, target, RelationType.DEPENDS_ON);
            setRelationId(relation, relationId);

            when(relationRepository.findById(relationId)).thenReturn(Optional.of(relation));

            service.deleteRelation(reqId, relationId);
            org.mockito.Mockito.verify(relationRepository).delete(relation);
        }

        @Test
        void throwsNotFoundForMissingRelation() {
            var reqId = UUID.randomUUID();
            var relationId = UUID.randomUUID();
            when(relationRepository.findById(relationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteRelation(reqId, relationId)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void throwsNotFoundWhenRelationDoesNotBelongToRequirement() {
            var reqId = UUID.randomUUID();
            var relationId = UUID.randomUUID();
            var source = makeRequirement("REQ-001");
            var target = makeRequirement("REQ-002");
            setId(source, UUID.randomUUID());
            setId(target, UUID.randomUUID());
            var relation = new RequirementRelation(source, target, RelationType.DEPENDS_ON);
            setRelationId(relation, relationId);

            when(relationRepository.findById(relationId)).thenReturn(Optional.of(relation));

            assertThatThrownBy(() -> service.deleteRelation(reqId, relationId)).isInstanceOf(NotFoundException.class);
        }
    }
}
