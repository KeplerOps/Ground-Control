package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.requirements.service.AuditExportService;
import com.keplerops.groundcontrol.domain.requirements.service.FieldChange;
import com.keplerops.groundcontrol.domain.requirements.service.TimelineEntry;
import com.keplerops.groundcontrol.domain.requirements.state.ChangeCategory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditExportServiceTest {

    private final AuditExportService service = new AuditExportService();

    @Test
    void toCsv_emptyList_returnsHeaderOnly() {
        var csv = service.toCsv(List.of());
        assertThat(csv).isEqualTo("timestamp,actor,reason,change_category,revision_type,entity_id,changes\n");
    }

    @Test
    void toCsv_singleEntry_formatsCorrectly() {
        var id = UUID.randomUUID();
        var timestamp = Instant.parse("2026-03-23T12:00:00Z");
        var entry = new TimelineEntry(
                1, "ADD", timestamp, "alice", "initial creation", ChangeCategory.REQUIREMENT, id, Map.of(), Map.of());

        var csv = service.toCsv(List.of(entry));
        var lines = csv.split("\n");

        assertThat(lines).hasSize(2);
        assertThat(lines[1]).isEqualTo("2026-03-23T12:00:00Z,alice,initial creation,REQUIREMENT,ADD," + id + ",");
    }

    @Test
    void toCsv_nullReason_outputsEmpty() {
        var id = UUID.randomUUID();
        var entry = new TimelineEntry(
                1, "MOD", Instant.now(), "bob", null, ChangeCategory.RELATION, id, Map.of(), Map.of());

        var csv = service.toCsv(List.of(entry));
        var dataLine = csv.split("\n")[1];
        // actor,reason should be "bob," (empty reason)
        assertThat(dataLine).contains("bob,,RELATION");
    }

    @Test
    void toCsv_withChanges_formatsFieldDiffs() {
        var id = UUID.randomUUID();
        var changes = Map.of("title", new FieldChange("old title", "new title"));
        var entry = new TimelineEntry(
                2,
                "MOD",
                Instant.parse("2026-03-23T13:00:00Z"),
                "carol",
                null,
                ChangeCategory.REQUIREMENT,
                id,
                Map.of(),
                changes);

        var csv = service.toCsv(List.of(entry));
        assertThat(csv).contains("title: old title -> new title");
    }

    @Test
    void toCsv_commaInReason_isQuoted() {
        var id = UUID.randomUUID();
        var entry = new TimelineEntry(
                1,
                "ADD",
                Instant.now(),
                "dave",
                "reason, with comma",
                ChangeCategory.REQUIREMENT,
                id,
                Map.of(),
                Map.of());

        var csv = service.toCsv(List.of(entry));
        assertThat(csv).contains("\"reason, with comma\"");
    }

    @Test
    void toCsv_formulaInjection_isPrefixed() {
        var id = UUID.randomUUID();
        var entry = new TimelineEntry(
                1, "ADD", Instant.now(), "eve", "=CMD('calc')", ChangeCategory.REQUIREMENT, id, Map.of(), Map.of());

        var csv = service.toCsv(List.of(entry));
        // Leading = should be prefixed with ' and then quoted (contains comma from parentheses)
        assertThat(csv).contains("'=CMD('calc')");
        assertThat(csv).doesNotContain(",=CMD");
    }
}
