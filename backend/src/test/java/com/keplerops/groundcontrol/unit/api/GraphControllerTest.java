package com.keplerops.groundcontrol.unit.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.admin.GraphController;
import com.keplerops.groundcontrol.domain.requirements.service.GraphClient;
import java.util.List;
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
        void returns200() throws Exception {
            when(graphClient.findPaths(anyString(), anyString()))
                    .thenReturn(List.of(List.of("REQ-A", "REQ-B", "REQ-C")));

            mockMvc.perform(get("/api/v1/graph/paths").param("source", "REQ-A").param("target", "REQ-C"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0]", hasSize(3)));
        }
    }
}
