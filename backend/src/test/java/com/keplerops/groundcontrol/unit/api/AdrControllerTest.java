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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.adrs.AdrController;
import com.keplerops.groundcontrol.domain.adrs.model.ArchitectureDecisionRecord;
import com.keplerops.groundcontrol.domain.adrs.service.AdrService;
import com.keplerops.groundcontrol.domain.adrs.state.AdrStatus;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdrController.class)
class AdrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdrService adrService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ADR_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private ArchitectureDecisionRecord makeAdr() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var adr = new ArchitectureDecisionRecord(
                project,
                "ADR-018",
                "AWS EC2 Deployment",
                LocalDate.of(2026, 3, 15),
                "Need a deployment target",
                "Deploy to EC2",
                "Simple and cost-effective",
                "test-user");
        setField(adr, "id", ADR_ID);
        setField(adr, "createdAt", Instant.now());
        setField(adr, "updatedAt", Instant.now());
        return adr;
    }

    private Requirement makeRequirement() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var req = new Requirement(project, "GC-J001", "ADR Management", "Manage ADRs");
        setField(req, "id", UUID.randomUUID());
        setField(req, "createdAt", Instant.now());
        setField(req, "updatedAt", Instant.now());
        return req;
    }

    @Test
    void createReturns201() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(adrService.create(any())).thenReturn(makeAdr());

        mockMvc.perform(
                        post("/api/v1/adrs")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"uid":"ADR-018","title":"AWS EC2 Deployment","decisionDate":"2026-03-15",
                 "context":"Need a deployment target","decision":"Deploy to EC2",
                 "consequences":"Simple and cost-effective"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uid", is("ADR-018")))
                .andExpect(jsonPath("$.title", is("AWS EC2 Deployment")))
                .andExpect(jsonPath("$.status", is("PROPOSED")))
                .andExpect(jsonPath("$.projectIdentifier", is("ground-control")));
    }

    @Test
    void listReturnsAdrs() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(adrService.listByProject(PROJECT_ID)).thenReturn(List.of(makeAdr()));

        mockMvc.perform(get("/api/v1/adrs").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uid", is("ADR-018")));
    }

    @Test
    void getByIdReturnsAdr() throws Exception {
        when(adrService.getById(ADR_ID)).thenReturn(makeAdr());

        mockMvc.perform(get("/api/v1/adrs/{id}", ADR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("ADR-018")));
    }

    @Test
    void getByUidReturnsAdr() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(adrService.getByUid("ADR-018", PROJECT_ID)).thenReturn(makeAdr());

        mockMvc.perform(get("/api/v1/adrs/uid/{uid}", "ADR-018").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("ADR-018")));
    }

    @Test
    void updateReturnsUpdated() throws Exception {
        var updated = makeAdr();
        updated.setTitle("Updated Title");
        when(adrService.update(eq(ADR_ID), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/adrs/{id}", ADR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"title":"Updated Title"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Title")));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/adrs/{id}", ADR_ID)).andExpect(status().isNoContent());

        verify(adrService).delete(ADR_ID);
    }

    @Test
    void transitionStatusReturnsUpdated() throws Exception {
        var accepted = makeAdr();
        setField(accepted, "status", AdrStatus.ACCEPTED);
        when(adrService.transitionStatus(ADR_ID, AdrStatus.ACCEPTED)).thenReturn(accepted);

        mockMvc.perform(put("/api/v1/adrs/{id}/status", ADR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"status":"ACCEPTED"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")));
    }

    @Test
    void getLinkedRequirementsReturnsList() throws Exception {
        when(adrService.findLinkedRequirements(ADR_ID)).thenReturn(List.of(makeRequirement()));

        mockMvc.perform(get("/api/v1/adrs/{id}/requirements", ADR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uid", is("GC-J001")));
    }
}
