package com.keplerops.groundcontrol.api.controls;

import com.keplerops.groundcontrol.domain.controls.state.ControlEffectivenessRating;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Update DTO for {@link com.keplerops.groundcontrol.domain.controls.model.ControlEffectivenessAssessment}.
 * Every field is optional; but when present, {@code assessor} cannot be blank because it is the
 * domain-provenance record of who made the rating. The {@code @Pattern} validator rejects
 * whitespace-only strings while still letting null mean "no change" (which {@code @NotBlank}
 * would not allow). A non-null (but possibly empty) {@code supportingTestIds} replaces the
 * existing list wholesale; pass {@code null} to leave it unchanged, or an empty list to clear it.
 */
public record UpdateControlEffectivenessAssessmentRequest(
        ControlEffectivenessRating designEffectiveness,
        ControlEffectivenessRating operatingEffectiveness,
        @PastOrPresent LocalDate assessedAt,
        @Pattern(regexp = ".*\\S.*", message = "must not be blank when present") @Size(max = 200) String assessor,
        String rationale,
        String notes,
        List<@NotNull UUID> supportingTestIds) {}
