package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementExportRecord;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementsExportData;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequirementsExportDataTest {

    private static final Project PROJECT = new Project("test", "Test");

    @Test
    void from_emptyRecords_producesEmptySnapshots() {
        var result = RequirementsExportData.from("test-project", List.of());
        assertThat(result.projectIdentifier()).isEqualTo("test-project");
        assertThat(result.requirements()).isEmpty();
        assertThat(result.timestamp()).isNotNull();
    }

    @Test
    void from_recordWithRequirement_mapsAllFields() {
        var req = new Requirement(PROJECT, "REQ-001", "Title", "Statement");
        req.setRationale("Rationale");
        req.setWave(2);
        var record = new RequirementExportRecord(req, List.of());

        var result = RequirementsExportData.from("proj", List.of(record));

        assertThat(result.requirements()).hasSize(1);
        var snapshot = result.requirements().get(0);
        assertThat(snapshot.uid()).isEqualTo("REQ-001");
        assertThat(snapshot.title()).isEqualTo("Title");
        assertThat(snapshot.statement()).isEqualTo("Statement");
        assertThat(snapshot.rationale()).isEqualTo("Rationale");
        assertThat(snapshot.requirementType()).isEqualTo("FUNCTIONAL");
        assertThat(snapshot.priority()).isEqualTo("MUST");
        assertThat(snapshot.status()).isEqualTo("DRAFT");
        assertThat(snapshot.wave()).isEqualTo(2);
    }

    @Test
    void from_recordWithTraceabilityLinks_mapsLinks() {
        var req = new Requirement(PROJECT, "REQ-002", "Title", "Statement");
        var link = new TraceabilityLink(req, ArtifactType.CODE_FILE, "Main.java", LinkType.IMPLEMENTS);
        link.setArtifactUrl("https://example.com");
        link.setArtifactTitle("Main class");
        var record = new RequirementExportRecord(req, List.of(link));

        var result = RequirementsExportData.from("proj", List.of(record));

        var snapshot = result.requirements().get(0);
        assertThat(snapshot.traceabilityLinks()).hasSize(1);
        var linkSnapshot = snapshot.traceabilityLinks().get(0);
        assertThat(linkSnapshot.artifactType()).isEqualTo("CODE_FILE");
        assertThat(linkSnapshot.artifactIdentifier()).isEqualTo("Main.java");
        assertThat(linkSnapshot.linkType()).isEqualTo("IMPLEMENTS");
        assertThat(linkSnapshot.artifactUrl()).isEqualTo("https://example.com");
        assertThat(linkSnapshot.artifactTitle()).isEqualTo("Main class");
    }
}
