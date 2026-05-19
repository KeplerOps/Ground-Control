package com.keplerops.groundcontrol.api.controls;

import com.keplerops.groundcontrol.domain.controls.state.ControlEffectivenessRating;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ControlEffectivenessAssessmentRequest(
        @NotNull UUID controlId,
        @NotBlank @Size(max = 50) String uid,
        @NotNull ControlEffectivenessRating designEffectiveness,
        @NotNull ControlEffectivenessRating operatingEffectiveness,
        @NotNull @PastOrPresent LocalDate assessedAt,
        @NotBlank @Size(max = 200) String assessor,
        String rationale,
        String notes,
        List<@NotNull UUID> supportingTestIds) {}
