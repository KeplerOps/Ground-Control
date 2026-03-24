package com.keplerops.groundcontrol.infrastructure.execution;

import com.keplerops.groundcontrol.domain.workflows.model.WorkflowNode;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Executes Docker containers as workflow tasks.
 *
 * <p>Config JSON schema:
 * <pre>
 * {
 *   "image": "alpine:latest",
 *   "command": "echo hello",
 *   "env": {"KEY": "value"},
 *   "volumes": ["/host/path:/container/path"]
 * }
 * </pre>
 */
@Component
public class DockerTaskExecutor implements TaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(DockerTaskExecutor.class);

    @Override
    public TaskResult execute(WorkflowNode node, String inputs) {
        try {
            var config = node.getConfig();
            String image = extractJsonField(config, "image");
            String command = extractJsonField(config, "command");

            if (image == null || image.isBlank()) {
                return TaskResult.failure("No Docker image specified in node config", "");
            }

            int timeoutSeconds = node.getTimeoutSeconds() != null ? node.getTimeoutSeconds() : 600;

            log.info("Executing Docker container {} for node {}", image, node.getName());

            var args = new ArrayList<String>();
            args.add("docker");
            args.add("run");
            args.add("--rm");
            args.add("--network=none");

            // Resource limits for safety
            args.add("--memory=512m");
            args.add("--cpus=1.0");
            args.add("--pids-limit=256");

            args.add(image);

            if (command != null && !command.isBlank()) {
                args.add("sh");
                args.add("-c");
                args.add(command);
            }

            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            var sb = new StringBuilder();
            try (var reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return TaskResult.failure("Docker container timed out after " + timeoutSeconds + "s", sb.toString());
            }

            int exitCode = process.exitValue();
            String output = sb.toString();

            if (exitCode == 0) {
                return TaskResult.success("{\"exitCode\":0,\"output\":" + jsonEscape(output) + "}", output);
            } else {
                return TaskResult.failure("Container exited with code " + exitCode, output);
            }
        } catch (Exception e) {
            log.error("Docker execution failed for node {}", node.getName(), e);
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
