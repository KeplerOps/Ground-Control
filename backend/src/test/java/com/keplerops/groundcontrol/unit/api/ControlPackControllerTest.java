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

import com.keplerops.groundcontrol.api.controlpacks.ControlPackController;
import com.keplerops.groundcontrol.domain.controlpacks.model.ControlPack;
import com.keplerops.groundcontrol.domain.controlpacks.model.ControlPackEntry;
import com.keplerops.groundcontrol.domain.controlpacks.model.ControlPackOverride;
import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackInstallResult;
import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackService;
import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackUpgradeResult;
import com.keplerops.groundcontrol.domain.controlpacks.state.ControlPackLifecycleState;
import com.keplerops.groundcontrol.domain.controls.model.Control;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ControlPackController.class)
class ControlPackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ControlPackService controlPackService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PACK_ID = UUID.fromString("00000000-0000-0000-0000-000000000050");
    private static final UUID ENTRY_ID = UUID.fromString("00000000-0000-0000-0000-000000000051");
    private static final UUID CONTROL_ID = UUID.fromString("00000000-0000-0000-0000-000000000052");
    private static final UUID OVERRIDE_ID = UUID.fromString("00000000-0000-0000-0000-000000000053");
    private static final Instant NOW = Instant.now();

    private Project makeProject() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        return project;
    }

    private ControlPack makePack() {
        var pack = new ControlPack(makeProject(), "nist-sp800-53", "1.0.0");
        setField(pack, "id", PACK_ID);
        setField(pack, "createdAt", NOW);
        setField(pack, "updatedAt", NOW);
        pack.setPublisher("NIST");
        pack.setDescription("NIST SP 800-53 Rev 5");
        return pack;
    }

    private Control makeControl() {
        var control = new Control(makeProject(), "AC-1", "Access Control Policy", ControlFunction.PREVENTIVE);
        setField(control, "id", CONTROL_ID);
        setField(control, "createdAt", NOW);
        setField(control, "updatedAt", NOW);
        return control;
    }

    private ControlPackEntry makeEntry() {
        var entry = new ControlPackEntry(makePack(), makeControl(), "AC-1");
        setField(entry, "id", ENTRY_ID);
        setField(entry, "createdAt", NOW);
        setField(entry, "updatedAt", NOW);
        entry.setOriginalDefinition(Map.of("uid", "AC-1", "title", "Access Control Policy"));
        return entry;
    }

    private ControlPackOverride makeOverride() {
        var override = new ControlPackOverride(makeEntry(), "title", "Custom Title", "Local policy");
        setField(override, "id", OVERRIDE_ID);
        setField(override, "createdAt", NOW);
        setField(override, "updatedAt", NOW);
        return override;
    }

    @Test
    void installReturns201() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        var result = new ControlPackInstallResult(makePack(), 2, 0, 2, 1, 0, false);
        when(controlPackService.install(any())).thenReturn(result);

        mockMvc.perform(
                        post("/api/v1/control-packs/install")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"packId":"nist-sp800-53","version":"1.0.0","publisher":"NIST",
                 "entries":[{"uid":"AC-1","title":"Access Control Policy","controlFunction":"PREVENTIVE"}]}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.controlPack.packId", is("nist-sp800-53")))
                .andExpect(jsonPath("$.controlsCreated", is(2)))
                .andExpect(jsonPath("$.wasIdempotent", is(false)));
    }

    @Test
    void upgradeReturns200() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        var pack = makePack();
        pack.setVersion("2.0.0");
        setField(pack, "lifecycleState", ControlPackLifecycleState.UPGRADED);
        var result = new ControlPackUpgradeResult(pack, "1.0.0", 1, 1, 0, 1, 1, 0);
        when(controlPackService.upgrade(any())).thenReturn(result);

        mockMvc.perform(
                        post("/api/v1/control-packs/upgrade")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"packId":"nist-sp800-53","newVersion":"2.0.0",
                 "entries":[{"uid":"AC-1","title":"Access Control Policy v2","controlFunction":"PREVENTIVE"}]}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controlPack.version", is("2.0.0")))
                .andExpect(jsonPath("$.previousVersion", is("1.0.0")));
    }

    @Test
    void listReturnsPacks() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(controlPackService.listByProject(PROJECT_ID)).thenReturn(List.of(makePack()));

        mockMvc.perform(get("/api/v1/control-packs").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].packId", is("nist-sp800-53")));
    }

    @Test
    void getByPackIdReturnsPack() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(controlPackService.getByPackId(PROJECT_ID, "nist-sp800-53")).thenReturn(makePack());

        mockMvc.perform(get("/api/v1/control-packs/{packId}", "nist-sp800-53").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packId", is("nist-sp800-53")))
                .andExpect(jsonPath("$.version", is("1.0.0")));
    }

    @Test
    void deprecateReturnsPack() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        var deprecated = makePack();
        setField(deprecated, "lifecycleState", ControlPackLifecycleState.DEPRECATED);
        when(controlPackService.deprecate(PROJECT_ID, "nist-sp800-53")).thenReturn(deprecated);

        mockMvc.perform(put("/api/v1/control-packs/{packId}/deprecate", "nist-sp800-53")
                        .param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifecycleState", is("DEPRECATED")));
    }

    @Test
    void removeReturns204() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/control-packs/{packId}", "nist-sp800-53")
                        .param("project", "ground-control"))
                .andExpect(status().isNoContent());

        verify(controlPackService).remove(PROJECT_ID, "nist-sp800-53");
    }

    @Test
    void listEntriesReturnsEntries() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(controlPackService.listEntries(PROJECT_ID, "nist-sp800-53")).thenReturn(List.of(makeEntry()));

        mockMvc.perform(get("/api/v1/control-packs/{packId}/entries", "nist-sp800-53")
                        .param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].entryUid", is("AC-1")));
    }

    @Test
    void getEntryReturnsEntry() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(controlPackService.getEntry(PROJECT_ID, "nist-sp800-53", "AC-1")).thenReturn(makeEntry());

        mockMvc.perform(get("/api/v1/control-packs/{packId}/entries/{entryUid}", "nist-sp800-53", "AC-1")
                        .param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryUid", is("AC-1")));
    }

    @Test
    void createOverrideReturns201() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(controlPackService.createOverride(eq(PROJECT_ID), eq("nist-sp800-53"), eq("AC-1"), any()))
                .thenReturn(makeOverride());

        mockMvc.perform(
                        post("/api/v1/control-packs/{packId}/entries/{entryUid}/overrides", "nist-sp800-53", "AC-1")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"fieldName":"title","overrideValue":"Custom Title","reason":"Local policy"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fieldName", is("title")))
                .andExpect(jsonPath("$.overrideValue", is("Custom Title")));
    }

    @Test
    void listOverridesReturnsOverrides() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(controlPackService.listOverrides(PROJECT_ID, "nist-sp800-53", "AC-1"))
                .thenReturn(List.of(makeOverride()));

        mockMvc.perform(get("/api/v1/control-packs/{packId}/entries/{entryUid}/overrides", "nist-sp800-53", "AC-1")
                        .param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fieldName", is("title")));
    }

    @Test
    void deleteOverrideReturns204() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete(
                                "/api/v1/control-packs/{packId}/entries/{entryUid}/overrides/{overrideId}",
                                "nist-sp800-53",
                                "AC-1",
                                OVERRIDE_ID)
                        .param("project", "ground-control"))
                .andExpect(status().isNoContent());

        verify(controlPackService).deleteOverride(PROJECT_ID, "nist-sp800-53", "AC-1", OVERRIDE_ID);
    }
    // Review fixes: CreateControlPackOverrideCommand updated, enum validation added
}
