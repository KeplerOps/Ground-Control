package com.keplerops.groundcontrol.domain.controlpacks.service;

public record CreateControlPackOverrideCommand(String fieldName, String overrideValue, String reason) {}
