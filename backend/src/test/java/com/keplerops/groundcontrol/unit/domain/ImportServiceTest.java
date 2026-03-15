package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementImport;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementImportRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.requirements.service.CreateRequirementCommand;
import com.keplerops.groundcontrol.domain.requirements.service.CreateTraceabilityLinkCommand;
import com.keplerops.groundcontrol.domain.requirements.service.ImportService;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementService;
import com.keplerops.groundcontrol.domain.requirements.service.TraceabilityService;
import com.keplerops.groundcontrol.domain.requirements.service.UpdateRequirementCommand;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    @Mock
    private RequirementService requirementService;

    @Mock
    private TraceabilityService traceabilityService;

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private RequirementRelationRepository relationRepository;

    @Mock
    private TraceabilityLinkRepository traceabilityLinkRepository;

    @Mock
    private RequirementImportRepository importRepository;

    private ImportService service;

    @BeforeEach
    void setUp() {
        service = new ImportService(
                requirementService,
                traceabilityService,
                requirementRepository,
                relationRepository,
                traceabilityLinkRepository,
                importRepository);
    }

    private static Requirement makeRequirement(String uid, UUID id) {
        var req = new Requirement(uid, "Title for " + uid, "Statement for " + uid);
        setField(req, "id", id);
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

    private static String minimalSdoc(String uid) {
        return """
                [REQUIREMENT]
                UID: %s
                TITLE: Test
                STATEMENT: >>>
                Statement.
                <<<
                """
                .formatted(uid);
    }

    private static String sdocWithParent(String childUid, String parentUid) {
        return """
                [REQUIREMENT]
                UID: %s
                TITLE: Parent
                STATEMENT: >>>
                Statement.
                <<<

                [REQUIREMENT]
                UID: %s
                TITLE: Child
                STATEMENT: >>>
                Statement.
                <<<
                RELATIONS:
                - TYPE: Parent
                  VALUE: %s
                """
                .formatted(parentUid, childUid, parentUid);
    }

    private static String sdocWithIssueRef(String uid, int issueNum) {
        return """
                [REQUIREMENT]
                UID: %s
                TITLE: Test
                STATEMENT: >>>
                Statement.
                <<<
                COMMENT: GitHub issues: #%d
                """
                .formatted(uid, issueNum);
    }

    @Nested
    class UpsertRequirements {

        @Test
        void createsNewRequirements() {
            String sdoc = minimalSdoc("REQ-NEW");
            UUID newId = UUID.randomUUID();
            var created = makeRequirement("REQ-NEW", newId);

            when(requirementRepository.findByUid("REQ-NEW")).thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class))).thenReturn(created);
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            var result = service.importStrictdoc("test.sdoc", sdoc);

            assertThat(result.requirementsCreated()).isEqualTo(1);
            assertThat(result.requirementsUpdated()).isZero();
            verify(requirementService).create(any(CreateRequirementCommand.class));
        }

        @Test
        void updatesExistingRequirements() {
            String sdoc = minimalSdoc("REQ-EXISTING");
            UUID existingId = UUID.randomUUID();
            var existing = makeRequirement("REQ-EXISTING", existingId);

            when(requirementRepository.findByUid("REQ-EXISTING")).thenReturn(Optional.of(existing));
            when(requirementService.update(eq(existingId), any(UpdateRequirementCommand.class)))
                    .thenReturn(existing);
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            var result = service.importStrictdoc("test.sdoc", sdoc);

            assertThat(result.requirementsUpdated()).isEqualTo(1);
            assertThat(result.requirementsCreated()).isZero();
            verify(requirementService).update(eq(existingId), any(UpdateRequirementCommand.class));
        }
    }

    @Nested
    class CreateRelations {

        @Test
        void createsParentRelations() {
            String sdoc = sdocWithParent("REQ-CHILD", "REQ-PARENT");
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            var parent = makeRequirement("REQ-PARENT", parentId);
            var child = makeRequirement("REQ-CHILD", childId);

            when(requirementRepository.findByUid("REQ-PARENT")).thenReturn(Optional.empty());
            when(requirementRepository.findByUid("REQ-CHILD")).thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class)))
                    .thenReturn(parent)
                    .thenReturn(child);
            when(relationRepository.existsBySourceIdAndTargetIdAndRelationType(childId, parentId, RelationType.PARENT))
                    .thenReturn(false);
            when(requirementService.createRelation(childId, parentId, RelationType.PARENT))
                    .thenReturn(new RequirementRelation(child, parent, RelationType.PARENT));
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            var result = service.importStrictdoc("test.sdoc", sdoc);

            assertThat(result.relationsCreated()).isEqualTo(1);
            verify(requirementService).createRelation(childId, parentId, RelationType.PARENT);
        }

        @Test
        void skipsExistingRelations() {
            String sdoc = sdocWithParent("REQ-CHILD", "REQ-PARENT");
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            var parent = makeRequirement("REQ-PARENT", parentId);
            var child = makeRequirement("REQ-CHILD", childId);

            when(requirementRepository.findByUid("REQ-PARENT")).thenReturn(Optional.empty());
            when(requirementRepository.findByUid("REQ-CHILD")).thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class)))
                    .thenReturn(parent)
                    .thenReturn(child);
            when(relationRepository.existsBySourceIdAndTargetIdAndRelationType(childId, parentId, RelationType.PARENT))
                    .thenReturn(true);
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            var result = service.importStrictdoc("test.sdoc", sdoc);

            assertThat(result.relationsSkipped()).isEqualTo(1);
            assertThat(result.relationsCreated()).isZero();
            verify(requirementService, never()).createRelation(any(), any(), any());
        }

        @Test
        void lookupParentFromDb_whenNotInBatch() {
            // Import only the child; parent is not in the sdoc but exists in DB
            String sdoc =
                    """
                    [REQUIREMENT]
                    UID: REQ-CHILD
                    TITLE: Child
                    STATEMENT: >>>
                    Statement.
                    <<<
                    RELATIONS:
                    - TYPE: Parent
                      VALUE: REQ-PREEXISTING-PARENT
                    """;
            UUID childId = UUID.randomUUID();
            UUID parentId = UUID.randomUUID();
            var child = makeRequirement("REQ-CHILD", childId);
            var parentReq = makeRequirement("REQ-PREEXISTING-PARENT", parentId);

            when(requirementRepository.findByUid("REQ-CHILD")).thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class))).thenReturn(child);
            // Parent not in batch, so Phase 2 looks it up in the DB
            when(requirementRepository.findByUid("REQ-PREEXISTING-PARENT")).thenReturn(Optional.of(parentReq));
            when(relationRepository.existsBySourceIdAndTargetIdAndRelationType(childId, parentId, RelationType.PARENT))
                    .thenReturn(false);
            when(requirementService.createRelation(childId, parentId, RelationType.PARENT))
                    .thenReturn(new RequirementRelation(child, parentReq, RelationType.PARENT));
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            var result = service.importStrictdoc("test.sdoc", sdoc);

            assertThat(result.relationsCreated()).isEqualTo(1);
            verify(requirementService).createRelation(childId, parentId, RelationType.PARENT);
        }

        @Test
        void parentNotFoundAnywhere_collectsError() {
            // Import only the child; parent is not in the sdoc AND not in DB
            String sdoc =
                    """
                    [REQUIREMENT]
                    UID: REQ-CHILD
                    TITLE: Child
                    STATEMENT: >>>
                    Statement.
                    <<<
                    RELATIONS:
                    - TYPE: Parent
                      VALUE: REQ-MISSING-PARENT
                    """;
            UUID childId = UUID.randomUUID();
            var child = makeRequirement("REQ-CHILD", childId);

            when(requirementRepository.findByUid("REQ-CHILD")).thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class))).thenReturn(child);
            when(requirementRepository.findByUid("REQ-MISSING-PARENT")).thenReturn(Optional.empty());
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            var result = service.importStrictdoc("test.sdoc", sdoc);

            assertThat(result.relationsCreated()).isZero();
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0).get("error").toString()).contains("Parent not found");
        }

        @Test
        void relationCreationError_collectsError() {
            String sdoc = sdocWithParent("REQ-CHILD", "REQ-PARENT");
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            var parent = makeRequirement("REQ-PARENT", parentId);
            var child = makeRequirement("REQ-CHILD", childId);

            when(requirementRepository.findByUid("REQ-PARENT")).thenReturn(Optional.empty());
            when(requirementRepository.findByUid("REQ-CHILD")).thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class)))
                    .thenReturn(parent)
                    .thenReturn(child);
            when(relationRepository.existsBySourceIdAndTargetIdAndRelationType(childId, parentId, RelationType.PARENT))
                    .thenReturn(false);
            when(requirementService.createRelation(childId, parentId, RelationType.PARENT))
                    .thenThrow(new DomainValidationException("Simulated relation failure"));
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            var result = service.importStrictdoc("test.sdoc", sdoc);

            assertThat(result.relationsCreated()).isZero();
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0).get("phase")).isEqualTo("relations");
            assertThat(result.errors().get(0).get("error").toString()).contains("Simulated relation failure");
        }
    }

    @Nested
    class CreateTraceabilityLinks {

        @Test
        void createsTraceabilityLinks() {
            String sdoc = sdocWithIssueRef("REQ-TRACE", 42);
            UUID reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-TRACE", reqId);

            when(requirementRepository.findByUid("REQ-TRACE")).thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class))).thenReturn(req);
            when(traceabilityLinkRepository.existsByRequirementIdAndArtifactTypeAndArtifactIdentifierAndLinkType(
                            reqId, ArtifactType.GITHUB_ISSUE, "42", LinkType.IMPLEMENTS))
                    .thenReturn(false);
            when(traceabilityService.createLink(eq(reqId), any(CreateTraceabilityLinkCommand.class)))
                    .thenReturn(new TraceabilityLink(req, ArtifactType.GITHUB_ISSUE, "42", LinkType.IMPLEMENTS));
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            var result = service.importStrictdoc("test.sdoc", sdoc);

            assertThat(result.traceabilityLinksCreated()).isEqualTo(1);
            verify(traceabilityService).createLink(eq(reqId), any(CreateTraceabilityLinkCommand.class));
        }

        @Test
        void skipsExistingTraceabilityLinks() {
            String sdoc = sdocWithIssueRef("REQ-TRACE", 42);
            UUID reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-TRACE", reqId);

            when(requirementRepository.findByUid("REQ-TRACE")).thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class))).thenReturn(req);
            when(traceabilityLinkRepository.existsByRequirementIdAndArtifactTypeAndArtifactIdentifierAndLinkType(
                            reqId, ArtifactType.GITHUB_ISSUE, "42", LinkType.IMPLEMENTS))
                    .thenReturn(true);
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            var result = service.importStrictdoc("test.sdoc", sdoc);

            assertThat(result.traceabilityLinksSkipped()).isEqualTo(1);
            assertThat(result.traceabilityLinksCreated()).isZero();
            verify(traceabilityService, never()).createLink(any(), any());
        }

        @Test
        void traceabilityLinkCreationError_collectsError() {
            String sdoc = sdocWithIssueRef("REQ-TRACE", 42);
            UUID reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-TRACE", reqId);

            when(requirementRepository.findByUid("REQ-TRACE")).thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class))).thenReturn(req);
            when(traceabilityLinkRepository.existsByRequirementIdAndArtifactTypeAndArtifactIdentifierAndLinkType(
                            reqId, ArtifactType.GITHUB_ISSUE, "42", LinkType.IMPLEMENTS))
                    .thenReturn(false);
            when(traceabilityService.createLink(eq(reqId), any(CreateTraceabilityLinkCommand.class)))
                    .thenThrow(new DomainValidationException("Simulated traceability failure"));
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            var result = service.importStrictdoc("test.sdoc", sdoc);

            assertThat(result.traceabilityLinksCreated()).isZero();
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0).get("phase")).isEqualTo("traceability");
            assertThat(result.errors().get(0).get("error").toString()).contains("Simulated traceability failure");
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void collectsErrorsAndContinues() {
            // Two requirements: first one throws, second succeeds
            String sdoc =
                    """
                    [REQUIREMENT]
                    UID: REQ-FAIL
                    TITLE: Fail
                    STATEMENT: >>>
                    Fails.
                    <<<

                    [REQUIREMENT]
                    UID: REQ-OK
                    TITLE: OK
                    STATEMENT: >>>
                    Succeeds.
                    <<<
                    """;
            UUID okId = UUID.randomUUID();
            var okReq = makeRequirement("REQ-OK", okId);

            when(requirementRepository.findByUid("REQ-FAIL")).thenReturn(Optional.empty());
            when(requirementRepository.findByUid("REQ-OK")).thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class)))
                    .thenThrow(new DomainValidationException("Simulated failure"))
                    .thenReturn(okReq);
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            var result = service.importStrictdoc("test.sdoc", sdoc);

            assertThat(result.requirementsCreated()).isEqualTo(1);
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0).get("uid")).isEqualTo("REQ-FAIL");
        }
    }

    @Nested
    class AuditRecord {

        @Test
        void savesImportAuditRecord() {
            String sdoc = minimalSdoc("REQ-AUDIT");
            UUID reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-AUDIT", reqId);

            when(requirementRepository.findByUid("REQ-AUDIT")).thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class))).thenReturn(req);
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            service.importStrictdoc("test.sdoc", sdoc);

            verify(importRepository).save(any(RequirementImport.class));
        }
    }
}
