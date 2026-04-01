package com.keplerops.groundcontrol.api.assets;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SubgraphRequest(@NotEmpty @Size(max = 100) List<String> rootUids) {}
