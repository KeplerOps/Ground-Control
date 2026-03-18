package com.keplerops.groundcontrol.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class AnalysisIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    void detectCycles_withAcyclicGraph_returnsEmpty() throws Exception {
        var parent =
                requirementRepository.save(new Requirement(testProject, "INT-PARENT", "Parent", "Parent statement"));
        var child = requirementRepository.save(new Requirement(testProject, "INT-CHILD", "Child", "Child statement"));
        relationRepository.save(new RequirementRelation(child, parent, RelationType.PARENT));

        mockMvc.perform(get("/api/v1/analysis/cycles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void findOrphans_withOrphan_returnsIt() throws Exception {
        requirementRepository.save(new Requirement(testProject, "INT-ORPHAN", "Orphan", "Orphan statement"));

        mockMvc.perform(get("/api/v1/analysis/orphans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.uid == 'INT-ORPHAN')]").exists());
    }

    @Test
    void impactAnalysis_returnsTransitiveDependents() throws Exception {
        var a = requirementRepository.save(new Requirement(testProject, "INT-A", "A", "A statement"));
        var b = requirementRepository.save(new Requirement(testProject, "INT-B", "B", "B statement"));
        var c = requirementRepository.save(new Requirement(testProject, "INT-C", "C", "C statement"));

        // c -> b -> a (child -> parent)
        relationRepository.save(new RequirementRelation(b, a, RelationType.PARENT));
        relationRepository.save(new RequirementRelation(c, b, RelationType.PARENT));

        mockMvc.perform(get("/api/v1/analysis/impact/" + a.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void consistencyViolations_detectsActiveConflict() throws Exception {
        var a = new Requirement(testProject, "INT-CONF-A", "Conflict A", "Conflict A statement");
        a.transitionStatus(Status.ACTIVE);
        a = requirementRepository.save(a);

        var b = new Requirement(testProject, "INT-CONF-B", "Conflict B", "Conflict B statement");
        b.transitionStatus(Status.ACTIVE);
        b = requirementRepository.save(b);

        relationRepository.save(new RequirementRelation(a, b, RelationType.CONFLICTS_WITH));

        mockMvc.perform(get("/api/v1/analysis/consistency-violations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sourceUid == 'INT-CONF-A')]").exists())
                .andExpect(jsonPath("$[0].violationType", is("ACTIVE_CONFLICT")));
    }

    @Test
    void crossWaveValidation_detectsForwardDeps() throws Exception {
        var wave1 = new Requirement(testProject, "INT-W1", "Wave 1", "Wave 1 statement");
        wave1.setWave(1);
        wave1 = requirementRepository.save(wave1);

        var wave3 = new Requirement(testProject, "INT-W3", "Wave 3", "Wave 3 statement");
        wave3.setWave(3);
        wave3 = requirementRepository.save(wave3);

        // wave1 depends on wave3 — source.wave(1) < target.wave(3), forward dependency violation
        relationRepository.save(new RequirementRelation(wave1, wave3, RelationType.DEPENDS_ON));

        mockMvc.perform(get("/api/v1/analysis/cross-wave"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sourceUid == 'INT-W1')]").exists())
                .andExpect(jsonPath("$[0].sourceWave", is(1)))
                .andExpect(jsonPath("$[0].targetWave", is(3)));
    }
}
