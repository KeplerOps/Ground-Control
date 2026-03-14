package com.keplerops.groundcontrol.infrastructure.age;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgeConfig {

    @Value("${groundcontrol.age.enabled:false}")
    private boolean enabled;

    @Value("${groundcontrol.age.graph-name:requirements}")
    private String graphName;

    @Bean
    AgeProperties ageProperties() {
        return new AgeProperties(enabled, graphName);
    }
}
