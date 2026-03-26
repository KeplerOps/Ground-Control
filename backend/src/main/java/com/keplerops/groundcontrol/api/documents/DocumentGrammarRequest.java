package com.keplerops.groundcontrol.api.documents;

import java.util.List;

public record DocumentGrammarRequest(
        List<FieldDef> fields, List<String> allowedRequirementTypes, List<String> allowedRelationTypes) {

    public record FieldDef(String name, String type, boolean required, List<String> enumValues) {}
}
