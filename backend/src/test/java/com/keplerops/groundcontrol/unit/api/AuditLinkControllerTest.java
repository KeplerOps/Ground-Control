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

import com.keplerops.groundcontrol.api.audits.AuditLinkController;
import com.keplerops.groundcontrol.domain.audits.model.Audit;
import com.keplerops.groundcontrol.domain.audits.model.AuditLink;
import com.keplerops.groundcontrol.domain.audits.service.AuditLinkService;
import com.keplerops.groundcontrol.domain.audits.service.CreateAuditLinkCommand;
import com.keplerops.groundcontrol.domain.audits.state.AuditLinkTargetType;
import com.keplerops.groundcontrol.domain.audits.state.AuditLinkType;
import com.keplerops.groundcontrol.domain.audits.state.AuditType;
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
@WebMvcTest(AuditLinkController.class)
class AuditLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLinkService linkService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID AUDIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000200");
    private static final UUID LINK_ID = UUID.fromString("00000000-0000-0000-0000-000000000300");
    private static final UUID CONTROL_ID = UUID.fromString("00000000-0000-0000-0000-000000000400");
    private static final Instant NOW = Instant.parse("2026-05-18T12:00:00Z");

    private Audit makeAudit() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var a = new Audit(project, "AUDIT-001", "Annual compliance audit", AuditType.INTERNAL, "All prod systems.");
        setField(a, "id", AUDIT_ID);
        return a;
    }

    private AuditLink makeInternalLink() {
        var link = new AuditLink(makeAudit(), AuditLinkTargetType.CONTROL, CONTROL_ID, null, AuditLinkType.ASSESSES);
        link.setTargetTitle("Access policy");
        setField(link, "id", LINK_ID);
        setField(link, "createdAt", NOW);
        setField(link, "updatedAt", NOW);
        return link;
    }

    private AuditLink makeExternalLink() {
        var link = new AuditLink(makeAudit(), AuditLinkTargetType.FRAMEWORK, null, "ISO-27001", AuditLinkType.SCOPES);
        setField(link, "id", LINK_ID);
        setField(link, "createdAt", NOW);
        setField(link, "updatedAt", NOW);
        return link;
    }

    @Test
    void createInternalLinkReturns201() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(linkService.create(eq(PROJECT_ID), eq(AUDIT_ID), any())).thenReturn(makeInternalLink());

        mockMvc.perform(post("/api/v1/audits/{auditId}/links", AUDIT_ID)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                    "targetType": "CONTROL",
                                    "targetEntityId": "%s",
                                    "linkType": "ASSESSES",
                                    "targetTitle": "Access policy"
                                }
                                """
                                        .formatted(CONTROL_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(LINK_ID.toString())))
                .andExpect(jsonPath("$.auditId", is(AUDIT_ID.toString())))
                .andExpect(jsonPath("$.targetType", is("CONTROL")))
                .andExpect(jsonPath("$.targetEntityId", is(CONTROL_ID.toString())))
                .andExpect(jsonPath("$.linkType", is("ASSESSES")));

        var captor = ArgumentCaptor.forClass(CreateAuditLinkCommand.class);
        verify(linkService).create(eq(PROJECT_ID), eq(AUDIT_ID), captor.capture());
        var command = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(AuditLinkTargetType.CONTROL, command.targetType());
        org.junit.jupiter.api.Assertions.assertEquals(CONTROL_ID, command.targetEntityId());
        org.junit.jupiter.api.Assertions.assertNull(command.targetIdentifier());
        org.junit.jupiter.api.Assertions.assertEquals(AuditLinkType.ASSESSES, command.linkType());
        org.junit.jupiter.api.Assertions.assertEquals("Access policy", command.targetTitle());
    }

    @Test
    void createExternalLinkReturns201() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(linkService.create(eq(PROJECT_ID), eq(AUDIT_ID), any())).thenReturn(makeExternalLink());

        mockMvc.perform(
                        post("/api/v1/audits/{auditId}/links", AUDIT_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "targetType": "FRAMEWORK",
                                    "targetIdentifier": "ISO-27001",
                                    "linkType": "SCOPES"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType", is("FRAMEWORK")))
                .andExpect(jsonPath("$.targetIdentifier", is("ISO-27001")))
                .andExpect(jsonPath("$.linkType", is("SCOPES")));

        var captor = ArgumentCaptor.forClass(CreateAuditLinkCommand.class);
        verify(linkService).create(eq(PROJECT_ID), eq(AUDIT_ID), captor.capture());
        var command = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(AuditLinkTargetType.FRAMEWORK, command.targetType());
        org.junit.jupiter.api.Assertions.assertNull(command.targetEntityId());
        org.junit.jupiter.api.Assertions.assertEquals("ISO-27001", command.targetIdentifier());
        org.junit.jupiter.api.Assertions.assertEquals(AuditLinkType.SCOPES, command.linkType());
    }

    @Test
    void createReturns422WhenTargetTypeMissing() throws Exception {
        mockMvc.perform(
                        post("/api/v1/audits/{auditId}/links", AUDIT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "targetIdentifier": "ISO-27001",
                                    "linkType": "SCOPES"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listReturnsLinks() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(linkService.listByAudit(PROJECT_ID, AUDIT_ID)).thenReturn(List.of(makeInternalLink()));

        mockMvc.perform(get("/api/v1/audits/{auditId}/links", AUDIT_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].targetType", is("CONTROL")))
                .andExpect(jsonPath("$[0].auditId", is(AUDIT_ID.toString())));
    }

    @Test
    void deleteReturns204() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/audits/{auditId}/links/{linkId}", AUDIT_ID, LINK_ID)
                        .param("project", "ground-control"))
                .andExpect(status().isNoContent());

        verify(linkService).delete(PROJECT_ID, AUDIT_ID, LINK_ID);
    }
}
