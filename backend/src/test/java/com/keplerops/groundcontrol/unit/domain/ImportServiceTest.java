package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
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
import com.keplerops.groundcontrol.domain.requirements.service.ImportResult;
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
        service = new ImportService(
                requirementService,
                traceabilityService,
                requirementRepository,
                relationRepository,
                traceabilityLinkRepository,
                importRepository);
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

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "REQ-NEW"))
                    .thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class))).thenReturn(created);
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            var result = service.importStrictdoc(PROJECT_ID, "test.sdoc", sdoc);

            assertThat(result.requirementsCreated()).isEqualTo(1);
            assertThat(result.requirementsUpdated()).isZero();
            verify(requirementService).create(any(CreateRequirementCommand.class));
        }

        @Test
        void updatesExistingRequirements() {
            String sdoc = minimalSdoc("REQ-EXISTING");
            UUID existingId = UUID.randomUUID();
            var existing = makeRequirement("REQ-EXISTING", existingId);

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "REQ-EXISTING"))
                    .thenReturn(Optional.of(existing));
            when(requirementService.update(eq(existingId), any(UpdateRequirementCommand.class)))
                    .thenReturn(existing);
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            var result = service.importStrictdoc(PROJECT_ID, "test.sdoc", sdoc);

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

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "REQ-PARENT"))
                    .thenReturn(Optional.empty());
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "REQ-CHILD"))
                    .thenReturn(Optional.empty());
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

            var result = service.importStrictdoc(PROJECT_ID, "test.sdoc", sdoc);

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

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "REQ-PARENT"))
                    .thenReturn(Optional.empty());
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "REQ-CHILD"))
                    .thenReturn(Optional.empty());
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

            var result = service.importStrictdoc(PROJECT_ID, "test.sdoc", sdoc);

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

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "REQ-CHILD"))
                    .thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class))).thenReturn(child);
            // Parent not in batch, so Phase 2 looks it up in the DB
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "REQ-PREEXISTING-PARENT"))
                    .thenReturn(Optional.of(parentReq));
            when(relationRepository.existsBySourceIdAndTargetIdAndRelationType(childId, parentId, RelationType.PARENT))
                    .thenReturn(false);
            when(requirementService.createRelation(childId, parentId, RelationType.PARENT))
                    .thenReturn(new RequirementRelation(child, parentReq, RelationType.PARENT));
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            var result = service.importStrictdoc(PROJECT_ID, "test.sdoc", sdoc);

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

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "REQ-CHILD"))
                    .thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class))).thenReturn(child);
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "REQ-MISSING-PARENT"))
                    .thenReturn(Optional.empty());
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            var result = service.importStrictdoc(PROJECT_ID, "test.sdoc", sdoc);

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

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "REQ-PARENT"))
                    .thenReturn(Optional.empty());
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "REQ-CHILD"))
                    .thenReturn(Optional.empty());
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

            var result = service.importStrictdoc(PROJECT_ID, "test.sdoc", sdoc);

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

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "REQ-TRACE"))
                    .thenReturn(Optional.empty());
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

            var result = service.importStrictdoc(PROJECT_ID, "test.sdoc", sdoc);

            assertThat(result.traceabilityLinksCreated()).isEqualTo(1);
            verify(traceabilityService).createLink(eq(reqId), any(CreateTraceabilityLinkCommand.class));
        }

        @Test
        void skipsExistingTraceabilityLinks() {
            String sdoc = sdocWithIssueRef("REQ-TRACE", 42);
            UUID reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-TRACE", reqId);

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "REQ-TRACE"))
                    .thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class))).thenReturn(req);
            when(traceabilityLinkRepository.existsByRequirementIdAndArtifactTypeAndArtifactIdentifierAndLinkType(
                            reqId, ArtifactType.GITHUB_ISSUE, "42", LinkType.IMPLEMENTS))
                    .thenReturn(true);
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            var result = service.importStrictdoc(PROJECT_ID, "test.sdoc", sdoc);

            assertThat(result.traceabilityLinksSkipped()).isEqualTo(1);
            assertThat(result.traceabilityLinksCreated()).isZero();
            verify(traceabilityService, never()).createLink(any(), any());
        }

        @Test
        void traceabilityLinkCreationError_collectsError() {
            String sdoc = sdocWithIssueRef("REQ-TRACE", 42);
            UUID reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-TRACE", reqId);

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "REQ-TRACE"))
                    .thenReturn(Optional.empty());
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

            var result = service.importStrictdoc(PROJECT_ID, "test.sdoc", sdoc);

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

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "REQ-FAIL"))
                    .thenReturn(Optional.empty());
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "REQ-OK"))
                    .thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class)))
                    .thenThrow(new DomainValidationException("Simulated failure"))
                    .thenReturn(okReq);
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            var result = service.importStrictdoc(PROJECT_ID, "test.sdoc", sdoc);

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

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "REQ-AUDIT"))
                    .thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class))).thenReturn(req);
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            service.importStrictdoc(PROJECT_ID, "test.sdoc", sdoc);

            verify(importRepository).save(any(RequirementImport.class));
        }
    }

    // =====================================================================
    // ReqIF import tests
    // =====================================================================

    private static String minimalReqif(String identifier, String title) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <REQ-IF xmlns="http://www.omg.org/spec/ReqIF/20110401/reqif.xsd">
                  <THE-HEADER>
                    <REQ-IF-HEADER IDENTIFIER="h1"><TITLE>Test</TITLE></REQ-IF-HEADER>
                  </THE-HEADER>
                  <CORE-CONTENT>
                    <REQ-IF-CONTENT>
                      <DATATYPES/>
                      <SPEC-TYPES/>
                      <SPEC-OBJECTS>
                        <SPEC-OBJECT IDENTIFIER="%s" LONG-NAME="%s"/>
                      </SPEC-OBJECTS>
                      <SPEC-RELATIONS/>
                      <SPECIFICATIONS/>
                    </REQ-IF-CONTENT>
                  </CORE-CONTENT>
                </REQ-IF>
                """
                .formatted(identifier, title);
    }

    private static String reqifWithHierarchy(String parentId, String parentTitle, String childId, String childTitle) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <REQ-IF xmlns="http://www.omg.org/spec/ReqIF/20110401/reqif.xsd">
                  <THE-HEADER>
                    <REQ-IF-HEADER IDENTIFIER="h1"><TITLE>Test</TITLE></REQ-IF-HEADER>
                  </THE-HEADER>
                  <CORE-CONTENT>
                    <REQ-IF-CONTENT>
                      <DATATYPES/>
                      <SPEC-TYPES/>
                      <SPEC-OBJECTS>
                        <SPEC-OBJECT IDENTIFIER="%s" LONG-NAME="%s"/>
                        <SPEC-OBJECT IDENTIFIER="%s" LONG-NAME="%s"/>
                      </SPEC-OBJECTS>
                      <SPEC-RELATIONS/>
                      <SPECIFICATIONS>
                        <SPECIFICATION IDENTIFIER="spec-1" LONG-NAME="Test Spec">
                          <CHILDREN>
                            <SPEC-HIERARCHY IDENTIFIER="sh-1">
                              <OBJECT><OBJECT-REF>%s</OBJECT-REF></OBJECT>
                              <CHILDREN>
                                <SPEC-HIERARCHY IDENTIFIER="sh-2">
                                  <OBJECT><OBJECT-REF>%s</OBJECT-REF></OBJECT>
                                </SPEC-HIERARCHY>
                              </CHILDREN>
                            </SPEC-HIERARCHY>
                          </CHILDREN>
                        </SPECIFICATION>
                      </SPECIFICATIONS>
                    </REQ-IF-CONTENT>
                  </CORE-CONTENT>
                </REQ-IF>
                """
                .formatted(parentId, parentTitle, childId, childTitle, parentId, childId);
    }

    private static String reqifWithExplicitRelation(String sourceId, String targetId, String relTypeName) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <REQ-IF xmlns="http://www.omg.org/spec/ReqIF/20110401/reqif.xsd">
                  <THE-HEADER>
                    <REQ-IF-HEADER IDENTIFIER="h1"><TITLE>Test</TITLE></REQ-IF-HEADER>
                  </THE-HEADER>
                  <CORE-CONTENT>
                    <REQ-IF-CONTENT>
                      <DATATYPES/>
                      <SPEC-TYPES>
                        <SPEC-RELATION-TYPE IDENTIFIER="srt-1" LONG-NAME="%s"/>
                      </SPEC-TYPES>
                      <SPEC-OBJECTS>
                        <SPEC-OBJECT IDENTIFIER="%s" LONG-NAME="Source"/>
                        <SPEC-OBJECT IDENTIFIER="%s" LONG-NAME="Target"/>
                      </SPEC-OBJECTS>
                      <SPEC-RELATIONS>
                        <SPEC-RELATION IDENTIFIER="rel-1">
                          <TYPE><SPEC-RELATION-TYPE-REF>srt-1</SPEC-RELATION-TYPE-REF></TYPE>
                          <SOURCE><SOURCE-REF>%s</SOURCE-REF></SOURCE>
                          <TARGET><TARGET-REF>%s</TARGET-REF></TARGET>
                        </SPEC-RELATION>
                      </SPEC-RELATIONS>
                      <SPECIFICATIONS/>
                    </REQ-IF-CONTENT>
                  </CORE-CONTENT>
                </REQ-IF>
                """
                .formatted(relTypeName, sourceId, targetId, sourceId, targetId);
    }

    @Nested
    class ReqifUpsertRequirements {

        @Test
        void createsNewRequirementsFromReqif() {
            String reqif = minimalReqif("RIF-NEW", "New Requirement");
            UUID newId = UUID.randomUUID();
            var created = makeRequirement("RIF-NEW", newId);

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-NEW"))
                    .thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class))).thenReturn(created);
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            ImportResult result = service.importReqif(PROJECT_ID, "test.reqif", reqif);

            assertThat(result.requirementsCreated()).isEqualTo(1);
            assertThat(result.requirementsUpdated()).isZero();
            verify(requirementService).create(any(CreateRequirementCommand.class));
        }

        @Test
        void updatesExistingRequirementsFromReqif() {
            String reqif = minimalReqif("RIF-EXISTING", "Updated");
            UUID existingId = UUID.randomUUID();
            var existing = makeRequirement("RIF-EXISTING", existingId);

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-EXISTING"))
                    .thenReturn(Optional.of(existing));
            when(requirementService.update(eq(existingId), any(UpdateRequirementCommand.class)))
                    .thenReturn(existing);
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            ImportResult result = service.importReqif(PROJECT_ID, "test.reqif", reqif);

            assertThat(result.requirementsUpdated()).isEqualTo(1);
            assertThat(result.requirementsCreated()).isZero();
        }
    }

    @Nested
    class ReqifCreateRelations {

        @Test
        void createsHierarchyRelations() {
            String reqif = reqifWithHierarchy("RIF-PARENT", "Parent", "RIF-CHILD", "Child");
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            var parent = makeRequirement("RIF-PARENT", parentId);
            var child = makeRequirement("RIF-CHILD", childId);

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-PARENT"))
                    .thenReturn(Optional.empty());
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-CHILD"))
                    .thenReturn(Optional.empty());
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

            ImportResult result = service.importReqif(PROJECT_ID, "test.reqif", reqif);

            assertThat(result.relationsCreated()).isEqualTo(1);
            verify(requirementService).createRelation(childId, parentId, RelationType.PARENT);
        }

        @Test
        void createsExplicitSpecRelations() {
            String reqif = reqifWithExplicitRelation("RIF-SRC", "RIF-TGT", "depends on");
            UUID srcId = UUID.randomUUID();
            UUID tgtId = UUID.randomUUID();
            var src = makeRequirement("RIF-SRC", srcId);
            var tgt = makeRequirement("RIF-TGT", tgtId);

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-SRC"))
                    .thenReturn(Optional.empty());
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-TGT"))
                    .thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class)))
                    .thenReturn(src)
                    .thenReturn(tgt);
            when(relationRepository.existsBySourceIdAndTargetIdAndRelationType(srcId, tgtId, RelationType.DEPENDS_ON))
                    .thenReturn(false);
            when(requirementService.createRelation(srcId, tgtId, RelationType.DEPENDS_ON))
                    .thenReturn(new RequirementRelation(src, tgt, RelationType.DEPENDS_ON));
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            ImportResult result = service.importReqif(PROJECT_ID, "test.reqif", reqif);

            assertThat(result.relationsCreated()).isEqualTo(1);
            verify(requirementService).createRelation(srcId, tgtId, RelationType.DEPENDS_ON);
        }

        @Test
        void skipsExplicitRelationWhenHierarchyAlreadyCreatedIt() {
            // ReqIF with both hierarchy parent AND an explicit SpecRelation expressing the same
            // PARENT relationship — Phase 2 creates it from hierarchy, Phase 2b skips the duplicate.
            String reqif =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <REQ-IF xmlns="http://www.omg.org/spec/ReqIF/20110401/reqif.xsd">
                      <THE-HEADER>
                        <REQ-IF-HEADER IDENTIFIER="h1"><TITLE>Test</TITLE></REQ-IF-HEADER>
                      </THE-HEADER>
                      <CORE-CONTENT>
                        <REQ-IF-CONTENT>
                          <DATATYPES/>
                          <SPEC-TYPES>
                            <SPEC-RELATION-TYPE IDENTIFIER="srt-1" LONG-NAME="Parent Relationship"/>
                          </SPEC-TYPES>
                          <SPEC-OBJECTS>
                            <SPEC-OBJECT IDENTIFIER="RIF-PARENT" LONG-NAME="Parent"/>
                            <SPEC-OBJECT IDENTIFIER="RIF-CHILD" LONG-NAME="Child"/>
                          </SPEC-OBJECTS>
                          <SPEC-RELATIONS>
                            <SPEC-RELATION IDENTIFIER="rel-dup">
                              <TYPE><SPEC-RELATION-TYPE-REF>srt-1</SPEC-RELATION-TYPE-REF></TYPE>
                              <SOURCE><SOURCE-REF>RIF-CHILD</SOURCE-REF></SOURCE>
                              <TARGET><TARGET-REF>RIF-PARENT</TARGET-REF></TARGET>
                            </SPEC-RELATION>
                          </SPEC-RELATIONS>
                          <SPECIFICATIONS>
                            <SPECIFICATION IDENTIFIER="spec-1" LONG-NAME="Spec">
                              <CHILDREN>
                                <SPEC-HIERARCHY IDENTIFIER="sh-1">
                                  <OBJECT><OBJECT-REF>RIF-PARENT</OBJECT-REF></OBJECT>
                                  <CHILDREN>
                                    <SPEC-HIERARCHY IDENTIFIER="sh-2">
                                      <OBJECT><OBJECT-REF>RIF-CHILD</OBJECT-REF></OBJECT>
                                    </SPEC-HIERARCHY>
                                  </CHILDREN>
                                </SPEC-HIERARCHY>
                              </CHILDREN>
                            </SPECIFICATION>
                          </SPECIFICATIONS>
                        </REQ-IF-CONTENT>
                      </CORE-CONTENT>
                    </REQ-IF>
                    """;
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            var parent = makeRequirement("RIF-PARENT", parentId);
            var child = makeRequirement("RIF-CHILD", childId);

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-PARENT"))
                    .thenReturn(Optional.empty());
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-CHILD"))
                    .thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class)))
                    .thenReturn(parent)
                    .thenReturn(child);
            // First call (Phase 2 hierarchy): relation does not exist yet → create
            // Second call (Phase 2b explicit): relation already exists → skip
            when(relationRepository.existsBySourceIdAndTargetIdAndRelationType(childId, parentId, RelationType.PARENT))
                    .thenReturn(false)
                    .thenReturn(true);
            when(requirementService.createRelation(childId, parentId, RelationType.PARENT))
                    .thenReturn(new RequirementRelation(child, parent, RelationType.PARENT));
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            ImportResult result = service.importReqif(PROJECT_ID, "test.reqif", reqif);

            assertThat(result.relationsCreated()).isEqualTo(1);
            assertThat(result.relationsSkipped()).isEqualTo(1);
        }

        @Test
        void lookupParentFromDb_whenNotInBatch_reqif() {
            // Parent creation fails in Phase 1 but exists in DB for Phase 2 fallback
            String reqifWithMissingParent =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <REQ-IF xmlns="http://www.omg.org/spec/ReqIF/20110401/reqif.xsd">
                      <THE-HEADER>
                        <REQ-IF-HEADER IDENTIFIER="h1"><TITLE>Test</TITLE></REQ-IF-HEADER>
                      </THE-HEADER>
                      <CORE-CONTENT>
                        <REQ-IF-CONTENT>
                          <DATATYPES/>
                          <SPEC-TYPES/>
                          <SPEC-OBJECTS>
                            <SPEC-OBJECT IDENTIFIER="RIF-PARENT-DB" LONG-NAME="Parent"/>
                            <SPEC-OBJECT IDENTIFIER="RIF-CHILD-DB" LONG-NAME="Child"/>
                          </SPEC-OBJECTS>
                          <SPEC-RELATIONS/>
                          <SPECIFICATIONS>
                            <SPECIFICATION IDENTIFIER="spec-1" LONG-NAME="Spec">
                              <CHILDREN>
                                <SPEC-HIERARCHY IDENTIFIER="sh-1">
                                  <OBJECT><OBJECT-REF>RIF-PARENT-DB</OBJECT-REF></OBJECT>
                                  <CHILDREN>
                                    <SPEC-HIERARCHY IDENTIFIER="sh-2">
                                      <OBJECT><OBJECT-REF>RIF-CHILD-DB</OBJECT-REF></OBJECT>
                                    </SPEC-HIERARCHY>
                                  </CHILDREN>
                                </SPEC-HIERARCHY>
                              </CHILDREN>
                            </SPECIFICATION>
                          </SPECIFICATIONS>
                        </REQ-IF-CONTENT>
                      </CORE-CONTENT>
                    </REQ-IF>
                    """;
            UUID childId = UUID.randomUUID();
            UUID parentId = UUID.randomUUID();
            var child = makeRequirement("RIF-CHILD-DB", childId);
            var parentReq = makeRequirement("RIF-PARENT-DB", parentId);

            // Phase 1: child is new, parent creation fails (simulating parent only in DB)
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-PARENT-DB"))
                    .thenReturn(Optional.empty());
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-CHILD-DB"))
                    .thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class)))
                    .thenThrow(new DomainValidationException("Simulated failure"))
                    .thenReturn(child);
            // Phase 2: parent not in batch, so lookup from DB
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-PARENT-DB"))
                    .thenReturn(Optional.empty()) // Phase 1 call
                    .thenReturn(Optional.of(parentReq)); // Phase 2 DB fallback
            when(relationRepository.existsBySourceIdAndTargetIdAndRelationType(childId, parentId, RelationType.PARENT))
                    .thenReturn(false);
            when(requirementService.createRelation(childId, parentId, RelationType.PARENT))
                    .thenReturn(new RequirementRelation(child, parentReq, RelationType.PARENT));
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            ImportResult result = service.importReqif(PROJECT_ID, "test.reqif", reqifWithMissingParent);

            assertThat(result.relationsCreated()).isEqualTo(1);
            verify(requirementService).createRelation(childId, parentId, RelationType.PARENT);
        }

        @Test
        void parentNotFoundAnywhere_collectsError_reqif() {
            String reqif = reqifWithHierarchy("RIF-PARENT-MISS", "Parent", "RIF-CHILD-MISS", "Child");
            UUID childId = UUID.randomUUID();
            var child = makeRequirement("RIF-CHILD-MISS", childId);

            // Only child gets created; parent creation fails
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-PARENT-MISS"))
                    .thenReturn(Optional.empty());
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-CHILD-MISS"))
                    .thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class)))
                    .thenThrow(new DomainValidationException("Simulated failure"))
                    .thenReturn(child);
            // Phase 2: parent not in batch and not in DB
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-PARENT-MISS"))
                    .thenReturn(Optional.empty());
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            ImportResult result = service.importReqif(PROJECT_ID, "test.reqif", reqif);

            assertThat(result.relationsCreated()).isZero();
            // One error from Phase 1 (parent create failed) + one from Phase 2 (parent not found)
            assertThat(result.errors().stream()
                            .filter(e -> e.get("error").toString().contains("Parent not found"))
                            .count())
                    .isEqualTo(1);
        }

        @Test
        void relationCreationError_collectsError_reqif() {
            String reqif = reqifWithHierarchy("RIF-PARENT-ERR", "Parent", "RIF-CHILD-ERR", "Child");
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            var parent = makeRequirement("RIF-PARENT-ERR", parentId);
            var child = makeRequirement("RIF-CHILD-ERR", childId);

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-PARENT-ERR"))
                    .thenReturn(Optional.empty());
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-CHILD-ERR"))
                    .thenReturn(Optional.empty());
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

            ImportResult result = service.importReqif(PROJECT_ID, "test.reqif", reqif);

            assertThat(result.relationsCreated()).isZero();
            assertThat(result.errors())
                    .anyMatch(e -> e.get("phase").equals("relations")
                            && e.get("error").toString().contains("Simulated relation failure"));
        }

        @Test
        void sourceNotFoundForExplicitRelation_collectsError() {
            String reqif = reqifWithExplicitRelation("RIF-MISSING-SRC", "RIF-TGT-OK", "depends on");
            UUID tgtId = UUID.randomUUID();
            var tgt = makeRequirement("RIF-TGT-OK", tgtId);

            // Only target gets created; source creation fails
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-MISSING-SRC"))
                    .thenReturn(Optional.empty());
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-TGT-OK"))
                    .thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class)))
                    .thenThrow(new DomainValidationException("Simulated failure"))
                    .thenReturn(tgt);
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            ImportResult result = service.importReqif(PROJECT_ID, "test.reqif", reqif);

            assertThat(result.errors()).anyMatch(e -> e.get("error").toString().contains("Source not found"));
        }

        @Test
        void targetNotFoundForExplicitRelation_collectsError() {
            String reqif = reqifWithExplicitRelation("RIF-SRC-OK", "RIF-MISSING-TGT", "depends on");
            UUID srcId = UUID.randomUUID();
            var src = makeRequirement("RIF-SRC-OK", srcId);

            // Only source gets created; target creation fails
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-SRC-OK"))
                    .thenReturn(Optional.empty());
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-MISSING-TGT"))
                    .thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class)))
                    .thenReturn(src)
                    .thenThrow(new DomainValidationException("Simulated failure"));
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            ImportResult result = service.importReqif(PROJECT_ID, "test.reqif", reqif);

            assertThat(result.errors()).anyMatch(e -> e.get("error").toString().contains("Target not found"));
        }

        @Test
        void explicitRelationSourceLookedUpFromDb() {
            String reqif = reqifWithExplicitRelation("RIF-DB-SRC", "RIF-BATCH-TGT", "depends on");
            UUID srcId = UUID.randomUUID();
            UUID tgtId = UUID.randomUUID();
            var srcReq = makeRequirement("RIF-DB-SRC", srcId);
            var tgtReq = makeRequirement("RIF-BATCH-TGT", tgtId);

            // Source creation fails, target succeeds
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-DB-SRC"))
                    .thenReturn(Optional.empty()) // Phase 1
                    .thenReturn(Optional.of(srcReq)); // Phase 2b DB fallback
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-BATCH-TGT"))
                    .thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class)))
                    .thenThrow(new DomainValidationException("Simulated failure"))
                    .thenReturn(tgtReq);
            when(relationRepository.existsBySourceIdAndTargetIdAndRelationType(srcId, tgtId, RelationType.DEPENDS_ON))
                    .thenReturn(false);
            when(requirementService.createRelation(srcId, tgtId, RelationType.DEPENDS_ON))
                    .thenReturn(new RequirementRelation(srcReq, tgtReq, RelationType.DEPENDS_ON));
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            ImportResult result = service.importReqif(PROJECT_ID, "test.reqif", reqif);

            assertThat(result.relationsCreated()).isEqualTo(1);
            verify(requirementService).createRelation(srcId, tgtId, RelationType.DEPENDS_ON);
        }

        @Test
        void explicitRelationCreationError_collectsError() {
            String reqif = reqifWithExplicitRelation("RIF-SRC-REL", "RIF-TGT-REL", "depends on");
            UUID srcId = UUID.randomUUID();
            UUID tgtId = UUID.randomUUID();
            var src = makeRequirement("RIF-SRC-REL", srcId);
            var tgt = makeRequirement("RIF-TGT-REL", tgtId);

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-SRC-REL"))
                    .thenReturn(Optional.empty());
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-TGT-REL"))
                    .thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class)))
                    .thenReturn(src)
                    .thenReturn(tgt);
            when(relationRepository.existsBySourceIdAndTargetIdAndRelationType(srcId, tgtId, RelationType.DEPENDS_ON))
                    .thenReturn(false);
            when(requirementService.createRelation(srcId, tgtId, RelationType.DEPENDS_ON))
                    .thenThrow(new DomainValidationException("Simulated SpecRelation failure"));
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            ImportResult result = service.importReqif(PROJECT_ID, "test.reqif", reqif);

            assertThat(result.relationsCreated()).isZero();
            assertThat(result.errors())
                    .anyMatch(e -> e.get("phase").equals("relations")
                            && e.get("error").toString().contains("Simulated SpecRelation failure"));
        }

        @Test
        void skipsExistingRelationsFromReqif() {
            String reqif = reqifWithHierarchy("RIF-PARENT", "Parent", "RIF-CHILD", "Child");
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            var parent = makeRequirement("RIF-PARENT", parentId);
            var child = makeRequirement("RIF-CHILD", childId);

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-PARENT"))
                    .thenReturn(Optional.empty());
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-CHILD"))
                    .thenReturn(Optional.empty());
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

            ImportResult result = service.importReqif(PROJECT_ID, "test.reqif", reqif);

            assertThat(result.relationsSkipped()).isEqualTo(1);
            assertThat(result.relationsCreated()).isZero();
            verify(requirementService, never()).createRelation(any(), any(), any());
        }
    }

    @Nested
    class ReqifErrorHandling {

        @Test
        void collectsErrorsAndContinuesForReqif() {
            // Two requirements: first one throws, second succeeds
            String reqif =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <REQ-IF xmlns="http://www.omg.org/spec/ReqIF/20110401/reqif.xsd">
                      <THE-HEADER>
                        <REQ-IF-HEADER IDENTIFIER="h1"><TITLE>Test</TITLE></REQ-IF-HEADER>
                      </THE-HEADER>
                      <CORE-CONTENT>
                        <REQ-IF-CONTENT>
                          <DATATYPES/>
                          <SPEC-TYPES/>
                          <SPEC-OBJECTS>
                            <SPEC-OBJECT IDENTIFIER="RIF-FAIL" LONG-NAME="Fail"/>
                            <SPEC-OBJECT IDENTIFIER="RIF-OK" LONG-NAME="OK"/>
                          </SPEC-OBJECTS>
                          <SPEC-RELATIONS/>
                          <SPECIFICATIONS/>
                        </REQ-IF-CONTENT>
                      </CORE-CONTENT>
                    </REQ-IF>
                    """;
            UUID okId = UUID.randomUUID();
            var okReq = makeRequirement("RIF-OK", okId);

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-FAIL"))
                    .thenReturn(Optional.empty());
            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-OK"))
                    .thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class)))
                    .thenThrow(new DomainValidationException("Simulated failure"))
                    .thenReturn(okReq);
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            ImportResult result = service.importReqif(PROJECT_ID, "test.reqif", reqif);

            assertThat(result.requirementsCreated()).isEqualTo(1);
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0).get("uid")).isEqualTo("RIF-FAIL");
        }
    }

    @Nested
    class ReqifAuditRecord {

        @Test
        void savesReqifImportAuditRecord() {
            String reqif = minimalReqif("RIF-AUDIT", "Audit Test");
            UUID reqId = UUID.randomUUID();
            var req = makeRequirement("RIF-AUDIT", reqId);

            when(requirementRepository.findByProjectIdAndUid(PROJECT_ID, "RIF-AUDIT"))
                    .thenReturn(Optional.empty());
            when(requirementService.create(any(CreateRequirementCommand.class))).thenReturn(req);
            when(importRepository.save(any(RequirementImport.class))).thenAnswer(inv -> {
                var audit = inv.<RequirementImport>getArgument(0);
                setField(audit, "id", UUID.randomUUID());
                return audit;
            });

            service.importReqif(PROJECT_ID, "test.reqif", reqif);

            verify(importRepository).save(any(RequirementImport.class));
        }
    }
}
