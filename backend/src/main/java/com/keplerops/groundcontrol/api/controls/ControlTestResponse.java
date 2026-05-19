package com.keplerops.groundcontrol.api.controls;

import com.keplerops.groundcontrol.domain.controls.model.ControlTest;
import com.keplerops.groundcontrol.domain.controls.state.ControlTestConclusion;
import com.keplerops.groundcontrol.domain.controls.state.ControlTestMethodology;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ControlTestResponse(
        UUID id,
        String graphNodeId,
        String projectIdentifier,
        UUID controlId,
        String controlUid,
        String uid,
        ControlTestMethodology methodology,
        String testSteps,
        String expectedResults,
        String actualResults,
        ControlTestConclusion conclusion,
        String testerIdentity,
        LocalDate testDate,
        String notes,
        Instant createdAt,
        Instant updatedAt) {

    public static ControlTestResponse from(ControlTest controlTest) {
        return new ControlTestResponse(
                controlTest.getId(),
                GraphIds.nodeId(GraphEntityType.CONTROL_TEST, controlTest.getId()),
                controlTest.getProject().getIdentifier(),
                controlTest.getControl().getId(),
                controlTest.getControl().getUid(),
                controlTest.getUid(),
                controlTest.getMethodology(),
                controlTest.getTestSteps(),
                controlTest.getExpectedResults(),
                controlTest.getActualResults(),
                controlTest.getConclusion(),
                controlTest.getTesterIdentity(),
                controlTest.getTestDate(),
                controlTest.getNotes(),
                controlTest.getCreatedAt(),
                controlTest.getUpdatedAt());
    }
}
