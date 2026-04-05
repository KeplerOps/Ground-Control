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

import com.keplerops.groundcontrol.api.controls.ControlLinkController;
import com.keplerops.groundcontrol.domain.controls.model.Control;
import com.keplerops.groundcontrol.domain.controls.model.ControlLink;
import com.keplerops.groundcontrol.domain.controls.service.ControlLinkService;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkType;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ControlLinkController.class)
class ControlLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ControlLinkService controlLinkService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CONTROL_ID = UUID.fromString("00000000-0000-0000-0000-000000000500");
    private static final UUID LINK_ID = UUID.fromString("00000000-0000-0000-0000-000000000600");
    private static final Instant NOW = Instant.parse("2026-04-05T12:00:00Z");

    private ControlLink makeLink() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var control = new Control(project, "CTRL-001", "Access Control", ControlFunction.PREVENTIVE);
        setField(control, "id", CONTROL_ID);
        var link = new ControlLink(control, ControlLinkTargetType.ASSET, null, "ASSET-001", ControlLinkType.PROTECTS);
        link.setTargetTitle("Web Server");
        setField(link, "id", LINK_ID);
        setField(link, "createdAt", NOW);
        setField(link, "updatedAt", NOW);
        return link;
    }

    @Test
    void createReturns201() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(controlLinkService.create(eq(PROJECT_ID), eq(CONTROL_ID), any(), any(), any(), any(), any(), any()))
                .thenReturn(makeLink());

        mockMvc.perform(
                        post("/api/v1/controls/{controlId}/links", CONTROL_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "targetType": "ASSET",
                                  "targetIdentifier": "ASSET-001",
                                  "linkType": "PROTECTS"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(LINK_ID.toString())))
                .andExpect(jsonPath("$.targetType", is("ASSET")))
                .andExpect(jsonPath("$.linkType", is("PROTECTS")));
    }

    @Test
    void listReturnsLinks() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(controlLinkService.listByControl(PROJECT_ID, CONTROL_ID, null)).thenReturn(List.of(makeLink()));

        mockMvc.perform(get("/api/v1/controls/{controlId}/links", CONTROL_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].targetIdentifier", is("ASSET-001")));
    }

    @Test
    void deleteReturns204() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/controls/{controlId}/links/{linkId}", CONTROL_ID, LINK_ID)
                        .param("project", "ground-control"))
                .andExpect(status().isNoContent());

        verify(controlLinkService).delete(PROJECT_ID, CONTROL_ID, LINK_ID);
    }
}
