package com.keplerops.groundcontrol.unit.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.plugins.PluginController;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.plugins.service.PluginInfo;
import com.keplerops.groundcontrol.domain.plugins.service.PluginRegistry;
import com.keplerops.groundcontrol.domain.plugins.state.PluginLifecycleState;
import com.keplerops.groundcontrol.domain.plugins.state.PluginType;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PluginController.class)
class PluginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PluginRegistry pluginRegistry;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private PluginInfo makePluginInfo(String name, PluginType type, boolean builtin) {
        return new PluginInfo(
                name,
                "1.0.0",
                "Test plugin: " + name,
                type,
                Set.of("test"),
                Map.of(),
                PluginLifecycleState.STARTED,
                true,
                builtin);
    }

    @Test
    void listReturnsAllPlugins() throws Exception {
        when(pluginRegistry.listPlugins())
                .thenReturn(List.of(
                        makePluginInfo("builtin-verifier", PluginType.VERIFIER, true),
                        makePluginInfo("dynamic-handler", PluginType.PACK_HANDLER, false)));

        mockMvc.perform(get("/api/v1/plugins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("builtin-verifier")))
                .andExpect(jsonPath("$[0].builtin", is(true)))
                .andExpect(jsonPath("$[1].name", is("dynamic-handler")))
                .andExpect(jsonPath("$[1].builtin", is(false)));
    }

    @Test
    void listByTypeFilters() throws Exception {
        when(pluginRegistry.listByType(PluginType.VERIFIER))
                .thenReturn(List.of(makePluginInfo("my-verifier", PluginType.VERIFIER, true)));

        mockMvc.perform(get("/api/v1/plugins").param("type", "VERIFIER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("VERIFIER")));
    }

    @Test
    void listByCapabilityFilters() throws Exception {
        when(pluginRegistry.listByCapability("test"))
                .thenReturn(List.of(makePluginInfo("cap-plugin", PluginType.CUSTOM, true)));

        mockMvc.perform(get("/api/v1/plugins").param("capability", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void listByProjectFilters() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(pluginRegistry.listPlugins(PROJECT_ID))
                .thenReturn(List.of(makePluginInfo("project-plugin", PluginType.PACK_HANDLER, false)));

        mockMvc.perform(get("/api/v1/plugins").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("project-plugin")));
    }

    @Test
    void getByNameReturnsPlugin() throws Exception {
        when(pluginRegistry.getPlugin("my-verifier"))
                .thenReturn(makePluginInfo("my-verifier", PluginType.VERIFIER, true));

        mockMvc.perform(get("/api/v1/plugins/{name}", "my-verifier"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("my-verifier")))
                .andExpect(jsonPath("$.type", is("VERIFIER")))
                .andExpect(jsonPath("$.state", is("STARTED")));
    }

    @Test
    void getByNameReturns404ForUnknown() throws Exception {
        when(pluginRegistry.getPlugin("nonexistent")).thenThrow(new NotFoundException("Plugin not found: nonexistent"));

        mockMvc.perform(get("/api/v1/plugins/{name}", "nonexistent")).andExpect(status().isNotFound());
    }

    @Test
    void registerReturns201() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(pluginRegistry.registerPlugin(org.mockito.ArgumentMatchers.any()))
                .thenReturn(makePluginInfo("new-handler", PluginType.PACK_HANDLER, false));

        mockMvc.perform(
                        post("/api/v1/plugins")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"name":"new-handler","version":"1.0.0","type":"PACK_HANDLER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("new-handler")))
                .andExpect(jsonPath("$.type", is("PACK_HANDLER")))
                .andExpect(jsonPath("$.builtin", is(false)));
    }

    @Test
    void registerReturns422WhenNameMissing() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(
                        post("/api/v1/plugins")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"version":"1.0.0","type":"PACK_HANDLER"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unregisterReturns204() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/plugins/{name}", "old-handler").param("project", "ground-control"))
                .andExpect(status().isNoContent());

        verify(pluginRegistry).unregisterPlugin(PROJECT_ID, "old-handler");
    }
}
