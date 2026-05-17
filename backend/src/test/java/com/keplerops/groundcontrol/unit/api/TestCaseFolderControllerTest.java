package com.keplerops.groundcontrol.unit.api;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.testcases.TestCaseFolderController;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.model.TestCaseFolder;
import com.keplerops.groundcontrol.domain.testcases.service.MoveTestCaseFolderCommand;
import com.keplerops.groundcontrol.domain.testcases.service.ReorderTestCaseFoldersCommand;
import com.keplerops.groundcontrol.domain.testcases.service.TestCaseFolderService;
import com.keplerops.groundcontrol.domain.testcases.service.UpdateTestCaseFolderCommand;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(TestCaseFolderController.class)
class TestCaseFolderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TestCaseFolderService folderService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FOLDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000900");
    private static final Instant NOW = Instant.parse("2026-05-17T12:00:00Z");

    private TestCaseFolder makeFolder() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var folder = new TestCaseFolder(project, null, "Smoke tests", "set of smoke tests", 0);
        setField(folder, "id", FOLDER_ID);
        setField(folder, "createdAt", NOW);
        setField(folder, "updatedAt", NOW);
        return folder;
    }

    @Test
    void createReturns201() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(folderService.create(any())).thenReturn(makeFolder());

        mockMvc.perform(
                        post("/api/v1/test-cases/folders")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "title": "Smoke tests",
                                  "description": "set of smoke tests"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Smoke tests")));
    }

    @Test
    void createReturns422WhenTitleBlank() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(post("/api/v1/test-cases/folders")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(folderService);
    }

    @Test
    void listReturnsFolders() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(folderService.listByProject(PROJECT_ID)).thenReturn(List.of(makeFolder()));

        mockMvc.perform(get("/api/v1/test-cases/folders").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Smoke tests")));
    }

    @Test
    void getByIdReturnsFolder() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(folderService.getById(PROJECT_ID, FOLDER_ID)).thenReturn(makeFolder());

        mockMvc.perform(get("/api/v1/test-cases/folders/{id}", FOLDER_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Smoke tests")));
    }

    @Test
    void updateAcceptsBodyAndReturnsUpdatedFolder() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(folderService.update(eq(PROJECT_ID), eq(FOLDER_ID), any())).thenReturn(makeFolder());

        mockMvc.perform(put("/api/v1/test-cases/folders/{id}", FOLDER_ID)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Renamed\"}"))
                .andExpect(status().isOk());
        // Capture the command so a regression in body binding (wrong field
        // name, null-coalesced title) is caught at the unit level instead
        // of silently passing through with any().
        ArgumentCaptor<UpdateTestCaseFolderCommand> captor = ArgumentCaptor.forClass(UpdateTestCaseFolderCommand.class);
        verify(folderService).update(eq(PROJECT_ID), eq(FOLDER_ID), captor.capture());
        assertThat(captor.getValue().title()).isEqualTo("Renamed");
    }

    @Test
    void deleteReturns204() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/test-cases/folders/{id}", FOLDER_ID).param("project", "ground-control"))
                .andExpect(status().isNoContent());
        verify(folderService).delete(PROJECT_ID, FOLDER_ID);
    }

    @Test
    void moveAcceptsParentBody() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(folderService.move(eq(PROJECT_ID), eq(FOLDER_ID), any(MoveTestCaseFolderCommand.class)))
                .thenReturn(makeFolder());

        UUID newParent = UUID.randomUUID();
        mockMvc.perform(put("/api/v1/test-cases/folders/{id}/move", FOLDER_ID)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentFolderId\":\"" + newParent + "\",\"sortOrder\":2}"))
                .andExpect(status().isOk());
        // Capture so a body-binding regression that drops parentFolderId or
        // sortOrder is caught at the unit level (test-quality cycle 1).
        ArgumentCaptor<MoveTestCaseFolderCommand> captor = ArgumentCaptor.forClass(MoveTestCaseFolderCommand.class);
        verify(folderService).move(eq(PROJECT_ID), eq(FOLDER_ID), captor.capture());
        assertThat(captor.getValue().parentFolderId()).isEqualTo(newParent);
        assertThat(captor.getValue().sortOrder()).isEqualTo(2);
    }

    @Test
    void reorderAcceptsOrderedIds() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/test-cases/folders/reorder")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentFolderId\":null,\"orderedFolderIds\":[\"" + a + "\",\"" + b + "\"]}"))
                .andExpect(status().isNoContent());
        // Capture: a wrong DTO field name would deserialize orderedFolderIds
        // to an empty list and silently no-op the reorder; the previous
        // any()-based verify wouldn't catch that (test-quality cycle 1).
        ArgumentCaptor<ReorderTestCaseFoldersCommand> captor =
                ArgumentCaptor.forClass(ReorderTestCaseFoldersCommand.class);
        verify(folderService).reorder(eq(PROJECT_ID), captor.capture());
        assertThat(captor.getValue().parentFolderId()).isNull();
        assertThat(captor.getValue().orderedFolderIds()).containsExactly(a, b);
    }
}
