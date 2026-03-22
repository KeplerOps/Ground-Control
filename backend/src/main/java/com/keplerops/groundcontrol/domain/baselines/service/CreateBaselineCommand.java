package com.keplerops.groundcontrol.domain.baselines.service;

import java.util.UUID;

public record CreateBaselineCommand(UUID projectId, String name, String description) {}
