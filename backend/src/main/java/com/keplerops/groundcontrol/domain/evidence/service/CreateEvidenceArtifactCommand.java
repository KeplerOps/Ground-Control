package com.keplerops.groundcontrol.domain.evidence.service;

import com.keplerops.groundcontrol.domain.evidence.model.EvidenceSourceRef;
import com.keplerops.groundcontrol.domain.evidence.state.EvidenceType;
import com.keplerops.groundcontrol.domain.verification.state.AssuranceLevel;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateEvidenceArtifactCommand(
        UUID projectId,
        String uid,
        String title,
        String summary,
        EvidenceType evidenceType,
        String derivationMethod,
        Instant derivedAt,
        AssuranceLevel assuranceLevel,
        String confidence,
        String notes,
        List<EvidenceSourceRef> sources) {}
