package com.keplerops.groundcontrol.domain.documents.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SectionTreeNode(
        UUID id,
        UUID parentId,
        String title,
        String description,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt,
        List<SectionTreeNode> children) {}
