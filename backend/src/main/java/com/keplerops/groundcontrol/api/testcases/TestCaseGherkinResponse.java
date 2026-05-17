package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.model.TestCaseGherkin;
import java.time.Instant;
import java.util.UUID;

public record TestCaseGherkinResponse(UUID id, UUID testCaseId, String source, Instant createdAt, Instant updatedAt) {

    public static TestCaseGherkinResponse from(TestCaseGherkin gherkin) {
        return new TestCaseGherkinResponse(
                gherkin.getId(),
                gherkin.getTestCase().getId(),
                gherkin.getSource(),
                gherkin.getCreatedAt(),
                gherkin.getUpdatedAt());
    }
}
