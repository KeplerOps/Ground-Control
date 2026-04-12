package com.keplerops.groundcontrol.domain.threatmodels.service;

import com.keplerops.groundcontrol.domain.threatmodels.state.StrideCategory;

/**
 * Partial update for a threat model.
 *
 * <p>String/enum fields use a tri-state convention:
 * <ul>
 *   <li>{@code null} → leave the field unchanged</li>
 *   <li>non-null value → replace the field</li>
 * </ul>
 *
 * <p>For optional fields ({@code stride}, {@code narrative}) the {@code clearStride}
 * and {@code clearNarrative} flags allow callers to explicitly null the field. When
 * set to {@code true}, any value passed in the corresponding {@code stride} /
 * {@code narrative} field is ignored and the entity is updated to {@code null}.
 *
 * <p>Required fields ({@code title}, {@code threatSource}, {@code threatEvent},
 * {@code effect}) are validated as non-blank in the service when present; passing a
 * blank string is rejected with {@code DomainValidationException}.
 */
public record UpdateThreatModelCommand(
        String title,
        String threatSource,
        String threatEvent,
        String effect,
        StrideCategory stride,
        String narrative,
        boolean clearStride,
        boolean clearNarrative) {}
