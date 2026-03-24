package com.keplerops.groundcontrol.api.audit;

import com.keplerops.groundcontrol.api.requirements.TimelineEntryResponse;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.service.AuditExportService;
import com.keplerops.groundcontrol.domain.requirements.service.AuditService;
import com.keplerops.groundcontrol.domain.requirements.state.ChangeCategory;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private static final int MAX_EXPORT_LIMIT = 50_000;

    private final AuditService auditService;
    private final AuditExportService auditExportService;
    private final ProjectService projectService;

    public AuditController(
            AuditService auditService, AuditExportService auditExportService, ProjectService projectService) {
        this.auditService = auditService;
        this.auditExportService = auditExportService;
        this.projectService = projectService;
    }

    @GetMapping("/timeline")
    public List<TimelineEntryResponse> getProjectTimeline(
            @RequestParam(required = false) String project,
            @RequestParam(required = false) ChangeCategory changeCategory,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        var projectId = projectService.resolveProjectId(project);
        return auditService.getProjectTimeline(projectId, changeCategory, actor, from, to, limit, offset).stream()
                .map(TimelineEntryResponse::from)
                .toList();
    }

    @GetMapping("/timeline/export")
    public ResponseEntity<String> exportProjectTimeline(
            @RequestParam(required = false) String project,
            @RequestParam(required = false) ChangeCategory changeCategory,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "10000") int limit) {
        var projectId = projectService.resolveProjectId(project);
        var cappedLimit = Math.min(limit, MAX_EXPORT_LIMIT);
        var entries = auditService.getProjectTimeline(projectId, changeCategory, actor, from, to, cappedLimit, 0);
        var csv = auditExportService.toCsv(entries);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-timeline.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
