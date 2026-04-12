package com.keplerops.groundcontrol.domain.threatmodels.service;

import com.keplerops.groundcontrol.domain.threatmodels.state.StrideCategory;

public record UpdateThreatModelCommand(
        String title,
        String threatSource,
        String threatEvent,
        String effect,
        StrideCategory stride,
        String narrative) {}
