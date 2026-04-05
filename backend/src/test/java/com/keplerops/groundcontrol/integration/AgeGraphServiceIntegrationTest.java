package com.keplerops.groundcontrol.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.service.GraphClient;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AgeGraphServiceIntegrationTest extends BaseAgeIntegrationTest {

    @Autowired
    private GraphClient graphClient;

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private RequirementRelationRepository relationRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private Project testProject;

    @BeforeEach
    void setUp() {
        testProject = projectRepository.findByIdentifier("ground-control").orElseThrow();
    }

    @Test
    void materializeAndQueryAncestors() {
        var grandparent =
                requirementRepository.save(new Requirement(testProject, "AGE-GP", "Grandparent", "GP statement"));
        var parent = requirementRepository.save(new Requirement(testProject, "AGE-P", "Parent", "P statement"));
        var child = requirementRepository.save(new Requirement(testProject, "AGE-C", "Child", "C statement"));

        relationRepository.save(new RequirementRelation(parent, grandparent, RelationType.PARENT));
        relationRepository.save(new RequirementRelation(child, parent, RelationType.PARENT));

        graphClient.materializeGraph();

        var ancestors = graphClient.getAncestors(testProject.getId(), "AGE-C", 10);
        assertThat(ancestors).contains("AGE-P", "AGE-GP");
    }

    @Test
    void materializeAndQueryDescendants() {
        var root = requirementRepository.save(new Requirement(testProject, "AGE-ROOT", "Root", "Root statement"));
        var leaf = requirementRepository.save(new Requirement(testProject, "AGE-LEAF", "Leaf", "Leaf statement"));

        relationRepository.save(new RequirementRelation(leaf, root, RelationType.PARENT));

        graphClient.materializeGraph();

        var descendants = graphClient.getDescendants(testProject.getId(), "AGE-ROOT", 10);
        assertThat(descendants).contains("AGE-LEAF");
    }

    @Test
    void materializeAndFindPaths() {
        var a = requirementRepository.save(new Requirement(testProject, "AGE-A", "A", "A statement"));
        var b = requirementRepository.save(new Requirement(testProject, "AGE-B", "B", "B statement"));
        var c = requirementRepository.save(new Requirement(testProject, "AGE-C2", "C", "C statement"));

        relationRepository.save(new RequirementRelation(a, b, RelationType.DEPENDS_ON));
        relationRepository.save(new RequirementRelation(b, c, RelationType.DEPENDS_ON));

        graphClient.materializeGraph();

        var paths = graphClient.findPaths(testProject.getId(), "AGE-A", "AGE-C2");
        assertThat(paths).isNotEmpty();
        var firstPath = paths.get(0);
        assertThat(firstPath.nodeUids()).containsExactly("AGE-A", "AGE-B", "AGE-C2");
        assertThat(firstPath.edgeLabels()).containsExactly("DEPENDS_ON", "DEPENDS_ON");
    }
}
