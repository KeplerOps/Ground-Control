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
    void crossWaveValidation_detectsBackwardDeps() throws Exception {
        var wave3 = new Requirement(testProject, "INT-W3", "Wave 3", "Wave 3 statement");
        wave3.setWave(3);
        wave3 = requirementRepository.save(wave3);

        var wave1 = new Requirement(testProject, "INT-W1", "Wave 1", "Wave 1 statement");
        wave1.setWave(1);
        wave1 = requirementRepository.save(wave1);

        // wave3 depends on wave1 — source.wave(3) > target.wave(1)
        relationRepository.save(new RequirementRelation(wave3, wave1, RelationType.DEPENDS_ON));

        mockMvc.perform(get("/api/v1/analysis/cross-wave"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sourceUid == 'INT-W3')]").exists())
                .andExpect(jsonPath("$[0].sourceWave", is(3)))
                .andExpect(jsonPath("$[0].targetWave", is(1)));
    }
}
