package com.keplerops.groundcontrol.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import com.keplerops.groundcontrol.infrastructure.age.AgeGraphService;
import com.keplerops.groundcontrol.infrastructure.age.AgeProperties;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

@ExtendWith(MockitoExtension.class)
class AgeGraphServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private RequirementRelationRepository relationRepository;

    private AgeGraphService service;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Project TEST_PROJECT = createTestProject();

    private static Project createTestProject() {
        var project = new Project("test-project", "Test Project");
        try {
            var field = Project.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(project, PROJECT_ID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return project;
    }

    @BeforeEach
    void setUp() {
        var disabledProperties = new AgeProperties(false, "requirements");
        service = new AgeGraphService(jdbcTemplate, disabledProperties, requirementRepository, relationRepository);
    }

    @Nested
    class WhenDisabled {

        @Test
        void materializeGraph_isNoOp() {
            service.materializeGraph();

            verifyNoInteractions(jdbcTemplate, requirementRepository, relationRepository);
        }

        @Test
        void getAncestors_returnsEmpty() {
            var result = service.getAncestors("REQ-001", 10);

            assertThat(result).isEmpty();
            verifyNoInteractions(jdbcTemplate);
        }

        @Test
        void getDescendants_returnsEmpty() {
            var result = service.getDescendants("REQ-001", 10);

            assertThat(result).isEmpty();
            verifyNoInteractions(jdbcTemplate);
        }

        @Test
        void findPaths_returnsEmpty() {
            var result = service.findPaths("REQ-001", "REQ-002");

            assertThat(result).isEmpty();
            verifyNoInteractions(jdbcTemplate);
        }
    }

    @Nested
    class WhenEnabled {

        private AgeGraphService enabledService;

        @BeforeEach
        void setUp() {
            var enabledProperties = new AgeProperties(true, "test_graph");
            enabledService =
                    new AgeGraphService(jdbcTemplate, enabledProperties, requirementRepository, relationRepository);
        }

        @Test
        void materializeGraph_createsNodesAndEdges() {
            when(requirementRepository.findAll()).thenReturn(List.of());
            when(relationRepository.findAllWithSourceAndTarget()).thenReturn(List.of());

            enabledService.materializeGraph();

            verify(jdbcTemplate).execute("LOAD 'age'");
            verify(jdbcTemplate).execute("SET search_path = ag_catalog, \"$user\", public");
            verify(jdbcTemplate).execute(contains("DETACH DELETE"));
        }

        @Test
        void materializeGraph_withRequirements_createsNodes() {
            var req = new Requirement(TEST_PROJECT, "GC-A001", "Test Req", "Statement");
            when(requirementRepository.findAll()).thenReturn(List.of(req));
            when(relationRepository.findAllWithSourceAndTarget()).thenReturn(List.of());

            enabledService.materializeGraph();

            // LOAD, SET, DETACH DELETE, and one bulk CREATE for the requirement
            verify(jdbcTemplate, atLeast(4)).execute(anyString());
        }

        @Test
        void materializeGraph_batchesAllNodesIntoOneCypherStatement() {
            var req1 = new Requirement(TEST_PROJECT, "GC-A001", "Req One", "Statement");
            var req2 = new Requirement(TEST_PROJECT, "GC-A002", "Req Two", "Statement");
            var req3 = new Requirement(TEST_PROJECT, "GC-A003", "Req Three", "Statement");
            when(requirementRepository.findAll()).thenReturn(List.of(req1, req2, req3));
            when(relationRepository.findAllWithSourceAndTarget()).thenReturn(List.of());

            enabledService.materializeGraph();

            // Exactly one CREATE statement regardless of node count (LOAD + SET + DETACH DELETE + 1 bulk CREATE = 4)
            verify(jdbcTemplate, times(4)).execute(anyString());
            verify(jdbcTemplate).execute(contains("GC-A001"));
        }

        @Test
        void materializeGraph_batchesEdgesByRelationType() {
            var req1 = new Requirement(TEST_PROJECT, "GC-A001", "Req One", "Statement");
            var req2 = new Requirement(TEST_PROJECT, "GC-A002", "Req Two", "Statement");
            var req3 = new Requirement(TEST_PROJECT, "GC-A003", "Req Three", "Statement");
            var rel1 = new RequirementRelation(req1, req2, RelationType.PARENT);
            var rel2 = new RequirementRelation(req2, req3, RelationType.PARENT);
            var rel3 = new RequirementRelation(req1, req3, RelationType.DEPENDS_ON);
            when(requirementRepository.findAll()).thenReturn(List.of(req1, req2, req3));
            when(relationRepository.findAllWithSourceAndTarget()).thenReturn(List.of(rel1, rel2, rel3));

            enabledService.materializeGraph();

            // LOAD + SET + DETACH DELETE + 1 node CREATE + 2 edge UNWIND (one per relation type) = 6
            verify(jdbcTemplate, times(6)).execute(anyString());
        }

        @Test
        void materializeGraph_skipsNodeCreateWhenNoRequirements() {
            when(requirementRepository.findAll()).thenReturn(List.of());
            when(relationRepository.findAllWithSourceAndTarget()).thenReturn(List.of());

            enabledService.materializeGraph();

            // Only LOAD + SET + DETACH DELETE, no CREATE
            verify(jdbcTemplate, times(3)).execute(anyString());
            verify(jdbcTemplate, never()).execute(contains("CREATE"));
        }

        @Test
        void getAncestors_queriesGraph() {
            enabledService.getAncestors("REQ-001", 5);

            // setupSearchPath: LOAD + SET
            verify(jdbcTemplate, times(2)).execute(anyString());
            verify(jdbcTemplate).query(contains("PARENT"), any(RowCallbackHandler.class));
        }

        @Test
        void getDescendants_queriesGraph() {
            enabledService.getDescendants("REQ-001", 5);

            verify(jdbcTemplate, times(2)).execute(anyString());
            verify(jdbcTemplate).query(contains("PARENT"), any(RowCallbackHandler.class));
        }

        @Test
        void findPaths_queriesGraphWithRelationships() {
            enabledService.findPaths("REQ-001", "REQ-002");

            verify(jdbcTemplate, times(2)).execute(anyString());
            verify(jdbcTemplate).query(contains("label(r)"), any(RowCallbackHandler.class));
        }
    }
}
