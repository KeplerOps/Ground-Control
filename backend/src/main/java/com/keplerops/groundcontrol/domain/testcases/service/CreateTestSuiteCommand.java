package com.keplerops.groundcontrol.domain.testcases.service;

import com.keplerops.groundcontrol.domain.testcases.state.TestSuitePopulationMode;
import java.util.UUID;

public record CreateTestSuiteCommand(
        UUID projectId,
        String uid,
        String name,
        String description,
        TestSuitePopulationMode populationMode,
        TestSuiteCriteriaCommand criteria) {}
