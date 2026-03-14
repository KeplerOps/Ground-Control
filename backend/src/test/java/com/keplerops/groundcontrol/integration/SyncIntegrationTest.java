package com.keplerops.groundcontrol.integration;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import com.keplerops.groundcontrol.domain.requirements.repository.GitHubIssueSyncRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.requirements.service.GitHubClient;
import com.keplerops.groundcontrol.domain.requirements.service.GitHubIssueData;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class SyncIntegrationTest extends BaseIntegrationTest {

    @TestConfiguration
    static class MockGitHubClientConfig {

        @Bean
        @Primary
        GitHubClient mockGitHubClient() {
            return (owner, repo) -> List.of(
                    new GitHubIssueData(
                            1,
                            "Setup CI",
                            "CLOSED",
                            "https://github.com/test/repo/issues/1",
                            "Initial CI setup. See #2",
                            List.of("phase-0", "P0")),
                    new GitHubIssueData(
                            2,
                            "Add linting",
                            "OPEN",
                            "https://github.com/test/repo/issues/2",
                            "Add linting tools",
                            List.of("phase-0", "P1", "enhancement")));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GitHubIssueSyncRepository issueSyncRepository;

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private TraceabilityLinkRepository traceabilityLinkRepository;

    @Test
    void syncGithubIssues_createsIssueSyncRecords() throws Exception {
        mockMvc.perform(post("/api/v1/admin/sync/github").param("owner", "test").param("repo", "repo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncId", notNullValue()))
                .andExpect(jsonPath("$.issuesFetched", is(2)))
                .andExpect(jsonPath("$.issuesCreated", is(2)))
                .andExpect(jsonPath("$.issuesUpdated", is(0)));

        var sync1 = issueSyncRepository.findByIssueNumber(1);
        assert sync1.isPresent();
        assert sync1.get().getIssueTitle().equals("Setup CI");
        assert sync1.get().getPhase() == 0;
        assert sync1.get().getPriorityLabel().equals("P0");
        assert sync1.get().getCrossReferences().contains(2);
    }

    @Test
    void syncGithubIssues_isIdempotent() throws Exception {
        mockMvc.perform(post("/api/v1/admin/sync/github").param("owner", "test").param("repo", "repo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuesCreated", is(2)));

        mockMvc.perform(post("/api/v1/admin/sync/github").param("owner", "test").param("repo", "repo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuesCreated", is(0)))
                .andExpect(jsonPath("$.issuesUpdated", is(2)));
    }

    @Test
    void syncGithubIssues_updatesTraceabilityLinks() throws Exception {
        // Pre-create a requirement with a traceability link to issue #1
        var requirement = new Requirement("SYNC-REQ-001", "Test requirement", "Statement");
        requirement = requirementRepository.save(requirement);

        var link = new TraceabilityLink(requirement, ArtifactType.GITHUB_ISSUE, "1", LinkType.IMPLEMENTS);
        traceabilityLinkRepository.save(link);

        mockMvc.perform(post("/api/v1/admin/sync/github").param("owner", "test").param("repo", "repo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linksUpdated", is(1)));

        var updatedLinks = traceabilityLinkRepository.findByRequirementId(requirement.getId());
        assert updatedLinks.size() == 1;
        assert updatedLinks.get(0).getArtifactUrl().equals("https://github.com/test/repo/issues/1");
        assert updatedLinks.get(0).getArtifactTitle().equals("Setup CI");
    }
}
