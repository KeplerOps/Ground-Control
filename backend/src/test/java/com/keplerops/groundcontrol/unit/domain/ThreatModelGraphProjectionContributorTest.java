package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.graph.service.ThreatModelGraphProjectionContributor;
import com.keplerops.groundcontrol.domain.projects.model.Project;
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

        var assetId = UUID.randomUUID();
        var internalAssetLink =
                new ThreatModelLink(tm, ThreatModelLinkTargetType.ASSET, assetId, null, ThreatModelLinkType.AFFECTS);
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

        var edges = contributor.contributeEdges(projectId);

        assertThat(edges).hasSize(2);
        assertThat(edges.stream().map(e -> e.targetEntityType()))
                .containsExactlyInAnyOrder(GraphEntityType.OPERATIONAL_ASSET, GraphEntityType.CONTROL);
        assertThat(edges).allMatch(e -> e.sourceEntityType() == GraphEntityType.THREAT_MODEL);
    }
}
