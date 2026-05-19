package com.keplerops.groundcontrol.domain.audits.service;

import com.keplerops.groundcontrol.domain.audits.model.AuditPhase;
import com.keplerops.groundcontrol.domain.audits.state.AuditType;
import java.util.List;

/**
 * Partial update for an audit.
 *
 * <p>String / enum fields use a tri-state convention:
 * <ul>
 *   <li>{@code null} → leave the field unchanged</li>
 *   <li>non-null value → replace the field</li>
 * </ul>
 *
 * <p>Optional list fields ({@code objectives}, {@code phases}, {@code teamMembers})
 * accept an explicit clear flag. When the clear flag is {@code true}, any value
 * passed in the corresponding field is ignored and the entity is updated to an
 * empty list.
 *
 * <p>Required fields ({@code title}, {@code scopeDescription}) are validated as
 * non-blank / present in the service when supplied; passing a blank string is
 * rejected with {@code DomainValidationException}.
 */
public record UpdateAuditCommand(
        String title,
        AuditType auditType,
        String scopeDescription,
        List<String> objectives,
        List<AuditPhase> phases,
        List<String> teamMembers,
        boolean clearObjectives,
        boolean clearPhases,
        boolean clearTeamMembers) {}
