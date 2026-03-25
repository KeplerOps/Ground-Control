package com.keplerops.groundcontrol.domain.documents.service;

import java.util.Optional;

public record UpdateDocumentCommand(
        String title, String version, Optional<String> description, boolean descriptionProvided) {

    public static UpdateDocumentCommand of(String title, String version, Optional<String> description) {
        return new UpdateDocumentCommand(
                title, version, description != null ? description : Optional.empty(), description != null);
    }
}
