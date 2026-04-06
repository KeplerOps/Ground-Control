package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.controls.model.Control;
import com.keplerops.groundcontrol.domain.controls.model.ControlLink;
import com.keplerops.groundcontrol.domain.controls.repository.ControlLinkRepository;
import com.keplerops.groundcontrol.domain.controls.repository.ControlRepository;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkType;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.graph.service.ControlGraphProjectionContributor;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ControlGraphProjectionContributorTest {

    @Mock
    private ControlRepository controlRepository;

    @Mock
    private ControlLinkRepository controlLinkRepository;

    @InjectMocks
    private ControlGraphProjectionContributor contributor;

    @Test
    void contributesControlNodes() {
        var project = new Project("ground-control", "Ground Control");
        var projectId = UUID.randomUUID();
        setField(project, "id", projectId);

        var control = new Control(project, "CTL-001", "TLS Enforcement", ControlFunction.PREVENTIVE);
        setField(control, "id", UUID.randomUUID());
        control.setDescription("Enforce TLS 1.2+");
        control.setOwner("security-team");
        control.setCategory("network");
        control.setSource("NIST 800-53");

        when(controlRepository.findByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(List.of(control));

        var nodes = contributor.contributeNodes(projectId);

        assertThat(nodes).hasSize(1);
        var node = nodes.get(0);
        assertThat(node.entityType()).isEqualTo(GraphEntityType.CONTROL);
        assertThat(node.uid()).isEqualTo("CTL-001");
        assertThat(node.label()).isEqualTo("TLS Enforcement");
        assertThat(node.id()).isEqualTo(GraphIds.nodeId(GraphEntityType.CONTROL, control.getId()));
        assertThat(node.properties()).containsEntry("owner", "security-team");
        assertThat(node.properties()).containsEntry("category", "network");
        assertThat(node.properties()).containsEntry("controlFunction", "PREVENTIVE");
    }

    @Test
    void contributesInternalLinkEdgesAndFiltersExternalLinks() {
        var project = new Project("ground-control", "Ground Control");
        var projectId = UUID.randomUUID();
        setField(project, "id", projectId);

        var control = new Control(project, "CTL-001", "TLS Enforcement", ControlFunction.PREVENTIVE);
        setField(control, "id", UUID.randomUUID());

        var assetTargetId = UUID.randomUUID();
        var internalLink =
                new ControlLink(control, ControlLinkTargetType.ASSET, assetTargetId, null, ControlLinkType.MITIGATES);
        setField(internalLink, "id", UUID.randomUUID());

        var externalLink =
                new ControlLink(control, ControlLinkTargetType.EXTERNAL, null, "EXT-1", ControlLinkType.ASSOCIATED);
        setField(externalLink, "id", UUID.randomUUID());

        when(controlLinkRepository.findByProjectId(projectId)).thenReturn(List.of(internalLink, externalLink));

        var edges = contributor.contributeEdges(projectId);

        assertThat(edges).hasSize(1);
        var edge = edges.get(0);
        assertThat(edge.edgeType()).isEqualTo("MITIGATES");
        assertThat(edge.sourceEntityType()).isEqualTo(GraphEntityType.CONTROL);
        assertThat(edge.targetEntityType()).isEqualTo(GraphEntityType.OPERATIONAL_ASSET);
        assertThat(edge.sourceId()).isEqualTo(GraphIds.nodeId(GraphEntityType.CONTROL, control.getId()));
        assertThat(edge.targetId()).isEqualTo(GraphIds.nodeId(GraphEntityType.OPERATIONAL_ASSET, assetTargetId));
    }
}
