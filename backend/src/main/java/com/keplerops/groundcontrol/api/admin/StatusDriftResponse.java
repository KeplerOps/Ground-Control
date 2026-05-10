package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.StatusDriftResult;
import com.keplerops.groundcontrol.domain.requirements.state.ConfidenceLevel;
import com.keplerops.groundcontrol.domain.requirements.state.StatusDriftSignal;
import java.util.List;

/** API view of a status-drift analysis. */
public record StatusDriftResponse(
        int draftRequirementsScanned, ConfidenceLevel minimumConfidence, List<FindingRef> findings) {

    public static StatusDriftResponse from(StatusDriftResult result) {
        return new StatusDriftResponse(
                result.draftRequirementsScanned(),
                result.minimumConfidence(),
                result.findings().stream().map(FindingRef::from).toList());
    }

    public record FindingRef(
            String uid,
            String title,
            ConfidenceLevel confidence,
            StatusDriftSignal strongestSignal,
            List<EvidenceRef> evidence) {

        public static FindingRef from(StatusDriftResult.Finding finding) {
            return new FindingRef(
                    finding.uid(),
                    finding.title(),
                    finding.confidence(),
                    finding.strongestSignal(),
                    finding.evidence().stream().map(EvidenceRef::from).toList());
        }
    }

    public record EvidenceRef(
            StatusDriftSignal signal,
            ConfidenceLevel confidence,
            String artifactType,
            String artifactIdentifier,
            String artifactTitle,
            String artifactUrl,
            String detail) {

        public static EvidenceRef from(StatusDriftResult.Evidence evidence) {
            return new EvidenceRef(
                    evidence.signal(),
                    evidence.confidence(),
                    evidence.artifactType(),
                    evidence.artifactIdentifier(),
                    evidence.artifactTitle(),
                    evidence.artifactUrl(),
                    evidence.detail());
        }
    }
}
