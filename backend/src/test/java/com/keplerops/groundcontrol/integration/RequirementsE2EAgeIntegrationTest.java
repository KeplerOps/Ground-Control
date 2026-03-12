package com.keplerops.groundcontrol.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.requirements.service.GraphClient;
import com.keplerops.groundcontrol.domain.requirements.service.ImportService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequirementsE2EAgeIntegrationTest extends BaseAgeIntegrationTest {

    @Autowired
    private ImportService importService;

    @Autowired
    private GraphClient graphClient;

    @Test
    @Order(1)
    void importFixtureForGraph() throws Exception {
        var sdocContent = new String(
                getClass()
                        .getResourceAsStream("/fixtures/test-requirements.sdoc")
                        .readAllBytes(),
                StandardCharsets.UTF_8);

        var result = importService.importStrictdoc("test-requirements.sdoc", sdocContent);
        assertThat(result.requirementsCreated()).isEqualTo(5);
        assertThat(result.relationsCreated()).isEqualTo(2);
    }

    @Test
    @Order(2)
    void materializeGraph() {
        graphClient.materializeGraph();
    }

    @Test
    @Order(3)
    void ancestorQueryMatchesJPA() {
        var ancestors = graphClient.getAncestors("E2E-REQ-003", 10);
        assertThat(ancestors).contains("E2E-REQ-002", "E2E-REQ-001");
    }

    @Test
    @Order(4)
    void descendantQueryMatchesJPA() {
        var descendants = graphClient.getDescendants("E2E-REQ-001", 10);
        assertThat(descendants).contains("E2E-REQ-002", "E2E-REQ-003");
    }
}
