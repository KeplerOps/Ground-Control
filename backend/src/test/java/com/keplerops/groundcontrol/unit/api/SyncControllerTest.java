package com.keplerops.groundcontrol.unit.api;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.admin.SyncController;
import com.keplerops.groundcontrol.domain.requirements.service.GitHubIssueSyncService;
import com.keplerops.groundcontrol.domain.requirements.service.SyncResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SyncController.class)
class SyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GitHubIssueSyncService syncService;

    @Nested
    class SyncGithub {

        @Test
        void returns200WithStats() throws Exception {
            var result = new SyncResult(UUID.randomUUID(), Instant.now(), 50, 45, 5, 12, List.of());

            when(syncService.syncGitHubIssues(anyString(), anyString())).thenReturn(result);

            mockMvc.perform(post("/api/v1/admin/sync/github")
                            .param("owner", "KeplerOps")
                            .param("repo", "Ground-Control"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.syncId", notNullValue()))
                    .andExpect(jsonPath("$.issuesFetched", is(50)))
                    .andExpect(jsonPath("$.issuesCreated", is(45)))
                    .andExpect(jsonPath("$.issuesUpdated", is(5)))
                    .andExpect(jsonPath("$.linksUpdated", is(12)));
        }

        @Test
        void withMissingOwner_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/admin/sync/github").param("repo", "Ground-Control"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void withMissingRepo_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/admin/sync/github").param("owner", "KeplerOps"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void withMaliciousOwner_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/admin/sync/github")
                            .param("owner", "$(whoami)")
                            .param("repo", "Ground-Control"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void withMaliciousRepo_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/admin/sync/github")
                            .param("owner", "KeplerOps")
                            .param("repo", "repo;rm -rf /"))
                    .andExpect(status().isBadRequest());
        }
    }
}
