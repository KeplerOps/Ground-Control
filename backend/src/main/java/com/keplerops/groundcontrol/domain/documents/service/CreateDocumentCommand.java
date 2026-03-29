package com.keplerops.groundcontrol.domain.documents.service;

import java.util.UUID;

public record CreateDocumentCommand(UUID projectId, String title, String version, String description) {}
