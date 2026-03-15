package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import java.util.List;
import java.util.Map;

public record BulkTransitionResult(List<Requirement> succeeded, List<Map<String, Object>> failed) {

    public BulkTransitionResult {
        succeeded = List.copyOf(succeeded);
        failed = List.copyOf(failed);
    }
}
