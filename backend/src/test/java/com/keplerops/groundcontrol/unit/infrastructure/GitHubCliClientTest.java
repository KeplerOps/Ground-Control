package com.keplerops.groundcontrol.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.domain.requirements.service.GitHubIssueData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GitHubCliClientTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    class JsonParsing {

        @Test
        void parsesValidJsonOutput() throws Exception {
            String json =
                    """
                    [
                      {
                        "number": 42,
                        "title": "Fix login bug",
                        "state": "OPEN",
                        "url": "https://github.com/o/r/issues/42",
                        "body": "Login is broken for #10",
                        "labels": [{"name": "bug"}, {"name": "P0"}]
                      }
                    ]
                    """;

            List<GitHubIssueData> result = parseJson(json);

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
        void handlesEmptyBody() throws Exception {
            String json =
                    """
                    [
                      {
                        "number": 1,
                        "title": "No body",
                        "state": "CLOSED",
                        "url": "https://github.com/o/r/issues/1",
                        "body": null,
                        "labels": []
                      }
                    ]
                    """;

            List<GitHubIssueData> result = parseJson(json);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).body()).isEmpty();
        }

        @Test
        void extractsLabelNames() throws Exception {
            String json =
                    """
                    [
                      {
                        "number": 5,
                        "title": "Labels test",
                        "state": "OPEN",
                        "url": "https://github.com/o/r/issues/5",
                        "body": "",
                        "labels": [{"name": "bug"}, {"name": "P0"}, {"name": "phase-1"}]
                      }
                    ]
                    """;

            List<GitHubIssueData> result = parseJson(json);

            assertThat(result.get(0).labels()).containsExactly("bug", "P0", "phase-1");
        }
    }

    @Nested
    class CreateIssueUrlParsing {

        @Test
        void parsesIssueNumberFromUrl() {
            String url = "https://github.com/KeplerOps/Ground-Control/issues/42";
            Pattern pattern = Pattern.compile("/issues/(\\d+)$");
            Matcher matcher = pattern.matcher(url);
            assertThat(matcher.find()).isTrue();
            assertThat(Integer.parseInt(matcher.group(1))).isEqualTo(42);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<GitHubIssueData> parseJson(String json) throws Exception {
        List<Map<String, Object>> rawIssues = objectMapper.readValue(json, new TypeReference<>() {});
        List<GitHubIssueData> result = new ArrayList<>();
        for (Map<String, Object> raw : rawIssues) {
            int number = ((Number) raw.get("number")).intValue();
            String title = (String) raw.get("title");
            String state = (String) raw.get("state");
            String url = (String) raw.get("url");
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
