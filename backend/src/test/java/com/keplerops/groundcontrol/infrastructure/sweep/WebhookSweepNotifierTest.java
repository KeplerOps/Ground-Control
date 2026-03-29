package com.keplerops.groundcontrol.infrastructure.sweep;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.keplerops.groundcontrol.domain.requirements.service.CompletenessResult;
import com.keplerops.groundcontrol.domain.requirements.service.SweepReport;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WebhookSweepNotifierTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void validateFailsOnBlankUrl() {
        var props = new SweepProperties(
                true,
                "0 0 6 * * *",
                new SweepProperties.GitHubNotification(false, "", List.of()),
                new SweepProperties.WebhookNotification(true, ""));

        var notifier = new WebhookSweepNotifier(props, MAPPER);
        assertThatThrownBy(notifier::validate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateFailsOnNullUrl() {
        var props = new SweepProperties(
                true,
                "0 0 6 * * *",
                new SweepProperties.GitHubNotification(false, "", List.of()),
                new SweepProperties.WebhookNotification(true, null));

        var notifier = new WebhookSweepNotifier(props, MAPPER);
        assertThatThrownBy(notifier::validate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void handlesConnectionFailureGracefully() {
        var props = new SweepProperties(
                true,
                "0 0 6 * * *",
                new SweepProperties.GitHubNotification(false, "", List.of()),
                new SweepProperties.WebhookNotification(true, "http://localhost:1"));

        var notifier = new WebhookSweepNotifier(props, MAPPER);
        // Should not throw — errors are logged, not propagated
        notifier.notify(reportWithOrphans());
    }

    private static SweepReport reportWithOrphans() {
        return new SweepReport(
                "test-project",
                Instant.parse("2026-03-20T06:00:00Z"),
                List.of(),
                List.of(new SweepReport.RequirementSummary("GC-A", "Title A")),
                Map.of(),
                List.of(),
                List.of(),
                new CompletenessResult(1, Map.of("DRAFT", 1), List.of()),
                null);
    }
}
