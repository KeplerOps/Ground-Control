package com.keplerops.groundcontrol.domain.requirements.service;

/** A content item within a StrictDoc section: either a requirement reference or a text block. */
public sealed interface SdocContentItem {

    record RequirementRef(String uid) implements SdocContentItem {}

    record TextBlock(String text) implements SdocContentItem {}
}
