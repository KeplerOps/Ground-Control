package com.keplerops.groundcontrol.domain.findings.service;

import com.keplerops.groundcontrol.domain.findings.state.FindingSeverity;
import com.keplerops.groundcontrol.domain.findings.state.FindingType;
import java.time.LocalDate;

/**
 * Partial update for a finding.
 *
 * <p>String / enum / date fields use a tri-state convention:
 * <ul>
 *   <li>{@code null} → leave the field unchanged</li>
 *   <li>non-null value → replace the field</li>
 * </ul>
 *
 * <p>Optional fields ({@code rootCauseAnalysis}, {@code owner}, {@code dueDate})
 * accept an explicit clear flag. When the clear flag is {@code true}, any value
 * passed in the corresponding field is ignored and the entity is updated to
 * {@code null}.
 *
 * <p>Required fields ({@code title}, {@code description}, plus the enums) are
 * validated as non-blank / present in the service when supplied; passing a blank
 * string is rejected with {@code DomainValidationException}.
 */
public record UpdateFindingCommand(
        String title,
        FindingType findingType,
        FindingSeverity severity,
        String description,
        String rootCauseAnalysis,
        String owner,
        LocalDate dueDate,
        boolean clearRootCauseAnalysis,
        boolean clearOwner,
        boolean clearDueDate) {}
