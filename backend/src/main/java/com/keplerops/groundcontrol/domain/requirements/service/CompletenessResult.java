package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;
import java.util.Map;

public record CompletenessResult(int total, Map<String, Integer> byStatus, List<CompletenessIssue> issues) {}
