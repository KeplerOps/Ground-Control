package com.keplerops.groundcontrol.unit.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.requirements.RequirementGraphController;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.service.GraphClient;
import com.keplerops.groundcontrol.domain.requirements.service.PathResult;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RequirementGraphController.class)
class RequirementGraphControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GraphClient graphClient;

    @MockitoBean
    private ProjectService projectService;

    @Test
    void ancestorsReturnsRequirementDagData() throws Exception {
        var projectId = UUID.randomUUID();
        when(projectService.requireProjectId("test")).thenReturn(projectId);
        when(graphClient.getAncestors(eq(projectId), eq("REQ-CHILD"), anyInt()))
                .thenReturn(List.of("REQ-PARENT", "REQ-GRANDPARENT"));

        mockMvc.perform(get("/api/v1/requirements/graph/ancestors/REQ-CHILD").param("project", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]", is("REQ-PARENT")));
    }

    @Test
    void descendantsReturnsRequirementDagData() throws Exception {
        var projectId = UUID.randomUUID();
        when(projectService.requireProjectId("test")).thenReturn(projectId);
        when(graphClient.getDescendants(eq(projectId), eq("REQ-PARENT"), anyInt()))
                .thenReturn(List.of("REQ-CHILD"));

        mockMvc.perform(get("/api/v1/requirements/graph/descendants/REQ-PARENT").param("project", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]", is("REQ-CHILD")));
    }

    @Test
    void findPathsReturnsRequirementDagPaths() throws Exception {
        var projectId = UUID.randomUUID();
        when(projectService.requireProjectId("test")).thenReturn(projectId);
        when(graphClient.findPaths(eq(projectId), anyString(), anyString()))
                .thenReturn(
                        List.of(new PathResult(List.of("REQ-A", "REQ-B", "REQ-C"), List.of("DEPENDS_ON", "PARENT"))));

        mockMvc.perform(get("/api/v1/requirements/graph/paths")
                        .param("project", "test")
                        .param("source", "REQ-A")
                        .param("target", "REQ-C"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nodes[0]", is("REQ-A")))
                .andExpect(jsonPath("$[0].edges[0].relationType", is("DEPENDS_ON")));
    }
}
