package com.keplerops.groundcontrol.api.adrs;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateAdrRequest(
        @Size(max = 200) String title,
        LocalDate decisionDate,
        String context,
        String decision,
        String consequences,
        @Size(max = 20) String supersededBy) {}
