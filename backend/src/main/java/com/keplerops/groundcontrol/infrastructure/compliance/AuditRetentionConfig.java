package com.keplerops.groundcontrol.infrastructure.compliance;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ConditionalOnExpression("${groundcontrol.compliance.audit-retention-days:-1} > 0")
@EnableScheduling
public class AuditRetentionConfig {

    @Bean
    AuditRetentionProperties auditRetentionProperties(
            @Value("${groundcontrol.compliance.audit-retention-days}") int retentionDays,
            @Value("${groundcontrol.compliance.audit-retention-cron:0 0 3 * * *}") String cron) {
        return new AuditRetentionProperties(retentionDays, cron);
    }

    @Bean
    AuditRetentionJob auditRetentionJob(AuditRetentionProperties properties, EntityManager entityManager) {
        return new AuditRetentionJob(properties, entityManager);
    }
}
