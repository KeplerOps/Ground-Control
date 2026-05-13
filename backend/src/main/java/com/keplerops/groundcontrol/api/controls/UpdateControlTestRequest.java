package com.keplerops.groundcontrol.api.controls;

import com.keplerops.groundcontrol.domain.controls.state.ControlTestConclusion;
import com.keplerops.groundcontrol.domain.controls.state.ControlTestMethodology;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Update DTO for {@link com.keplerops.groundcontrol.domain.controls.model.ControlTest}. Every
 * field is optional (null means "don't change"). Blank-when-present validation lives in the
 * service layer rather than a {@code @Pattern} regex — a {@code .*\\S.*} pattern would carry a
 * polynomial-backtracking risk on adversarial input, and {@code @NotBlank} would reject null
 * (which breaks the null-means-no-change contract). The service checks {@code !s.isBlank()} on
 * every present evidence/provenance field before applying it.
 */
public record UpdateControlTestRequest(
        ControlTestMethodology methodology,
        String testSteps,
        String expectedResults,
        String actualResults,
        ControlTestConclusion conclusion,
        @Size(max = 200) String testerIdentity,
        @PastOrPresent LocalDate testDate,
        String notes) {}
