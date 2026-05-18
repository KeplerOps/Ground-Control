package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.model.TestSuiteMember;
import java.time.Instant;
import java.util.UUID;

public record TestSuiteMemberResponse(
        UUID id,
        UUID testSuiteId,
        UUID testCaseId,
        String testCaseUid,
        int position,
        Instant createdAt,
        Instant updatedAt) {

    public static TestSuiteMemberResponse from(TestSuiteMember member) {
        return new TestSuiteMemberResponse(
                member.getId(),
                member.getTestSuite().getId(),
                member.getTestCase().getId(),
                member.getTestCase().getUid(),
                member.getPosition(),
                member.getCreatedAt(),
                member.getUpdatedAt());
    }
}
