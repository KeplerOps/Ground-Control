package com.keplerops.groundcontrol.unit.api;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.admin.ImportController;
import com.keplerops.groundcontrol.domain.requirements.service.ImportResult;
import com.keplerops.groundcontrol.domain.requirements.service.ImportService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ImportController.class)
class ImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportService importService;

    @Nested
    class ImportStrictdoc {

        @Test
        void returns200WithStats() throws Exception {
            var result = new ImportResult(UUID.randomUUID(), Instant.now(), 10, 8, 2, 5, 1, 12, 3, List.of());

            when(importService.importStrictdoc(anyString(), anyString())).thenReturn(result);

            var file = new MockMultipartFile(
                    "file", "project.sdoc", "text/plain", "sdoc content".getBytes(StandardCharsets.UTF_8));

            mockMvc.perform(multipart("/api/v1/admin/import/strictdoc").file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.importId", notNullValue()))
                    .andExpect(jsonPath("$.requirementsParsed", is(10)))
                    .andExpect(jsonPath("$.requirementsCreated", is(8)))
                    .andExpect(jsonPath("$.requirementsUpdated", is(2)))
                    .andExpect(jsonPath("$.relationsCreated", is(5)))
                    .andExpect(jsonPath("$.relationsSkipped", is(1)))
                    .andExpect(jsonPath("$.traceabilityLinksCreated", is(12)))
                    .andExpect(jsonPath("$.traceabilityLinksSkipped", is(3)));
        }

        @Test
        void withNoFile_returns400() throws Exception {
            mockMvc.perform(multipart("/api/v1/admin/import/strictdoc")).andExpect(status().isBadRequest());
        }
    }
}
