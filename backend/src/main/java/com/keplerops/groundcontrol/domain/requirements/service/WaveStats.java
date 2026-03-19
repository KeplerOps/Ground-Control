package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.Map;

public record WaveStats(Integer wave, int total, Map<String, Integer> byStatus) {}
