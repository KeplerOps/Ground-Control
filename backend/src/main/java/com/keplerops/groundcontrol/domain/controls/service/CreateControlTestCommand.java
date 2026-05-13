package com.keplerops.groundcontrol.domain.controls.service;

import com.keplerops.groundcontrol.domain.controls.state.ControlTestConclusion;
import com.keplerops.groundcontrol.domain.controls.state.ControlTestMethodology;
import java.time.LocalDate;
import java.util.UUID;

public record CreateControlTestCommand(
        UUID projectId,
        UUID controlId,
        String uid,
        ControlTestMethodology methodology,
        String testSteps,
        String expectedResults,
        String actualResults,
        ControlTestConclusion conclusion,
        String testerIdentity,
        LocalDate testDate,
        String notes) {}
