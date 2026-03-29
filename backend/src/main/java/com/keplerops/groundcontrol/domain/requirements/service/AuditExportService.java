package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AuditExportService {

    private static final String CSV_HEADER = "timestamp,actor,reason,change_category,revision_type,entity_id,changes";

    public String toCsv(List<TimelineEntry> entries) {
        var sb = new StringBuilder();
        sb.append(CSV_HEADER).append('\n');
        for (var entry : entries) {
            sb.append(CsvUtils.escapeCsv(entry.timestamp().toString())).append(',');
            sb.append(CsvUtils.escapeCsv(entry.actor())).append(',');
            sb.append(CsvUtils.escapeCsv(entry.reason())).append(',');
            sb.append(CsvUtils.escapeCsv(entry.changeCategory().name())).append(',');
            sb.append(CsvUtils.escapeCsv(entry.revisionType())).append(',');
            sb.append(CsvUtils.escapeCsv(entry.entityId().toString())).append(',');
            sb.append(CsvUtils.escapeCsv(formatChanges(entry)));
            sb.append('\n');
        }
        return sb.toString();
    }

    private String formatChanges(TimelineEntry entry) {
        if (entry.changes() == null || entry.changes().isEmpty()) {
            return "";
        }
        return entry.changes().entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue().oldValue() + " -> "
                        + e.getValue().newValue())
                .collect(Collectors.joining("; "));
    }
}
