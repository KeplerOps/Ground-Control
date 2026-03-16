package com.keplerops.groundcontrol.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementSpecifications;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementFilter;
import com.keplerops.groundcontrol.domain.requirements.state.Priority;
import com.keplerops.groundcontrol.domain.requirements.state.RequirementType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class RequirementSpecificationsIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RequirementRepository repository;

    @Autowired
    private ProjectRepository projectRepository;

    private Project testProject;
    private Requirement mustFunctionalWave1;
    private Requirement shouldConstraintWave2;
    private Requirement couldFunctionalWave1;

    @BeforeEach
    void setUp() {
        testProject = projectRepository.findByIdentifier("ground-control").orElseThrow();

        mustFunctionalWave1 = new Requirement(testProject, "SPEC-001", "Alpha Feature", "Must have alpha");
        mustFunctionalWave1.setPriority(Priority.MUST);
        mustFunctionalWave1.setRequirementType(RequirementType.FUNCTIONAL);
        mustFunctionalWave1.setWave(1);
        mustFunctionalWave1.transitionStatus(Status.ACTIVE);
        repository.save(mustFunctionalWave1);

        shouldConstraintWave2 = new Requirement(testProject, "SPEC-002", "Beta Constraint", "Should have beta");
        shouldConstraintWave2.setPriority(Priority.SHOULD);
        shouldConstraintWave2.setRequirementType(RequirementType.CONSTRAINT);
        shouldConstraintWave2.setWave(2);
        repository.save(shouldConstraintWave2);

        couldFunctionalWave1 = new Requirement(testProject, "SPEC-003", "Gamma Feature", "Could have gamma");
        couldFunctionalWave1.setPriority(Priority.COULD);
        couldFunctionalWave1.setRequirementType(RequirementType.FUNCTIONAL);
        couldFunctionalWave1.setWave(1);
        repository.save(couldFunctionalWave1);
    }

    /** Filter results to only our SPEC-* test data, ignoring other integration test leftovers. */
    private List<Requirement> specOnly(List<Requirement> all) {
        return all.stream().filter(r -> r.getUid().startsWith("SPEC-")).toList();
    }

    @Nested
    class IndividualSpecs {

        @Test
        void hasStatus_filtersCorrectly() {
            var results = repository.findAll(RequirementSpecifications.hasStatus(Status.ACTIVE));
            assertThat(results).extracting(Requirement::getUid).contains("SPEC-001");
        }

        @Test
        void hasRequirementType_filtersCorrectly() {
            var results = repository.findAll(RequirementSpecifications.hasRequirementType(RequirementType.CONSTRAINT));
            assertThat(results).extracting(Requirement::getUid).contains("SPEC-002");
        }

        @Test
        void hasPriority_filtersCorrectly() {
            var results = repository.findAll(RequirementSpecifications.hasPriority(Priority.COULD));
            assertThat(results).extracting(Requirement::getUid).contains("SPEC-003");
        }

        @Test
        void hasWave_filtersCorrectly() {
            var results = repository.findAll(RequirementSpecifications.hasWave(2));
            assertThat(results).extracting(Requirement::getUid).contains("SPEC-002");
        }

        @Test
        void searchTitleOrStatement_matchesTitle() {
            var results = repository.findAll(RequirementSpecifications.searchTitleOrStatement("Alpha Feature"));
            assertThat(results).extracting(Requirement::getUid).contains("SPEC-001");
        }

        @Test
        void searchTitleOrStatement_matchesStatement() {
            var results = repository.findAll(RequirementSpecifications.searchTitleOrStatement("Could have gamma"));
            assertThat(results).extracting(Requirement::getUid).contains("SPEC-003");
        }

        @Test
        void searchTitleOrStatement_isCaseInsensitive() {
            var results = repository.findAll(RequirementSpecifications.searchTitleOrStatement("BETA CONSTRAINT"));
            assertThat(results).extracting(Requirement::getUid).contains("SPEC-002");
        }

        @Test
        void notArchived_excludesArchivedRequirements() {
            mustFunctionalWave1.transitionStatus(Status.ARCHIVED);
            repository.save(mustFunctionalWave1);

            var results = specOnly(repository.findAll(RequirementSpecifications.notArchived()));
            assertThat(results).extracting(Requirement::getUid).containsExactlyInAnyOrder("SPEC-002", "SPEC-003");
        }
    }

    @Nested
    class FromFilter {

        @Test
        void nullFilter_returnsAllNonArchived() {
            var results = repository.findAll(RequirementSpecifications.fromFilter(null), Pageable.unpaged());
            var specResults = specOnly(results.getContent());
            assertThat(specResults).hasSize(3);
        }

        @Test
        void emptyFilter_returnsAllNonArchived() {
            var filter = new RequirementFilter(null, null, null, null, null);
            var results = repository.findAll(RequirementSpecifications.fromFilter(filter), Pageable.unpaged());
            var specResults = specOnly(results.getContent());
            assertThat(specResults).hasSize(3);
        }

        @Test
        void filterByStatus() {
            var filter = new RequirementFilter(Status.ACTIVE, null, null, null, null);
            var results = repository.findAll(RequirementSpecifications.fromFilter(filter), Pageable.unpaged());
            assertThat(results.getContent()).extracting(Requirement::getUid).contains("SPEC-001");
            assertThat(specOnly(results.getContent()))
                    .extracting(Requirement::getUid)
                    .doesNotContain("SPEC-002", "SPEC-003");
        }

        @Test
        void filterByRequirementType() {
            var filter = new RequirementFilter(null, RequirementType.FUNCTIONAL, null, null, null);
            var results = repository.findAll(RequirementSpecifications.fromFilter(filter), Pageable.unpaged());
            assertThat(results.getContent()).extracting(Requirement::getUid).contains("SPEC-001", "SPEC-003");
        }

        @Test
        void filterByPriority() {
            var filter = new RequirementFilter(null, null, Priority.SHOULD, null, null);
            var results = repository.findAll(RequirementSpecifications.fromFilter(filter), Pageable.unpaged());
            assertThat(results.getContent()).extracting(Requirement::getUid).contains("SPEC-002");
        }

        @Test
        void filterByWave() {
            var filter = new RequirementFilter(null, null, null, 2, null);
            var results = repository.findAll(RequirementSpecifications.fromFilter(filter), Pageable.unpaged());
            assertThat(results.getContent()).extracting(Requirement::getUid).contains("SPEC-002");
        }

        @Test
        void filterBySearch() {
            var filter = new RequirementFilter(null, null, null, null, "Beta Constraint");
            var results = repository.findAll(RequirementSpecifications.fromFilter(filter), Pageable.unpaged());
            assertThat(results.getContent()).extracting(Requirement::getUid).contains("SPEC-002");
        }

        @Test
        void filterBySearch_blankIsIgnored() {
            var filter = new RequirementFilter(null, null, null, null, "   ");
            var results = repository.findAll(RequirementSpecifications.fromFilter(filter), Pageable.unpaged());
            var specResults = specOnly(results.getContent());
            assertThat(specResults).hasSize(3);
        }

        @Test
        void combinedFilters_narrowResults() {
            var filter =
                    new RequirementFilter(Status.ACTIVE, RequirementType.FUNCTIONAL, Priority.MUST, 1, "Alpha Feature");
            var results = repository.findAll(RequirementSpecifications.fromFilter(filter), Pageable.unpaged());
            assertThat(results.getContent()).extracting(Requirement::getUid).containsExactly("SPEC-001");
        }

        @Test
        void archivedStatusFilter_includesArchivedRequirements() {
            mustFunctionalWave1.transitionStatus(Status.ARCHIVED);
            repository.save(mustFunctionalWave1);

            var filter = new RequirementFilter(Status.ARCHIVED, null, null, null, null);
            var results = repository.findAll(RequirementSpecifications.fromFilter(filter), Pageable.unpaged());
            assertThat(results.getContent()).extracting(Requirement::getUid).contains("SPEC-001");
        }
    }
}
