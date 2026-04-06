package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.assets.model.AssetLink;
import com.keplerops.groundcontrol.domain.assets.model.AssetRelation;
import com.keplerops.groundcontrol.domain.assets.model.Observation;
import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import com.keplerops.groundcontrol.domain.assets.repository.AssetLinkRepository;
import com.keplerops.groundcontrol.domain.assets.repository.AssetRelationRepository;
import com.keplerops.groundcontrol.domain.assets.repository.ObservationRepository;
import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkType;
import com.keplerops.groundcontrol.domain.assets.state.AssetRelationType;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import com.keplerops.groundcontrol.domain.assets.state.ObservationCategory;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.graph.service.AssetGraphProjectionContributor;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetGraphProjectionContributorTest {

    @Mock
    private OperationalAssetRepository assetRepository;

    @Mock
    private ObservationRepository observationRepository;

    @Mock
    private AssetRelationRepository assetRelationRepository;

    @Mock
    private AssetLinkRepository assetLinkRepository;

    @InjectMocks
    private AssetGraphProjectionContributor contributor;

    @Test
    void contributesAssetObservationAndInternalLinkGraphElements() {
        var project = new Project("ground-control", "Ground Control");
        var projectId = UUID.randomUUID();
        setField(project, "id", projectId);

        var source = asset(project, "ASSET-1", "Gateway");
        var target = asset(project, "ASSET-2", "Database");
        var observation = new Observation(
                source,
                ObservationCategory.CONFIGURATION,
                "tls_enabled",
                "true",
                "scanner",
                Instant.parse("2026-04-02T00:00:00Z"));
        setField(observation, "id", UUID.randomUUID());
        observation.setConfidence("HIGH");

        var relation = new AssetRelation(source, target, AssetRelationType.DEPENDS_ON);
        setField(relation, "id", UUID.randomUUID());
        setField(relation, "createdAt", Instant.parse("2026-04-02T12:00:00Z"));

        var internalLink = new AssetLink(
                source, AssetLinkTargetType.RISK_SCENARIO, UUID.randomUUID(), null, AssetLinkType.ASSOCIATED);
        setField(internalLink, "id", UUID.randomUUID());

        var externalLink = new AssetLink(source, AssetLinkTargetType.EXTERNAL, null, "EXT-1", AssetLinkType.ASSOCIATED);
        setField(externalLink, "id", UUID.randomUUID());

        when(assetRepository.findByProjectIdAndArchivedAtIsNull(projectId)).thenReturn(List.of(source, target));
        when(observationRepository.findByProjectId(projectId)).thenReturn(List.of(observation));
        when(assetRelationRepository.findActiveByProjectId(projectId)).thenReturn(List.of(relation));
        when(assetLinkRepository.findByProjectId(projectId)).thenReturn(List.of(internalLink, externalLink));

        var nodes = contributor.contributeNodes(projectId);
        var edges = contributor.contributeEdges(projectId);

        assertThat(nodes).hasSize(3);
        assertThat(nodes)
                .extracting(node -> node.entityType().name())
                .containsExactlyInAnyOrder("OPERATIONAL_ASSET", "OPERATIONAL_ASSET", "OBSERVATION");
        assertThat(nodes).anySatisfy(node -> {
            if (node.entityType() == GraphEntityType.OPERATIONAL_ASSET) {
                assertThat(node.properties()).containsKey("assetType").containsKey("name");
            }
        });
        assertThat(edges).hasSize(3);
        assertThat(edges)
                .extracting(edge -> edge.edgeType())
                .containsExactlyInAnyOrder("DEPENDS_ON", "OBSERVED_ON", "ASSOCIATED");
        assertThat(edges).anySatisfy(edge -> {
            if (edge.edgeType().equals("ASSOCIATED")) {
                assertThat(edge.targetId())
                        .isEqualTo(GraphIds.nodeId(GraphEntityType.RISK_SCENARIO, internalLink.getTargetEntityId()));
            }
        });
    }

    @Test
    void controlTypedAssetLinkProducesGraphEdge() {
        var project = new Project("ground-control", "Ground Control");
        var projectId = UUID.randomUUID();
        setField(project, "id", projectId);

        var source = asset(project, "ASSET-1", "Gateway");
        var controlTargetId = UUID.randomUUID();

        var controlLink =
                new AssetLink(source, AssetLinkTargetType.CONTROL, controlTargetId, null, AssetLinkType.GOVERNED_BY);
        setField(controlLink, "id", UUID.randomUUID());

        when(assetRelationRepository.findActiveByProjectId(projectId)).thenReturn(List.of());
        when(observationRepository.findByProjectId(projectId)).thenReturn(List.of());
        when(assetLinkRepository.findByProjectId(projectId)).thenReturn(List.of(controlLink));

        var edges = contributor.contributeEdges(projectId);

        assertThat(edges).hasSize(1);
        assertThat(edges.get(0).edgeType()).isEqualTo("GOVERNED_BY");
        assertThat(edges.get(0).targetId()).isEqualTo(GraphIds.nodeId(GraphEntityType.CONTROL, controlTargetId));
        assertThat(edges.get(0).sourceEntityType()).isEqualTo(GraphEntityType.OPERATIONAL_ASSET);
        assertThat(edges.get(0).targetEntityType()).isEqualTo(GraphEntityType.CONTROL);
    }

    @ParameterizedTest
    @EnumSource(
            value = AssetLinkTargetType.class,
            names = {"ISSUE", "CODE", "CONFIGURATION"})
    void newExternalTargetTypesAreFilteredFromGraph(AssetLinkTargetType targetType) {
        var project = new Project("ground-control", "Ground Control");
        var projectId = UUID.randomUUID();
        setField(project, "id", projectId);

        var source = asset(project, "ASSET-1", "Gateway");
        var link = new AssetLink(source, targetType, null, "ref-1", AssetLinkType.ASSOCIATED);
        setField(link, "id", UUID.randomUUID());

        when(assetRelationRepository.findActiveByProjectId(projectId)).thenReturn(List.of());
        when(observationRepository.findByProjectId(projectId)).thenReturn(List.of());
        when(assetLinkRepository.findByProjectId(projectId)).thenReturn(List.of(link));

        var edges = contributor.contributeEdges(projectId);

        assertThat(edges).isEmpty();
    }

    private OperationalAsset asset(Project project, String uid, String name) {
        var asset = new OperationalAsset(project, uid, name);
        setField(asset, "id", UUID.randomUUID());
        asset.setAssetType(AssetType.SERVICE);
        asset.setDescription(name + " description");
        return asset;
    }
}
