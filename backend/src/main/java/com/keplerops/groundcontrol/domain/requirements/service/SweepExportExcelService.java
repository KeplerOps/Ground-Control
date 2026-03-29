package com.keplerops.groundcontrol.domain.requirements.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class SweepExportExcelService {

    public byte[] toExcel(SweepReport report) {
        try (var workbook = new XSSFWorkbook()) {
            var headerStyle = createHeaderStyle(workbook);

            writeSummarySheet(workbook, headerStyle, report);
            writeCyclesSheet(workbook, headerStyle, report.cycles());
            writeOrphansSheet(workbook, headerStyle, report.orphans());
            writeCoverageGapsSheet(workbook, headerStyle, report.coverageGaps());
            writeViolationsSheet(workbook, headerStyle, report);
            writeCompletenessSheet(workbook, headerStyle, report.completeness());
            writeQualityGatesSheet(workbook, headerStyle, report);

            var out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ExportException("Failed to generate Excel sweep export", e);
        }
    }

    private void writeSummarySheet(XSSFWorkbook workbook, CellStyle headerStyle, SweepReport report) {
        Sheet sheet = workbook.createSheet("Summary");
        writeHeader(sheet, headerStyle, "Project", "Timestamp", "Has Problems", "Total Problems");

        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue(nullSafe(report.projectIdentifier()));
        row.createCell(1).setCellValue(report.timestamp().toString());
        row.createCell(2).setCellValue(report.hasProblems() ? "YES" : "NO");
        row.createCell(3).setCellValue(report.totalProblems());
        autoSize(sheet, 4);
    }

    private void writeCyclesSheet(XSSFWorkbook workbook, CellStyle headerStyle, List<CycleResult> cycles) {
        Sheet sheet = workbook.createSheet("Cycles");
        writeHeader(sheet, headerStyle, "Cycle Members", "Edge Source", "Edge Target", "Edge Type");

        int rowNum = 1;
        for (var cycle : cycles) {
            String members = String.join(" -> ", cycle.members());
            for (var edge : cycle.edges()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(members);
                row.createCell(1).setCellValue(nullSafe(edge.sourceUid()));
                row.createCell(2).setCellValue(nullSafe(edge.targetUid()));
                row.createCell(3)
                        .setCellValue(
                                edge.relationType() != null
                                        ? edge.relationType().name()
                                        : "");
            }
        }
        autoSize(sheet, 4);
    }

    private void writeOrphansSheet(
            XSSFWorkbook workbook, CellStyle headerStyle, List<SweepReport.RequirementSummary> orphans) {
        Sheet sheet = workbook.createSheet("Orphans");
        writeHeader(sheet, headerStyle, "UID", "Title");

        int rowNum = 1;
        for (var orphan : orphans) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(nullSafe(orphan.uid()));
            row.createCell(1).setCellValue(nullSafe(orphan.title()));
        }
        autoSize(sheet, 2);
    }

    private void writeCoverageGapsSheet(
            XSSFWorkbook workbook,
            CellStyle headerStyle,
            Map<String, List<SweepReport.RequirementSummary>> coverageGaps) {
        Sheet sheet = workbook.createSheet("Coverage Gaps");
        writeHeader(sheet, headerStyle, "Link Type", "UID", "Title");

        int rowNum = 1;
        for (var entry : coverageGaps.entrySet()) {
            for (var req : entry.getValue()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(nullSafe(req.uid()));
                row.createCell(2).setCellValue(nullSafe(req.title()));
            }
        }
        autoSize(sheet, 3);
    }

    private void writeViolationsSheet(XSSFWorkbook workbook, CellStyle headerStyle, SweepReport report) {
        Sheet sheet = workbook.createSheet("Violations");
        writeHeader(
                sheet, headerStyle, "Type", "Source UID", "Source Detail", "Target UID", "Target Detail", "Relation");

        int rowNum = 1;
        for (var v : report.crossWaveViolations()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("CROSS_WAVE");
            row.createCell(1).setCellValue(nullSafe(v.sourceUid()));
            row.createCell(2).setCellValue(v.sourceWave() != null ? "Wave " + v.sourceWave() : "");
            row.createCell(3).setCellValue(nullSafe(v.targetUid()));
            row.createCell(4).setCellValue(v.targetWave() != null ? "Wave " + v.targetWave() : "");
            row.createCell(5).setCellValue(nullSafe(v.relationType()));
        }
        for (var v : report.consistencyViolations()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(nullSafe(v.violationType()));
            row.createCell(1).setCellValue(nullSafe(v.sourceUid()));
            row.createCell(2).setCellValue(nullSafe(v.sourceStatus()));
            row.createCell(3).setCellValue(nullSafe(v.targetUid()));
            row.createCell(4).setCellValue(nullSafe(v.targetStatus()));
            row.createCell(5).setCellValue("");
        }
        autoSize(sheet, 6);
    }

    private void writeCompletenessSheet(XSSFWorkbook workbook, CellStyle headerStyle, CompletenessResult completeness) {
        Sheet sheet = workbook.createSheet("Completeness");
        writeHeader(sheet, headerStyle, "Status", "Count");

        int rowNum = 1;
        for (var entry : completeness.byStatus().entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }

        if (!completeness.issues().isEmpty()) {
            rowNum++;
            Row issueHeader = sheet.createRow(rowNum++);
            issueHeader.createCell(0).setCellValue("Issues");
            for (var issue : completeness.issues()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(nullSafe(issue.uid()));
                row.createCell(1).setCellValue(nullSafe(issue.issue()));
            }
        }
        autoSize(sheet, 2);
    }

    private void writeQualityGatesSheet(XSSFWorkbook workbook, CellStyle headerStyle, SweepReport report) {
        Sheet sheet = workbook.createSheet("Quality Gates");
        writeHeader(
                sheet,
                headerStyle,
                "Gate Name",
                "Metric Type",
                "Metric Param",
                "Operator",
                "Threshold",
                "Actual Value",
                "Passed");

        if (report.qualityGateResults() == null) {
            return;
        }

        int rowNum = 1;
        for (var gate : report.qualityGateResults().gates()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(nullSafe(gate.gateName()));
            row.createCell(1).setCellValue(nullSafe(gate.metricType()));
            row.createCell(2).setCellValue(nullSafe(gate.metricParam()));
            row.createCell(3).setCellValue(nullSafe(gate.operator()));
            row.createCell(4).setCellValue(gate.threshold());
            row.createCell(5).setCellValue(gate.actualValue());
            row.createCell(6).setCellValue(gate.passed() ? "PASS" : "FAIL");
        }
        autoSize(sheet, 7);
    }

    private void writeHeader(Sheet sheet, CellStyle headerStyle, String... headers) {
        sheet.createFreezePane(0, 1);
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            var cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void autoSize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static String nullSafe(String value) {
        return value != null ? value : "";
    }
}
