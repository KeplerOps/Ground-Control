package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import com.keplerops.groundcontrol.domain.testcases.state.TestSuitePopulationMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record TestSuiteRequest(
        @NotBlank @Size(max = 50) String uid,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 8192) String description,
        @NotNull TestSuitePopulationMode populationMode,
        TestCaseStatus criteriaStatus,
        TestCaseType criteriaType,
        TestCasePriority criteriaPriority,
        TestCaseFormat criteriaFormat,
        UUID criteriaFolderId,
        @Size(max = 200) String criteriaTextSearch) {}
