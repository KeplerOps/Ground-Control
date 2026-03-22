package com.keplerops.groundcontrol.domain.baselines.service;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import java.util.List;
import java.util.UUID;

public record BaselineComparison(
        UUID baselineId,
        String baselineName,
        UUID otherBaselineId,
        String otherBaselineName,
        List<Requirement> added,
        List<Requirement> removed,
        List<ModifiedRequirement> modified) {}
