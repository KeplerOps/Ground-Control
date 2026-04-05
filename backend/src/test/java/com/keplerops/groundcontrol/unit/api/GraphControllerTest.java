package com.keplerops.groundcontrol.unit.api;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.admin.GraphController;
import com.keplerops.groundcontrol.domain.graph.model.GraphEdge;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphNode;
import com.keplerops.groundcontrol.domain.graph.model.GraphProjection;
import com.keplerops.groundcontrol.domain.graph.service.GraphPathResult;
import com.keplerops.groundcontrol.domain.graph.service.MixedGraphService;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.service.GraphClient;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import java.time.Instant;
import java.util.LinkedHashMap;
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
    private MixedGraphService mixedGraphService;

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
    class Visualization {

        @Test
        void returns200WithNodesAndEdges() throws Exception {
            var projectId = UUID.randomUUID();
            var project = new Project("test", "Test");
            var a = new Requirement(project, "REQ-A", "Title A", "Statement A");
            var b = new Requirement(project, "REQ-B", "Title B", "Statement B");
            setField(a, "id", UUID.randomUUID());
            setField(b, "id", UUID.randomUUID());
            var rel = new RequirementRelation(a, b, RelationType.DEPENDS_ON);
            setField(rel, "id", UUID.randomUUID());

            when(projectService.requireProjectId("test")).thenReturn(projectId);
            when(mixedGraphService.getVisualization(projectId, null))
                    .thenReturn(projection(List.of(a, b), List.of(rel)));

            mockMvc.perform(get("/api/v1/graph/visualization").param("project", "test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nodes", hasSize(2)))
                    .andExpect(jsonPath("$.edges", hasSize(1)))
                    .andExpect(jsonPath("$.totalNodes", is(2)))
                    .andExpect(jsonPath("$.totalEdges", is(1)))
                    .andExpect(jsonPath("$.nodes[0].uid", is("REQ-A")))
                    .andExpect(jsonPath("$.nodes[0].label", is("REQ-A")))
                    .andExpect(jsonPath("$.nodes[0].entityType", is("REQUIREMENT")))
                    .andExpect(jsonPath("$.nodes[0].properties.title", is("Title A")))
                    .andExpect(jsonPath("$.edges[0].edgeType", is("DEPENDS_ON")));
        }

        @Test
        void filtersNodesByEntityType() throws Exception {
            var projectId = UUID.randomUUID();
            var project = new Project("test", "Test");
            var a = new Requirement(project, "REQ-A", "Title A", "Statement A");
            setField(a, "id", UUID.randomUUID());

            when(projectService.requireProjectId("test")).thenReturn(projectId);
            when(mixedGraphService.getVisualization(projectId, List.of("REQUIREMENT")))
                    .thenReturn(projection(List.of(a), List.of()));

            mockMvc.perform(get("/api/v1/graph/visualization")
                            .param("project", "test")
                            .param("entityTypes", "REQUIREMENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nodes", hasSize(1)));

            verify(mixedGraphService).getVisualization(projectId, List.of("REQUIREMENT"));
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
            setField(a, "id", UUID.randomUUID());
            setField(b, "id", UUID.randomUUID());
            var rel = new RequirementRelation(a, b, RelationType.DEPENDS_ON);
            setField(rel, "id", UUID.randomUUID());

            when(projectService.requireProjectId("test")).thenReturn(projectId);
            when(mixedGraphService.extractSubgraph(
                            projectId, List.of(a.getId().toString(), b.getId().toString()), 3, null))
                    .thenReturn(projection(List.of(a, b), List.of(rel)));

            mockMvc.perform(post("/api/v1/graph/subgraph/query")
                            .param("project", "test")
                            .contentType("application/json")
                            .content(
                                    """
                                    {"rootNodeIds":["%s","%s"],"maxDepth":3}
                                    """
                                            .formatted(a.getId(), b.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nodes", hasSize(2)))
                    .andExpect(jsonPath("$.edges", hasSize(1)))
                    .andExpect(jsonPath("$.totalNodes", is(2)))
                    .andExpect(jsonPath("$.totalEdges", is(1)))
                    .andExpect(jsonPath("$.rootNodeIds", hasSize(2)))
                    .andExpect(jsonPath("$.nodes[0].uid", is("REQ-A")))
                    .andExpect(jsonPath("$.edges[0].edgeType", is("DEPENDS_ON")));
        }

        @Test
        void traversesGraph() throws Exception {
            var projectId = UUID.randomUUID();
            var project = new Project("test", "Test");
            var a = new Requirement(project, "REQ-A", "Title A", "Statement A");
            setField(a, "id", UUID.randomUUID());

            when(projectService.requireProjectId("test")).thenReturn(projectId);
            when(mixedGraphService.traverse(projectId, List.of(a.getId().toString()), 2, List.of("REQUIREMENT")))
                    .thenReturn(projection(List.of(a), List.of()));

            mockMvc.perform(post("/api/v1/graph/traversal/query")
                            .param("project", "test")
                            .contentType("application/json")
                            .content(
                                    """
                                    {"rootNodeIds":["%s"],"maxDepth":2,"entityTypes":["REQUIREMENT"]}
                                    """
                                            .formatted(a.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nodes", hasSize(1)));
        }

        @Test
        void findsMixedGraphPaths() throws Exception {
            var projectId = UUID.randomUUID();
            when(projectService.requireProjectId("test")).thenReturn(projectId);
            when(mixedGraphService.findPaths(projectId, "REQUIREMENT:a", "REQUIREMENT:c", 4, List.of("REQUIREMENT")))
                    .thenReturn(List.of(new GraphPathResult(
                            List.of("REQUIREMENT:a", "REQUIREMENT:b", "REQUIREMENT:c"),
                            List.of("DEPENDS_ON", "PARENT"))));

            mockMvc.perform(
                            post("/api/v1/graph/paths/query")
                                    .param("project", "test")
                                    .contentType("application/json")
                                    .content(
                                            """
                                    {
                                      "sourceNodeId":"REQUIREMENT:a",
                                      "targetNodeId":"REQUIREMENT:c",
                                      "maxDepth":4,
                                      "entityTypes":["REQUIREMENT"]
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].nodeIds", hasSize(3)))
                    .andExpect(jsonPath("$[0].edgeTypes", hasSize(2)))
                    .andExpect(jsonPath("$[0].edgeTypes[0]", is("DEPENDS_ON")));
        }
    }

    private GraphProjection projection(List<Requirement> requirements, List<RequirementRelation> relations) {
        var nodes = requirements.stream()
                .map(req -> {
                    var nodeId = req.getId() != null
                            ? req.getId().toString()
                            : UUID.randomUUID().toString();
                    var properties = new LinkedHashMap<String, Object>();
                    properties.put("title", req.getTitle());
                    properties.put("statement", req.getStatement());
                    properties.put("priority", req.getPriority().name());
                    properties.put("status", req.getStatus().name());
                    properties.put("requirementType", req.getRequirementType().name());
                    properties.put("wave", req.getWave());
                    return new GraphNode(
                            nodeId,
                            req.getId().toString(),
                            GraphEntityType.REQUIREMENT,
                            req.getProject().getIdentifier(),
                            req.getUid(),
                            req.getUid(),
                            properties);
                })
                .toList();
        var edges = relations.stream()
                .map(rel -> new GraphEdge(
                        rel.getId() != null
                                ? rel.getId().toString()
                                : UUID.randomUUID().toString(),
                        rel.getRelationType().name(),
                        rel.getSource().getId() != null
                                ? rel.getSource().getId().toString()
                                : UUID.randomUUID().toString(),
                        rel.getTarget().getId() != null
                                ? rel.getTarget().getId().toString()
                                : UUID.randomUUID().toString(),
                        GraphEntityType.REQUIREMENT,
                        GraphEntityType.REQUIREMENT,
                        java.util.Map.of(
                                "sourceUid", rel.getSource().getUid(),
                                "targetUid", rel.getTarget().getUid(),
                                "createdAt", Instant.now())))
                .toList();
        return new GraphProjection(nodes, edges);
    }
}
