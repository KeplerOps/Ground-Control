package com.keplerops.groundcontrol.domain.documents.service;

public record UpdateSectionCommand(String title, String description, Integer sortOrder) {}
