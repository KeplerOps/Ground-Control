package com.keplerops.groundcontrol.unit.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.admin.AdminGraphController;
import com.keplerops.groundcontrol.domain.requirements.service.GraphClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminGraphController.class)
class AdminGraphControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GraphClient graphClient;

    @Nested
    class Materialize {

        @Test
        void returns200() throws Exception {
            mockMvc.perform(post("/api/v1/admin/graph/materialize")).andExpect(status().isOk());

            Mockito.verify(graphClient).materializeGraph();
        }
    }
}
