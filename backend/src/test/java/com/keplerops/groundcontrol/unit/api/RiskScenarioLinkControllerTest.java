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

import com.keplerops.groundcontrol.api.riskscenarios.RiskScenarioLinkController;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenario;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenarioLink;
import com.keplerops.groundcontrol.domain.riskscenarios.service.RiskScenarioLinkService;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkTargetType;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RiskScenarioLinkController.class)
class RiskScenarioLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RiskScenarioLinkService linkService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RS_ID = UUID.fromString("00000000-0000-0000-0000-000000000100");
    private static final UUID LINK_ID = UUID.fromString("00000000-0000-0000-0000-000000000200");
    private static final Instant NOW = Instant.parse("2026-04-01T12:00:00Z");

    private RiskScenarioLink makeLink() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var scenario = new RiskScenario(project, "RS-001", "Test scenario", "Source", "Event", "Object", "Consequence");
        scenario.setTimeHorizon("12 months");
        setField(scenario, "id", RS_ID);
        var link = new RiskScenarioLink(
                scenario, RiskScenarioLinkTargetType.CONTROL, "CTRL-001", RiskScenarioLinkType.MITIGATED_BY);
        link.setTargetTitle("MFA Policy");
        link.setTargetUrl("https://controls.example.com/CTRL-001");
        setField(link, "id", LINK_ID);
        setField(link, "createdAt", NOW);
        setField(link, "updatedAt", NOW);
        return link;
    }

    @Test
    void createReturns201() throws Exception {
        when(linkService.create(eq(RS_ID), any(), any(), any(), any(), any())).thenReturn(makeLink());

        mockMvc.perform(
                        post("/api/v1/risk-scenarios/{riskScenarioId}/links", RS_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                        {
                            "targetType": "CONTROL",
                            "targetIdentifier": "CTRL-001",
                            "linkType": "MITIGATED_BY",
                            "targetTitle": "MFA Policy",
                            "targetUrl": "https://controls.example.com/CTRL-001"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(LINK_ID.toString())))
                .andExpect(jsonPath("$.targetType", is("CONTROL")))
                .andExpect(jsonPath("$.targetIdentifier", is("CTRL-001")))
                .andExpect(jsonPath("$.linkType", is("MITIGATED_BY")));
    }

    @Test
    void createReturns422WhenTargetTypeMissing() throws Exception {
        mockMvc.perform(
                        post("/api/v1/risk-scenarios/{riskScenarioId}/links", RS_ID)
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
        when(linkService.listByScenario(RS_ID, null)).thenReturn(List.of(makeLink()));

        mockMvc.perform(get("/api/v1/risk-scenarios/{riskScenarioId}/links", RS_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].targetIdentifier", is("CTRL-001")));
    }

    @Test
    void listFiltersByTargetType() throws Exception {
        when(linkService.listByScenario(RS_ID, RiskScenarioLinkTargetType.CONTROL))
                .thenReturn(List.of(makeLink()));

        mockMvc.perform(get("/api/v1/risk-scenarios/{riskScenarioId}/links", RS_ID)
                        .param("targetType", "CONTROL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/risk-scenarios/{riskScenarioId}/links/{linkId}", RS_ID, LINK_ID))
                .andExpect(status().isNoContent());

        verify(linkService).delete(RS_ID, LINK_ID);
    }
}
