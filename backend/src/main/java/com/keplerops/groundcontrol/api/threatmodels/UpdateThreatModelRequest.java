package com.keplerops.groundcontrol.api.threatmodels;

import com.keplerops.groundcontrol.domain.threatmodels.state.StrideCategory;
import jakarta.validation.constraints.Size;

public record UpdateThreatModelRequest(
        @Size(max = 200) String title,
        String threatSource,
        String threatEvent,
        String effect,
        StrideCategory stride,
        String narrative) {}
