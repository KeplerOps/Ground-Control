package com.keplerops.groundcontrol.api.sections;

import com.keplerops.groundcontrol.domain.documents.service.SectionTreeNode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SectionTreeResponse(
        UUID id,
        UUID parentId,
        String title,
        String description,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt,
        List<SectionTreeResponse> children) {

    public static SectionTreeResponse from(SectionTreeNode node) {
        return new SectionTreeResponse(
                node.id(),
                node.parentId(),
                node.title(),
                node.description(),
                node.sortOrder(),
                node.createdAt(),
                node.updatedAt(),
                node.children().stream().map(SectionTreeResponse::from).toList());
    }
}
