package com.keplerops.groundcontrol.domain.documents.service;

import java.util.UUID;

public record CreateSectionCommand(UUID documentId, UUID parentId, String title, String description, int sortOrder) {}
