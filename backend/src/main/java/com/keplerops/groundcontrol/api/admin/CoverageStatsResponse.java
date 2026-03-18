package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.CoverageStats;

public record CoverageStatsResponse(int total, int covered, double percentage) {

    public static CoverageStatsResponse from(CoverageStats c) {
        return new CoverageStatsResponse(c.total(), c.covered(), c.percentage());
    }
}
