package com.keplerops.groundcontrol.domain.requirements.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditServiceDiffTest {

    private static final UUID REQ_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private RequirementRelationRepository relationRepository;

    @Mock
    private TraceabilityLinkRepository traceabilityLinkRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private AuditReader auditReader;

    private AuditService service;

    private static void setField(Object obj, String fieldName, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Requirement makeRequirement(String uid, String title) {
        var project = new Project("test", "Test");
        setField(project, "id", UUID.randomUUID());
        var req = new Requirement(project, uid, title, "Statement");
        setField(req, "id", REQ_ID);
        return req;
    }

    @BeforeEach
    void setUp() {
        service =
                new AuditService(requirementRepository, relationRepository, traceabilityLinkRepository, entityManager);
    }

    @Nested
    class GetRequirementDiff {

        @Test
        void throwsNotFoundWhenRequirementDoesNotExist() {
            when(requirementRepository.existsById(REQ_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.getRequirementDiff(REQ_ID, 1, 5)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void throwsValidationErrorWhenFromRevisionNotLessThanToRevision() {
            when(requirementRepository.existsById(REQ_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.getRequirementDiff(REQ_ID, 5, 5))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("fromRevision must be less than toRevision");
        }

        @Test
        void throwsValidationErrorWhenRequirementNotAtFromRevision() {
            when(requirementRepository.existsById(REQ_ID)).thenReturn(true);

            try (MockedStatic<AuditReaderFactory> factory = Mockito.mockStatic(AuditReaderFactory.class)) {
                factory.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(auditReader);
                when(auditReader.find(Requirement.class, REQ_ID, 1)).thenReturn(null);

                assertThatThrownBy(() -> service.getRequirementDiff(REQ_ID, 1, 5))
                        .isInstanceOf(DomainValidationException.class)
                        .hasMessageContaining("does not exist at revision 1");
            }
        }

        @Test
        void throwsValidationErrorWhenRequirementNotAtToRevision() {
            when(requirementRepository.existsById(REQ_ID)).thenReturn(true);
            var fromReq = makeRequirement("REQ-001", "Title");

            try (MockedStatic<AuditReaderFactory> factory = Mockito.mockStatic(AuditReaderFactory.class)) {
                factory.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(auditReader);
                when(auditReader.find(Requirement.class, REQ_ID, 1)).thenReturn(fromReq);
                when(auditReader.find(Requirement.class, REQ_ID, 5)).thenReturn(null);

                assertThatThrownBy(() -> service.getRequirementDiff(REQ_ID, 1, 5))
                        .isInstanceOf(DomainValidationException.class)
                        .hasMessageContaining("does not exist at revision 5");
            }
        }

        @Test
        void returnsStructuredDiffBetweenTwoRevisions() {
            when(requirementRepository.existsById(REQ_ID)).thenReturn(true);
            var fromReq = makeRequirement("REQ-001", "Old Title");
            var toReq = makeRequirement("REQ-001", "New Title");

            var spyService = spy(service);

            try (MockedStatic<AuditReaderFactory> factory = Mockito.mockStatic(AuditReaderFactory.class)) {
                factory.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(auditReader);
                when(auditReader.find(Requirement.class, REQ_ID, 1)).thenReturn(fromReq);
                when(auditReader.find(Requirement.class, REQ_ID, 5)).thenReturn(toReq);

                doReturn(Map.<UUID, Map<String, Object>>of())
                        .when(spyService)
                        .getRelationSnapshotsAtRevision(REQ_ID, 1);
                doReturn(Map.<UUID, Map<String, Object>>of())
                        .when(spyService)
                        .getRelationSnapshotsAtRevision(REQ_ID, 5);
                doReturn(Map.<UUID, Map<String, Object>>of())
                        .when(spyService)
                        .getTraceabilityLinkSnapshotsAtRevision(REQ_ID, 1);
                doReturn(Map.<UUID, Map<String, Object>>of())
                        .when(spyService)
                        .getTraceabilityLinkSnapshotsAtRevision(REQ_ID, 5);

                var diff = spyService.getRequirementDiff(REQ_ID, 1, 5);

                assertThat(diff.requirementId()).isEqualTo(REQ_ID);
                assertThat(diff.fromRevision()).isEqualTo(1);
                assertThat(diff.toRevision()).isEqualTo(5);
                assertThat(diff.fieldChanges()).containsKey("title");
                assertThat(diff.fieldChanges().get("title").oldValue()).isEqualTo("Old Title");
                assertThat(diff.fieldChanges().get("title").newValue()).isEqualTo("New Title");
                assertThat(diff.relationChanges()).isEmpty();
                assertThat(diff.traceabilityLinkChanges()).isEmpty();
            }
        }

        @Test
        void includesRelationAndLinkChangesInDiff() {
            when(requirementRepository.existsById(REQ_ID)).thenReturn(true);
            var fromReq = makeRequirement("REQ-001", "Title");
            var toReq = makeRequirement("REQ-001", "Title");

            var relId = UUID.randomUUID();
            var linkId = UUID.randomUUID();
            var spyService = spy(service);

            try (MockedStatic<AuditReaderFactory> factory = Mockito.mockStatic(AuditReaderFactory.class)) {
                factory.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(auditReader);
                when(auditReader.find(Requirement.class, REQ_ID, 1)).thenReturn(fromReq);
                when(auditReader.find(Requirement.class, REQ_ID, 5)).thenReturn(toReq);

                doReturn(Map.<UUID, Map<String, Object>>of())
                        .when(spyService)
                        .getRelationSnapshotsAtRevision(REQ_ID, 1);
                doReturn(Map.of(relId, Map.<String, Object>of("relationType", "DEPENDS_ON")))
                        .when(spyService)
                        .getRelationSnapshotsAtRevision(REQ_ID, 5);

                doReturn(Map.of(linkId, Map.<String, Object>of("artifactType", "CODE_FILE")))
                        .when(spyService)
                        .getTraceabilityLinkSnapshotsAtRevision(REQ_ID, 1);
                doReturn(Map.<UUID, Map<String, Object>>of())
                        .when(spyService)
                        .getTraceabilityLinkSnapshotsAtRevision(REQ_ID, 5);

                var diff = spyService.getRequirementDiff(REQ_ID, 1, 5);

                assertThat(diff.fieldChanges()).isEmpty();
                assertThat(diff.relationChanges()).hasSize(1);
                assertThat(diff.relationChanges().get(0).changeType()).isEqualTo("ADDED");
                assertThat(diff.relationChanges().get(0).relationId()).isEqualTo(relId);
                assertThat(diff.traceabilityLinkChanges()).hasSize(1);
                assertThat(diff.traceabilityLinkChanges().get(0).changeType()).isEqualTo("REMOVED");
                assertThat(diff.traceabilityLinkChanges().get(0).linkId()).isEqualTo(linkId);
            }
        }
    }

    @Nested
    class ReplayToAliveSnapshots {

        @Test
        void addsEntitiesAndRemovesDeleted() {
            var id1 = UUID.randomUUID();
            var id2 = UUID.randomUUID();
            var snap1 = Map.<String, Object>of("field", "value1");
            var snap2 = Map.<String, Object>of("field", "value2");

            List<Object[]> results = List.of(
                    new Object[] {null, null, RevisionType.ADD, id1, snap1},
                    new Object[] {null, null, RevisionType.ADD, id2, snap2},
                    new Object[] {null, null, RevisionType.DEL, id1, snap1});

            var alive = service.replayToAliveSnapshots(
                    results, row -> Map.entry((UUID) row[3], (Map<String, Object>) row[4]), row ->
                            (RevisionType) row[2]);

            assertThat(alive).hasSize(1);
            assertThat(alive).containsKey(id2);
            assertThat(alive).doesNotContainKey(id1);
        }

        @Test
        void emptyResultsProduceEmptyMap() {
            var alive = service.replayToAliveSnapshots(
                    List.of(), row -> Map.entry(UUID.randomUUID(), Map.of()), row -> RevisionType.ADD);

            assertThat(alive).isEmpty();
        }

        @Test
        void modUpdatesSnapshot() {
            var id = UUID.randomUUID();
            var snap1 = Map.<String, Object>of("field", "v1");
            var snap2 = Map.<String, Object>of("field", "v2");

            List<Object[]> results = List.of(
                    new Object[] {null, null, RevisionType.ADD, id, snap1},
                    new Object[] {null, null, RevisionType.MOD, id, snap2});

            var alive = service.replayToAliveSnapshots(
                    results, row -> Map.entry((UUID) row[3], (Map<String, Object>) row[4]), row ->
                            (RevisionType) row[2]);

            assertThat(alive).hasSize(1);
            assertThat(alive.get(id)).containsEntry("field", "v2");
        }
    }
}
