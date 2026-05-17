package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.service.GherkinValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestCaseGherkinRequest(@NotBlank @Size(max = GherkinValidator.MAX_SOURCE_LENGTH) String source) {}
