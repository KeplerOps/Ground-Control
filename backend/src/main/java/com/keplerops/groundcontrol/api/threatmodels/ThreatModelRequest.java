package com.keplerops.groundcontrol.api.threatmodels;

import com.keplerops.groundcontrol.domain.threatmodels.state.StrideCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ThreatModelRequest(
        @NotBlank @Size(max = 30) String uid,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String threatSource,
        @NotBlank String threatEvent,
        @NotBlank String effect,
        StrideCategory stride,
        String narrative) {}
