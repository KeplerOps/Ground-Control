package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.service.GherkinValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Update DTO for Gherkin source. Source is required and full-replace — there
 * is no null-means-no-change semantic because the resource is a single field.
 */
public record UpdateTestCaseGherkinRequest(@NotBlank @Size(max = GherkinValidator.MAX_SOURCE_LENGTH) String source) {}
