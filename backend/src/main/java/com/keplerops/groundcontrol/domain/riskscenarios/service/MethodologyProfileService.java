package com.keplerops.groundcontrol.domain.riskscenarios.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.riskscenarios.model.MethodologyProfile;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.MethodologyProfileRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.state.MethodologyFamily;
import com.keplerops.groundcontrol.domain.riskscenarios.state.MethodologyProfileStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MethodologyProfileService {

    private final MethodologyProfileRepository repository;
    private final ProjectService projectService;

    public MethodologyProfileService(MethodologyProfileRepository repository, ProjectService projectService) {
        this.repository = repository;
        this.projectService = projectService;
    }

    public MethodologyProfile create(CreateMethodologyProfileCommand command) {
        var project = projectService.getById(command.projectId());
        if (repository.existsByProjectIdAndProfileKeyAndVersion(
                project.getId(), command.profileKey(), command.version())) {
            throw new ConflictException(
                    "Methodology profile " + command.profileKey() + "@" + command.version() + " already exists");
        }
        var profile = new MethodologyProfile(
                project, command.profileKey(), command.name(), command.version(), command.family());
        applyUpdates(profile, command.description(), command.inputSchema(), command.outputSchema(), command.status());
        return repository.save(profile);
    }

    @Transactional(readOnly = true)
    public List<MethodologyProfile> listByProject(UUID projectId) {
        ensureSeeded(projectId);
        return repository.findByProjectIdOrderByNameAscVersionDesc(projectId);
    }

    @Transactional(readOnly = true)
    public MethodologyProfile getById(UUID projectId, UUID id) {
        return repository
                .findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new NotFoundException("Methodology profile not found: " + id));
    }

    public MethodologyProfile update(UUID projectId, UUID id, UpdateMethodologyProfileCommand command) {
        var profile = getById(projectId, id);
        if (command.name() != null) {
            profile.setName(command.name());
        }
        if (command.version() != null) {
            profile.setVersion(command.version());
        }
        if (command.family() != null) {
            profile.setFamily(command.family());
        }
        applyUpdates(profile, command.description(), command.inputSchema(), command.outputSchema(), command.status());
        return repository.save(profile);
    }

    public void delete(UUID projectId, UUID id) {
        repository.delete(getById(projectId, id));
    }

    public void ensureSeeded(UUID projectId) {
        var project = projectService.getById(projectId);
        seedIfMissing(project, "LEGACY_QUALITATIVE_V1", "Legacy Qualitative Profile", "1.0", MethodologyFamily.CUSTOM);
        seedIfMissing(project, "FAIR_V3_0", "FAIR Model", "3.0", MethodologyFamily.FAIR);
        seedIfMissing(project, "NIST_SP800_30_R1", "NIST SP 800-30 Rev. 1", "1.0", MethodologyFamily.NIST_SP800_30_R1);
        seedIfMissing(project, "ISO_27005_V2022", "ISO 27005", "2022", MethodologyFamily.ISO_27005);
    }

    private void seedIfMissing(
            com.keplerops.groundcontrol.domain.projects.model.Project project,
            String key,
            String name,
            String version,
            MethodologyFamily family) {
        if (repository.existsByProjectIdAndProfileKeyAndVersion(project.getId(), key, version)) {
            return;
        }
        var profile = new MethodologyProfile(project, key, name, version, family);
        profile.setDescription("Seeded methodology profile");
        profile.setInputSchema(Map.of("type", "object"));
        profile.setOutputSchema(Map.of("type", "object"));
        profile.setStatus(MethodologyProfileStatus.ACTIVE);
        repository.save(profile);
    }

    private void applyUpdates(
            MethodologyProfile profile,
            String description,
            Map<String, Object> inputSchema,
            Map<String, Object> outputSchema,
            MethodologyProfileStatus status) {
        if (description != null) {
            profile.setDescription(description);
        }
        if (inputSchema != null) {
            profile.setInputSchema(inputSchema);
        }
        if (outputSchema != null) {
            profile.setOutputSchema(outputSchema);
        }
        if (status != null) {
            profile.setStatus(status);
        }
    }
}
