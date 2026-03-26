package com.keplerops.groundcontrol.domain.documents.service;

public record ReadingOrderContentItem(
        String contentType, String requirementUid, String requirementTitle, String textContent, int sortOrder) {}
