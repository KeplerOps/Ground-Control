package com.keplerops.groundcontrol.domain.controlpacks.service;

import com.keplerops.groundcontrol.domain.controlpacks.model.ControlPack;
import com.keplerops.groundcontrol.domain.controlpacks.model.ControlPackEntry;
import com.keplerops.groundcontrol.domain.controlpacks.model.ControlPackOverride;
import com.keplerops.groundcontrol.domain.controlpacks.repository.ControlPackEntryRepository;
import com.keplerops.groundcontrol.domain.controlpacks.repository.ControlPackOverrideRepository;
import com.keplerops.groundcontrol.domain.controlpacks.repository.ControlPackRepository;
import com.keplerops.groundcontrol.domain.controlpacks.state.ControlPackEntryStatus;
import com.keplerops.groundcontrol.domain.controlpacks.state.ControlPackLifecycleState;
import com.keplerops.groundcontrol.domain.controls.model.Control;
import com.keplerops.groundcontrol.domain.controls.repository.ControlLinkRepository;
import com.keplerops.groundcontrol.domain.controls.repository.ControlRepository;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkType;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ControlPackService {

    private static final Logger log = LoggerFactory.getLogger(ControlPackService.class);

    private static final String FIELD_TITLE = "title";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_OBJECTIVE = "objective";
    private static final String FIELD_CONTROL_FUNCTION = "controlFunction";
    private static final String FIELD_OWNER = "owner";
    private static final String FIELD_IMPLEMENTATION_SCOPE = "implementationScope";
    private static final String FIELD_CATEGORY = "category";
    private static final String DETAIL_KEY_FIELD = "field";

    private static final Set<String> OVERRIDABLE_FIELDS = Set.of(
            FIELD_TITLE,
            FIELD_DESCRIPTION,
            FIELD_OBJECTIVE,
            FIELD_CONTROL_FUNCTION,
            FIELD_OWNER,
            FIELD_IMPLEMENTATION_SCOPE,
            FIELD_CATEGORY);

    private final ControlPackRepository controlPackRepository;
    private final ControlPackEntryRepository entryRepository;
    private final ControlPackOverrideRepository overrideRepository;
    private final ControlRepository controlRepository;
    private final ControlLinkRepository controlLinkRepository;
    private final ProjectService projectService;

    public ControlPackService(
            ControlPackRepository controlPackRepository,
            ControlPackEntryRepository entryRepository,
            ControlPackOverrideRepository overrideRepository,
            ControlRepository controlRepository,
            ControlLinkRepository controlLinkRepository,
            ProjectService projectService) {
        this.controlPackRepository = controlPackRepository;
        this.entryRepository = entryRepository;
        this.overrideRepository = overrideRepository;
        this.controlRepository = controlRepository;
        this.controlLinkRepository = controlLinkRepository;
        this.projectService = projectService;
    }

    public ControlPackInstallResult install(InstallControlPackCommand command) {
        var project = projectService.getById(command.projectId());

        var existing = controlPackRepository.findByProjectIdAndPackId(project.getId(), command.packId());
        if (existing.isPresent()) {
            var pack = existing.get();
            if (pack.getVersion().equals(command.version())) {
                var counters = reconcileEntries(pack, command.entries());
                log.info("control_pack_install_idempotent: packId={} version={}", command.packId(), command.version());
                return new ControlPackInstallResult(
                        pack,
                        counters.controlsCreated,
                        counters.controlsLinked,
                        counters.entriesCreated,
                        counters.mappingsCreated,
                        counters.mappingsSkipped,
                        true);
            }
            throw new ConflictException("Pack '" + command.packId() + "' is already installed at version "
                    + pack.getVersion() + ". Use upgrade to change versions.");
        }

        var pack = new ControlPack(project, command.packId(), command.version());
        pack.setPublisher(command.publisher());
        pack.setDescription(command.description());
        pack.setSourceUrl(command.sourceUrl());
        pack.setChecksum(command.checksum());
        pack.setCompatibility(command.compatibility());
        pack.setPackMetadata(command.packMetadata());
        pack = controlPackRepository.save(pack);

        var counters = new InstallCounters();
        for (var entryDef : command.entries()) {
            materializeEntry(project.getId(), pack, entryDef, counters);
        }

        log.info(
                "control_pack_installed: packId={} version={} controlsCreated={} entriesCreated={}",
                command.packId(),
                command.version(),
                counters.controlsCreated,
                counters.entriesCreated);

        return new ControlPackInstallResult(
                pack,
                counters.controlsCreated,
                counters.controlsLinked,
                counters.entriesCreated,
                counters.mappingsCreated,
                counters.mappingsSkipped,
                false);
    }

    public ControlPackUpgradeResult upgrade(UpgradeControlPackCommand command) {
        var project = projectService.getById(command.projectId());

        var pack = controlPackRepository
                .findByProjectIdAndPackId(project.getId(), command.packId())
                .orElseThrow(() -> new NotFoundException("Pack not installed: " + command.packId()));

        if (pack.getLifecycleState() == ControlPackLifecycleState.REMOVED) {
            throw new DomainValidationException(
                    "Cannot upgrade a removed pack. Reinstall instead.",
                    "pack_removed",
                    Map.of("packId", command.packId()));
        }

        var previousVersion = pack.getVersion();
        if (previousVersion.equals(command.newVersion())) {
            throw new ConflictException("Pack is already at version " + command.newVersion());
        }

        var existingEntries = entryRepository.findByControlPackId(pack.getId());
        var existingEntryMap =
                existingEntries.stream().collect(Collectors.toMap(ControlPackEntry::getEntryUid, Function.identity()));

        var processedUids = new java.util.HashSet<String>();
        var counters = new UpgradeCounters();

        for (var entryDef : command.entries()) {
            processedUids.add(entryDef.uid());
            var existingEntry = existingEntryMap.get(entryDef.uid());

            if (existingEntry == null) {
                var installCounters = new InstallCounters();
                materializeEntry(project.getId(), pack, entryDef, installCounters);
                counters.entriesAdded++;
                counters.controlsCreated += installCounters.controlsCreated;
            } else {
                upgradeEntry(pack, existingEntry, entryDef, counters);
            }
        }

        for (var entry : existingEntries) {
            if (!processedUids.contains(entry.getEntryUid())
                    && entry.getEntryStatus() == ControlPackEntryStatus.ACTIVE) {
                entry.setEntryStatus(ControlPackEntryStatus.DEPRECATED);
                entryRepository.save(entry);
                counters.entriesDeprecated++;
            }
        }

        pack.setVersion(command.newVersion());
        pack.setPublisher(command.publisher());
        pack.setDescription(command.description());
        pack.setSourceUrl(command.sourceUrl());
        pack.setChecksum(command.checksum());
        pack.setCompatibility(command.compatibility());
        pack.setPackMetadata(command.packMetadata());
        pack.transitionLifecycleState(ControlPackLifecycleState.UPGRADED);
        controlPackRepository.save(pack);

        log.info(
                "control_pack_upgraded: packId={} from={} to={} added={} updated={} deprecated={}",
                command.packId(),
                previousVersion,
                command.newVersion(),
                counters.entriesAdded,
                counters.entriesUpdated,
                counters.entriesDeprecated);

        return new ControlPackUpgradeResult(
                pack,
                previousVersion,
                counters.entriesAdded,
                counters.entriesUpdated,
                counters.entriesDeprecated,
                counters.controlsCreated,
                counters.controlsUpdated,
                counters.overridesPreserved);
    }

    public ControlPack deprecate(UUID projectId, String packId) {
        var pack = findPackOrThrow(projectId, packId);
        pack.transitionLifecycleState(ControlPackLifecycleState.DEPRECATED);
        pack = controlPackRepository.save(pack);
        log.info("control_pack_deprecated: packId={}", pack.getPackId());
        return pack;
    }

    public void remove(UUID projectId, String packId) {
        var pack = findPackOrThrow(projectId, packId);
        pack.transitionLifecycleState(ControlPackLifecycleState.REMOVED);
        controlPackRepository.save(pack);
        log.info("control_pack_removed: packId={}", pack.getPackId());
    }

    @Transactional(readOnly = true)
    public ControlPack getByPackId(UUID projectId, String packId) {
        return findPackOrThrow(projectId, packId);
    }

    @Transactional(readOnly = true)
    public List<ControlPack> listByProject(UUID projectId) {
        return controlPackRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public List<ControlPackEntry> listEntries(UUID projectId, String packId) {
        var pack = findPackOrThrow(projectId, packId);
        return entryRepository.findByControlPackId(pack.getId());
    }

    @Transactional(readOnly = true)
    public ControlPackEntry getEntry(UUID projectId, String packId, String entryUid) {
        var pack = findPackOrThrow(projectId, packId);
        return findEntryOrThrow(pack.getId(), entryUid);
    }

    public ControlPackOverride createOverride(
            UUID projectId, String packId, String entryUid, CreateControlPackOverrideCommand command) {
        var pack = findPackOrThrow(projectId, packId);
        var entry = findEntryOrThrow(pack.getId(), entryUid);

        if (!OVERRIDABLE_FIELDS.contains(command.fieldName())) {
            throw new DomainValidationException(
                    "Cannot override field: " + command.fieldName(),
                    "invalid_override_field",
                    Map.of(DETAIL_KEY_FIELD, command.fieldName(), "allowed", String.join(", ", OVERRIDABLE_FIELDS)));
        }

        var existing = overrideRepository.findByControlPackEntryIdAndFieldName(entry.getId(), command.fieldName());
        ControlPackOverride override;
        if (existing.isPresent()) {
            override = existing.get();
            override.setOverrideValue(command.overrideValue());
            override.setReason(command.reason());
        } else {
            override = new ControlPackOverride(entry, command.fieldName(), command.overrideValue(), command.reason());
        }
        override = overrideRepository.save(override);

        var control = entry.getControl();
        applyFieldToControl(control, command.fieldName(), command.overrideValue());
        controlRepository.save(control);

        log.info(
                "control_pack_override_created: packId={} entryUid={} field={}",
                pack.getPackId(),
                entry.getEntryUid(),
                override.getFieldName());
        return override;
    }

    @Transactional(readOnly = true)
    public List<ControlPackOverride> listOverrides(UUID projectId, String packId, String entryUid) {
        var pack = findPackOrThrow(projectId, packId);
        var entry = findEntryOrThrow(pack.getId(), entryUid);
        return overrideRepository.findByControlPackEntryId(entry.getId());
    }

    public void deleteOverride(UUID projectId, String packId, String entryUid, UUID overrideId) {
        var pack = findPackOrThrow(projectId, packId);
        var entry = findEntryOrThrow(pack.getId(), entryUid);
        var override = overrideRepository
                .findById(overrideId)
                .filter(o -> o.getControlPackEntry().getId().equals(entry.getId()))
                .orElseThrow(() -> new NotFoundException("Override not found: " + overrideId));

        var originalDef = entry.getOriginalDefinition();
        var originalValue = originalDef != null ? originalDef.get(override.getFieldName()) : null;
        applyFieldToControl(
                entry.getControl(), override.getFieldName(), originalValue != null ? originalValue.toString() : null);
        controlRepository.save(entry.getControl());

        overrideRepository.delete(override);
        log.info(
                "control_pack_override_deleted: packId={} entryUid={} field={}",
                pack.getPackId(),
                entry.getEntryUid(),
                override.getFieldName());
    }

    // --- Private helpers ---

    private ControlPack findPackOrThrow(UUID projectId, String packId) {
        return controlPackRepository
                .findByProjectIdAndPackId(projectId, packId)
                .orElseThrow(() -> new NotFoundException("Control pack not found: " + packId));
    }

    private ControlPackEntry findEntryOrThrow(UUID controlPackId, String entryUid) {
        return entryRepository
                .findByControlPackIdAndEntryUid(controlPackId, entryUid)
                .orElseThrow(() -> new NotFoundException("Pack entry not found: " + entryUid));
    }

    private void materializeEntry(
            UUID projectId, ControlPack pack, ControlPackEntryDefinition entryDef, InstallCounters counters) {
        var provenanceSource = "pack:" + pack.getPackId() + ":" + pack.getVersion();

        if (entryRepository.existsByControlPackIdAndEntryUid(pack.getId(), entryDef.uid())) {
            return;
        }

        var existingControl = controlRepository.findByProjectIdAndUid(projectId, entryDef.uid());
        Control control;

        if (existingControl.isPresent()) {
            control = existingControl.get();
            if (control.getSource() == null || control.getSource().isBlank()) {
                control.setSource(provenanceSource);
                controlRepository.save(control);
            }
            counters.controlsLinked++;
        } else {
            control = createControlFromDefinition(pack, entryDef, provenanceSource);
            counters.controlsCreated++;
        }

        var entry = new ControlPackEntry(pack, control, entryDef.uid());
        entry.setOriginalDefinition(buildOriginalDefinitionSnapshot(entryDef));
        entry.setExpectedEvidence(entryDef.expectedEvidence());
        entry.setImplementationGuidance(entryDef.implementationGuidance());
        entry.setFrameworkMappings(entryDef.frameworkMappings());
        entryRepository.save(entry);
        counters.entriesCreated++;

        if (entryDef.frameworkMappings() != null) {
            for (var mapping : entryDef.frameworkMappings()) {
                materializeFrameworkMapping(control, mapping, counters);
            }
        }
    }

    private Control createControlFromDefinition(
            ControlPack pack, ControlPackEntryDefinition entryDef, String provenanceSource) {
        var project = pack.getProject();
        var control = new Control(project, entryDef.uid(), entryDef.title(), entryDef.controlFunction());
        control.setDescription(entryDef.description());
        control.setObjective(entryDef.objective());
        control.setOwner(entryDef.owner());
        control.setImplementationScope(entryDef.implementationScope());
        control.setMethodologyFactors(entryDef.methodologyFactors());
        control.setEffectiveness(entryDef.effectiveness());
        control.setCategory(entryDef.category());
        control.setSource(provenanceSource);
        control = controlRepository.save(control);
        log.info("control_created_from_pack: uid={} packId={}", control.getUid(), pack.getPackId());
        return control;
    }

    private void materializeFrameworkMapping(Control control, Map<String, Object> mapping, InstallCounters counters) {
        var targetIdentifier = buildMappingIdentifier(mapping);
        if (targetIdentifier == null) {
            return;
        }

        if (controlLinkRepository.existsByControlIdAndTargetTypeAndTargetIdentifierAndLinkType(
                control.getId(), ControlLinkTargetType.EXTERNAL, targetIdentifier, ControlLinkType.MAPS_TO)) {
            counters.mappingsSkipped++;
            return;
        }

        var link = new com.keplerops.groundcontrol.domain.controls.model.ControlLink(
                control, ControlLinkTargetType.EXTERNAL, null, targetIdentifier, ControlLinkType.MAPS_TO);
        var targetUrl = mapping.get("url");
        if (targetUrl != null) {
            link.setTargetUrl(targetUrl.toString());
        }
        var targetTitle = mapping.get(FIELD_TITLE);
        if (targetTitle != null) {
            link.setTargetTitle(targetTitle.toString());
        }
        controlLinkRepository.save(link);
        counters.mappingsCreated++;
    }

    private String buildMappingIdentifier(Map<String, Object> mapping) {
        var framework = mapping.get("framework");
        var identifier = mapping.get("identifier");
        if (framework != null && identifier != null) {
            return framework + ":" + identifier;
        }
        if (identifier != null) {
            return identifier.toString();
        }
        return null;
    }

    private void upgradeEntry(
            ControlPack pack,
            ControlPackEntry existingEntry,
            ControlPackEntryDefinition newEntryDef,
            UpgradeCounters counters) {
        var control = existingEntry.getControl();
        var newOriginalDef = buildOriginalDefinitionSnapshot(newEntryDef);
        var oldOriginalDef = existingEntry.getOriginalDefinition();

        var overrides = overrideRepository.findByControlPackEntryId(existingEntry.getId());
        var overriddenFields =
                overrides.stream().map(ControlPackOverride::getFieldName).collect(Collectors.toSet());

        boolean changed = false;
        for (var fieldName : OVERRIDABLE_FIELDS) {
            var oldValue = oldOriginalDef != null ? oldOriginalDef.get(fieldName) : null;
            var newValue = newOriginalDef.get(fieldName);

            if (!Objects.equals(oldValue, newValue)) {
                if (!overriddenFields.contains(fieldName)) {
                    applyFieldToControl(control, fieldName, newValue != null ? newValue.toString() : null);
                    changed = true;
                } else {
                    counters.overridesPreserved++;
                }
            }
        }

        if (changed) {
            control.setSource("pack:" + pack.getPackId() + ":" + pack.getVersion());
            controlRepository.save(control);
            counters.controlsUpdated++;
        }

        existingEntry.setOriginalDefinition(newOriginalDef);
        existingEntry.setExpectedEvidence(newEntryDef.expectedEvidence());
        existingEntry.setImplementationGuidance(newEntryDef.implementationGuidance());
        existingEntry.setFrameworkMappings(newEntryDef.frameworkMappings());
        if (existingEntry.getEntryStatus() == ControlPackEntryStatus.DEPRECATED) {
            existingEntry.setEntryStatus(ControlPackEntryStatus.ACTIVE);
        }
        entryRepository.save(existingEntry);
        counters.entriesUpdated++;
    }

    private InstallCounters reconcileEntries(ControlPack pack, List<ControlPackEntryDefinition> entries) {
        var counters = new InstallCounters();
        for (var entryDef : entries) {
            materializeEntry(pack.getProject().getId(), pack, entryDef, counters);
        }
        return counters;
    }

    private Map<String, Object> buildOriginalDefinitionSnapshot(ControlPackEntryDefinition entryDef) {
        var snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("uid", entryDef.uid());
        snapshot.put(FIELD_TITLE, entryDef.title());
        snapshot.put(FIELD_DESCRIPTION, entryDef.description());
        snapshot.put(FIELD_OBJECTIVE, entryDef.objective());
        if (entryDef.controlFunction() != null) {
            snapshot.put(FIELD_CONTROL_FUNCTION, entryDef.controlFunction().name());
        }
        snapshot.put(FIELD_OWNER, entryDef.owner());
        snapshot.put(FIELD_IMPLEMENTATION_SCOPE, entryDef.implementationScope());
        snapshot.put(FIELD_CATEGORY, entryDef.category());
        snapshot.put("source", entryDef.source());
        if (entryDef.methodologyFactors() != null) {
            snapshot.put("methodologyFactors", entryDef.methodologyFactors());
        }
        if (entryDef.effectiveness() != null) {
            snapshot.put("effectiveness", entryDef.effectiveness());
        }
        return snapshot;
    }

    private void applyFieldToControl(Control control, String fieldName, String value) {
        switch (fieldName) {
            case FIELD_TITLE -> {
                if (value == null || value.isBlank()) {
                    throw new DomainValidationException(
                            "Control title cannot be null or blank",
                            "invalid_override_value",
                            Map.of(DETAIL_KEY_FIELD, FIELD_TITLE));
                }
                control.setTitle(value);
            }
            case FIELD_DESCRIPTION -> control.setDescription(value);
            case FIELD_OBJECTIVE -> control.setObjective(value);
            case FIELD_CONTROL_FUNCTION -> {
                if (value != null) {
                    try {
                        control.setControlFunction(
                                com.keplerops.groundcontrol.domain.controls.state.ControlFunction.valueOf(value));
                    } catch (IllegalArgumentException e) {
                        throw new DomainValidationException(
                                "Invalid control function: " + value,
                                "invalid_override_value",
                                Map.of(DETAIL_KEY_FIELD, FIELD_CONTROL_FUNCTION, "value", value));
                    }
                }
            }
            case FIELD_OWNER -> control.setOwner(value);
            case FIELD_IMPLEMENTATION_SCOPE -> control.setImplementationScope(value);
            case FIELD_CATEGORY -> control.setCategory(value);
            default -> throw new DomainValidationException(
                    "Unknown control field: " + fieldName, "unknown_field", Map.of(DETAIL_KEY_FIELD, fieldName));
        }
    }

    private static class InstallCounters {
        int controlsCreated;
        int controlsLinked;
        int entriesCreated;
        int mappingsCreated;
        int mappingsSkipped;
    }

    private static class UpgradeCounters {
        int entriesAdded;
        int entriesUpdated;
        int entriesDeprecated;
        int controlsCreated;
        int controlsUpdated;
        int overridesPreserved;
    }
}
