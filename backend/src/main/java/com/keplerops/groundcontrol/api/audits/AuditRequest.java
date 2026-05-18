package com.keplerops.groundcontrol.api.audits;

import com.keplerops.groundcontrol.domain.audits.state.AuditType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AuditRequest(
        @NotBlank @Size(max = 30) String uid,
        @NotBlank @Size(max = 200) String title,
        @NotNull AuditType auditType,
        @NotBlank String scopeDescription,
        @Size(max = 50) List<@Size(max = 500) String> objectives,
        @Valid @Size(max = 50) List<AuditPhaseDto> phases,
        @Size(max = 50) List<@Size(max = 100) String> teamMembers) {}
