package com.keplerops.groundcontrol.domain.riskscenarios.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record UpdateRiskRegisterRecordCommand(
        String title,
        String owner,
        String reviewCadence,
        Instant nextReviewAt,
        List<String> categoryTags,
        Map<String, Object> decisionMetadata,
        String assetScopeSummary,
        List<UUID> riskScenarioIds) {}
