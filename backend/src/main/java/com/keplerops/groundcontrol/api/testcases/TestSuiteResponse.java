package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.model.TestSuite;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import com.keplerops.groundcontrol.domain.testcases.state.TestSuitePopulationMode;
import java.time.Instant;
import java.util.UUID;

public record TestSuiteResponse(
        UUID id,
        String projectIdentifier,
        String uid,
        String name,
        String description,
        TestSuitePopulationMode populationMode,
        TestCaseStatus criteriaStatus,
        TestCaseType criteriaType,
        TestCasePriority criteriaPriority,
        TestCaseFormat criteriaFormat,
        UUID criteriaFolderId,
        String criteriaTextSearch,
        Instant createdAt,
        Instant updatedAt) {

    public static TestSuiteResponse from(TestSuite suite) {
        return new TestSuiteResponse(
                suite.getId(),
                suite.getProject().getIdentifier(),
                suite.getUid(),
                suite.getName(),
                suite.getDescription(),
                suite.getPopulationMode(),
                suite.getCriteriaStatus(),
                suite.getCriteriaType(),
                suite.getCriteriaPriority(),
                suite.getCriteriaFormat(),
                suite.getCriteriaFolderId(),
                suite.getCriteriaTextSearch(),
                suite.getCreatedAt(),
                suite.getUpdatedAt());
    }
}
