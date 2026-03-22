package com.keplerops.groundcontrol.infrastructure.sweep;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.domain.requirements.service.SweepNotifier;
import com.keplerops.groundcontrol.domain.requirements.service.SweepReport;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = {"groundcontrol.sweep.enabled", "groundcontrol.sweep.webhook.enabled"},
        havingValue = "true")
public class WebhookSweepNotifier implements SweepNotifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookSweepNotifier.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final SweepProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WebhookSweepNotifier(SweepProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    @PostConstruct
    void validate() {
        if (properties.webhook().url() == null || properties.webhook().url().isBlank()) {
            throw new IllegalStateException(
                    "groundcontrol.sweep.webhook.url must be set when webhook notifications are enabled");
        }
    }

    @Override
    public void notify(SweepReport report) {
        try {
            var json = objectMapper.writeValueAsString(report);
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.webhook().url()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(TIMEOUT)
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("sweep_webhook_sent: project={} status={}", report.projectIdentifier(), response.statusCode());
            } else {
                log.warn(
                        "sweep_webhook_failed: project={} status={} body={}",
                        report.projectIdentifier(),
                        response.statusCode(),
                        response.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("sweep_webhook_interrupted: project={}", report.projectIdentifier());
        } catch (Exception e) {
            log.warn("sweep_webhook_error: project={} error={}", report.projectIdentifier(), e.getMessage());
        }
    }
}
