package com.keplerops.groundcontrol.infrastructure.sweep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.requirements.service.CompletenessResult;
import com.keplerops.groundcontrol.domain.requirements.service.CycleEdge;
import com.keplerops.groundcontrol.domain.requirements.service.CycleResult;
import com.keplerops.groundcontrol.domain.requirements.service.GitHubClient;
import com.keplerops.groundcontrol.domain.requirements.service.GitHubIssueData;
import com.keplerops.groundcontrol.domain.requirements.service.SweepReport;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GitHubIssueSweepNotifierTest {

    @Nested
    class Notify {

        private GitHubClient gitHubClient;
        private GitHubIssueSweepNotifier notifier;
        private SweepReport report;

        @BeforeEach
        void setUp() {
            gitHubClient = mock(GitHubClient.class);
            var props = new SweepProperties(
                    true,
                    "0 * * * *",
                    new SweepProperties.GitHubNotification(true, "owner/repo", List.of("sweep")),
                    new SweepProperties.WebhookNotification(false, null));
            notifier = new GitHubIssueSweepNotifier(gitHubClient, props);
            report = new SweepReport(
                    "my-project",
                    Instant.now(),
                    List.of(),
                    List.of(new SweepReport.RequirementSummary("GC-001", "Orphan")),
                    Map.of(),
                    List.of(),
                    List.of(),
                    new CompletenessResult(1, Map.of("DRAFT", 1), List.of()));
        }

        @Test
        void createsIssueWhenNoOpenSweepIssueExists() {
            when(gitHubClient.fetchAllIssues("owner", "repo")).thenReturn(List.of());
            when(gitHubClient.createIssue(anyString(), anyString(), anyString(), anyList()))
                    .thenReturn(new GitHubIssueData(42, "title", "OPEN", "url", "body", List.of()));

            notifier.notify(report);

            verify(gitHubClient).createIssue(anyString(), anyString(), anyString(), anyList());
        }

        @Test
        void skipsCreationWhenOpenSweepIssueAlreadyExists() {
            var existingIssue = new GitHubIssueData(
                    10, "[Sweep] 1 problems detected in my-project", "OPEN", "url", "body", List.of());
            when(gitHubClient.fetchAllIssues("owner", "repo")).thenReturn(List.of(existingIssue));

            notifier.notify(report);

            verify(gitHubClient, never()).createIssue(any(), any(), any(), any());
        }

        @Test
        void createsIssueWhenExistingSweepIssueIsClosed() {
            var closedIssue = new GitHubIssueData(
                    10, "[Sweep] 1 problems detected in my-project", "CLOSED", "url", "body", List.of());
            when(gitHubClient.fetchAllIssues("owner", "repo")).thenReturn(List.of(closedIssue));
            when(gitHubClient.createIssue(anyString(), anyString(), anyString(), anyList()))
                    .thenReturn(new GitHubIssueData(11, "title", "OPEN", "url", "body", List.of()));

            notifier.notify(report);

            verify(gitHubClient).createIssue(anyString(), anyString(), anyString(), anyList());
        }

        @Test
        void createsIssueWhenFetchThrowsException() {
            when(gitHubClient.fetchAllIssues("owner", "repo"))
                    .thenThrow(new RuntimeException("network error"));
            when(gitHubClient.createIssue(anyString(), anyString(), anyString(), anyList()))
                    .thenReturn(new GitHubIssueData(42, "title", "OPEN", "url", "body", List.of()));

            notifier.notify(report);

            verify(gitHubClient).createIssue(anyString(), anyString(), anyString(), anyList());
        }
    }

    @Test
    void formatsBodyWithAllSections() {
        var report = new SweepReport(
                "test-project",
                Instant.parse("2026-03-20T06:00:00Z"),
                List.of(new CycleResult(
                        List.of("GC-A", "GC-B"), List.of(new CycleEdge("GC-A", "GC-B", RelationType.DEPENDS_ON)))),
                List.of(new SweepReport.RequirementSummary("GC-ORPH1", "Orphan One")),
                Map.of("IMPLEMENTS", List.of(new SweepReport.RequirementSummary("GC-GAP1", "Gap One"))),
                List.of(new SweepReport.CrossWaveViolationSummary("GC-A", 1, "GC-B", 2, "DEPENDS_ON")),
                List.of(new SweepReport.ConsistencyViolationSummary(
                        "GC-X", "ACTIVE", "GC-Y", "ACTIVE", "ACTIVE_CONFLICT")),
                new CompletenessResult(5, Map.of("DRAFT", 3), List.of()));

        var body = GitHubIssueSweepNotifier.formatBody(report);

        assertThat(body).contains("## Analysis Sweep Report");
        assertThat(body).contains("**Project:** test-project");
        assertThat(body).contains("**Total problems:** 5");
        assertThat(body).contains("### Dependency Cycles");
        assertThat(body).contains("GC-A -> GC-B");
        assertThat(body).contains("### Orphan Requirements");
        assertThat(body).contains("GC-ORPH1: Orphan One");
        assertThat(body).contains("### Coverage Gaps");
        assertThat(body).contains("**IMPLEMENTS:**");
        assertThat(body).contains("GC-GAP1: Gap One");
        assertThat(body).contains("### Cross-Wave Violations");
        assertThat(body).contains("GC-A (wave 1) -> GC-B (wave 2)");
        assertThat(body).contains("### Consistency Violations");
        assertThat(body).contains("GC-X [ACTIVE] <-> GC-Y [ACTIVE]: ACTIVE_CONFLICT");
    }

    @Test
    void omitsEmptySections() {
        var report = new SweepReport(
                "test-project",
                Instant.parse("2026-03-20T06:00:00Z"),
                List.of(),
                List.of(new SweepReport.RequirementSummary("GC-ORPH1", "Orphan One")),
                Map.of(),
                List.of(),
                List.of(),
                new CompletenessResult(1, Map.of("DRAFT", 1), List.of()));

        var body = GitHubIssueSweepNotifier.formatBody(report);

        assertThat(body).contains("### Orphan Requirements");
        assertThat(body).doesNotContain("### Dependency Cycles");
        assertThat(body).doesNotContain("### Coverage Gaps");
        assertThat(body).doesNotContain("### Cross-Wave Violations");
        assertThat(body).doesNotContain("### Consistency Violations");
    }
}
