package com.keplerops.groundcontrol.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class TestCaseFolderControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Map<String, Object> folderRequest(String title) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("title", title);
        request.put("description", "set of regression tests");
        return request;
    }

    private String createRootFolder(String title) throws Exception {
        var result = mockMvc.perform(post("/api/v1/test-cases/folders")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(folderRequest(title))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is(title)))
                .andReturn();
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText();
    }

    @Test
    void create_list_update_move_reorder_delete_roundTrip() throws Exception {
        String folderId = createRootFolder("Smoke");
        String anotherRoot = createRootFolder("Regression");

        // Duplicate sibling title rejected
        mockMvc.perform(post("/api/v1/test-cases/folders")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(folderRequest("Smoke"))))
                .andExpect(status().isConflict());

        // List
        mockMvc.perform(get("/api/v1/test-cases/folders").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Get by id
        mockMvc.perform(get("/api/v1/test-cases/folders/{id}", folderId).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Smoke")));

        // Update title
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("title", "Smoke (renamed)");
        mockMvc.perform(put("/api/v1/test-cases/folders/{id}", folderId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Smoke (renamed)")));

        // Move under sibling
        Map<String, Object> move = new LinkedHashMap<>();
        move.put("parentFolderId", anotherRoot);
        mockMvc.perform(put("/api/v1/test-cases/folders/{id}/move", folderId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(move)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId", is(anotherRoot)));

        // Move under self is rejected (cycle)
        Map<String, Object> selfMove = new LinkedHashMap<>();
        selfMove.put("parentFolderId", folderId);
        mockMvc.perform(put("/api/v1/test-cases/folders/{id}/move", folderId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selfMove)))
                .andExpect(status().isConflict());

        // Delete non-empty parent rejected (has the moved child)
        mockMvc.perform(delete("/api/v1/test-cases/folders/{id}", anotherRoot).param("project", "ground-control"))
                .andExpect(status().isConflict());

        // Move back to root then delete (now empty)
        Map<String, Object> backToRoot = new LinkedHashMap<>();
        backToRoot.put("parentFolderId", null);
        mockMvc.perform(put("/api/v1/test-cases/folders/{id}/move", folderId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(backToRoot)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/test-cases/folders/{id}", anotherRoot).param("project", "ground-control"))
                .andExpect(status().isNoContent());
    }

    @Test
    void reorderRenumbersSiblings() throws Exception {
        String a = createRootFolder("A");
        String b = createRootFolder("B");
        String c = createRootFolder("C");

        Map<String, Object> reorder = new LinkedHashMap<>();
        reorder.put("parentFolderId", null);
        reorder.put("orderedFolderIds", List.of(c, a, b));

        mockMvc.perform(put("/api/v1/test-cases/folders/reorder")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reorder)))
                .andExpect(status().isNoContent());

        var listResult = mockMvc.perform(get("/api/v1/test-cases/folders").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andReturn();
        // Folders come back ordered by sort_order; verify the renumbering took.
        var json = objectMapper.readTree(listResult.getResponse().getContentAsString());
        // Find each folder by id and confirm its sortOrder.
        int sortA = -1;
        int sortB = -1;
        int sortC = -1;
        for (var node : json) {
            String id = node.get("id").asText();
            int order = node.get("sortOrder").asInt();
            if (id.equals(a)) {
                sortA = order;
            } else if (id.equals(b)) {
                sortB = order;
            } else if (id.equals(c)) {
                sortC = order;
            }
        }
        org.assertj.core.api.Assertions.assertThat(sortC).isZero();
        org.assertj.core.api.Assertions.assertThat(sortA).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(sortB).isEqualTo(2);
    }

    @Test
    void deletionRequiresEmptyFolder() throws Exception {
        String folderId = createRootFolder("BucketA");

        // Place a test case inside, then attempt deletion.
        Map<String, Object> testCase = new LinkedHashMap<>();
        testCase.put("uid", "TC-FOLD-001");
        testCase.put("title", "inside folder");
        testCase.put("type", "MANUAL");
        testCase.put("priority", "LOW");
        testCase.put("parentFolderId", folderId);
        mockMvc.perform(post("/api/v1/test-cases")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCase)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentFolderId", is(folderId)));

        mockMvc.perform(delete("/api/v1/test-cases/folders/{id}", folderId).param("project", "ground-control"))
                .andExpect(status().isConflict());
    }

    @Test
    void treeIncludesFoldersAndTestCases() throws Exception {
        String root = createRootFolder("Top");

        Map<String, Object> child = new LinkedHashMap<>();
        child.put("parentFolderId", root);
        child.put("title", "Nested");
        mockMvc.perform(post("/api/v1/test-cases/folders")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(child)))
                .andExpect(status().isCreated());

        Map<String, Object> tc = new LinkedHashMap<>();
        tc.put("uid", "TC-TREE-001");
        tc.put("title", "leaf");
        tc.put("type", "MANUAL");
        tc.put("priority", "LOW");
        tc.put("parentFolderId", root);
        mockMvc.perform(post("/api/v1/test-cases")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tc)))
                .andExpect(status().isCreated());

        // Assert child kind and title at each position so any inversion of
        // the documented folders-before-test-cases ordering fails the test.
        mockMvc.perform(get("/api/v1/test-cases/tree").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind", is("FOLDER")))
                .andExpect(jsonPath("$[0].title", is("Top")))
                .andExpect(jsonPath("$[0].children", hasSize(2)))
                .andExpect(jsonPath("$[0].children[0].kind", is("FOLDER")))
                .andExpect(jsonPath("$[0].children[0].title", is("Nested")))
                .andExpect(jsonPath("$[0].children[1].kind", is("TEST_CASE")))
                .andExpect(jsonPath("$[0].children[1].title", is("leaf")));
    }
}
