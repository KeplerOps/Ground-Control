package com.keplerops.groundcontrol.unit.domain.graph;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.evidence.model.EvidenceArtifact;
import com.keplerops.groundcontrol.domain.evidence.model.EvidenceSourceRef;
import com.keplerops.groundcontrol.domain.evidence.repository.EvidenceArtifactRepository;
import com.keplerops.groundcontrol.domain.evidence.state.EvidenceSourceKind;
import com.keplerops.groundcontrol.domain.evidence.state.EvidenceType;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.graph.service.EvidenceArtifactGraphProjectionContributor;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvidenceArtifactGraphProjectionContributorTest {

    @Mock
    private EvidenceArtifactRepository repository;

    @InjectMocks
    private EvidenceArtifactGraphProjectionContributor contributor;

    private Project project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
    }

    @Test
    void projectsOneNodePerArtifactWithEvidenceArtifactType() {
        var artifact = buildArtifact("EVD-0001");
        setField(artifact, "id", UUID.randomUUID());
        when(repository.findByProjectIdOrderByDerivedAtDesc(projectId)).thenReturn(List.of(artifact));

        var nodes = contributor.contributeNodes(projectId);

        assertThat(nodes).hasSize(1);
        assertThat(nodes.get(0).entityType()).isEqualTo(GraphEntityType.EVIDENCE_ARTIFACT);
        assertThat(nodes.get(0).properties())
                .containsEntry("evidenceType", "ATTESTATION")
                .containsEntry("derivationMethod", "method-v1");
    }

    @Test
    void projectsHasSourceEdgesForEachInternalSourceKind() {
        var artifactId = UUID.randomUUID();
        var observationId = UUID.randomUUID();
        var controlTestId = UUID.randomUUID();
        var findingId = UUID.randomUUID();
        var artifact = buildArtifact("EVD-0001");
        setField(artifact, "id", artifactId);
        artifact.setSources(List.of(
                new EvidenceSourceRef(EvidenceSourceKind.OBSERVATION, observationId, null, "primary"),
                new EvidenceSourceRef(EvidenceSourceKind.CONTROL_TEST, controlTestId, null, null),
                new EvidenceSourceRef(EvidenceSourceKind.FINDING, findingId, null, null)));
        when(repository.findByProjectIdOrderByDerivedAtDesc(projectId)).thenReturn(List.of(artifact));

        var edges = contributor.contributeEdges(projectId);

        // Node-id assertions guard against a regression in toSourceEdge that
        // swapped the source/target sides or used the wrong UUID. Every edge
        // originates at the artifact and points at the right source node.
        var expectedArtifactNodeId = GraphIds.nodeId(GraphEntityType.EVIDENCE_ARTIFACT, artifactId);
        assertThat(edges)
                .hasSize(3)
                .allMatch(e -> e.edgeType().equals("HAS_SOURCE"))
                .allMatch(e -> e.sourceId().equals(expectedArtifactNodeId))
                .extracting(e -> e.targetEntityType())
                .containsExactlyInAnyOrder(
                        GraphEntityType.OBSERVATION, GraphEntityType.CONTROL_TEST, GraphEntityType.FINDING);
        assertThat(edges)
                .extracting(e -> e.targetId())
                .containsExactlyInAnyOrder(
                        GraphIds.nodeId(GraphEntityType.OBSERVATION, observationId),
                        GraphIds.nodeId(GraphEntityType.CONTROL_TEST, controlTestId),
                        GraphIds.nodeId(GraphEntityType.FINDING, findingId));
    }

    @Test
    void externalKindsProduceNoEdges() {
        var artifactId = UUID.randomUUID();
        var artifact = buildArtifact("EVD-0001");
        setField(artifact, "id", artifactId);
        artifact.setSources(List.of(
                new EvidenceSourceRef(EvidenceSourceKind.ATTESTATION, null, "vendor-soc2-2026", null),
                new EvidenceSourceRef(EvidenceSourceKind.EXTERNAL, null, "third-party-id", null)));
        when(repository.findByProjectIdOrderByDerivedAtDesc(projectId)).thenReturn(List.of(artifact));

        assertThat(contributor.contributeEdges(projectId)).isEmpty();
    }

    @Test
    void emitsSupersededByEdgeWhenSupersededByArtifactIdSet() {
        var artifactId = UUID.randomUUID();
        var replacementId = UUID.randomUUID();
        var artifact = buildArtifact("EVD-0001");
        setField(artifact, "id", artifactId);
        artifact.setSources(
                List.of(new EvidenceSourceRef(EvidenceSourceKind.ATTESTATION, null, "vendor-soc2-2026", null)));
        artifact.setSupersededByArtifactId(replacementId);
        when(repository.findByProjectIdOrderByDerivedAtDesc(projectId)).thenReturn(List.of(artifact));

        var edges = contributor.contributeEdges(projectId);

        assertThat(edges).hasSize(1);
        assertThat(edges.get(0).edgeType()).isEqualTo("SUPERSEDED_BY");
        assertThat(edges.get(0).sourceEntityType()).isEqualTo(GraphEntityType.EVIDENCE_ARTIFACT);
        assertThat(edges.get(0).targetEntityType()).isEqualTo(GraphEntityType.EVIDENCE_ARTIFACT);
        // Both endpoints are EVIDENCE_ARTIFACT, so a swapped source/target in
        // toSupersededEdge would not be caught by the entity-type assertions
        // alone. Lock in the direction: edge runs FROM the prior artifact TO
        // its replacement.
        assertThat(edges.get(0).sourceId()).isEqualTo(GraphIds.nodeId(GraphEntityType.EVIDENCE_ARTIFACT, artifactId));
        assertThat(edges.get(0).targetId())
                .isEqualTo(GraphIds.nodeId(GraphEntityType.EVIDENCE_ARTIFACT, replacementId));
    }

    private EvidenceArtifact buildArtifact(String uid) {
        return new EvidenceArtifact(
                project,
                uid,
                "title-" + uid,
                "summary-" + uid,
                EvidenceType.ATTESTATION,
                "method-v1",
                Instant.parse("2026-04-30T17:00:00Z"));
    }
}
