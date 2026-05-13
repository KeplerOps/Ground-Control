package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.findings.model.Finding;
import com.keplerops.groundcontrol.domain.findings.model.FindingLink;
import com.keplerops.groundcontrol.domain.findings.repository.FindingLinkRepository;
import com.keplerops.groundcontrol.domain.findings.repository.FindingRepository;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkTargetType;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkType;
import com.keplerops.groundcontrol.domain.findings.state.FindingSeverity;
import com.keplerops.groundcontrol.domain.findings.state.FindingStatus;
import com.keplerops.groundcontrol.domain.findings.state.FindingType;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.graph.service.FindingGraphProjectionContributor;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenario;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindingGraphProjectionContributorTest {

    @Mock
    private FindingRepository findingRepository;

    @Mock
    private FindingLinkRepository findingLinkRepository;

    @Mock
    private OperationalAssetRepository operationalAssetRepository;

    @Mock
    private RiskScenarioRepository riskScenarioRepository;

    @InjectMocks
    private FindingGraphProjectionContributor contributor;

    @Test
    void contributesNodesForAllStatusesIncludingVerifiedClosed() {
        var project = new Project("ground-control", "Ground Control");
        var projectId = UUID.randomUUID();
        setField(project, "id", projectId);

        var open = newFinding(project, "FIND-1", "Open finding");
        var inProgress = newFinding(project, "FIND-2", "In progress");
        inProgress.transitionStatus(FindingStatus.REMEDIATION_IN_PROGRESS);
        var closed = newFinding(project, "FIND-3", "Closed");
        closed.transitionStatus(FindingStatus.REMEDIATION_IN_PROGRESS);
        closed.transitionStatus(FindingStatus.REMEDIATION_COMPLETE);
        closed.transitionStatus(FindingStatus.VERIFIED_CLOSED);

        when(findingRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(List.of(open, inProgress, closed));

        var nodes = contributor.contributeNodes(projectId);

        // VERIFIED_CLOSED findings stay in the graph as historical evidence so
        // inbound AssetLink/ControlLink/RiskScenarioLink edges to them remain valid.
        assertThat(nodes).hasSize(3);
        assertThat(nodes).allMatch(node -> node.entityType() == GraphEntityType.FINDING);
        assertThat(nodes.stream().map(n -> n.properties().get("status")))
                .containsExactlyInAnyOrder("OPEN", "REMEDIATION_IN_PROGRESS", "VERIFIED_CLOSED");
        assertThat(nodes.get(0).id()).isEqualTo(GraphIds.nodeId(GraphEntityType.FINDING, open.getId()));
        assertThat(nodes.get(0).properties().get("severity")).isEqualTo("HIGH");
        assertThat(nodes.get(0).properties().get("findingType")).isEqualTo("CONTROL_DEFICIENCY");
    }

    @Test
    void omitsNullOptionalFieldsFromNodeProperties() {
        var project = new Project("ground-control", "Ground Control");
        var projectId = UUID.randomUUID();
        setField(project, "id", projectId);

        var f = newFinding(project, "FIND-SPARSE", "Sparse finding");
        // rootCauseAnalysis / owner / dueDate / createdBy remain null
        when(findingRepository.findByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(List.of(f));

        var nodes = contributor.contributeNodes(projectId);

        assertThat(nodes).hasSize(1);
        var properties = nodes.get(0).properties();
        // Null-valued optional fields must be absent rather than present-with-null,
        // because Apache AGE / Cypher property maps reject null property values.
        assertThat(properties).doesNotContainKey("rootCauseAnalysis");
        assertThat(properties).doesNotContainKey("owner");
        assertThat(properties).doesNotContainKey("dueDate");
        assertThat(properties).doesNotContainKey("createdBy");
        // Required fields are still present.
        assertThat(properties).containsEntry("title", "Sparse finding");
        assertThat(properties).containsEntry("description", "desc");
    }

    @Test
    void exposesPopulatedOptionalFields() {
        var project = new Project("ground-control", "Ground Control");
        var projectId = UUID.randomUUID();
        setField(project, "id", projectId);

        var f = newFinding(project, "FIND-FULL", "Full finding");
        f.setRootCauseAnalysis("Identity provider misconfigured during migration.");
        f.setOwner("alice");
        f.setDueDate(LocalDate.of(2026, 6, 30));
        f.setCreatedBy("analyst");
        when(findingRepository.findByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(List.of(f));

        var nodes = contributor.contributeNodes(projectId);

        var properties = nodes.get(0).properties();
        assertThat(properties).containsEntry("rootCauseAnalysis", "Identity provider misconfigured during migration.");
        assertThat(properties).containsEntry("owner", "alice");
        assertThat(properties).containsEntry("dueDate", "2026-06-30");
        assertThat(properties).containsEntry("createdBy", "analyst");
    }

    @Test
    void contributesEdgesOnlyForInternalTargets() {
        var project = new Project("ground-control", "Ground Control");
        var projectId = UUID.randomUUID();
        setField(project, "id", projectId);

        var f = newFinding(project, "FIND-1", "Finding");

        var asset = new OperationalAsset(project, "ASSET-1", "Auth Service");
        setField(asset, "id", UUID.randomUUID());
        var assetLink = new FindingLink(f, FindingLinkTargetType.ASSET, asset.getId(), null, FindingLinkType.AFFECTS);
        setField(assetLink, "id", UUID.randomUUID());

        var controlId = UUID.randomUUID();
        var controlLink =
                new FindingLink(f, FindingLinkTargetType.CONTROL, controlId, null, FindingLinkType.MITIGATED_BY);
        setField(controlLink, "id", UUID.randomUUID());

        var externalEvidenceLink = new FindingLink(
                f,
                FindingLinkTargetType.EVIDENCE,
                null,
                "s3://evidence/audit-2026-q2.pdf",
                FindingLinkType.EVIDENCED_BY);
        setField(externalEvidenceLink, "id", UUID.randomUUID());

        when(findingLinkRepository.findByProjectId(projectId))
                .thenReturn(List.of(assetLink, controlLink, externalEvidenceLink));
        when(operationalAssetRepository.findIdsByProjectIdAndArchivedAtIsNull(projectId))
                .thenReturn(List.of(asset.getId()));
        when(riskScenarioRepository.findIdsByProjectIdAndStatusNot(projectId, RiskScenarioStatus.ARCHIVED))
                .thenReturn(List.of());

        var edges = contributor.contributeEdges(projectId);

        assertThat(edges).hasSize(2);
        assertThat(edges.stream().map(e -> e.targetEntityType()))
                .containsExactlyInAnyOrder(GraphEntityType.OPERATIONAL_ASSET, GraphEntityType.CONTROL);
        assertThat(edges).allMatch(e -> e.sourceEntityType() == GraphEntityType.FINDING);
    }

    @Test
    void skipsEdgesToArchivedAssetTargets() {
        var project = new Project("ground-control", "Ground Control");
        var projectId = UUID.randomUUID();
        setField(project, "id", projectId);

        var f = newFinding(project, "FIND-1", "Finding");

        var liveAsset = new OperationalAsset(project, "ASSET-LIVE", "Live");
        setField(liveAsset, "id", UUID.randomUUID());
        var archivedAssetId = UUID.randomUUID();

        var liveLink =
                new FindingLink(f, FindingLinkTargetType.ASSET, liveAsset.getId(), null, FindingLinkType.AFFECTS);
        setField(liveLink, "id", UUID.randomUUID());
        var archivedLink =
                new FindingLink(f, FindingLinkTargetType.ASSET, archivedAssetId, null, FindingLinkType.AFFECTS);
        setField(archivedLink, "id", UUID.randomUUID());

        when(findingLinkRepository.findByProjectId(projectId)).thenReturn(List.of(liveLink, archivedLink));
        when(operationalAssetRepository.findIdsByProjectIdAndArchivedAtIsNull(projectId))
                .thenReturn(List.of(liveAsset.getId()));
        when(riskScenarioRepository.findIdsByProjectIdAndStatusNot(projectId, RiskScenarioStatus.ARCHIVED))
                .thenReturn(List.of());

        var edges = contributor.contributeEdges(projectId);

        assertThat(edges).hasSize(1);
        assertThat(edges.get(0).targetId())
                .isEqualTo(GraphIds.nodeId(GraphEntityType.OPERATIONAL_ASSET, liveAsset.getId()));
    }

    @Test
    void skipsEdgesToArchivedRiskScenarioTargets() {
        var project = new Project("ground-control", "Ground Control");
        var projectId = UUID.randomUUID();
        setField(project, "id", projectId);

        var f = newFinding(project, "FIND-1", "Finding");

        var liveScenario = new RiskScenario(project, "RS-LIVE", "Live", "Source", "Event", "Asset", "Consequence");
        setField(liveScenario, "id", UUID.randomUUID());
        var archivedScenario =
                new RiskScenario(project, "RS-ARCH", "Archived", "Source", "Event", "Asset", "Consequence");
        setField(archivedScenario, "id", UUID.randomUUID());
        setField(archivedScenario, "status", RiskScenarioStatus.ARCHIVED);

        var liveLink = new FindingLink(
                f, FindingLinkTargetType.RISK_SCENARIO, liveScenario.getId(), null, FindingLinkType.CAUSED_BY);
        setField(liveLink, "id", UUID.randomUUID());
        var archivedLink = new FindingLink(
                f, FindingLinkTargetType.RISK_SCENARIO, archivedScenario.getId(), null, FindingLinkType.CAUSED_BY);
        setField(archivedLink, "id", UUID.randomUUID());

        when(findingLinkRepository.findByProjectId(projectId)).thenReturn(List.of(liveLink, archivedLink));
        when(operationalAssetRepository.findIdsByProjectIdAndArchivedAtIsNull(projectId))
                .thenReturn(List.of());
        when(riskScenarioRepository.findIdsByProjectIdAndStatusNot(projectId, RiskScenarioStatus.ARCHIVED))
                .thenReturn(List.of(liveScenario.getId()));

        var edges = contributor.contributeEdges(projectId);

        assertThat(edges).hasSize(1);
        assertThat(edges.get(0).targetId())
                .isEqualTo(GraphIds.nodeId(GraphEntityType.RISK_SCENARIO, liveScenario.getId()));
    }

    private Finding newFinding(Project project, String uid, String title) {
        var f = new Finding(project, uid, title, FindingType.CONTROL_DEFICIENCY, FindingSeverity.HIGH, "desc");
        setField(f, "id", UUID.randomUUID());
        return f;
    }
}
