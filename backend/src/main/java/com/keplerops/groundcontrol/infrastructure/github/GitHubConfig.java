package com.keplerops.groundcontrol.infrastructure.github;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GitHubConfig {

    @Value("${groundcontrol.github.token:}")
    private String token;

    @Bean
    GitHubProperties gitHubProperties() {
        return new GitHubProperties(token);
    }
}
