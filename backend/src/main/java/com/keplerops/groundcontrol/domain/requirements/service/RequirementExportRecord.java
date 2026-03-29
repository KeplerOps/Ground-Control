package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import java.util.List;

public record RequirementExportRecord(Requirement requirement, List<TraceabilityLink> traceabilityLinks) {}
