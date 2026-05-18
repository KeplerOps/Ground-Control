package com.keplerops.groundcontrol.domain.audits.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.keplerops.groundcontrol.domain.audits.state.AuditPhaseKind;
import java.time.LocalDate;

/**
 * Value type representing a single phase of an audit timeline per GC-U001 /
 * ADR-048. Serialised as a JSON object in the {@code phases} TEXT column via
 * {@link com.keplerops.groundcontrol.shared.persistence.JacksonTextCollectionConverters.AuditPhaseListConverter}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditPhase(
        AuditPhaseKind kind,
        LocalDate plannedStart,
        LocalDate plannedEnd,
        LocalDate actualStart,
        LocalDate actualEnd) {}
