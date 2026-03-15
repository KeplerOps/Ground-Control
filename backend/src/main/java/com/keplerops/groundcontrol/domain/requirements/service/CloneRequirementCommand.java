package com.keplerops.groundcontrol.domain.requirements.service;

public record CloneRequirementCommand(String newUid, boolean copyRelations) {}
