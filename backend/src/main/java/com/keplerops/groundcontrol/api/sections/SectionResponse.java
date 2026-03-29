package com.keplerops.groundcontrol.api.sections;

import com.keplerops.groundcontrol.domain.documents.model.Section;
import java.time.Instant;
import java.util.UUID;

public record SectionResponse(
        UUID id,
        UUID documentId,
        UUID parentId,
        String title,
        String description,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt) {

    public static SectionResponse from(Section section) {
        return new SectionResponse(
                section.getId(),
                section.getDocument().getId(),
                section.getParent() != null ? section.getParent().getId() : null,
                section.getTitle(),
                section.getDescription(),
                section.getSortOrder(),
                section.getCreatedAt(),
                section.getUpdatedAt());
    }
}
