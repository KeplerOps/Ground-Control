package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.WaveStats;
import java.util.Map;

public record WaveStatsResponse(Integer wave, int total, Map<String, Integer> byStatus) {

    public static WaveStatsResponse from(WaveStats w) {
        return new WaveStatsResponse(w.wave(), w.total(), w.byStatus());
    }
}
