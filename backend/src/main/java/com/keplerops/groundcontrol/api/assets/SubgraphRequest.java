package com.keplerops.groundcontrol.api.assets;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SubgraphRequest(@NotEmpty List<String> rootUids) {}
