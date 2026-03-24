package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import java.util.List;

public record BulkTransitionResult(List<Requirement> succeeded, List<BulkFailureDetail> failed) {

    public BulkTransitionResult {
        succeeded = List.copyOf(succeeded);
        failed = List.copyOf(failed);
    }
}
