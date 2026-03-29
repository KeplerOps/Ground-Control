package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RequirementsExportCsvService {

    private static final String CSV_HEADER =
            "uid,title,statement,rationale,type,priority,status,wave,traceability_links,created_at,updated_at";

    public String toCsv(RequirementsExportData data) {
        var sb = new StringBuilder();
        sb.append(CSV_HEADER).append('\n');
        for (var req : data.requirements()) {
            sb.append(CsvUtils.escapeCsv(req.uid())).append(',');
            sb.append(CsvUtils.escapeCsv(req.title())).append(',');
            sb.append(CsvUtils.escapeCsv(req.statement())).append(',');
            sb.append(CsvUtils.escapeCsv(req.rationale())).append(',');
            sb.append(CsvUtils.escapeCsv(req.requirementType())).append(',');
            sb.append(CsvUtils.escapeCsv(req.priority())).append(',');
            sb.append(CsvUtils.escapeCsv(req.status())).append(',');
            sb.append(CsvUtils.escapeCsv(req.wave() != null ? req.wave().toString() : ""))
                    .append(',');
            sb.append(CsvUtils.escapeCsv(formatLinks(req))).append(',');
            sb.append(CsvUtils.escapeCsv(
                            req.createdAt() != null ? req.createdAt().toString() : ""))
                    .append(',');
            sb.append(
                    CsvUtils.escapeCsv(req.updatedAt() != null ? req.updatedAt().toString() : ""));
            sb.append('\n');
        }
        return sb.toString();
    }

    private String formatLinks(RequirementsExportData.RequirementSnapshot req) {
        if (req.traceabilityLinks() == null || req.traceabilityLinks().isEmpty()) {
            return "";
        }
        return req.traceabilityLinks().stream()
                .map(link -> link.linkType() + ":" + link.artifactIdentifier())
                .collect(Collectors.joining("; "));
    }
}
