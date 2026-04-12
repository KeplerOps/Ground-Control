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

import com.keplerops.groundcontrol.api.threatmodels.ThreatModelLinkController;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.threatmodels.model.ThreatModel;
import com.keplerops.groundcontrol.domain.threatmodels.model.ThreatModelLink;
import com.keplerops.groundcontrol.domain.threatmodels.service.CreateThreatModelLinkCommand;
import com.keplerops.groundcontrol.domain.threatmodels.service.ThreatModelLinkService;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelLinkTargetType;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelLinkType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ThreatModelLinkController.class)
class ThreatModelLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ThreatModelLinkService linkService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TM_ID = UUID.fromString("00000000-0000-0000-0000-000000000200");
    private static final UUID LINK_ID = UUID.fromString("00000000-0000-0000-0000-000000000300");
    private static final UUID ASSET_ID = UUID.fromString("00000000-0000-0000-0000-000000000400");
    private static final Instant NOW = Instant.parse("2026-04-11T12:00:00Z");

    private ThreatModelLink makeInternalLink() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var tm = new ThreatModel(
                project, "TM-001", "Credential stuffing", "External actor", "Credential replay", "Account takeover");
        setField(tm, "id", TM_ID);
        var link =
                new ThreatModelLink(tm, ThreatModelLinkTargetType.ASSET, ASSET_ID, null, ThreatModelLinkType.AFFECTS);
        link.setTargetTitle("Customer portal");
        setField(link, "id", LINK_ID);
        setField(link, "createdAt", NOW);
        setField(link, "updatedAt", NOW);
        return link;
    }

    private ThreatModelLink makeExternalLink() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var tm = new ThreatModel(
                project, "TM-001", "Credential stuffing", "External actor", "Credential replay", "Account takeover");
        setField(tm, "id", TM_ID);
        var link = new ThreatModelLink(
                tm,
                ThreatModelLinkTargetType.CODE,
                null,
                "backend/src/main/java/Auth.java",
                ThreatModelLinkType.DOCUMENTED_IN);
        setField(link, "id", LINK_ID);
        setField(link, "createdAt", NOW);
        setField(link, "updatedAt", NOW);
        return link;
    }

    @Test
    void createInternalLinkReturns201() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(linkService.create(eq(PROJECT_ID), eq(TM_ID), any())).thenReturn(makeInternalLink());

        mockMvc.perform(post("/api/v1/threat-models/{threatModelId}/links", TM_ID)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                    "targetType": "ASSET",
                                    "targetEntityId": "%s",
                                    "linkType": "AFFECTS",
                                    "targetTitle": "Customer portal"
                                }
                                """
                                        .formatted(ASSET_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(LINK_ID.toString())))
                .andExpect(jsonPath("$.targetType", is("ASSET")))
                .andExpect(jsonPath("$.targetEntityId", is(ASSET_ID.toString())))
                .andExpect(jsonPath("$.linkType", is("AFFECTS")));

        // Lock in the request→command mapping for the internal-target branch.
        // Without this capture the test would still pass if the controller
        // swapped targetEntityId ↔ targetIdentifier or dropped targetTitle,
        // because the mock returns the canned link regardless of input.
        var captor = ArgumentCaptor.forClass(CreateThreatModelLinkCommand.class);
        verify(linkService).create(eq(PROJECT_ID), eq(TM_ID), captor.capture());
        var command = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(ThreatModelLinkTargetType.ASSET, command.targetType());
        org.junit.jupiter.api.Assertions.assertEquals(ASSET_ID, command.targetEntityId());
        org.junit.jupiter.api.Assertions.assertNull(command.targetIdentifier());
        org.junit.jupiter.api.Assertions.assertEquals(ThreatModelLinkType.AFFECTS, command.linkType());
        org.junit.jupiter.api.Assertions.assertEquals("Customer portal", command.targetTitle());
    }

    @Test
    void createExternalLinkReturns201() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(linkService.create(eq(PROJECT_ID), eq(TM_ID), any())).thenReturn(makeExternalLink());

        mockMvc.perform(
                        post("/api/v1/threat-models/{threatModelId}/links", TM_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "targetType": "CODE",
                                    "targetIdentifier": "backend/src/main/java/Auth.java",
                                    "linkType": "DOCUMENTED_IN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType", is("CODE")))
                .andExpect(jsonPath("$.targetIdentifier", is("backend/src/main/java/Auth.java")))
                .andExpect(jsonPath("$.linkType", is("DOCUMENTED_IN")));

        // Lock in the request→command mapping for the external-identifier branch.
        var captor = ArgumentCaptor.forClass(CreateThreatModelLinkCommand.class);
        verify(linkService).create(eq(PROJECT_ID), eq(TM_ID), captor.capture());
        var command = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(ThreatModelLinkTargetType.CODE, command.targetType());
        org.junit.jupiter.api.Assertions.assertNull(command.targetEntityId());
        org.junit.jupiter.api.Assertions.assertEquals("backend/src/main/java/Auth.java", command.targetIdentifier());
        org.junit.jupiter.api.Assertions.assertEquals(ThreatModelLinkType.DOCUMENTED_IN, command.linkType());
    }

    @Test
    void createReturns422WhenTargetTypeMissing() throws Exception {
        mockMvc.perform(
                        post("/api/v1/threat-models/{threatModelId}/links", TM_ID)
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
        when(linkService.listByThreatModel(PROJECT_ID, TM_ID)).thenReturn(List.of(makeInternalLink()));

        mockMvc.perform(get("/api/v1/threat-models/{threatModelId}/links", TM_ID)
                        .param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].targetType", is("ASSET")));
    }

    @Test
    void deleteReturns204() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/threat-models/{threatModelId}/links/{linkId}", TM_ID, LINK_ID)
                        .param("project", "ground-control"))
                .andExpect(status().isNoContent());

        verify(linkService).delete(PROJECT_ID, TM_ID, LINK_ID);
    }
}
