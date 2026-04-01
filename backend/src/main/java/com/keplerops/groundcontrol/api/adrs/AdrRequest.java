package com.keplerops.groundcontrol.api.adrs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AdrRequest(
        @NotBlank @Size(max = 20) String uid,
        @NotBlank @Size(max = 200) String title,
        @NotNull LocalDate decisionDate,
        String context,
        String decision,
        String consequences) {}
