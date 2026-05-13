package com.keplerops.groundcontrol.domain.controls.service;

import com.keplerops.groundcontrol.domain.controls.state.ControlEffectivenessRating;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateControlEffectivenessAssessmentCommand(
        ControlEffectivenessRating designEffectiveness,
        ControlEffectivenessRating operatingEffectiveness,
        LocalDate assessedAt,
        String assessor,
        String rationale,
        String notes,
        List<UUID> supportingTestIds) {}
