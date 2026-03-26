package com.keplerops.groundcontrol.domain.documents.service;

import java.util.List;

public record DocumentGrammar(
        List<GrammarField> fields, List<String> allowedRequirementTypes, List<String> allowedRelationTypes) {}
