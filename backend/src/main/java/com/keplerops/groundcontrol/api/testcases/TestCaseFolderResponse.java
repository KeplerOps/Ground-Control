package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.model.TestCaseFolder;
import java.time.Instant;
import java.util.UUID;

public record TestCaseFolderResponse(
        UUID id,
        String projectIdentifier,
        UUID parentFolderId,
        String title,
        String description,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt) {

    public static TestCaseFolderResponse from(TestCaseFolder folder) {
        return new TestCaseFolderResponse(
                folder.getId(),
                folder.getProject().getIdentifier(),
                folder.getParent() != null ? folder.getParent().getId() : null,
                folder.getTitle(),
                folder.getDescription(),
                folder.getSortOrder(),
                folder.getCreatedAt(),
                folder.getUpdatedAt());
    }
}
