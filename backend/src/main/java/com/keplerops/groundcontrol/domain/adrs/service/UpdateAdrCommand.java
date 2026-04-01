package com.keplerops.groundcontrol.domain.adrs.service;

import java.time.LocalDate;

public record UpdateAdrCommand(
        String title,
        LocalDate decisionDate,
        String context,
        String decision,
        String consequences,
        String supersededBy) {}
