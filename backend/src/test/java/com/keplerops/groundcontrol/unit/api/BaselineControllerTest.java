package com.keplerops.groundcontrol.unit.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.api.baselines.BaselineController;
import com.keplerops.groundcontrol.domain.baselines.model.Baseline;
import com.keplerops.groundcontrol.domain.baselines.service.BaselineComparison;
import com.keplerops.groundcontrol.domain.baselines.service.BaselineService;
import com.keplerops.groundcontrol.domain.baselines.service.BaselineSnapshot;
import com.keplerops.groundcontrol.domain.baselines.service.CreateBaselineCommand;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BaselineController.class)
class BaselineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BaselineService baselineService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BASELINE_ID = UUID.fromString("b0000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_BASELINE_ID = UUID.fromString("b0000000-0000-0000-0000-000000000002");
    private static final Project TEST_PROJECT = createTestProject();

    private static Project createTestProject() {
        var project = new Project("test-project", "Test Project");
        setField(project, "id", PROJECT_ID);
        return project;
    }

    @BeforeEach
    void setUp() {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
    }

    private static Baseline createBaseline(UUID id, String name, int revisionNumber) {
        var baseline = new Baseline(TEST_PROJECT, name, "Description for " + name, revisionNumber, "test-actor");
        setField(baseline, "id", id);
        setField(baseline, "createdAt", Instant.now());
        return baseline;
    }

    private static Requirement createRequirement(String uid) {
        var req = new Requirement(TEST_PROJECT, uid, "Title for " + uid, "Statement for " + uid);
        setField(req, "id", UUID.randomUUID());
        setField(req, "createdAt", Instant.now());
        setField(req, "updatedAt", Instant.now());
        return req;
    }

    private static void setField(Object obj, String fieldName, Object value) {
        com.keplerops.groundcontrol.TestUtil.setField(obj, fieldName, value);
    }

    @Nested
    class Create {

        @Test
        void returns201() throws Exception {
            var baseline = createBaseline(BASELINE_ID, "v1.0", 42);
            when(baselineService.create(any(CreateBaselineCommand.class))).thenReturn(baseline);

            mockMvc.perform(post("/api/v1/baselines")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("name", "v1.0", "description", "Release"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.name", is("v1.0")))
                    .andExpect(jsonPath("$.revisionNumber", is(42)));
        }

        @Test
        void blankName_returns422() throws Exception {
            mockMvc.perform(post("/api/v1/baselines")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("name", ""))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.code", is("validation_error")));
        }

        @Test
        void duplicateName_returns409() throws Exception {
            when(baselineService.create(any(CreateBaselineCommand.class)))
                    .thenThrow(new ConflictException("Already exists"));

            mockMvc.perform(post("/api/v1/baselines")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("name", "v1.0"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code", is("conflict")));
        }
    }

    @Nested
    class ListBaselines {

        @Test
        void returns200() throws Exception {
            var b1 = createBaseline(BASELINE_ID, "v1.0", 10);
            var b2 = createBaseline(OTHER_BASELINE_ID, "v2.0", 20);
            when(baselineService.listByProject(PROJECT_ID)).thenReturn(List.of(b2, b1));

            mockMvc.perform(get("/api/v1/baselines"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name", is("v2.0")));
        }
    }

    @Nested
    class GetById {

        @Test
        void returns200() throws Exception {
            var baseline = createBaseline(BASELINE_ID, "v1.0", 42);
            when(baselineService.getById(BASELINE_ID)).thenReturn(baseline);

            mockMvc.perform(get("/api/v1/baselines/" + BASELINE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("v1.0")));
        }

        @Test
        void notFound_returns404() throws Exception {
            when(baselineService.getById(BASELINE_ID)).thenThrow(new NotFoundException("Not found"));

            mockMvc.perform(get("/api/v1/baselines/" + BASELINE_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code", is("not_found")));
        }
    }

    @Nested
    class GetSnapshot {

        @Test
        void returns200() throws Exception {
            var req = createRequirement("REQ-001");
            var snapshot = new BaselineSnapshot(BASELINE_ID, "v1.0", 42, Instant.now(), List.of(req));
            when(baselineService.getSnapshot(BASELINE_ID)).thenReturn(snapshot);

            mockMvc.perform(get("/api/v1/baselines/" + BASELINE_ID + "/snapshot"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("v1.0")))
                    .andExpect(jsonPath("$.requirementCount", is(1)))
                    .andExpect(jsonPath("$.requirements[0].uid", is("REQ-001")));
        }
    }

    @Nested
    class Compare {

        @Test
        void returns200() throws Exception {
            var addedReq = createRequirement("REQ-NEW");
            var comparison = new BaselineComparison(
                    BASELINE_ID, "v1.0", OTHER_BASELINE_ID, "v2.0", List.of(addedReq), List.of(), List.of());
            when(baselineService.compare(BASELINE_ID, OTHER_BASELINE_ID)).thenReturn(comparison);

            mockMvc.perform(get("/api/v1/baselines/" + BASELINE_ID + "/compare/" + OTHER_BASELINE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.baselineName", is("v1.0")))
                    .andExpect(jsonPath("$.otherBaselineName", is("v2.0")))
                    .andExpect(jsonPath("$.addedCount", is(1)))
                    .andExpect(jsonPath("$.removedCount", is(0)))
                    .andExpect(jsonPath("$.modifiedCount", is(0)));
        }
    }

    @Nested
    class DeleteBaseline {

        @Test
        void returns204() throws Exception {
            doNothing().when(baselineService).delete(BASELINE_ID);

            mockMvc.perform(delete("/api/v1/baselines/" + BASELINE_ID)).andExpect(status().isNoContent());
        }
    }
}
