package com.keplerops.groundcontrol.api.baselines;

import com.keplerops.groundcontrol.api.requirements.RequirementResponse;
import com.keplerops.groundcontrol.domain.baselines.service.BaselineSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BaselineSnapshotResponse(
        UUID baselineId,
        String name,
        int revisionNumber,
        Instant timestamp,
        int requirementCount,
        List<RequirementResponse> requirements) {

    public static BaselineSnapshotResponse from(BaselineSnapshot snapshot) {
        var reqs =
                snapshot.requirements().stream().map(RequirementResponse::from).toList();
        return new BaselineSnapshotResponse(
                snapshot.baselineId(),
                snapshot.name(),
                snapshot.revisionNumber(),
                snapshot.timestamp(),
                reqs.size(),
                reqs);
    }
}
