package com.keplerops.groundcontrol.infrastructure.compliance;

public record AuditRetentionProperties(int retentionDays, String cron) {}
