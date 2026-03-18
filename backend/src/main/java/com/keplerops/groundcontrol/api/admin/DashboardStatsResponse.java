package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.DashboardStats;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DashboardStatsResponse(
        int totalRequirements,
        Map<String, Integer> byStatus,
        List<WaveStatsResponse> byWave,
        Map<String, CoverageStatsResponse> coverageByLinkType,
        List<RecentChangeResponse> recentChanges) {

    public static DashboardStatsResponse from(DashboardStats s) {
        Map<String, CoverageStatsResponse> coverage = new LinkedHashMap<>();
        s.coverageByLinkType().forEach((k, v) -> coverage.put(k, CoverageStatsResponse.from(v)));

        return new DashboardStatsResponse(
                s.totalRequirements(),
                s.byStatus(),
                s.byWave().stream().map(WaveStatsResponse::from).toList(),
                coverage,
                s.recentChanges().stream().map(RecentChangeResponse::from).toList());
    }
}
