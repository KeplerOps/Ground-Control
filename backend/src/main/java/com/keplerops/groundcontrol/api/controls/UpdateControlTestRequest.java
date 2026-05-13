package com.keplerops.groundcontrol.api.controls;

import com.keplerops.groundcontrol.domain.controls.state.ControlTestConclusion;
import com.keplerops.groundcontrol.domain.controls.state.ControlTestMethodology;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Update DTO for {@link com.keplerops.groundcontrol.domain.controls.model.ControlTest}. Every
 * field is optional (null means "don't change"); but when present, evidence-bearing fields must
 * be non-blank — the {@code (?s)} flag lets the pattern match across newlines for multi-line
 * test steps / results, and {@code \\S} requires at least one non-whitespace character.
 * {@code @NotBlank} would reject null, which would break the null-means-no-change contract.
 */
public record UpdateControlTestRequest(
        ControlTestMethodology methodology,
        @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank when present") String testSteps,
        @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank when present") String expectedResults,
        @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank when present") String actualResults,
        ControlTestConclusion conclusion,
        @Pattern(regexp = ".*\\S.*", message = "must not be blank when present") @Size(max = 200) String testerIdentity,
        @PastOrPresent LocalDate testDate,
        String notes) {}
