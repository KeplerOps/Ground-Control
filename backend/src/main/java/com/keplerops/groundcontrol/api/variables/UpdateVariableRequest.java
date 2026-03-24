package com.keplerops.groundcontrol.api.variables;

public record UpdateVariableRequest(String value, String description, Boolean secret) {}
