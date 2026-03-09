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
import com.keplerops.groundcontrol.domain.requirements.service.CreateRequirementCommand;
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
            when(relationRepository.findBySourceId(id)).thenReturn(new java.util.ArrayList<>());
            when(relationRepository.findByTargetId(id)).thenReturn(List.of());

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

    @Nested
    class ListRequirements {

        @Test
        void returnsPage() {
            var page = new PageImpl<>(List.of(makeRequirement("REQ-001")));
            when(requirementRepository.findAll(any(Pageable.class))).thenReturn(page);

            Page<Requirement> result = service.list(Pageable.unpaged());
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }
}
