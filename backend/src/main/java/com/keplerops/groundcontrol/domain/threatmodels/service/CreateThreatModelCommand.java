package com.keplerops.groundcontrol.domain.threatmodels.service;

import com.keplerops.groundcontrol.domain.threatmodels.state.StrideCategory;
import java.util.UUID;

public record CreateThreatModelCommand(
        UUID projectId,
        String uid,
        String title,
        String threatSource,
        String threatEvent,
        String effect,
        StrideCategory stride,
        String narrative) {}
