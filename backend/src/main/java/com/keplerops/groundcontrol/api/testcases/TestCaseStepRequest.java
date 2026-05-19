package com.keplerops.groundcontrol.api.testcases;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Create request for a step. Rich-text fields are bounded at 10,000 characters
 * each (extends ADR-040 §Rich text to TC-002 steps): generous enough for real
 * prose with embedded Markdown image references, finite enough to constrain
 * untrusted-content footprint and response size. See ADR-041.
 */
public record TestCaseStepRequest(
        @NotNull @Positive Integer stepNumber,
        @NotBlank @Size(max = 10000) String action,
        @NotBlank @Size(max = 10000) String expectedResult,
        @Size(max = 10000) String actualResult) {}
