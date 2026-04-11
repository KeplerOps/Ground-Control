package com.keplerops.groundcontrol.api.packregistry;

public record CompatibilityCheckResponse(String packId, String resolvedVersion, boolean compatible) {}
