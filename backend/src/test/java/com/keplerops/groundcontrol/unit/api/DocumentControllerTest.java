package com.keplerops.groundcontrol.unit.api;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.documents.DocumentController;
import com.keplerops.groundcontrol.domain.documents.model.Document;
import com.keplerops.groundcontrol.domain.documents.service.DocumentService;
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

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DOC_ID = UUID.fromString("00000000-0000-0000-0000-000000000088");

    @Test
    void createReturns201() throws Exception {
        when(projectService.resolveProjectId("test")).thenReturn(PROJECT_ID);
        when(documentService.create(any())).thenReturn(makeDocument());

        mockMvc.perform(
                        post("/api/v1/documents")
                                .param("project", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"title":"SRS","version":"1.0.0","description":"System Requirements"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("SRS")))
                .andExpect(jsonPath("$.version", is("1.0.0")));
    }

    @Test
    void listReturnsDocuments() throws Exception {
        when(projectService.resolveProjectId("test")).thenReturn(PROJECT_ID);
        when(documentService.listByProject(PROJECT_ID)).thenReturn(List.of(makeDocument()));

        mockMvc.perform(get("/api/v1/documents").param("project", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("SRS")));
    }

    @Test
    void getByIdReturnsDocument() throws Exception {
        when(documentService.getById(DOC_ID)).thenReturn(makeDocument());

        mockMvc.perform(get("/api/v1/documents/{id}", DOC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("SRS")));
    }

    @Test
    void updateReturnsUpdated() throws Exception {
        when(documentService.update(any(), any())).thenReturn(makeDocument());

        mockMvc.perform(put("/api/v1/documents/{id}", DOC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"title":"SRS","version":"2.0.0"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("SRS")));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/documents/{id}", DOC_ID)).andExpect(status().isNoContent());
        verify(documentService).delete(DOC_ID);
    }

    private static Document makeDocument() {
        var project = new Project("test", "Test Project");
        setField(project, "id", PROJECT_ID);
        var doc = new Document(project, "SRS", "1.0.0", "System Requirements", null);
        setField(doc, "id", DOC_ID);
        setField(doc, "createdAt", Instant.parse("2026-03-25T06:00:00Z"));
        setField(doc, "updatedAt", Instant.parse("2026-03-25T06:00:00Z"));
        return doc;
    }
}
