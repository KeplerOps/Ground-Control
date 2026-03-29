package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;

/** Root result of parsing a StrictDoc (.sdoc) file. */
public record SdocDocument(List<SdocSection> sections, List<SdocRequirement> requirements) {

    public SdocDocument {
        sections = List.copyOf(sections);
        requirements = List.copyOf(requirements);
    }
}
