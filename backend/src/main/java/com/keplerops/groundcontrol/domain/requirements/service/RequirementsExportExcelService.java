package com.keplerops.groundcontrol.domain.requirements.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class RequirementsExportExcelService {

    private static final String[] REQ_HEADERS = {
        "UID",
        "Title",
        "Statement",
        "Rationale",
        "Type",
        "Priority",
        "Status",
        "Wave",
        "Traceability Links",
        "Created At",
        "Updated At"
    };

    private static final String[] TRACE_HEADERS = {
        "Requirement UID", "Artifact Type", "Artifact Identifier", "Link Type", "Artifact URL", "Artifact Title"
    };

    public byte[] toExcel(RequirementsExportData data) {
        try (var workbook = new XSSFWorkbook()) {
            var headerStyle = createHeaderStyle(workbook);

            writeRequirementsSheet(workbook, headerStyle, data);
            writeTraceabilitySheet(workbook, headerStyle, data);

            var out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ExportException("Failed to generate Excel export", e);
        }
    }

    private void writeRequirementsSheet(XSSFWorkbook workbook, CellStyle headerStyle, RequirementsExportData data) {
        Sheet sheet = workbook.createSheet("Requirements");
        sheet.createFreezePane(0, 1);

        Row header = sheet.createRow(0);
        for (int i = 0; i < REQ_HEADERS.length; i++) {
            var cell = header.createCell(i);
            cell.setCellValue(REQ_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (var req : data.requirements()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(nullSafe(req.uid()));
            row.createCell(1).setCellValue(nullSafe(req.title()));
            row.createCell(2).setCellValue(nullSafe(req.statement()));
            row.createCell(3).setCellValue(nullSafe(req.rationale()));
            row.createCell(4).setCellValue(nullSafe(req.requirementType()));
            row.createCell(5).setCellValue(nullSafe(req.priority()));
            row.createCell(6).setCellValue(nullSafe(req.status()));
            row.createCell(7).setCellValue(req.wave() != null ? req.wave().toString() : "");
            row.createCell(8).setCellValue(formatLinks(req));
            row.createCell(9)
                    .setCellValue(req.createdAt() != null ? req.createdAt().toString() : "");
            row.createCell(10)
                    .setCellValue(req.updatedAt() != null ? req.updatedAt().toString() : "");
        }

        for (int i = 0; i < REQ_HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void writeTraceabilitySheet(XSSFWorkbook workbook, CellStyle headerStyle, RequirementsExportData data) {
        Sheet sheet = workbook.createSheet("Traceability");
        sheet.createFreezePane(0, 1);

        Row header = sheet.createRow(0);
        for (int i = 0; i < TRACE_HEADERS.length; i++) {
            var cell = header.createCell(i);
            cell.setCellValue(TRACE_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (var req : data.requirements()) {
            if (req.traceabilityLinks() == null) {
                continue;
            }
            for (var link : req.traceabilityLinks()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(nullSafe(req.uid()));
                row.createCell(1).setCellValue(nullSafe(link.artifactType()));
                row.createCell(2).setCellValue(nullSafe(link.artifactIdentifier()));
                row.createCell(3).setCellValue(nullSafe(link.linkType()));
                row.createCell(4).setCellValue(nullSafe(link.artifactUrl()));
                row.createCell(5).setCellValue(nullSafe(link.artifactTitle()));
            }
        }

        for (int i = 0; i < TRACE_HEADERS.length; i++) {
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

    private String formatLinks(RequirementsExportData.RequirementSnapshot req) {
        if (req.traceabilityLinks() == null || req.traceabilityLinks().isEmpty()) {
            return "";
        }
        return req.traceabilityLinks().stream()
                .map(link -> link.linkType() + ":" + link.artifactIdentifier())
                .collect(Collectors.joining("; "));
    }

    private static String nullSafe(String value) {
        return value != null ? value : "";
    }
}
