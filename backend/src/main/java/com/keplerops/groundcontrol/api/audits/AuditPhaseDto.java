package com.keplerops.groundcontrol.api.audits;

import com.keplerops.groundcontrol.domain.audits.model.AuditPhase;
import com.keplerops.groundcontrol.domain.audits.state.AuditPhaseKind;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AuditPhaseDto(
        @NotNull AuditPhaseKind kind,
        LocalDate plannedStart,
        LocalDate plannedEnd,
        LocalDate actualStart,
        LocalDate actualEnd) {

    public AuditPhase toDomain() {
        return new AuditPhase(kind, plannedStart, plannedEnd, actualStart, actualEnd);
    }

    public static AuditPhaseDto fromDomain(AuditPhase phase) {
        return new AuditPhaseDto(
                phase.kind(), phase.plannedStart(), phase.plannedEnd(), phase.actualStart(), phase.actualEnd());
    }
}
