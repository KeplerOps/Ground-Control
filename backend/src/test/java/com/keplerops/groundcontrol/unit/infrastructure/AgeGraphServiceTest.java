package com.keplerops.groundcontrol.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.infrastructure.age.AgeGraphService;
import com.keplerops.groundcontrol.infrastructure.age.AgeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class AgeGraphServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private RequirementRelationRepository relationRepository;

    private AgeGraphService service;

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
}
