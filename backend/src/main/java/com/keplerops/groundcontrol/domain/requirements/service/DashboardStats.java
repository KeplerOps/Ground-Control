package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;
import java.util.Map;

public record DashboardStats(
        int totalRequirements,
        Map<String, Integer> byStatus,
        List<WaveStats> byWave,
        Map<String, CoverageStats> coverageByLinkType,
        List<RecentChange> recentChanges) {}
