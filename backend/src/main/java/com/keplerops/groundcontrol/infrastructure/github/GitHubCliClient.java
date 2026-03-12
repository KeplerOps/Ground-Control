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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GitHubCliClient implements GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubCliClient.class);
    private static final long TIMEOUT_SECONDS = 60;

    private final ObjectMapper objectMapper;

    public GitHubCliClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<GitHubIssueData> fetchAllIssues(String owner, String repo) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "gh",
                    "issue",
                    "list",
                    "--repo",
                    owner + "/" + repo,
                    "--state",
                    "all",
                    "--limit",
                    "500",
                    "--json",
                    "number,title,labels,body,state,url");
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

            List<GitHubIssueData> result = new ArrayList<>();
            for (Map<String, Object> raw : rawIssues) {
                int number = ((Number) raw.get("number")).intValue();
                String title = (String) raw.get("title");
                String state = (String) raw.get("state");
                String url = (String) raw.get("url");
                String body = raw.get("body") != null ? (String) raw.get("body") : "";

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> labelObjects =
                        raw.get("labels") != null ? (List<Map<String, Object>>) raw.get("labels") : List.of();

                List<String> labels = new ArrayList<>();
                for (Map<String, Object> labelObj : labelObjects) {
                    labels.add((String) labelObj.get("name"));
                }

                result.add(new GitHubIssueData(number, title, state, url, body, labels));
            }

            log.info("Fetched {} issues from {}/{}", result.size(), owner, repo);
            return result;
        } catch (IOException e) {
            throw new GroundControlException("Failed to execute gh CLI: " + e.getMessage(), "github_cli_error", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GroundControlException("gh CLI execution interrupted", "github_interrupted", e);
        }
    }
}
