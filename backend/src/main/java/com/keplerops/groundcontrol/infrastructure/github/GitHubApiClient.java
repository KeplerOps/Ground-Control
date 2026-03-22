package com.keplerops.groundcontrol.infrastructure.github;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.domain.exception.GroundControlException;
import com.keplerops.groundcontrol.domain.requirements.service.GitHubClient;
import com.keplerops.groundcontrol.domain.requirements.service.GitHubIssueData;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GitHubApiClient implements GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubApiClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final String BASE_URL = "https://api.github.com";
    private static final Pattern LINK_NEXT_RE = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");

    private final ObjectMapper objectMapper;
    private final GitHubProperties properties;
    private final HttpClient httpClient;

    public GitHubApiClient(ObjectMapper objectMapper, GitHubProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        if (properties.token() == null || properties.token().isBlank()) {
            log.warn("github_token_not_configured: set GC_GITHUB_TOKEN to enable GitHub integration");
        } else {
            log.info("github_api_client_initialized");
        }
    }

    @Override
    public List<GitHubIssueData> fetchAllIssues(String owner, String repo) {
        requireToken();
        List<GitHubIssueData> result = new ArrayList<>();
        String url = BASE_URL + "/repos/" + owner + "/" + repo + "/issues?state=all&per_page=100";

        while (url != null) {
            HttpResponse<String> response = sendGet(url);
            handleErrorStatus(response);

            List<Map<String, Object>> page = parseJsonArray(response.body());
            for (Map<String, Object> raw : page) {
                if (raw.containsKey("pull_request")) {
                    continue;
                }
                result.add(mapIssue(raw));
            }

            url = parseNextLink(response);
        }

        log.info("github_issues_fetched: count={} repo={}/{}", result.size(), owner, repo);
        return result;
    }

    @Override
    public GitHubIssueData createIssue(String repo, String title, String body, List<String> labels) {
        requireToken();
        try {
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("title", title);
            payload.put("body", body);
            if (labels != null && !labels.isEmpty()) {
                payload.put("labels", labels);
            }

            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = newRequest(BASE_URL + "/repos/" + repo + "/issues")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            handleErrorStatus(response);

            Map<String, Object> raw = objectMapper.readValue(response.body(), new TypeReference<>() {});
            GitHubIssueData issue = mapIssue(raw);

            log.info("github_issue_created: number={} repo={}", issue.number(), repo);
            return issue;
        } catch (GroundControlException e) {
            throw e;
        } catch (IOException e) {
            throw new GroundControlException("Failed to call GitHub API: " + e.getMessage(), "github_api_error", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GroundControlException("GitHub API call interrupted", "github_interrupted", e);
        }
    }

    private void requireToken() {
        if (properties.token() == null || properties.token().isBlank()) {
            throw new GroundControlException(
                    "GitHub integration not configured: set GC_GITHUB_TOKEN environment variable",
                    "github_not_configured");
        }
    }

    private HttpRequest.Builder newRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + properties.token())
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT);
    }

    private HttpResponse<String> sendGet(String url) {
        try {
            HttpRequest request = newRequest(url).GET().build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new GroundControlException("Failed to call GitHub API: " + e.getMessage(), "github_api_error", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GroundControlException("GitHub API call interrupted", "github_interrupted", e);
        }
    }

    private void handleErrorStatus(HttpResponse<String> response) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return;
        }
        String body = response.body();
        if (status == 401 || status == 403) {
            throw new GroundControlException(
                    "GitHub API auth error (HTTP " + status + "): " + body, "github_auth_error");
        } else if (status == 404) {
            throw new GroundControlException("GitHub API not found (HTTP 404): " + body, "github_not_found");
        } else if (status == 422) {
            throw new GroundControlException(
                    "GitHub API validation error (HTTP 422): " + body, "github_validation_error");
        } else {
            throw new GroundControlException("GitHub API error (HTTP " + status + "): " + body, "github_api_error");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJsonArray(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (IOException e) {
            throw new GroundControlException(
                    "Failed to parse GitHub API response: " + e.getMessage(), "github_api_error", e);
        }
    }

    private String parseNextLink(HttpResponse<String> response) {
        return response.headers()
                .firstValue("link")
                .flatMap(header -> {
                    Matcher m = LINK_NEXT_RE.matcher(header);
                    return m.find() ? java.util.Optional.of(m.group(1)) : java.util.Optional.empty();
                })
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    static GitHubIssueData mapIssue(Map<String, Object> raw) {
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

        return new GitHubIssueData(number, title, state, url, body, labels);
    }
}
