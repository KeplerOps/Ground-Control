package com.keplerops.groundcontrol.domain.documents.service;

import java.util.List;
import java.util.UUID;

public record ReadingOrderNode(
        UUID id,
        String title,
        String description,
        int sortOrder,
        List<ReadingOrderContentItem> content,
        List<ReadingOrderNode> children) {}
