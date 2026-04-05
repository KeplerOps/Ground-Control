package com.keplerops.groundcontrol.unit.infrastructure;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.domain.requirements.service.GitHubPullRequestData;
import com.keplerops.groundcontrol.infrastructure.github.GitHubCliClient;
import com.keplerops.groundcontrol.infrastructure.github.GitHubCliClient.PrPage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GitHubCliClientPrTest {

    @Nested
    class ParsePullRequests {

        @Test
        void parsesOpenPr() {
            var raw = buildRawPr(1, "Add feature", "open", false, "main", "feature/add");
            List<GitHubPullRequestData> result = GitHubCliClient.parsePullRequests(List.of(raw));

            assertThat(result).hasSize(1);
            var pr = result.get(0);
            assertThat(pr.number()).isEqualTo(1);
            assertThat(pr.title()).isEqualTo("Add feature");
            assertThat(pr.state()).isEqualTo("OPEN");
            assertThat(pr.merged()).isFalse();
            assertThat(pr.baseBranch()).isEqualTo("main");
            assertThat(pr.headBranch()).isEqualTo("feature/add");
        }

        @Test
        void parsesClosedPr() {
            var raw = buildRawPr(2, "Fix bug", "closed", false, "dev", "fix/bug");
            List<GitHubPullRequestData> result = GitHubCliClient.parsePullRequests(List.of(raw));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).state()).isEqualTo("CLOSED");
            assertThat(result.get(0).merged()).isFalse();
        }

        @Test
        void parsesMergedPr() {
            var raw = buildRawPr(3, "Ship it", "closed", true, "main", "feature/ship");
            List<GitHubPullRequestData> result = GitHubCliClient.parsePullRequests(List.of(raw));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).state()).isEqualTo("CLOSED");
            assertThat(result.get(0).merged()).isTrue();
        }

        @Test
        void parsesLabels() {
            var raw = buildRawPr(4, "Labeled PR", "open", false, "main", "feature/labeled");
            List<Map<String, Object>> labels = new ArrayList<>();
            labels.add(Map.of("name", "enhancement"));
            labels.add(Map.of("name", "ready-for-review"));
            raw.put("labels", labels);

            List<GitHubPullRequestData> result = GitHubCliClient.parsePullRequests(List.of(raw));

            assertThat(result.get(0).labels()).containsExactly("enhancement", "ready-for-review");
        }

        @Test
        void handlesNullBody() {
            var raw = buildRawPr(5, "No body", "open", false, "main", "feature/nobody");
            raw.put("body", null);

            List<GitHubPullRequestData> result = GitHubCliClient.parsePullRequests(List.of(raw));

            assertThat(result.get(0).body()).isEmpty();
        }

        @Test
        void handlesNullLabels() {
            var raw = buildRawPr(6, "No labels", "open", false, "main", "feature/nolabels");
            raw.put("labels", null);

            List<GitHubPullRequestData> result = GitHubCliClient.parsePullRequests(List.of(raw));

            assertThat(result.get(0).labels()).isEmpty();
        }

        @Test
        void handlesMultiplePrs() {
            var raw1 = buildRawPr(10, "PR 1", "open", false, "main", "feature/one");
            var raw2 = buildRawPr(11, "PR 2", "closed", true, "main", "feature/two");

            List<GitHubPullRequestData> result = GitHubCliClient.parsePullRequests(List.of(raw1, raw2));

            assertThat(result).hasSize(2);
            assertThat(result.get(0).number()).isEqualTo(10);
            assertThat(result.get(1).number()).isEqualTo(11);
        }

        @Test
        void handlesNullBaseAndHead() {
            var raw = buildRawPr(7, "No refs", "open", false, null, null);
            raw.put("base", null);
            raw.put("head", null);

            List<GitHubPullRequestData> result = GitHubCliClient.parsePullRequests(List.of(raw));

            assertThat(result.get(0).baseBranch()).isEmpty();
            assertThat(result.get(0).headBranch()).isEmpty();
        }

        private Map<String, Object> buildRawPr(
                int number, String title, String state, boolean merged, String baseBranch, String headBranch) {
            Map<String, Object> raw = new LinkedHashMap<>();
            raw.put("number", number);
            raw.put("title", title);
            raw.put("state", state);
            raw.put("merged", merged);
            raw.put("html_url", "https://github.com/org/repo/pull/" + number);
            raw.put("body", "PR body for " + title);
            raw.put("labels", List.of());

            if (baseBranch != null) {
                raw.put("base", Map.of("ref", baseBranch));
            }
            if (headBranch != null) {
                raw.put("head", Map.of("ref", headBranch));
            }

            return raw;
        }
    }

    @Nested
    class PrPagination {

        @Test
        void singlePageReturnsAllPrs() {
            var client = stubbedClient(List.of(new PrPage(List.of(pr(1), pr(2)), 2)));

            List<GitHubPullRequestData> result = client.fetchAllPullRequests("o", "r");

            assertThat(result).hasSize(2);
            assertThat(result.get(0).number()).isEqualTo(1);
            assertThat(result.get(1).number()).isEqualTo(2);
        }

        @Test
        void paginatesUntilPartialPage() {
            var client = stubbedClient(List.of(new PrPage(makePrs(1, 100), 100), new PrPage(makePrs(101, 120), 20)));

            List<GitHubPullRequestData> result = client.fetchAllPullRequests("o", "r");

            assertThat(result).hasSize(120);
            assertThat(result.get(0).number()).isEqualTo(1);
            assertThat(result.get(119).number()).isEqualTo(120);
        }

        @Test
        void emptyPageReturnsEmpty() {
            var client = stubbedClient(List.of(new PrPage(List.of(), 0)));

            List<GitHubPullRequestData> result = client.fetchAllPullRequests("o", "r");

            assertThat(result).isEmpty();
        }

        private GitHubCliClient stubbedClient(List<PrPage> pages) {
            return new GitHubCliClient(new ObjectMapper(), "gh") {
                private int callCount = 0;

                @Override
                protected PrPage fetchPrPage(String owner, String repo, int page) {
                    return pages.get(callCount++);
                }
            };
        }

        private List<GitHubPullRequestData> makePrs(int from, int to) {
            List<GitHubPullRequestData> prs = new ArrayList<>();
            for (int i = from; i <= to; i++) {
                prs.add(pr(i));
            }
            return prs;
        }

        private GitHubPullRequestData pr(int number) {
            return new GitHubPullRequestData(
                    number,
                    "PR " + number,
                    "OPEN",
                    false,
                    "https://github.com/o/r/pull/" + number,
                    "",
                    "main",
                    "feat/" + number,
                    List.of());
        }
    }
}
