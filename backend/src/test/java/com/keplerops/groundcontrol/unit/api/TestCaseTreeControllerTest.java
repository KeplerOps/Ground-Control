package com.keplerops.groundcontrol.unit.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.testcases.TestCaseTreeController;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.service.TestCaseFolderService;
import com.keplerops.groundcontrol.domain.testcases.service.TestCaseTreeNode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(TestCaseTreeController.class)
class TestCaseTreeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TestCaseFolderService folderService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void getTreeReturnsNestedNodes() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        var folderId = UUID.randomUUID();
        var node =
                new TestCaseTreeNode(TestCaseTreeNode.Kind.FOLDER, folderId, null, "Suite", null, 0, null, List.of());
        when(folderService.getTree(PROJECT_ID)).thenReturn(List.of(node));

        mockMvc.perform(get("/api/v1/test-cases/tree").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].kind", is("FOLDER")))
                .andExpect(jsonPath("$[0].title", is("Suite")));
    }

    @Test
    void getTreeReturnsEmptyArrayWhenNothingExists() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(folderService.getTree(PROJECT_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/test-cases/tree").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
