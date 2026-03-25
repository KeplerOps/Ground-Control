package com.keplerops.groundcontrol.domain.documents.service;

import java.util.Optional;

public record UpdateDocumentCommand(String title, String version, Optional<String> description) {}
