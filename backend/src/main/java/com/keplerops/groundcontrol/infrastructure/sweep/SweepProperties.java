package com.keplerops.groundcontrol.infrastructure.sweep;

import java.util.List;

public record SweepProperties(boolean enabled, String cron, GitHubNotification github, WebhookNotification webhook) {

    public record GitHubNotification(boolean enabled, String repo, List<String> labels) {

        public GitHubNotification {
            labels = labels != null ? List.copyOf(labels) : List.of();
        }
    }

    public record WebhookNotification(boolean enabled, String url) {}
}
