package com.keplerops.groundcontrol.domain.documents.service;

import com.keplerops.groundcontrol.domain.documents.model.ContentType;
import java.util.UUID;

public record CreateSectionContentCommand(
        UUID sectionId, ContentType contentType, UUID requirementId, String textContent, int sortOrder) {}
