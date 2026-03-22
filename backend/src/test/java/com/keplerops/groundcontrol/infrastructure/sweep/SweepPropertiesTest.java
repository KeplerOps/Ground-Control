package com.keplerops.groundcontrol.infrastructure.sweep;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SweepPropertiesTest {

    @Test
    void gitHubNotificationDefensiveCopiesLabels() {
        var mutableList = new java.util.ArrayList<>(List.of("a", "b"));
        var notification = new SweepProperties.GitHubNotification(true, "owner/repo", mutableList);
        mutableList.add("c");
        assertThat(notification.labels()).hasSize(2);
    }

    @Test
    void gitHubNotificationHandlesNullLabels() {
        var notification = new SweepProperties.GitHubNotification(true, "owner/repo", null);
        assertThat(notification.labels()).isEmpty();
    }

    @Test
    void recordAccessors() {
        var props = new SweepProperties(
                true,
                "0 0 6 * * *",
                new SweepProperties.GitHubNotification(true, "owner/repo", List.of("sweep")),
                new SweepProperties.WebhookNotification(true, "http://example.com/hook"));

        assertThat(props.enabled()).isTrue();
        assertThat(props.cron()).isEqualTo("0 0 6 * * *");
        assertThat(props.github().enabled()).isTrue();
        assertThat(props.github().repo()).isEqualTo("owner/repo");
        assertThat(props.github().labels()).containsExactly("sweep");
        assertThat(props.webhook().enabled()).isTrue();
        assertThat(props.webhook().url()).isEqualTo("http://example.com/hook");
    }
}
