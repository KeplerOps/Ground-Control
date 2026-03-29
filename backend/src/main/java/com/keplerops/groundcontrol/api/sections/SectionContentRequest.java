package com.keplerops.groundcontrol.api.sections;

import com.keplerops.groundcontrol.domain.documents.model.ContentType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SectionContentRequest(
        @NotNull ContentType contentType, UUID requirementId, String textContent, Integer sortOrder) {}
