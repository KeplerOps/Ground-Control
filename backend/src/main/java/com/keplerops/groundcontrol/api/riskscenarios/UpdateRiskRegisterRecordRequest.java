package com.keplerops.groundcontrol.api.riskscenarios;

import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record UpdateRiskRegisterRecordRequest(
        @Size(max = 200) String title,
        @Size(max = 200) String owner,
        @Size(max = 100) String reviewCadence,
        Instant nextReviewAt,
        List<String> categoryTags,
        Map<String, Object> decisionMetadata,
        String assetScopeSummary,
        List<UUID> riskScenarioIds) {}
