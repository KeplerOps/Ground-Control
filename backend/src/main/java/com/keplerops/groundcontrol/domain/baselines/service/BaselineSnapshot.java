package com.keplerops.groundcontrol.domain.baselines.service;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BaselineSnapshot(
        UUID baselineId, String name, int revisionNumber, Instant timestamp, List<Requirement> requirements) {}
