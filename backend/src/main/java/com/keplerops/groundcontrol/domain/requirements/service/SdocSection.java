package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;

/** A parsed StrictDoc section containing ordered content items. */
public record SdocSection(String title, Integer wave, List<SdocContentItem> items) {

    public SdocSection {
        items = List.copyOf(items);
    }
}
