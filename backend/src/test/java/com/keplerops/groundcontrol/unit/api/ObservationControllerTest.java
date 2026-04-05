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

import com.keplerops.groundcontrol.api.assets.ObservationController;
import com.keplerops.groundcontrol.domain.assets.model.Observation;
import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import com.keplerops.groundcontrol.domain.assets.service.ObservationService;
import com.keplerops.groundcontrol.domain.assets.state.ObservationCategory;
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

@WebMvcTest(ObservationController.class)
class ObservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ObservationService observationService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ASSET_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID OBS_ID = UUID.fromString("00000000-0000-0000-0000-000000000200");
    private static final Instant NOW = Instant.parse("2026-04-01T12:00:00Z");

    private OperationalAsset makeAsset() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var asset = new OperationalAsset(project, "WEB-001", "Web Server");
        setField(asset, "id", ASSET_ID);
        setField(asset, "createdAt", NOW);
        setField(asset, "updatedAt", NOW);
        return asset;
    }

    private Observation makeObservation() {
        var asset = makeAsset();
        var obs = new Observation(
                asset, ObservationCategory.CONFIGURATION, "os_version", "Ubuntu 22.04", "scanner-agent", NOW);
        obs.setExpiresAt(NOW.plusSeconds(86400));
        obs.setConfidence("HIGH");
        obs.setEvidenceRef("https://evidence.example.com/scan/123");
        setField(obs, "id", OBS_ID);
        setField(obs, "createdAt", NOW);
        setField(obs, "updatedAt", NOW);
        return obs;
    }

    @Test
    void createReturns201() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(observationService.create(eq(PROJECT_ID), eq(ASSET_ID), any())).thenReturn(makeObservation());

        mockMvc.perform(
                        post("/api/v1/assets/{assetId}/observations", ASSET_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                        {
                            "category": "CONFIGURATION",
                            "observationKey": "os_version",
                            "observationValue": "Ubuntu 22.04",
                            "source": "scanner-agent",
                            "observedAt": "2026-04-01T12:00:00Z",
                            "expiresAt": "2026-04-02T12:00:00Z",
                            "confidence": "HIGH",
                            "evidenceRef": "https://evidence.example.com/scan/123"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(OBS_ID.toString())))
                .andExpect(jsonPath("$.category", is("CONFIGURATION")))
                .andExpect(jsonPath("$.observationKey", is("os_version")))
                .andExpect(jsonPath("$.observationValue", is("Ubuntu 22.04")))
                .andExpect(jsonPath("$.source", is("scanner-agent")))
                .andExpect(jsonPath("$.confidence", is("HIGH")))
                .andExpect(jsonPath("$.evidenceRef", is("https://evidence.example.com/scan/123")));
    }

    @Test
    void createReturns422WhenCategoryMissing() throws Exception {
        mockMvc.perform(
                        post("/api/v1/assets/{assetId}/observations", ASSET_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                        {
                            "observationKey": "os_version",
                            "observationValue": "Ubuntu 22.04",
                            "source": "scanner-agent",
                            "observedAt": "2026-04-01T12:00:00Z"
                        }
                        """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listReturnsObservations() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(observationService.listByAsset(PROJECT_ID, ASSET_ID, null, null)).thenReturn(List.of(makeObservation()));

        mockMvc.perform(get("/api/v1/assets/{assetId}/observations", ASSET_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].observationKey", is("os_version")));
    }

    @Test
    void listFiltersByCategory() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(observationService.listByAsset(PROJECT_ID, ASSET_ID, ObservationCategory.CONFIGURATION, null))
                .thenReturn(List.of(makeObservation()));

        mockMvc.perform(get("/api/v1/assets/{assetId}/observations", ASSET_ID)
                        .param("project", "ground-control")
                        .param("category", "CONFIGURATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void listFiltersByKey() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(observationService.listByAsset(PROJECT_ID, ASSET_ID, null, "os_version"))
                .thenReturn(List.of(makeObservation()));

        mockMvc.perform(get("/api/v1/assets/{assetId}/observations", ASSET_ID)
                        .param("project", "ground-control")
                        .param("key", "os_version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getByIdReturnsObservation() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(observationService.getById(PROJECT_ID, ASSET_ID, OBS_ID)).thenReturn(makeObservation());

        mockMvc.perform(get("/api/v1/assets/{assetId}/observations/{observationId}", ASSET_ID, OBS_ID)
                        .param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(OBS_ID.toString())))
                .andExpect(jsonPath("$.assetId", is(ASSET_ID.toString())))
                .andExpect(jsonPath("$.assetUid", is("WEB-001")));
    }

    @Test
    void updateReturnsUpdatedObservation() throws Exception {
        var updated = makeObservation();
        updated.setObservationValue("Ubuntu 24.04");
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(observationService.update(eq(PROJECT_ID), eq(ASSET_ID), eq(OBS_ID), any()))
                .thenReturn(updated);

        mockMvc.perform(
                        put("/api/v1/assets/{assetId}/observations/{observationId}", ASSET_ID, OBS_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                        {
                            "observationValue": "Ubuntu 24.04"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observationValue", is("Ubuntu 24.04")));
    }

    @Test
    void deleteReturns204() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/assets/{assetId}/observations/{observationId}", ASSET_ID, OBS_ID)
                        .param("project", "ground-control"))
                .andExpect(status().isNoContent());

        verify(observationService).delete(PROJECT_ID, ASSET_ID, OBS_ID);
    }

    @Test
    void listLatestReturnsLatestObservations() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(observationService.listLatest(PROJECT_ID, ASSET_ID)).thenReturn(List.of(makeObservation()));

        mockMvc.perform(get("/api/v1/assets/{assetId}/observations/latest", ASSET_ID)
                        .param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].observationKey", is("os_version")));
    }
}
