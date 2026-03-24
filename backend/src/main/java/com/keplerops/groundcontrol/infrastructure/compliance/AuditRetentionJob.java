package com.keplerops.groundcontrol.infrastructure.compliance;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

public class AuditRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(AuditRetentionJob.class);

    private final AuditRetentionProperties properties;
    private final EntityManager entityManager;

    public AuditRetentionJob(AuditRetentionProperties properties, EntityManager entityManager) {
        this.properties = properties;
        this.entityManager = entityManager;
    }

    @Scheduled(cron = "${groundcontrol.compliance.audit-retention-cron:0 0 3 * * *}")
    @Transactional
    public void purgeOldAuditRecords() {
        long cutoffMs = System.currentTimeMillis() - ((long) properties.retentionDays() * 24 * 60 * 60 * 1000);

        int requirementAudit = entityManager
                .createNativeQuery("DELETE FROM requirement_audit WHERE rev IN "
                        + "(SELECT rev FROM revinfo WHERE revtstmp < :cutoff)")
                .setParameter("cutoff", cutoffMs)
                .executeUpdate();

        int relationAudit = entityManager
                .createNativeQuery("DELETE FROM requirement_relation_audit WHERE rev IN "
                        + "(SELECT rev FROM revinfo WHERE revtstmp < :cutoff)")
                .setParameter("cutoff", cutoffMs)
                .executeUpdate();

        int linkAudit = entityManager
                .createNativeQuery("DELETE FROM traceability_link_audit WHERE rev IN "
                        + "(SELECT rev FROM revinfo WHERE revtstmp < :cutoff)")
                .setParameter("cutoff", cutoffMs)
                .executeUpdate();

        int revinfo = entityManager
                .createNativeQuery("DELETE FROM revinfo WHERE revtstmp < :cutoff")
                .setParameter("cutoff", cutoffMs)
                .executeUpdate();

        log.info(
                "Audit retention cleanup: deleted {} requirement, {} relation, {} link audit records, {} revinfo entries (retention={} days)",
                requirementAudit,
                relationAudit,
                linkAudit,
                revinfo,
                properties.retentionDays());
    }
}
