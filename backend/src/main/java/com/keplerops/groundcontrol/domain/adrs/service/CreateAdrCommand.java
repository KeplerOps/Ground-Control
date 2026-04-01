package com.keplerops.groundcontrol.domain.adrs.service;

import java.time.LocalDate;
import java.util.UUID;

public record CreateAdrCommand(
        UUID projectId,
        String uid,
        String title,
        LocalDate decisionDate,
        String context,
        String decision,
        String consequences) {}
