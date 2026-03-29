package com.keplerops.groundcontrol.api.documents;

import jakarta.validation.constraints.Size;

public record UpdateDocumentRequest(
        @Size(max = 200) String title, @Size(max = 50) String version, String description) {}
