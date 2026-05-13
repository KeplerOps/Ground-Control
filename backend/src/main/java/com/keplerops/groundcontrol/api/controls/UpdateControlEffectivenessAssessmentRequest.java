package com.keplerops.groundcontrol.api.controls;

import com.keplerops.groundcontrol.domain.controls.state.ControlEffectivenessRating;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Update DTO for {@link com.keplerops.groundcontrol.domain.controls.model.ControlEffectivenessAssessment}.
 * Every field is optional. Blank-when-present validation for {@code assessor} lives in the
 * service layer rather than a {@code @Pattern} regex — a {@code .*\\S.*} pattern would carry a
 * polynomial-backtracking risk, and {@code @NotBlank} would reject null (which breaks the
 * null-means-no-change contract). A non-null (but possibly empty) {@code supportingTestIds}
 * replaces the existing list wholesale; pass {@code null} to leave it unchanged, or an empty
 * list to clear it.
 */
public record UpdateControlEffectivenessAssessmentRequest(
        ControlEffectivenessRating designEffectiveness,
        ControlEffectivenessRating operatingEffectiveness,
        @PastOrPresent LocalDate assessedAt,
        @Size(max = 200) String assessor,
        String rationale,
        String notes,
        List<@NotNull UUID> supportingTestIds) {}
