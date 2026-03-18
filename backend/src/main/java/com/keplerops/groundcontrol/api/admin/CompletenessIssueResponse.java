package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.CompletenessIssue;

public record CompletenessIssueResponse(String uid, String issue) {

    public static CompletenessIssueResponse from(CompletenessIssue i) {
        return new CompletenessIssueResponse(i.uid(), i.issue());
    }
}
