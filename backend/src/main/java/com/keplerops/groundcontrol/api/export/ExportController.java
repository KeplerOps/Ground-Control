package com.keplerops.groundcontrol.api.export;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.service.AnalysisService;
import com.keplerops.groundcontrol.domain.requirements.service.AnalysisSweepService;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementsExportCsvService;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementsExportExcelService;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementsExportPdfService;
import com.keplerops.groundcontrol.domain.requirements.service.SweepExportCsvService;
import com.keplerops.groundcontrol.domain.requirements.service.SweepExportExcelService;
import com.keplerops.groundcontrol.domain.requirements.service.SweepExportPdfService;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/export")
public class ExportController {

    private static final MediaType CSV_MEDIA_TYPE = MediaType.parseMediaType("text/csv");
    private static final MediaType XLSX_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ProjectService projectService;
    private final AnalysisService analysisService;
    private final AnalysisSweepService analysisSweepService;
    private final RequirementsExportCsvService requirementsCsvService;
    private final RequirementsExportExcelService requirementsExcelService;
    private final RequirementsExportPdfService requirementsPdfService;
    private final SweepExportCsvService sweepCsvService;
    private final SweepExportExcelService sweepExcelService;
    private final SweepExportPdfService sweepPdfService;

    public ExportController(
            ProjectService projectService,
            AnalysisService analysisService,
            AnalysisSweepService analysisSweepService,
            RequirementsExportCsvService requirementsCsvService,
            RequirementsExportExcelService requirementsExcelService,
            RequirementsExportPdfService requirementsPdfService,
            SweepExportCsvService sweepCsvService,
            SweepExportExcelService sweepExcelService,
            SweepExportPdfService sweepPdfService) {
        this.projectService = projectService;
        this.analysisService = analysisService;
        this.analysisSweepService = analysisSweepService;
        this.requirementsCsvService = requirementsCsvService;
        this.requirementsExcelService = requirementsExcelService;
        this.requirementsPdfService = requirementsPdfService;
        this.sweepCsvService = sweepCsvService;
        this.sweepExcelService = sweepExcelService;
        this.sweepPdfService = sweepPdfService;
    }

    @GetMapping("/requirements")
    public ResponseEntity<byte[]> exportRequirements(
            @RequestParam(required = false) String project, @RequestParam(defaultValue = "csv") String format) {
        var projectId = projectService.resolveProjectId(project);
        var data = analysisService.getRequirementsExportData(projectId);
        String baseName = sanitizeFilename(data.projectIdentifier() + "-requirements-" + LocalDate.now());

        return switch (format.toLowerCase(Locale.ROOT)) {
            case "xlsx" -> binaryResponse(requirementsExcelService.toExcel(data), XLSX_MEDIA_TYPE, baseName + ".xlsx");
            case "pdf" -> binaryResponse(
                    requirementsPdfService.toPdf(data), MediaType.APPLICATION_PDF, baseName + ".pdf");
            default -> textResponse(requirementsCsvService.toCsv(data), CSV_MEDIA_TYPE, baseName + ".csv");
        };
    }

    @PostMapping("/sweep")
    public ResponseEntity<byte[]> exportSweep(
            @RequestParam(required = false) String project, @RequestParam(defaultValue = "csv") String format) {
        var report = analysisSweepService.sweep(project);
        String baseName = sanitizeFilename(report.projectIdentifier() + "-sweep-" + LocalDate.now());

        return switch (format.toLowerCase(Locale.ROOT)) {
            case "xlsx" -> binaryResponse(sweepExcelService.toExcel(report), XLSX_MEDIA_TYPE, baseName + ".xlsx");
            case "pdf" -> binaryResponse(sweepPdfService.toPdf(report), MediaType.APPLICATION_PDF, baseName + ".pdf");
            default -> textResponse(sweepCsvService.toCsv(report), CSV_MEDIA_TYPE, baseName + ".csv");
        };
    }

    private ResponseEntity<byte[]> binaryResponse(byte[] content, MediaType mediaType, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(content);
    }

    private ResponseEntity<byte[]> textResponse(String content, MediaType mediaType, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
