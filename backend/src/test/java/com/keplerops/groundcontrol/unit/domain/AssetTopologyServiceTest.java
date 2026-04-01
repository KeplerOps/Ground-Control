package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.assets.model.AssetRelation;
import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import com.keplerops.groundcontrol.domain.assets.repository.AssetRelationRepository;
import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.assets.service.AssetTopologyService;
import com.keplerops.groundcontrol.domain.assets.state.AssetRelationType;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetTopologyServiceTest {

    @Mock
    private OperationalAssetRepository assetRepository;

    @Mock
    private AssetRelationRepository relationRepository;

    @InjectMocks
    private AssetTopologyService topologyService;

    private Project project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
    }

    private OperationalAsset createAsset(String uid, String name) {
        var asset = new OperationalAsset(project, uid, name);
        setField(asset, "id", UUID.randomUUID());
        return asset;
    }

    private AssetRelation createRelation(OperationalAsset source, OperationalAsset target, AssetRelationType type) {
        var relation = new AssetRelation(source, target, type);
        setField(relation, "id", UUID.randomUUID());
        return relation;
    }

    @Nested
    class DetectCycles {

        @Test
        void noCyclesReturnsEmpty() {
            var a = createAsset("A", "Asset A");
            var b = createAsset("B", "Asset B");
            var rel = createRelation(a, b, AssetRelationType.DEPENDS_ON);

            when(relationRepository.findActiveByProjectId(projectId)).thenReturn(List.of(rel));

            var result = topologyService.detectCycles(projectId);
            assertThat(result).isEmpty();
        }

        @Test
        void detectsCycle() {
            var a = createAsset("A", "Asset A");
            var b = createAsset("B", "Asset B");
            var c = createAsset("C", "Asset C");
            var ab = createRelation(a, b, AssetRelationType.DEPENDS_ON);
            var bc = createRelation(b, c, AssetRelationType.DEPENDS_ON);
            var ca = createRelation(c, a, AssetRelationType.DEPENDS_ON);

            when(relationRepository.findActiveByProjectId(projectId)).thenReturn(List.of(ab, bc, ca));

            var result = topologyService.detectCycles(projectId);
            assertThat(result).isNotEmpty();
            // Cycle contains all three nodes; first and last element are the same (cycle bookends)
            var members = result.get(0).memberUids();
            assertThat(members).hasSize(4);
            assertThat(members).contains("A", "B", "C");
            assertThat(members.get(0)).isEqualTo(members.get(members.size() - 1));
        }
    }

    @Nested
    class ImpactAnalysis {

        @Test
        void returnsTransitiveClosure() {
            var a = createAsset("A", "Asset A");
            var b = createAsset("B", "Asset B");
            var c = createAsset("C", "Asset C");
            // B depends on A, C depends on B => impacting A affects B and C
            var ba = createRelation(b, a, AssetRelationType.DEPENDS_ON);
            var cb = createRelation(c, b, AssetRelationType.DEPENDS_ON);

            when(assetRepository.findById(a.getId())).thenReturn(Optional.of(a));
            when(relationRepository.findActiveByProjectId(projectId)).thenReturn(List.of(ba, cb));
            when(assetRepository.findAllById(org.mockito.ArgumentMatchers.anyIterable()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        var ids = (Iterable<UUID>) inv.getArgument(0);
                        var allAssets = List.of(a, b, c);
                        var idSet = new java.util.HashSet<UUID>();
                        ids.forEach(idSet::add);
                        return allAssets.stream()
                                .filter(asset -> idSet.contains(asset.getId()))
                                .toList();
                    });

            var result = topologyService.impactAnalysis(a.getId());
            assertThat(result).extracting(OperationalAsset::getUid).containsExactlyInAnyOrder("A", "B", "C");
        }
    }

    @Nested
    class ExtractSubgraph {

        @Test
        void returnsConnectedComponent() {
            var a = createAsset("A", "Asset A");
            var b = createAsset("B", "Asset B");
            var c = createAsset("C", "Asset C");
            var d = createAsset("D", "Disconnected");
            var ab = createRelation(a, b, AssetRelationType.COMMUNICATES_WITH);
            var bc = createRelation(b, c, AssetRelationType.COMMUNICATES_WITH);

            when(assetRepository.findByProjectIdAndUidIgnoreCase(projectId, "A"))
                    .thenReturn(Optional.of(a));
            when(relationRepository.findActiveByProjectId(projectId)).thenReturn(List.of(ab, bc));
            when(assetRepository.findAllById(org.mockito.ArgumentMatchers.anyIterable()))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        var ids = (Iterable<UUID>) inv.getArgument(0);
                        var allAssets = List.of(a, b, c, d);
                        var idSet = new java.util.HashSet<UUID>();
                        ids.forEach(idSet::add);
                        return allAssets.stream()
                                .filter(asset -> idSet.contains(asset.getId()))
                                .toList();
                    });

            var result = topologyService.extractSubgraph(projectId, List.of("A"));

            assertThat(result.assets()).extracting(OperationalAsset::getUid).containsExactlyInAnyOrder("A", "B", "C");
            assertThat(result.relations()).hasSize(2);
        }
    }
}
