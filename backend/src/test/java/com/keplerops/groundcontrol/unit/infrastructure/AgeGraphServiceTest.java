package com.keplerops.groundcontrol.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
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

            // LOAD, SET, DETACH DELETE, and one CREATE for the requirement
            verify(jdbcTemplate, atLeast(4)).execute(anyString());
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
