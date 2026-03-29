package com.keplerops.groundcontrol.infrastructure.github;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.domain.exception.GroundControlException;
import com.keplerops.groundcontrol.domain.requirements.service.GitHubClient;
import com.keplerops.groundcontrol.domain.requirements.service.GitHubIssueData;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GitHubCliClient implements GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubCliClient.class);
    private static final long TIMEOUT_SECONDS = 60;
    private static final int PAGE_SIZE = 100;
    private static final Pattern ISSUE_URL_RE = Pattern.compile("/issues/(\\d+)$");

    private final ObjectMapper objectMapper;
    private final String ghPath;

    public GitHubCliClient(
            ObjectMapper objectMapper,
            @org.springframework.beans.factory.annotation.Value("${groundcontrol.github.gh-path:}") String ghPath) {
        this.objectMapper = objectMapper;
        this.ghPath = ghPath.isBlank() ? resolveGhPath() : ghPath;
        log.info("github_cli_path: {}", this.ghPath);
    }

    // Common Linux/macOS install locations; Windows is not supported.
    private static String resolveGhPath() {
        for (String candidate : List.of("/usr/bin/gh", "/usr/local/bin/gh", "/opt/homebrew/bin/gh")) {
            if (java.nio.file.Files.isExecutable(java.nio.file.Path.of(candidate))) {
                return candidate;
            }
        }
        return "gh";
    }

    public record IssuePage(List<GitHubIssueData> issues, int rawCount) {
        public IssuePage {
            issues = List.copyOf(issues);
        }
    }

    @Override
    public List<GitHubIssueData> fetchAllIssues(String owner, String repo) {
        List<GitHubIssueData> allIssues = new ArrayList<>();
        int page = 1;

        while (true) {
            IssuePage batch = fetchIssuePage(owner, repo, page);
            allIssues.addAll(batch.issues());

            if (batch.rawCount() < PAGE_SIZE) {
                break;
            }

            log.info(
                    "github_issues_page_full: page={} raw={} issues={} repo={}/{}, fetching next page",
                    page,
                    batch.rawCount(),
                    batch.issues().size(),
                    owner,
                    repo);
            page++;
        }

        log.info("github_issues_fetched: count={} pages={} repo={}/{}", allIssues.size(), page, owner, repo);
        return allIssues;
    }

    protected IssuePage fetchIssuePage(String owner, String repo, int page) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ghPath,
                    "api",
                    String.format("repos/%s/%s/issues", owner, repo),
                    "--method",
                    "GET",
                    "-f",
                    "state=all",
                    "-f",
                    "per_page=" + PAGE_SIZE,
                    "-f",
                    "page=" + page);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new GroundControlException("gh CLI timed out after " + TIMEOUT_SECONDS + "s", "github_timeout");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new GroundControlException(
                        "gh CLI exited with code " + exitCode + ": " + stderr, "github_cli_error");
            }

            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawIssues =
                    objectMapper.readValue(stdout, new TypeReference<List<Map<String, Object>>>() {});

            return new IssuePage(parseIssues(rawIssues), rawIssues.size());
        } catch (IOException e) {
            throw new GroundControlException("Failed to execute gh CLI: " + e.getMessage(), "github_cli_error", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GroundControlException("gh CLI execution interrupted", "github_interrupted", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<GitHubIssueData> parseIssues(List<Map<String, Object>> rawIssues) {
        List<GitHubIssueData> result = new ArrayList<>();
        for (Map<String, Object> raw : rawIssues) {
            // GitHub REST API returns PRs in the issues endpoint; skip them
            if (raw.containsKey("pull_request")) {
                continue;
            }

            int number = ((Number) raw.get("number")).intValue();
            String title = (String) raw.get("title");
            String apiState = (String) raw.get("state");
            String state = apiState.equalsIgnoreCase("open") ? "OPEN" : "CLOSED";
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

    @Override
    public GitHubIssueData createIssue(String repo, String title, String body, List<String> labels) {
        try {
            List<String> args = new ArrayList<>(
                    List.of(ghPath, "issue", "create", "--repo", repo, "--title", title, "--body", body));
            if (labels != null && !labels.isEmpty()) {
                args.add("--label");
                args.add(String.join(",", labels));
            }

            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new GroundControlException("gh CLI timed out after " + TIMEOUT_SECONDS + "s", "github_timeout");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new GroundControlException(
                        "gh CLI exited with code " + exitCode + ": " + stderr, "github_cli_error");
            }

            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String url = stdout.trim();

            Matcher matcher = ISSUE_URL_RE.matcher(url);
            if (!matcher.find()) {
                throw new GroundControlException(
                        "Could not parse issue number from gh output: " + url, "github_parse_error");
            }
            int number = Integer.parseInt(matcher.group(1));

            log.info("github_issue_created: number={} repo={}", number, repo);
            return new GitHubIssueData(number, title, "OPEN", url, body, labels != null ? labels : List.of());
        } catch (IOException e) {
            throw new GroundControlException("Failed to execute gh CLI: " + e.getMessage(), "github_cli_error", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GroundControlException("gh CLI execution interrupted", "github_interrupted", e);
        }
    }
}
