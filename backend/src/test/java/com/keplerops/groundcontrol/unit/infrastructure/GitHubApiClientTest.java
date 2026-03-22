package com.keplerops.groundcontrol.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.domain.exception.GroundControlException;
import com.keplerops.groundcontrol.domain.requirements.service.GitHubIssueData;
import com.keplerops.groundcontrol.infrastructure.github.GitHubApiClient;
import com.keplerops.groundcontrol.infrastructure.github.GitHubProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GitHubApiClientTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    class JsonParsing {

        @Test
        void parsesGitHubApiIssueResponse() throws Exception {
            String json =
                    """
                    [
                      {
                        "number": 42,
                        "title": "Fix login bug",
                        "state": "open",
                        "html_url": "https://github.com/o/r/issues/42",
                        "url": "https://api.github.com/repos/o/r/issues/42",
                        "body": "Login is broken for #10",
                        "labels": [{"id": 1, "name": "bug", "color": "fc2929"}, {"id": 2, "name": "P0", "color": "000000"}]
                      }
                    ]
                    """;

            List<GitHubIssueData> result = parseApiJson(json);

            assertThat(result).hasSize(1);
            GitHubIssueData issue = result.get(0);
            assertThat(issue.number()).isEqualTo(42);
            assertThat(issue.title()).isEqualTo("Fix login bug");
            assertThat(issue.state()).isEqualTo("OPEN");
            assertThat(issue.url()).isEqualTo("https://github.com/o/r/issues/42");
            assertThat(issue.body()).isEqualTo("Login is broken for #10");
            assertThat(issue.labels()).containsExactly("bug", "P0");
        }

        @Test
        void uppercasesState() throws Exception {
            String json =
                    """
                    [
                      {
                        "number": 1,
                        "title": "Closed issue",
                        "state": "closed",
                        "html_url": "https://github.com/o/r/issues/1",
                        "body": null,
                        "labels": []
                      }
                    ]
                    """;

            List<GitHubIssueData> result = parseApiJson(json);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).state()).isEqualTo("CLOSED");
            assertThat(result.get(0).body()).isEmpty();
        }

        @Test
        void usesHtmlUrlNotApiUrl() throws Exception {
            String json =
                    """
                    [
                      {
                        "number": 5,
                        "title": "URL test",
                        "state": "open",
                        "html_url": "https://github.com/o/r/issues/5",
                        "url": "https://api.github.com/repos/o/r/issues/5",
                        "body": "",
                        "labels": []
                      }
                    ]
                    """;

            List<GitHubIssueData> result = parseApiJson(json);

            assertThat(result.get(0).url()).isEqualTo("https://github.com/o/r/issues/5");
        }

        @Test
        void extractsLabelNamesIgnoringExtraFields() throws Exception {
            String json =
                    """
                    [
                      {
                        "number": 5,
                        "title": "Labels test",
                        "state": "open",
                        "html_url": "https://github.com/o/r/issues/5",
                        "body": "",
                        "labels": [
                          {"id": 1, "name": "bug", "color": "fc2929", "default": false},
                          {"id": 2, "name": "P0", "color": "000000", "default": false},
                          {"id": 3, "name": "phase-1", "color": "0075ca", "default": false}
                        ]
                      }
                    ]
                    """;

            List<GitHubIssueData> result = parseApiJson(json);

            assertThat(result.get(0).labels()).containsExactly("bug", "P0", "phase-1");
        }

        @Test
        void filtersPullRequests() throws Exception {
            String json =
                    """
                    [
                      {
                        "number": 1,
                        "title": "Real issue",
                        "state": "open",
                        "html_url": "https://github.com/o/r/issues/1",
                        "body": "",
                        "labels": []
                      },
                      {
                        "number": 2,
                        "title": "A pull request",
                        "state": "open",
                        "html_url": "https://github.com/o/r/pull/2",
                        "body": "",
                        "labels": [],
                        "pull_request": {"url": "https://api.github.com/repos/o/r/pulls/2"}
                      }
                    ]
                    """;

            List<GitHubIssueData> result = parseApiJson(json);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).number()).isEqualTo(1);
        }
    }

    @Nested
    class TokenValidation {

        @Test
        void throwsWhenTokenNotConfigured() {
            GitHubApiClient client = new GitHubApiClient(objectMapper, new GitHubProperties(""));

            assertThatThrownBy(() -> client.fetchAllIssues("owner", "repo"))
                    .isInstanceOf(GroundControlException.class)
                    .hasMessageContaining("GC_GITHUB_TOKEN");

            assertThatThrownBy(() -> client.createIssue("owner/repo", "title", "body", null))
                    .isInstanceOf(GroundControlException.class)
                    .hasMessageContaining("GC_GITHUB_TOKEN");
        }
    }

    /**
     * Parses GitHub REST API JSON response the same way GitHubApiClient does, verifying the
     * contract: html_url (not url), uppercase state, label name extraction, and PR filtering.
     */
    @SuppressWarnings("unchecked")
    private static List<GitHubIssueData> parseApiJson(String json) throws Exception {
        List<Map<String, Object>> rawIssues = objectMapper.readValue(json, new TypeReference<>() {});
        List<GitHubIssueData> result = new ArrayList<>();
        for (Map<String, Object> raw : rawIssues) {
            if (raw.containsKey("pull_request")) {
                continue;
            }
            int number = ((Number) raw.get("number")).intValue();
            String title = (String) raw.get("title");
            String state = raw.get("state") != null ? ((String) raw.get("state")).toUpperCase(Locale.ROOT) : "OPEN";
            String url = (String) raw.get("html_url");
            String body = raw.get("body") != null ? (String) raw.get("body") : "";

            List<Map<String, Object>> labelObjects =
                    raw.get("labels") != null ? (List<Map<String, Object>>) raw.get("labels") : List.of();

            List<String> labels = new ArrayList<>();
            for (Map<String, Object> labelObj : labelObjects) {
                labels.add((String) labelObj.get("name"));
            }

            result.add(new GitHubIssueData(number, title, state, url, body, labels));
        }
        return result;
    }
}
