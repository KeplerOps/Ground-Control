package com.keplerops.groundcontrol.api.controls;

import com.keplerops.groundcontrol.domain.controls.state.ControlTestConclusion;
import com.keplerops.groundcontrol.domain.controls.state.ControlTestMethodology;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record ControlTestRequest(
        @NotNull UUID controlId,
        @NotBlank @Size(max = 50) String uid,
        @NotNull ControlTestMethodology methodology,
        @NotBlank String testSteps,
        @NotBlank String expectedResults,
        @NotBlank String actualResults,
        @NotNull ControlTestConclusion conclusion,
        @NotBlank @Size(max = 200) String testerIdentity,
        @NotNull @PastOrPresent LocalDate testDate,
        String notes) {}
