package com.keplerops.groundcontrol.unit.infrastructure;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.infrastructure.web.SpaController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SpaController.class)
class SpaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void clientRoute_forwardsToIndexHtml() throws Exception {
        mockMvc.perform(get("/requirements")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void nestedClientRoute_forwardsToIndexHtml() throws Exception {
        mockMvc.perform(get("/graph")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void twoSegmentRoute_forwardsToIndexHtml() throws Exception {
        mockMvc.perform(get("/p/aptl")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void threeSegmentRoute_forwardsToIndexHtml() throws Exception {
        mockMvc.perform(get("/p/aptl/graph")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void fourSegmentRoute_forwardsToIndexHtml() throws Exception {
        mockMvc.perform(get("/p/aptl/requirements/some-id"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }
}
