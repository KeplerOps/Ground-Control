package com.keplerops.groundcontrol.unit.api;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.admin.GitHubIssueController;
import com.keplerops.groundcontrol.domain.requirements.service.CreateGitHubIssueCommand;
import com.keplerops.groundcontrol.domain.requirements.service.CreateGitHubIssueResult;
import com.keplerops.groundcontrol.domain.requirements.service.GitHubIssueSyncService;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GitHubIssueController.class)
class GitHubIssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GitHubIssueSyncService syncService;

    @Nested
    class CreateIssue {

        @Test
        void returns201WithIssueData() throws Exception {
            var result = new CreateGitHubIssueResult("https://github.com/o/r/issues/42", 42, UUID.randomUUID(), null);

            when(syncService.createIssueFromRequirement(any(CreateGitHubIssueCommand.class)))
                    .thenReturn(result);

            mockMvc.perform(
                            post("/api/v1/admin/github/issues")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {
                                      "requirementUid": "GC-A001",
                                      "repo": "KeplerOps/Ground-Control",
                                      "labels": ["enhancement"]
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.issueUrl", is("https://github.com/o/r/issues/42")))
                    .andExpect(jsonPath("$.issueNumber", is(42)))
                    .andExpect(jsonPath("$.warning", nullValue()));
        }

        @Test
        void blankUid_returns422() throws Exception {
            mockMvc.perform(
                            post("/api/v1/admin/github/issues")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                    {
                                      "requirementUid": "",
                                      "repo": "KeplerOps/Ground-Control"
                                    }
                                    """))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
