package com.keplerops.groundcontrol.api.audits;

import com.keplerops.groundcontrol.domain.audits.state.AuditType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateAuditRequest(
        @Size(max = 200) String title,
        AuditType auditType,
        String scopeDescription,
        @Size(max = 50) List<@Size(max = 500) String> objectives,
        @Valid @Size(max = 50) List<AuditPhaseDto> phases,
        @Size(max = 50) List<@Size(max = 100) String> teamMembers,
        Boolean clearObjectives,
        Boolean clearPhases,
        Boolean clearTeamMembers) {}
