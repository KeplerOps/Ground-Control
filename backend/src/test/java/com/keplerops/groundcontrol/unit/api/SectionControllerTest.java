package com.keplerops.groundcontrol.unit.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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

import com.keplerops.groundcontrol.api.sections.SectionController;
import com.keplerops.groundcontrol.domain.documents.model.Document;
import com.keplerops.groundcontrol.domain.documents.model.Section;
import com.keplerops.groundcontrol.domain.documents.service.SectionContentService;
import com.keplerops.groundcontrol.domain.documents.service.SectionService;
import com.keplerops.groundcontrol.domain.documents.service.SectionTreeNode;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SectionController.class)
class SectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SectionService sectionService;

    @MockitoBean
    private SectionContentService contentService;

    private static final UUID DOC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SECTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000077");

    @Test
    void createReturns201() throws Exception {
        when(sectionService.create(any())).thenReturn(makeSection());

        mockMvc.perform(post("/api/v1/documents/{docId}/sections", DOC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"title":"Chapter 1","sortOrder":0}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Chapter 1")))
                .andExpect(jsonPath("$.parentId", nullValue()));
    }

    @Test
    void listReturnsSections() throws Exception {
        when(sectionService.listByDocument(DOC_ID)).thenReturn(List.of(makeSection()));

        mockMvc.perform(get("/api/v1/documents/{docId}/sections", DOC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Chapter 1")));
    }

    @Test
    void treeReturnsNestedStructure() throws Exception {
        var node = new SectionTreeNode(
                SECTION_ID,
                null,
                "Chapter 1",
                null,
                0,
                Instant.parse("2026-03-25T06:00:00Z"),
                Instant.parse("2026-03-25T06:00:00Z"),
                List.of());
        when(sectionService.getTree(DOC_ID)).thenReturn(List.of(node));

        mockMvc.perform(get("/api/v1/documents/{docId}/sections/tree", DOC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Chapter 1")))
                .andExpect(jsonPath("$[0].children", hasSize(0)));
    }

    @Test
    void getByIdReturnsSection() throws Exception {
        when(sectionService.getById(SECTION_ID)).thenReturn(makeSection());

        mockMvc.perform(get("/api/v1/sections/{id}", SECTION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Chapter 1")));
    }

    @Test
    void updateReturnsUpdated() throws Exception {
        when(sectionService.update(eq(SECTION_ID), any())).thenReturn(makeSection());

        mockMvc.perform(put("/api/v1/sections/{id}", SECTION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"title":"Chapter 1 Updated"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Chapter 1")));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/sections/{id}", SECTION_ID)).andExpect(status().isNoContent());
        verify(sectionService).delete(SECTION_ID);
    }

    private static Section makeSection() {
        var project = new Project("test", "Test Project");
        setField(project, "id", UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var doc = new Document(project, "SRS", "1.0.0", null, null);
        setField(doc, "id", DOC_ID);
        var section = new Section(doc, null, "Chapter 1", null, 0);
        setField(section, "id", SECTION_ID);
        setField(section, "createdAt", Instant.parse("2026-03-25T06:00:00Z"));
        setField(section, "updatedAt", Instant.parse("2026-03-25T06:00:00Z"));
        return section;
    }

    private static void setField(Object obj, String fieldName, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
