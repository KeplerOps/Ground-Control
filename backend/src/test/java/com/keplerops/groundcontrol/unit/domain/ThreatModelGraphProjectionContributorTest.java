package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.graph.service.ThreatModelGraphProjectionContributor;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenario;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioStatus;
import com.keplerops.groundcontrol.domain.threatmodels.model.ThreatModel;
import com.keplerops.groundcontrol.domain.threatmodels.model.ThreatModelLink;
import com.keplerops.groundcontrol.domain.threatmodels.repository.ThreatModelLinkRepository;
import com.keplerops.groundcontrol.domain.threatmodels.repository.ThreatModelRepository;
import com.keplerops.groundcontrol.domain.threatmodels.state.StrideCategory;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelLinkTargetType;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelLinkType;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThreatModelGraphProjectionContributorTest {

    @Mock
    private ThreatModelRepository threatModelRepository;

    @Mock
    private ThreatModelLinkRepository threatModelLinkRepository;

    @Mock
    private OperationalAssetRepository operationalAssetRepository;

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private RiskScenarioRepository riskScenarioRepository;

    @InjectMocks
    private ThreatModelGraphProjectionContributor contributor;

    @Test
    void contributesNodesForAllStatusesIncludingArchived() {
        var project = new Project("ground-control", "Ground Control");
        var projectId = UUID.randomUUID();
        setField(project, "id", projectId);

        var draft = new ThreatModel(project, "TM-1", "Draft threat", "Actor", "Event", "Effect");
        setField(draft, "id", UUID.randomUUID());
        draft.setStride(StrideCategory.SPOOFING);

        var active = new ThreatModel(project, "TM-2", "Active threat", "Actor", "Event", "Effect");
        setField(active, "id", UUID.randomUUID());
        active.transitionStatus(ThreatModelStatus.ACTIVE);

        var archived = new ThreatModel(project, "TM-3", "Archived threat", "Actor", "Event", "Effect");
        setField(archived, "id", UUID.randomUUID());
        archived.transitionStatus(ThreatModelStatus.ARCHIVED);

        when(threatModelRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(List.of(draft, active, archived));

        var nodes = contributor.contributeNodes(projectId);

        // Archived threat models stay in the graph as historical evidence so
        // incoming AssetLink/RiskScenarioLink edges to them remain valid.
        assertThat(nodes).hasSize(3);
        assertThat(nodes).allMatch(node -> node.entityType() == GraphEntityType.THREAT_MODEL);
        assertThat(nodes.stream().map(n -> n.properties().get("status")))
                .containsExactlyInAnyOrder("DRAFT", "ACTIVE", "ARCHIVED");
        assertThat(nodes.get(0).id()).isEqualTo(GraphIds.nodeId(GraphEntityType.THREAT_MODEL, draft.getId()));
        assertThat(nodes.get(0).properties().get("stride")).isEqualTo("SPOOFING");
    }

    @Test
    void contributesEdgesOnlyForInternalTargets() {
        var project = new Project("ground-control", "Ground Control");
        var projectId = UUID.randomUUID();
        setField(project, "id", projectId);

        var tm = new ThreatModel(project, "TM-1", "Threat", "Actor", "Event", "Effect");
        setField(tm, "id", UUID.randomUUID());

        var asset = new OperationalAsset(project, "ASSET-1", "Auth Service");
        setField(asset, "id", UUID.randomUUID());
        var internalAssetLink = new ThreatModelLink(
                tm, ThreatModelLinkTargetType.ASSET, asset.getId(), null, ThreatModelLinkType.AFFECTS);
        setField(internalAssetLink, "id", UUID.randomUUID());

        var controlId = UUID.randomUUID();
        var internalControlLink = new ThreatModelLink(
                tm, ThreatModelLinkTargetType.CONTROL, controlId, null, ThreatModelLinkType.MITIGATED_BY);
        setField(internalControlLink, "id", UUID.randomUUID());

        var externalCodeLink = new ThreatModelLink(
                tm,
                ThreatModelLinkTargetType.CODE,
                null,
                "backend/src/main/java/Auth.java",
                ThreatModelLinkType.DOCUMENTED_IN);
        setField(externalCodeLink, "id", UUID.randomUUID());

        when(threatModelLinkRepository.findByProjectId(projectId))
                .thenReturn(List.of(internalAssetLink, internalControlLink, externalCodeLink));
        when(operationalAssetRepository.findByProjectIdAndArchivedAtIsNull(projectId))
                .thenReturn(List.of(asset));
        when(requirementRepository.findByProjectIdAndArchivedAtIsNull(projectId))
                .thenReturn(List.of());
        when(riskScenarioRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(List.of());

        var edges = contributor.contributeEdges(projectId);

        assertThat(edges).hasSize(2);
        assertThat(edges.stream().map(e -> e.targetEntityType()))
                .containsExactlyInAnyOrder(GraphEntityType.OPERATIONAL_ASSET, GraphEntityType.CONTROL);
        assertThat(edges).allMatch(e -> e.sourceEntityType() == GraphEntityType.THREAT_MODEL);
    }

    @Test
    void skipsEdgesToArchivedAssetTargets() {
        var project = new Project("ground-control", "Ground Control");
        var projectId = UUID.randomUUID();
        setField(project, "id", projectId);

        var tm = new ThreatModel(project, "TM-1", "Threat", "Actor", "Event", "Effect");
        setField(tm, "id", UUID.randomUUID());

        var liveAsset = new OperationalAsset(project, "ASSET-LIVE", "Live");
        setField(liveAsset, "id", UUID.randomUUID());
        var archivedAssetId = UUID.randomUUID();

        var liveLink = new ThreatModelLink(
                tm, ThreatModelLinkTargetType.ASSET, liveAsset.getId(), null, ThreatModelLinkType.AFFECTS);
        setField(liveLink, "id", UUID.randomUUID());
        var archivedLink = new ThreatModelLink(
                tm, ThreatModelLinkTargetType.ASSET, archivedAssetId, null, ThreatModelLinkType.AFFECTS);
        setField(archivedLink, "id", UUID.randomUUID());

        when(threatModelLinkRepository.findByProjectId(projectId)).thenReturn(List.of(liveLink, archivedLink));
        when(operationalAssetRepository.findByProjectIdAndArchivedAtIsNull(projectId))
                .thenReturn(List.of(liveAsset));
        when(requirementRepository.findByProjectIdAndArchivedAtIsNull(projectId))
                .thenReturn(List.of());
        when(riskScenarioRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(List.of());

        var edges = contributor.contributeEdges(projectId);

        assertThat(edges).hasSize(1);
        assertThat(edges.get(0).targetId())
                .isEqualTo(GraphIds.nodeId(GraphEntityType.OPERATIONAL_ASSET, liveAsset.getId()));
    }

    @Test
    void skipsEdgesToArchivedRequirementTargets() {
        var project = new Project("ground-control", "Ground Control");
        var projectId = UUID.randomUUID();
        setField(project, "id", projectId);

        var tm = new ThreatModel(project, "TM-1", "Threat", "Actor", "Event", "Effect");
        setField(tm, "id", UUID.randomUUID());

        var liveReq = new Requirement(project, "REQ-LIVE", "Live", "Statement");
        setField(liveReq, "id", UUID.randomUUID());
        var archivedReqId = UUID.randomUUID();

        var liveLink = new ThreatModelLink(
                tm, ThreatModelLinkTargetType.REQUIREMENT, liveReq.getId(), null, ThreatModelLinkType.EXPLOITS);
        setField(liveLink, "id", UUID.randomUUID());
        var archivedLink = new ThreatModelLink(
                tm, ThreatModelLinkTargetType.REQUIREMENT, archivedReqId, null, ThreatModelLinkType.EXPLOITS);
        setField(archivedLink, "id", UUID.randomUUID());

        when(threatModelLinkRepository.findByProjectId(projectId)).thenReturn(List.of(liveLink, archivedLink));
        when(operationalAssetRepository.findByProjectIdAndArchivedAtIsNull(projectId))
                .thenReturn(List.of());
        when(requirementRepository.findByProjectIdAndArchivedAtIsNull(projectId))
                .thenReturn(List.of(liveReq));
        when(riskScenarioRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(List.of());

        var edges = contributor.contributeEdges(projectId);

        assertThat(edges).hasSize(1);
        assertThat(edges.get(0).targetId()).isEqualTo(GraphIds.nodeId(GraphEntityType.REQUIREMENT, liveReq.getId()));
    }

    @Test
    void skipsEdgesToArchivedRiskScenarioTargets() {
        var project = new Project("ground-control", "Ground Control");
        var projectId = UUID.randomUUID();
        setField(project, "id", projectId);

        var tm = new ThreatModel(project, "TM-1", "Threat", "Actor", "Event", "Effect");
        setField(tm, "id", UUID.randomUUID());

        var liveScenario = new RiskScenario(project, "RS-LIVE", "Live", "Source", "Event", "Asset", "Consequence");
        setField(liveScenario, "id", UUID.randomUUID());
        var archivedScenario =
                new RiskScenario(project, "RS-ARCH", "Archived", "Source", "Event", "Asset", "Consequence");
        setField(archivedScenario, "id", UUID.randomUUID());
        setField(archivedScenario, "status", RiskScenarioStatus.ARCHIVED);

        var liveLink = new ThreatModelLink(
                tm,
                ThreatModelLinkTargetType.RISK_SCENARIO,
                liveScenario.getId(),
                null,
                ThreatModelLinkType.ASSESSED_IN);
        setField(liveLink, "id", UUID.randomUUID());
        var archivedLink = new ThreatModelLink(
                tm,
                ThreatModelLinkTargetType.RISK_SCENARIO,
                archivedScenario.getId(),
                null,
                ThreatModelLinkType.ASSESSED_IN);
        setField(archivedLink, "id", UUID.randomUUID());

        when(threatModelLinkRepository.findByProjectId(projectId)).thenReturn(List.of(liveLink, archivedLink));
        when(operationalAssetRepository.findByProjectIdAndArchivedAtIsNull(projectId))
                .thenReturn(List.of());
        when(requirementRepository.findByProjectIdAndArchivedAtIsNull(projectId))
                .thenReturn(List.of());
        when(riskScenarioRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(List.of(liveScenario, archivedScenario));

        var edges = contributor.contributeEdges(projectId);

        assertThat(edges).hasSize(1);
        assertThat(edges.get(0).targetId())
                .isEqualTo(GraphIds.nodeId(GraphEntityType.RISK_SCENARIO, liveScenario.getId()));
    }
}
