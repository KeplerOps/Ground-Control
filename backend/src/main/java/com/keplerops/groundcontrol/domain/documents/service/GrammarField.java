package com.keplerops.groundcontrol.domain.documents.service;

import java.util.List;

public record GrammarField(String name, String type, boolean required, List<String> enumValues) {}
