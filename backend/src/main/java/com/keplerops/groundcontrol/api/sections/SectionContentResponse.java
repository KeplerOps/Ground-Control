package com.keplerops.groundcontrol.api.sections;

import com.keplerops.groundcontrol.domain.documents.model.SectionContent;
import java.time.Instant;
import java.util.UUID;

public record SectionContentResponse(
        UUID id,
        UUID sectionId,
        String contentType,
        UUID requirementId,
        String requirementUid,
        String textContent,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt) {

    public static SectionContentResponse from(SectionContent content) {
        return new SectionContentResponse(
                content.getId(),
                content.getSection().getId(),
                content.getContentType().name(),
                content.getRequirement() != null ? content.getRequirement().getId() : null,
                content.getRequirement() != null ? content.getRequirement().getUid() : null,
                content.getTextContent(),
                content.getSortOrder(),
                content.getCreatedAt(),
                content.getUpdatedAt());
    }
}
