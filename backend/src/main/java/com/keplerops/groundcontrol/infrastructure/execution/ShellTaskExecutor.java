package com.keplerops.groundcontrol.infrastructure.execution;

import com.keplerops.groundcontrol.domain.workflows.model.WorkflowNode;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Executes shell commands as workflow tasks.
 *
 * <p>Config JSON schema:
 * <pre>
 * {
 *   "command": "echo hello",
 *   "workingDir": "/tmp",
 *   "env": {"KEY": "value"}
 * }
 * </pre>
 */
@Component
public class ShellTaskExecutor implements TaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(ShellTaskExecutor.class);

    @Override
    public TaskResult execute(WorkflowNode node, String inputs) {
        try {
            var config = node.getConfig();
            // Simple JSON parsing for command field
            String command = extractJsonField(config, "command");
            if (command == null || command.isBlank()) {
                return TaskResult.failure("No command specified in node config", "");
            }

            int timeoutSeconds = node.getTimeoutSeconds() != null ? node.getTimeoutSeconds() : 300;

            log.info("Executing shell command for node {}: {}", node.getName(), command);

            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.redirectErrorStream(true);

            String workingDir = extractJsonField(config, "workingDir");
            if (workingDir != null && !workingDir.isBlank()) {
                pb.directory(new java.io.File(workingDir));
            }

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
                return TaskResult.failure("Command timed out after " + timeoutSeconds + "s", sb.toString());
            }

            int exitCode = process.exitValue();
            String output = sb.toString();

            if (exitCode == 0) {
                return TaskResult.success("{\"exitCode\":0,\"output\":" + jsonEscape(output) + "}", output);
            } else {
                return TaskResult.failure("Command exited with code " + exitCode, output);
            }
        } catch (Exception e) {
            log.error("Shell execution failed for node {}", node.getName(), e);
            return TaskResult.failure(e.getMessage(), "");
        }
    }

    private String extractJsonField(String json, String field) {
        // Simple extraction without a JSON library dependency
        String pattern = "\"" + field + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;
        int colonIdx = json.indexOf(':', idx + pattern.length());
        if (colonIdx < 0) return null;
        int start = json.indexOf('"', colonIdx + 1);
        if (start < 0) return null;
        int end = start + 1;
        while (end < json.length() && json.charAt(end) != '"') {
            if (json.charAt(end) == '\\') end++; // skip escaped char
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
