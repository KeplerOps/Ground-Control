package com.keplerops.groundcontrol.infrastructure.execution;

import com.keplerops.groundcontrol.domain.workflows.model.WorkflowNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Executes HTTP requests as workflow tasks.
 *
 * <p>Config JSON schema:
 * <pre>
 * {
 *   "url": "https://api.example.com/data",
 *   "method": "POST",
 *   "headers": {"Content-Type": "application/json"},
 *   "body": "{\"key\":\"value\"}"
 * }
 * </pre>
 */
@Component
public class HttpTaskExecutor implements TaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(HttpTaskExecutor.class);
    private final HttpClient httpClient;

    public HttpTaskExecutor() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public TaskResult execute(WorkflowNode node, String inputs) {
        try {
            var config = node.getConfig();
            String url = extractJsonField(config, "url");
            String method = extractJsonField(config, "method");
            String body = extractJsonField(config, "body");

            if (url == null || url.isBlank()) {
                return TaskResult.failure("No URL specified in node config", "");
            }
            if (method == null) method = "GET";

            int timeoutSeconds = node.getTimeoutSeconds() != null ? node.getTimeoutSeconds() : 60;

            log.info("Executing HTTP {} {} for node {}", method, url, node.getName());

            var requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds));

            switch (method.toUpperCase()) {
                case "POST" -> requestBuilder.POST(body != null
                        ? HttpRequest.BodyPublishers.ofString(body)
                        : HttpRequest.BodyPublishers.noBody());
                case "PUT" -> requestBuilder.PUT(body != null
                        ? HttpRequest.BodyPublishers.ofString(body)
                        : HttpRequest.BodyPublishers.noBody());
                case "DELETE" -> requestBuilder.DELETE();
                default -> requestBuilder.GET();
            }

            var response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String responseBody = response.body();
            String logLine = "HTTP " + method + " " + url + " => " + statusCode;

            if (statusCode >= 200 && statusCode < 300) {
                return TaskResult.success(
                        "{\"statusCode\":" + statusCode + ",\"body\":" + jsonEscape(responseBody) + "}",
                        logLine);
            } else {
                return TaskResult.failure(
                        "HTTP " + statusCode + ": " + responseBody, logLine);
            }
        } catch (Exception e) {
            log.error("HTTP execution failed for node {}", node.getName(), e);
            return TaskResult.failure(e.getMessage(), "");
        }
    }

    private String extractJsonField(String json, String field) {
        String pattern = "\"" + field + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;
        int colonIdx = json.indexOf(':', idx + pattern.length());
        if (colonIdx < 0) return null;
        int start = json.indexOf('"', colonIdx + 1);
        if (start < 0) return null;
        int end = start + 1;
        while (end < json.length() && json.charAt(end) != '"') {
            if (json.charAt(end) == '\\') end++;
            end++;
        }
        return json.substring(start + 1, end);
    }

    private String jsonEscape(String s) {
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}
