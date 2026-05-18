package com.keplerops.groundcontrol.domain.audits.service;

import com.keplerops.groundcontrol.domain.audits.model.AuditPhase;
import com.keplerops.groundcontrol.domain.audits.state.AuditType;
import java.util.List;
import java.util.UUID;

public record CreateAuditCommand(
        UUID projectId,
        String uid,
        String title,
        AuditType auditType,
        String scopeDescription,
        List<String> objectives,
        List<AuditPhase> phases,
        List<String> teamMembers) {}
