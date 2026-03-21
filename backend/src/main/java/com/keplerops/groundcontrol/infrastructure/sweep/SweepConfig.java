package com.keplerops.groundcontrol.infrastructure.sweep;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ConditionalOnProperty(name = "groundcontrol.sweep.enabled", havingValue = "true")
@EnableScheduling
public class SweepConfig {

    @Bean
    SweepProperties sweepProperties(
            @Value("${groundcontrol.sweep.enabled:false}") boolean enabled,
            @Value("${groundcontrol.sweep.cron:0 0 6 * * *}") String cron,
            @Value("${groundcontrol.sweep.github.enabled:false}") boolean githubEnabled,
            @Value("${groundcontrol.sweep.github.repo:}") String githubRepo,
            @Value("${groundcontrol.sweep.github.labels:analysis-sweep}") String githubLabels,
            @Value("${groundcontrol.sweep.webhook.enabled:false}") boolean webhookEnabled,
            @Value("${groundcontrol.sweep.webhook.url:}") String webhookUrl) {
        var labelList = githubLabels.isBlank() ? List.<String>of() : List.of(githubLabels.split(","));
        return new SweepProperties(
                enabled,
                cron,
                new SweepProperties.GitHubNotification(githubEnabled, githubRepo, labelList),
                new SweepProperties.WebhookNotification(webhookEnabled, webhookUrl));
    }
}
