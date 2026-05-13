package com.keplerops.groundcontrol.unit.api;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.findings.FindingLinkController;
import com.keplerops.groundcontrol.domain.findings.model.Finding;
import com.keplerops.groundcontrol.domain.findings.model.FindingLink;
import com.keplerops.groundcontrol.domain.findings.service.CreateFindingLinkCommand;
import com.keplerops.groundcontrol.domain.findings.service.FindingLinkService;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkTargetType;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkType;
import com.keplerops.groundcontrol.domain.findings.state.FindingSeverity;
import com.keplerops.groundcontrol.domain.findings.state.FindingType;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(FindingLinkController.class)
class FindingLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindingLinkService linkService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FINDING_ID = UUID.fromString("00000000-0000-0000-0000-000000000200");
    private static final UUID LINK_ID = UUID.fromString("00000000-0000-0000-0000-000000000300");
    private static final UUID CONTROL_ID = UUID.fromString("00000000-0000-0000-0000-000000000400");
    private static final Instant NOW = Instant.parse("2026-05-13T12:00:00Z");

    private Finding makeFinding() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var f = new Finding(
                project, "FIND-001", "MFA missing", FindingType.CONTROL_DEFICIENCY, FindingSeverity.HIGH, "desc");
        setField(f, "id", FINDING_ID);
        return f;
    }

    private FindingLink makeInternalLink() {
        var link = new FindingLink(
                makeFinding(), FindingLinkTargetType.CONTROL, CONTROL_ID, null, FindingLinkType.MITIGATED_BY);
        link.setTargetTitle("Access policy");
        setField(link, "id", LINK_ID);
        setField(link, "createdAt", NOW);
        setField(link, "updatedAt", NOW);
        return link;
    }

    private FindingLink makeExternalLink() {
        var link = new FindingLink(
                makeFinding(),
                FindingLinkTargetType.EVIDENCE,
                null,
                "s3://evidence/audit-2026-q2.pdf",
                FindingLinkType.EVIDENCED_BY);
        setField(link, "id", LINK_ID);
        setField(link, "createdAt", NOW);
        setField(link, "updatedAt", NOW);
        return link;
    }

    @Test
    void createInternalLinkReturns201() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(linkService.create(eq(PROJECT_ID), eq(FINDING_ID), any())).thenReturn(makeInternalLink());

        mockMvc.perform(post("/api/v1/findings/{findingId}/links", FINDING_ID)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                    "targetType": "CONTROL",
                                    "targetEntityId": "%s",
                                    "linkType": "MITIGATED_BY",
                                    "targetTitle": "Access policy"
                                }
                                """
                                        .formatted(CONTROL_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(LINK_ID.toString())))
                .andExpect(jsonPath("$.targetType", is("CONTROL")))
                .andExpect(jsonPath("$.targetEntityId", is(CONTROL_ID.toString())))
                .andExpect(jsonPath("$.linkType", is("MITIGATED_BY")));

        // Lock in the request→command mapping for the internal-target branch.
        // Without this capture the test would still pass if the controller
        // swapped targetEntityId ↔ targetIdentifier or dropped targetTitle,
        // because the mock returns the canned link regardless of input.
        var captor = ArgumentCaptor.forClass(CreateFindingLinkCommand.class);
        verify(linkService).create(eq(PROJECT_ID), eq(FINDING_ID), captor.capture());
        var command = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(FindingLinkTargetType.CONTROL, command.targetType());
        org.junit.jupiter.api.Assertions.assertEquals(CONTROL_ID, command.targetEntityId());
        org.junit.jupiter.api.Assertions.assertNull(command.targetIdentifier());
        org.junit.jupiter.api.Assertions.assertEquals(FindingLinkType.MITIGATED_BY, command.linkType());
        org.junit.jupiter.api.Assertions.assertEquals("Access policy", command.targetTitle());
    }

    @Test
    void createExternalLinkReturns201() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(linkService.create(eq(PROJECT_ID), eq(FINDING_ID), any())).thenReturn(makeExternalLink());

        mockMvc.perform(
                        post("/api/v1/findings/{findingId}/links", FINDING_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "targetType": "EVIDENCE",
                                    "targetIdentifier": "s3://evidence/audit-2026-q2.pdf",
                                    "linkType": "EVIDENCED_BY"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType", is("EVIDENCE")))
                .andExpect(jsonPath("$.targetIdentifier", is("s3://evidence/audit-2026-q2.pdf")))
                .andExpect(jsonPath("$.linkType", is("EVIDENCED_BY")));

        var captor = ArgumentCaptor.forClass(CreateFindingLinkCommand.class);
        verify(linkService).create(eq(PROJECT_ID), eq(FINDING_ID), captor.capture());
        var command = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(FindingLinkTargetType.EVIDENCE, command.targetType());
        org.junit.jupiter.api.Assertions.assertNull(command.targetEntityId());
        org.junit.jupiter.api.Assertions.assertEquals("s3://evidence/audit-2026-q2.pdf", command.targetIdentifier());
        org.junit.jupiter.api.Assertions.assertEquals(FindingLinkType.EVIDENCED_BY, command.linkType());
    }

    @Test
    void createReturns422WhenTargetTypeMissing() throws Exception {
        mockMvc.perform(
                        post("/api/v1/findings/{findingId}/links", FINDING_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "targetIdentifier": "CTRL-001",
                                    "linkType": "MITIGATED_BY"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listReturnsLinks() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(linkService.listByFinding(PROJECT_ID, FINDING_ID)).thenReturn(List.of(makeInternalLink()));

        mockMvc.perform(get("/api/v1/findings/{findingId}/links", FINDING_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].targetType", is("CONTROL")))
                // The response must report the findingId from the path, not from a
                // lazy parent dereference: with spring.jpa.open-in-view=false the
                // session is closed by the time the response mapper runs. Carrying
                // the path variable through .from(link, findingId) sidesteps the
                // LazyInitializationException — see ADR-038 and the cycle-1
                // pre-push codex review on issue #279.
                .andExpect(jsonPath("$[0].findingId", is(FINDING_ID.toString())));
    }

    @Test
    void deleteReturns204() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/findings/{findingId}/links/{linkId}", FINDING_ID, LINK_ID)
                        .param("project", "ground-control"))
                .andExpect(status().isNoContent());

        verify(linkService).delete(PROJECT_ID, FINDING_ID, LINK_ID);
    }
}
