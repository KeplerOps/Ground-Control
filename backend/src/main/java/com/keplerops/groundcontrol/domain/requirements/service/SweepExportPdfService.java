package com.keplerops.groundcontrol.domain.requirements.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SweepExportPdfService {

    private static final com.lowagie.text.Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final com.lowagie.text.Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final com.lowagie.text.Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
    private static final com.lowagie.text.Font CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private static final com.lowagie.text.Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);

    public byte[] toPdf(SweepReport report) {
        var out = new ByteArrayOutputStream();
        var document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addTitle(document, report);
            addSummary(document, report);
            addCycles(document, report.cycles());
            addOrphans(document, report.orphans());
            addCoverageGaps(document, report.coverageGaps());
            addCrossWaveViolations(document, report.crossWaveViolations());
            addConsistencyViolations(document, report.consistencyViolations());
            addCompleteness(document, report.completeness());
            addQualityGates(document, report);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new ExportException("Failed to generate PDF sweep export", e);
        }
    }

    private void addTitle(Document document, SweepReport report) {
        var title = new Paragraph("Sweep Report - " + report.projectIdentifier(), TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        var subtitle = new Paragraph(
                "Generated: "
                        + report.timestamp()
                                .atZone(java.time.ZoneOffset.UTC)
                                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                SUBTITLE_FONT);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(15f);
        document.add(subtitle);
    }

    private void addSummary(Document document, SweepReport report) {
        var statusColor = report.hasProblems() ? java.awt.Color.RED : new java.awt.Color(0, 128, 0);
        var statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, statusColor);
        var status = new Paragraph(
                (report.hasProblems() ? "PROBLEMS FOUND: " + report.totalProblems() : "NO PROBLEMS"), statusFont);
        status.setAlignment(Element.ALIGN_CENTER);
        status.setSpacingAfter(15f);
        document.add(status);
    }

    private void addCycles(Document document, List<CycleResult> cycles) {
        if (cycles.isEmpty()) {
            return;
        }
        addSection(document, "Dependency Cycles (" + cycles.size() + ")");
        var table = new PdfPTable(3);
        table.setWidthPercentage(100);
        addTableHeader(table, "Cycle Members", "Edge", "Type");
        for (var cycle : cycles) {
            String members = String.join(" -> ", cycle.members());
            for (var edge : cycle.edges()) {
                addTableCell(table, members);
                addTableCell(table, edge.sourceUid() + " -> " + edge.targetUid());
                addTableCell(
                        table, edge.relationType() != null ? edge.relationType().name() : "");
            }
        }
        document.add(table);
    }

    private void addOrphans(Document document, List<SweepReport.RequirementSummary> orphans) {
        if (orphans.isEmpty()) {
            return;
        }
        addSection(document, "Orphaned Requirements (" + orphans.size() + ")");
        var table = new PdfPTable(2);
        table.setWidthPercentage(100);
        addTableHeader(table, "UID", "Title");
        for (var orphan : orphans) {
            addTableCell(table, orphan.uid());
            addTableCell(table, orphan.title());
        }
        document.add(table);
    }

    private void addCoverageGaps(Document document, Map<String, List<SweepReport.RequirementSummary>> coverageGaps) {
        if (coverageGaps.isEmpty()) {
            return;
        }
        int totalGaps = coverageGaps.values().stream().mapToInt(List::size).sum();
        addSection(document, "Coverage Gaps (" + totalGaps + ")");
        var table = new PdfPTable(3);
        table.setWidthPercentage(100);
        addTableHeader(table, "Link Type", "UID", "Title");
        for (var entry : coverageGaps.entrySet()) {
            for (var req : entry.getValue()) {
                addTableCell(table, entry.getKey());
                addTableCell(table, req.uid());
                addTableCell(table, req.title());
            }
        }
        document.add(table);
    }

    private void addCrossWaveViolations(Document document, List<SweepReport.CrossWaveViolationSummary> violations) {
        if (violations.isEmpty()) {
            return;
        }
        addSection(document, "Cross-Wave Violations (" + violations.size() + ")");
        var table = new PdfPTable(5);
        table.setWidthPercentage(100);
        addTableHeader(table, "Source UID", "Source Wave", "Target UID", "Target Wave", "Relation");
        for (var v : violations) {
            addTableCell(table, v.sourceUid());
            addTableCell(table, v.sourceWave() != null ? v.sourceWave().toString() : "");
            addTableCell(table, v.targetUid());
            addTableCell(table, v.targetWave() != null ? v.targetWave().toString() : "");
            addTableCell(table, v.relationType());
        }
        document.add(table);
    }

    private void addConsistencyViolations(Document document, List<SweepReport.ConsistencyViolationSummary> violations) {
        if (violations.isEmpty()) {
            return;
        }
        addSection(document, "Consistency Violations (" + violations.size() + ")");
        var table = new PdfPTable(5);
        table.setWidthPercentage(100);
        addTableHeader(table, "Source UID", "Source Status", "Target UID", "Target Status", "Violation");
        for (var v : violations) {
            addTableCell(table, v.sourceUid());
            addTableCell(table, v.sourceStatus());
            addTableCell(table, v.targetUid());
            addTableCell(table, v.targetStatus());
            addTableCell(table, v.violationType());
        }
        document.add(table);
    }

    private void addCompleteness(Document document, CompletenessResult completeness) {
        addSection(document, "Completeness (Total: " + completeness.total() + ")");
        var table = new PdfPTable(2);
        table.setWidthPercentage(50);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        addTableHeader(table, "Status", "Count");
        for (var entry : completeness.byStatus().entrySet()) {
            addTableCell(table, entry.getKey());
            addTableCell(table, entry.getValue().toString());
        }
        document.add(table);

        if (!completeness.issues().isEmpty()) {
            var issueTable = new PdfPTable(2);
            issueTable.setWidthPercentage(100);
            issueTable.setSpacingBefore(10f);
            addTableHeader(issueTable, "UID", "Issue");
            for (var issue : completeness.issues()) {
                addTableCell(issueTable, issue.uid());
                addTableCell(issueTable, issue.issue());
            }
            document.add(issueTable);
        }
    }

    private void addQualityGates(Document document, SweepReport report) {
        if (report.qualityGateResults() == null) {
            return;
        }
        var qg = report.qualityGateResults();
        addSection(
                document,
                "Quality Gates (" + (qg.passed() ? "PASSED" : "FAILED") + " - " + qg.passedCount() + "/"
                        + qg.totalGates() + ")");
        var table = new PdfPTable(5);
        table.setWidthPercentage(100);
        addTableHeader(table, "Gate Name", "Metric", "Threshold", "Actual", "Result");
        for (var gate : qg.gates()) {
            addTableCell(table, gate.gateName());
            addTableCell(
                    table, gate.metricType() + (gate.metricParam() != null ? " (" + gate.metricParam() + ")" : ""));
            addTableCell(table, gate.operator() + " " + gate.threshold());
            addTableCell(table, String.valueOf(gate.actualValue()));
            addTableCell(table, gate.passed() ? "PASS" : "FAIL");
        }
        document.add(table);
    }

    private void addSection(Document document, String title) {
        var paragraph = new Paragraph(title, SECTION_FONT);
        paragraph.setSpacingBefore(15f);
        paragraph.setSpacingAfter(8f);
        document.add(paragraph);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        for (String header : headers) {
            var cell = new PdfPCell(new Phrase(header, HEADER_FONT));
            cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(4f);
            table.addCell(cell);
        }
    }

    private void addTableCell(PdfPTable table, String text) {
        var cell = new PdfPCell(new Phrase(text != null ? text : "", CELL_FONT));
        cell.setPadding(3f);
        table.addCell(cell);
    }
}
