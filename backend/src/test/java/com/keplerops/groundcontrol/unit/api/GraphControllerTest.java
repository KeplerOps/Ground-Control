package com.keplerops.groundcontrol.unit.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.admin.GraphController;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.service.AnalysisService;
import com.keplerops.groundcontrol.domain.requirements.service.GraphClient;
import com.keplerops.groundcontrol.domain.requirements.service.GraphVisualizationResult;
import com.keplerops.groundcontrol.domain.requirements.service.PathResult;
import com.keplerops.groundcontrol.domain.requirements.service.SubgraphResult;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GraphController.class)
class GraphControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GraphClient graphClient;

    @MockitoBean
    private AnalysisService analysisService;

    @SuppressWarnings("UnusedVariable") // Required by Spring context
    @MockitoBean
    private ProjectService projectService;

    @Nested
    class Materialize {

        @Test
        void returns200() throws Exception {
            mockMvc.perform(post("/api/v1/admin/graph/materialize")).andExpect(status().isOk());

            verify(graphClient).materializeGraph();
        }
    }

    @Nested
    class Ancestors {

        @Test
        void returns200() throws Exception {
            when(graphClient.getAncestors(anyString(), anyInt())).thenReturn(List.of("REQ-PARENT", "REQ-GRANDPARENT"));

            mockMvc.perform(get("/api/v1/graph/ancestors/REQ-CHILD"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0]", is("REQ-PARENT")));
        }
    }

    @Nested
    class Descendants {

        @Test
        void returns200() throws Exception {
            when(graphClient.getDescendants(anyString(), anyInt())).thenReturn(List.of("REQ-CHILD"));

            mockMvc.perform(get("/api/v1/graph/descendants/REQ-PARENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0]", is("REQ-CHILD")));
        }
    }

    @Nested
    class FindPaths {

        @Test
        void returns200WithNodesAndEdges() throws Exception {
            when(graphClient.findPaths(anyString(), anyString()))
                    .thenReturn(List.of(
                            new PathResult(List.of("REQ-A", "REQ-B", "REQ-C"), List.of("DEPENDS_ON", "PARENT"))));

            mockMvc.perform(get("/api/v1/graph/paths").param("source", "REQ-A").param("target", "REQ-C"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].nodes", hasSize(3)))
                    .andExpect(jsonPath("$[0].nodes[0]", is("REQ-A")))
                    .andExpect(jsonPath("$[0].edges", hasSize(2)))
                    .andExpect(jsonPath("$[0].edges[0].sourceUid", is("REQ-A")))
                    .andExpect(jsonPath("$[0].edges[0].targetUid", is("REQ-B")))
                    .andExpect(jsonPath("$[0].edges[0].relationType", is("DEPENDS_ON")))
                    .andExpect(jsonPath("$[0].edges[1].relationType", is("PARENT")));
        }
    }

    @Nested
    class Visualization {

        @Test
        void returns200WithNodesAndEdges() throws Exception {
            var projectId = UUID.randomUUID();
            var project = new Project("test", "Test");
            var a = new Requirement(project, "REQ-A", "Title A", "Statement A");
            var b = new Requirement(project, "REQ-B", "Title B", "Statement B");
            var rel = new RequirementRelation(a, b, RelationType.DEPENDS_ON);

            when(projectService.resolveProjectId("test")).thenReturn(projectId);
            when(analysisService.getGraphVisualization(projectId))
                    .thenReturn(new GraphVisualizationResult(List.of(a, b), List.of(rel)));

            mockMvc.perform(get("/api/v1/graph/visualization").param("project", "test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nodes", hasSize(2)))
                    .andExpect(jsonPath("$.edges", hasSize(1)))
                    .andExpect(jsonPath("$.totalNodes", is(2)))
                    .andExpect(jsonPath("$.totalEdges", is(1)))
                    .andExpect(jsonPath("$.nodes[0].uid", is("REQ-A")))
                    .andExpect(jsonPath("$.nodes[0].entityType", is("REQUIREMENT")))
                    .andExpect(jsonPath("$.edges[0].relationType", is("DEPENDS_ON")));
        }

        @Test
        void filtersNodesByEntityType() throws Exception {
            var projectId = UUID.randomUUID();
            var project = new Project("test", "Test");
            var a = new Requirement(project, "REQ-A", "Title A", "Statement A");

            when(projectService.resolveProjectId("test")).thenReturn(projectId);
            when(analysisService.getGraphVisualization(projectId))
                    .thenReturn(new GraphVisualizationResult(List.of(a), List.of()));

            mockMvc.perform(get("/api/v1/graph/visualization")
                            .param("project", "test")
                            .param("entityTypes", "REQUIREMENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nodes", hasSize(1)));

            mockMvc.perform(get("/api/v1/graph/visualization")
                            .param("project", "test")
                            .param("entityTypes", "DOCUMENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nodes", hasSize(0)));
        }
    }

    @Nested
    class Subgraph {

        @Test
        @SuppressWarnings("unchecked")
        void returns200WithSubgraphNodesAndEdges() throws Exception {
            var projectId = UUID.randomUUID();
            var project = new Project("test", "Test");
            var a = new Requirement(project, "REQ-A", "Title A", "Statement A");
            var b = new Requirement(project, "REQ-B", "Title B", "Statement B");
            var rel = new RequirementRelation(a, b, RelationType.DEPENDS_ON);

            when(projectService.resolveProjectId("test")).thenReturn(projectId);
            when(analysisService.extractSubgraph(any(UUID.class), any(List.class)))
                    .thenReturn(new SubgraphResult(List.of(a, b), List.of(rel)));

            mockMvc.perform(get("/api/v1/graph/subgraph")
                            .param("roots", "REQ-A", "REQ-B")
                            .param("project", "test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nodes", hasSize(2)))
                    .andExpect(jsonPath("$.edges", hasSize(1)))
                    .andExpect(jsonPath("$.totalNodes", is(2)))
                    .andExpect(jsonPath("$.totalEdges", is(1)))
                    .andExpect(jsonPath("$.rootUids", hasSize(2)))
                    .andExpect(jsonPath("$.nodes[0].uid", is("REQ-A")))
                    .andExpect(jsonPath("$.edges[0].relationType", is("DEPENDS_ON")));
        }

        @Test
        @SuppressWarnings("unchecked")
        void filtersSubgraphByEntityType() throws Exception {
            var projectId = UUID.randomUUID();
            var project = new Project("test", "Test");
            var a = new Requirement(project, "REQ-A", "Title A", "Statement A");

            when(projectService.resolveProjectId("test")).thenReturn(projectId);
            when(analysisService.extractSubgraph(any(UUID.class), any(List.class)))
                    .thenReturn(new SubgraphResult(List.of(a), List.of()));

            mockMvc.perform(get("/api/v1/graph/subgraph")
                            .param("roots", "REQ-A")
                            .param("project", "test")
                            .param("entityTypes", "DOCUMENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nodes", hasSize(0)));
        }
    }
}
