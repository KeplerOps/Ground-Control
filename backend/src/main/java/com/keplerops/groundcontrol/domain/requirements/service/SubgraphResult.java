package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import java.util.List;

public record SubgraphResult(List<Requirement> requirements, List<RequirementRelation> relations) {}
