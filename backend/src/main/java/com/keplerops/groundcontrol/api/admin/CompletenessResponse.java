package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.CompletenessResult;
import java.util.List;
import java.util.Map;

public record CompletenessResponse(int total, Map<String, Integer> byStatus, List<CompletenessIssueResponse> issues) {

    public static CompletenessResponse from(CompletenessResult r) {
        return new CompletenessResponse(
                r.total(),
                r.byStatus(),
                r.issues().stream().map(CompletenessIssueResponse::from).toList());
    }
}
