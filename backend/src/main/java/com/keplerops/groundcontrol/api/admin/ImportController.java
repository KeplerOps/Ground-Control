package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
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

    /** Maximum import file size (5 MB). */
    private static final long MAX_IMPORT_SIZE = 5L * 1024 * 1024;

    public ImportController(ImportService importService, ProjectService projectService) {
        this.importService = importService;
        this.projectService = projectService;
    }

    @PostMapping(value = "/strictdoc", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResultResponse importStrictdoc(
            @RequestParam("file") MultipartFile file, @RequestParam(required = false) String project) {
        validateFile(file);
        var projectId = projectService.resolveProjectId(project);
        byte[] bytes = readFileBytes(file);
        var content = new String(bytes, StandardCharsets.UTF_8);
        var filename = sanitizeFilename(file.getOriginalFilename(), "unknown.sdoc");
        return ImportResultResponse.from(importService.importStrictdoc(projectId, filename, content));
    }

    @PostMapping(value = "/reqif", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResultResponse importReqif(
            @RequestParam("file") MultipartFile file, @RequestParam(required = false) String project) {
        validateFile(file);
        var projectId = projectService.resolveProjectId(project);
        byte[] bytes = readFileBytes(file);
        var content = new String(bytes, StandardCharsets.UTF_8);
        var filename = sanitizeFilename(file.getOriginalFilename(), "unknown.reqif");
        return ImportResultResponse.from(importService.importReqif(projectId, filename, content));
    }

    private static void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new DomainValidationException("Uploaded file is empty", "empty_file");
        }
        if (file.getSize() > MAX_IMPORT_SIZE) {
            throw new DomainValidationException("File exceeds maximum size of 5 MB", "file_too_large");
        }
    }

    private static byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new GroundControlException("Failed to read uploaded file", "file_read_error", e);
        }
    }

    /** Strip path separators from the original filename to prevent path traversal in audit logs. */
    private static String sanitizeFilename(String original, String fallback) {
        if (original == null || original.isBlank()) {
            return fallback;
        }
        // Strip any path components — keep only the base name
        String name = original.replace("\\", "/");
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        return name.isBlank() ? fallback : name;
    }
}
