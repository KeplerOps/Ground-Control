package com.keplerops.groundcontrol.unit.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.audit.AuditController;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.service.AuditExportService;
import com.keplerops.groundcontrol.domain.requirements.service.AuditService;
import com.keplerops.groundcontrol.domain.requirements.service.TimelineEntry;
import com.keplerops.groundcontrol.domain.requirements.state.ChangeCategory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuditController.class)
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private AuditExportService auditExportService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.randomUUID();

    @Test
    void getProjectTimeline_returns200() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        var entry = new TimelineEntry(
                1,
                "ADD",
                Instant.parse("2026-03-23T12:00:00Z"),
                "alice",
                null,
                ChangeCategory.REQUIREMENT,
                UUID.randomUUID(),
                Map.of(),
                Map.of());
        when(auditService.getProjectTimeline(eq(PROJECT_ID), any(), any(), any(), any(), eq(100), eq(0)))
                .thenReturn(List.of(entry));

        mockMvc.perform(get("/api/v1/audit/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].actor", is("alice")))
                .andExpect(jsonPath("$[0].changeCategory", is("REQUIREMENT")));
    }

    @Test
    void exportTimeline_returnsCsv() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(auditService.getProjectTimeline(eq(PROJECT_ID), any(), any(), any(), any(), eq(10000), eq(0)))
                .thenReturn(List.of());
        when(auditExportService.toCsv(any()))
                .thenReturn("timestamp,actor,reason,change_category,revision_type,entity_id,changes\n");

        mockMvc.perform(get("/api/v1/audit/timeline/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", containsString("audit-timeline.csv")))
                .andExpect(content().string(containsString("timestamp,actor")));
    }
}
