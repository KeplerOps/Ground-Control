package com.keplerops.groundcontrol.unit.api;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.api.riskscenarios.RiskRegisterRecordResponse;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskRegisterRecord;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenario;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskRegisterStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RiskRegisterRecordResponseTest {

    private final Project project = new Project("gc", "Ground Control");
    private final UUID projectId = UUID.randomUUID();

    @Nested
    class FromWithAllFields {

        @Test
        void mapsAllFieldsFromDomainModel() {
            setField(project, "id", projectId);

            var record = new RiskRegisterRecord(project, "RR-1", "Security incidents");
            var recordId = UUID.randomUUID();
            setField(record, "id", recordId);
            record.setOwner("bob");
            record.setReviewCadence("quarterly");
            var nextReview = Instant.parse("2026-07-01T00:00:00Z");
            record.setNextReviewAt(nextReview);
            record.setCategoryTags(List.of("security", "compliance"));
            record.setDecisionMetadata(Map.of("source", "audit", "priority", 1));
            record.setAssetScopeSummary("All production systems");
            var now = Instant.now();
            setField(record, "createdAt", now);
            setField(record, "updatedAt", now);

            var scenario1 = new RiskScenario(project, "RS-1", "Scenario 1", "Actor", "Event", "Object", "Consequence");
            scenario1.setTimeHorizon("12 months");
            var scenario1Id = UUID.randomUUID();
            setField(scenario1, "id", scenario1Id);

            var scenario2 =
                    new RiskScenario(project, "RS-2", "Scenario 2", "Actor2", "Event2", "Object2", "Consequence2");
            scenario2.setTimeHorizon("6 months");
            var scenario2Id = UUID.randomUUID();
            setField(scenario2, "id", scenario2Id);

            record.replaceRiskScenarios(List.of(scenario1, scenario2));

            var response = RiskRegisterRecordResponse.from(record);

            assertThat(response.id()).isEqualTo(recordId);
            assertThat(response.graphNodeId())
                    .isEqualTo(GraphIds.nodeId(GraphEntityType.RISK_REGISTER_RECORD, recordId));
            assertThat(response.projectIdentifier()).isEqualTo("gc");
            assertThat(response.uid()).isEqualTo("RR-1");
            assertThat(response.title()).isEqualTo("Security incidents");
            assertThat(response.owner()).isEqualTo("bob");
            assertThat(response.status()).isEqualTo(RiskRegisterStatus.IDENTIFIED);
            assertThat(response.reviewCadence()).isEqualTo("quarterly");
            assertThat(response.nextReviewAt()).isEqualTo(nextReview);
            assertThat(response.categoryTags()).containsExactly("security", "compliance");
            assertThat(response.decisionMetadata())
                    .containsEntry("source", "audit")
                    .containsEntry("priority", 1);
            assertThat(response.assetScopeSummary()).isEqualTo("All production systems");
            assertThat(response.riskScenarioIds()).containsExactly(scenario1Id, scenario2Id);
            assertThat(response.riskScenarioUids()).containsExactly("RS-1", "RS-2");
            assertThat(response.createdAt()).isEqualTo(now);
            assertThat(response.updatedAt()).isEqualTo(now);
        }
    }

    @Nested
    class FromWithEmptyScenarios {

        @Test
        void returnsEmptyListsWhenNoScenariosLinked() {
            setField(project, "id", projectId);

            var record = new RiskRegisterRecord(project, "RR-2", "Empty record");
            var recordId = UUID.randomUUID();
            setField(record, "id", recordId);
            var now = Instant.now();
            setField(record, "createdAt", now);
            setField(record, "updatedAt", now);
            // No scenarios added; default is empty LinkedHashSet

            var response = RiskRegisterRecordResponse.from(record);

            assertThat(response.riskScenarioIds()).isEmpty();
            assertThat(response.riskScenarioUids()).isEmpty();
        }
    }

    @Nested
    class FromWithNullOptionalFields {

        @Test
        void handlesNullOwnerCadenceTagsMetadataAndScopeSummary() {
            setField(project, "id", projectId);

            var record = new RiskRegisterRecord(project, "RR-3", "Minimal record");
            setField(record, "id", UUID.randomUUID());
            var now = Instant.now();
            setField(record, "createdAt", now);
            setField(record, "updatedAt", now);
            // owner, reviewCadence, nextReviewAt, categoryTags, decisionMetadata,
            // assetScopeSummary all null by default

            var response = RiskRegisterRecordResponse.from(record);

            assertThat(response.owner()).isNull();
            assertThat(response.reviewCadence()).isNull();
            assertThat(response.nextReviewAt()).isNull();
            assertThat(response.categoryTags()).isNull();
            assertThat(response.decisionMetadata()).isNull();
            assertThat(response.assetScopeSummary()).isNull();
        }
    }
}
