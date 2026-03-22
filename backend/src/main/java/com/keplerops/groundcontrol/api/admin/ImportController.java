package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.exception.GroundControlException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.service.ImportService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/import")
public class ImportController {

    private final ImportService importService;
    private final ProjectService projectService;

    public ImportController(ImportService importService, ProjectService projectService) {
        this.importService = importService;
        this.projectService = projectService;
    }

    @PostMapping(value = "/strictdoc", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResultResponse importStrictdoc(
            @RequestParam("file") MultipartFile file, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new GroundControlException("Failed to read uploaded file: " + e.getMessage(), "file_read_error", e);
        }
        var content = new String(bytes, StandardCharsets.UTF_8);
        var filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown.sdoc";
        return ImportResultResponse.from(importService.importStrictdoc(projectId, filename, content));
    }

    @PostMapping(value = "/reqif", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResultResponse importReqif(
            @RequestParam("file") MultipartFile file, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new GroundControlException("Failed to read uploaded file: " + e.getMessage(), "file_read_error", e);
        }
        var content = new String(bytes, StandardCharsets.UTF_8);
        var filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown.reqif";
        return ImportResultResponse.from(importService.importReqif(projectId, filename, content));
    }
}
