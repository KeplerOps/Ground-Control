package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.documents.service.DocumentExportService;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentExportServiceTest {

    @Mock
    private RequirementRepository requirementRepository;

    private DocumentExportService service;

    private static final UUID PROJECT_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID PROJECT_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    @BeforeEach
    void setUp() {
        service = new DocumentExportService(requirementRepository);
    }

    // --- helpers ---

    private static Project makeProject(UUID id, String identifier) {
        var p = new Project(identifier, identifier);
        setField(p, "id", id);
        return p;
    }

    private static Requirement makeRequirement(Project project, String uid) {
        var req = new Requirement(project, uid, "Title " + uid, "Statement " + uid);
        setField(req, "id", UUID.randomUUID());
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

    private static String sdocWithUid(String uid) {
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

    private static String sdocWithUids(String uid1, String uid2) {
        return """
                [REQUIREMENT]
                UID: %s
                TITLE: First
                STATEMENT: >>>
                First statement.
                <<<

                [REQUIREMENT]
                UID: %s
                TITLE: Second
                STATEMENT: >>>
                Second statement.
                <<<
                """
                .formatted(uid1, uid2);
    }

    // --- tests ---

    @Test
    void export_resolvedRequirementIsReturnedInResult() {
        var project = makeProject(PROJECT_A, "proj-a");
        var req = makeRequirement(project, "REQ-001");
        when(requirementRepository.findByProjectIdAndUidIgnoreCase(PROJECT_A, "REQ-001"))
                .thenReturn(Optional.of(req));

        var result = service.export(PROJECT_A, sdocWithUid("REQ-001"));

        assertThat(result.resolved()).hasSize(1);
        assertThat(result.resolved().get(0).requirement()).isSameAs(req);
        assertThat(result.resolved().get(0).sdocRequirement().uid()).isEqualTo("REQ-001");
        assertThat(result.unresolvedUids()).isEmpty();
    }

    @Test
    void export_uidNotInProject_isReportedUnresolved() {
        when(requirementRepository.findByProjectIdAndUidIgnoreCase(PROJECT_A, "REQ-999"))
                .thenReturn(Optional.empty());

        var result = service.export(PROJECT_A, sdocWithUid("REQ-999"));

        assertThat(result.resolved()).isEmpty();
        assertThat(result.unresolvedUids()).containsExactly("REQ-999");
    }

    @Test
    void export_emptyDocument_returnsEmptyResult() {
        var result = service.export(PROJECT_A, "");

        assertThat(result.resolved()).isEmpty();
        assertThat(result.unresolvedUids()).isEmpty();
    }

    @Test
    void export_partialMatch_separatesResolvedAndUnresolved() {
        var project = makeProject(PROJECT_A, "proj-a");
        var req1 = makeRequirement(project, "REQ-001");
        when(requirementRepository.findByProjectIdAndUidIgnoreCase(PROJECT_A, "REQ-001"))
                .thenReturn(Optional.of(req1));
        when(requirementRepository.findByProjectIdAndUidIgnoreCase(PROJECT_A, "REQ-002"))
                .thenReturn(Optional.empty());

        var result = service.export(PROJECT_A, sdocWithUids("REQ-001", "REQ-002"));

        assertThat(result.resolved()).hasSize(1);
        assertThat(result.resolved().get(0).requirement().getUid()).isEqualTo("REQ-001");
        assertThat(result.unresolvedUids()).containsExactly("REQ-002");
    }

    /**
     * Regression test: two projects share the same UID. Export scoped to project A must not return
     * the requirement from project B.
     */
    @Test
    void export_duplicateUidAcrossProjects_scopedToOwningProject() {
        var projectA = makeProject(PROJECT_A, "proj-a");
        var projectB = makeProject(PROJECT_B, "proj-b");
        var reqInA = makeRequirement(projectA, "SHARED-001");
        var reqInB = makeRequirement(projectB, "SHARED-001");

        when(requirementRepository.findByProjectIdAndUidIgnoreCase(PROJECT_A, "SHARED-001"))
                .thenReturn(Optional.of(reqInA));
        when(requirementRepository.findByProjectIdAndUidIgnoreCase(PROJECT_B, "SHARED-001"))
                .thenReturn(Optional.of(reqInB));

        var resultA = service.export(PROJECT_A, sdocWithUid("SHARED-001"));
        var resultB = service.export(PROJECT_B, sdocWithUid("SHARED-001"));

        // Each project gets its own requirement, not the other project's.
        assertThat(resultA.resolved()).hasSize(1);
        assertThat(resultA.resolved().get(0).requirement()).isSameAs(reqInA);
        assertThat(resultA.resolved().get(0).requirement()).isNotSameAs(reqInB);

        assertThat(resultB.resolved()).hasSize(1);
        assertThat(resultB.resolved().get(0).requirement()).isSameAs(reqInB);
        assertThat(resultB.resolved().get(0).requirement()).isNotSameAs(reqInA);
    }

    /**
     * Verify that the global {@code findByUid} is never called; project-scoped lookup is always
     * used.
     */
    @Test
    void export_neverCallsGlobalFindByUid() {
        when(requirementRepository.findByProjectIdAndUidIgnoreCase(PROJECT_A, "REQ-001"))
                .thenReturn(Optional.empty());

        service.export(PROJECT_A, sdocWithUid("REQ-001"));

        verify(requirementRepository, never()).findByUid("REQ-001");
    }
}
