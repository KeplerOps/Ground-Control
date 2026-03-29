package com.keplerops.groundcontrol.domain.documents.service;

import java.util.List;
import java.util.UUID;

public record DocumentReadingOrder(
        UUID documentId, String title, String version, String description, List<ReadingOrderNode> sections) {}
