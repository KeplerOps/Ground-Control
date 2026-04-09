package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.packregistry.model.PackRegistryEntry;
import com.keplerops.groundcontrol.domain.packregistry.repository.PackRegistryEntryRepository;
import com.keplerops.groundcontrol.domain.packregistry.state.CatalogStatus;
import com.keplerops.groundcontrol.domain.packregistry.state.PackType;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PackRegistryService {

    private static final Logger log = LoggerFactory.getLogger(PackRegistryService.class);

    private final PackRegistryEntryRepository registryRepository;
    private final ProjectService projectService;
    private final PackIntegrityVerifier packIntegrityVerifier;
    private final PackTypeHandlerRegistry packTypeHandlerRegistry;

    public PackRegistryService(
            PackRegistryEntryRepository registryRepository,
            ProjectService projectService,
            PackIntegrityVerifier packIntegrityVerifier,
            PackTypeHandlerRegistry packTypeHandlerRegistry) {
        this.registryRepository = registryRepository;
        this.projectService = projectService;
        this.packIntegrityVerifier = packIntegrityVerifier;
        this.packTypeHandlerRegistry = packTypeHandlerRegistry;
    }

    public PackRegistryEntry registerEntry(RegisterPackCommand command) {
        var project = projectService.getById(command.projectId());

        if (registryRepository.existsByProjectIdAndPackIdAndVersion(
                command.projectId(), command.packId(), command.version())) {
            throw new ConflictException(String.format(
                    "Pack '%s' version '%s' is already registered in this project",
                    command.packId(), command.version()));
        }

        var entry = new PackRegistryEntry(project, command.packId(), command.packType(), command.version());
        entry.setPublisher(command.publisher());
        entry.setDescription(command.description());
        entry.setSourceUrl(command.sourceUrl());
        entry.setChecksum(normalizeOptionalChecksum(command.checksum()));
        entry.setSignatureInfo(command.signatureInfo());
        entry.setCompatibility(command.compatibility());
        entry.setDependencies(command.dependencies());
        packTypeHandlerRegistry.get(command.packType()).applyRegistrationContent(entry, command.registrationContent());
        entry.setProvenance(command.provenance());
        entry.setRegistryMetadata(command.registryMetadata());
        applyIntegrityVerification(entry);

        var saved = registryRepository.save(entry);
        log.info(
                "pack_registry_entry_registered: pack_id={}, version={}, type={}",
                saved.getPackId(),
                saved.getVersion(),
                saved.getPackType());
        return saved;
    }

    public PackRegistryEntry updateEntry(UUID entryId, UpdatePackRegistryEntryCommand command) {
        var entry = registryRepository
                .findById(entryId)
                .orElseThrow(() -> new NotFoundException("Pack registry entry not found: " + entryId));

        if (command.publisher() != null) entry.setPublisher(command.publisher());
        if (command.description() != null) entry.setDescription(command.description());
        if (command.sourceUrl() != null) entry.setSourceUrl(command.sourceUrl());
        if (command.checksum() != null) entry.setChecksum(normalizeOptionalChecksum(command.checksum()));
        if (command.signatureInfo() != null) entry.setSignatureInfo(command.signatureInfo());
        if (command.compatibility() != null) entry.setCompatibility(command.compatibility());
        if (command.dependencies() != null) entry.setDependencies(command.dependencies());
        if (command.registrationContent() != null) {
            packTypeHandlerRegistry
                    .get(entry.getPackType())
                    .applyRegistrationContent(entry, command.registrationContent());
        }
        if (command.provenance() != null) entry.setProvenance(command.provenance());
        if (command.registryMetadata() != null) entry.setRegistryMetadata(command.registryMetadata());
        applyIntegrityVerification(entry);

        var saved = registryRepository.save(entry);
        log.info("pack_registry_entry_updated: id={}, pack_id={}", entryId, entry.getPackId());
        return saved;
    }

    public PackRegistryEntry updateEntry(
            UUID projectId, String packId, String version, UpdatePackRegistryEntryCommand command) {
        var entry = findEntry(projectId, packId, version);
        return updateEntry(entry.getId(), command);
    }

    public PackRegistryEntry withdrawEntry(UUID projectId, String packId, String version) {
        var entry = findEntry(projectId, packId, version);
        entry.transitionCatalogStatus(CatalogStatus.WITHDRAWN);
        var saved = registryRepository.save(entry);
        log.info("pack_registry_entry_withdrawn: pack_id={}, version={}", entry.getPackId(), entry.getVersion());
        return saved;
    }

    @Transactional(readOnly = true)
    public PackRegistryEntry findEntry(UUID projectId, String packId, String version) {
        return registryRepository
                .findByProjectIdAndPackIdAndVersion(projectId, packId, version)
                .orElseThrow(() ->
                        new NotFoundException(String.format("Pack registry entry not found: %s@%s", packId, version)));
    }

    @Transactional(readOnly = true)
    public PackRegistryEntry getEntry(UUID entryId) {
        return registryRepository
                .findById(entryId)
                .orElseThrow(() -> new NotFoundException("Pack registry entry not found: " + entryId));
    }

    @Transactional(readOnly = true)
    public List<PackRegistryEntry> listEntries(UUID projectId) {
        return registryRepository.findByProjectIdOrderByRegisteredAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public List<PackRegistryEntry> listEntries(UUID projectId, PackType packType) {
        return registryRepository.findByProjectIdAndPackTypeOrderByRegisteredAtDesc(projectId, packType);
    }

    @Transactional(readOnly = true)
    public List<PackRegistryEntry> listVersions(UUID projectId, String packId) {
        return registryRepository.findByProjectIdAndPackIdOrderByRegisteredAtDesc(projectId, packId);
    }

    public void deleteEntry(UUID entryId) {
        if (!registryRepository.existsById(entryId)) {
            throw new NotFoundException("Pack registry entry not found: " + entryId);
        }
        registryRepository.deleteById(entryId);
        log.info("pack_registry_entry_deleted: id={}", entryId);
    }

    public void deleteEntry(UUID projectId, String packId, String version) {
        var entry = findEntry(projectId, packId, version);
        var entryPackId = entry.getPackId();
        var entryVersion = entry.getVersion();
        registryRepository.deleteById(entry.getId());
        log.info("pack_registry_entry_deleted: pack_id={}, version={}", entryPackId, entryVersion);
    }

    private void applyIntegrityVerification(PackRegistryEntry entry) {
        var verification = packIntegrityVerifier.verify(entry);
        if (entry.getChecksum() != null && !entry.getChecksum().isBlank()) {
            entry.setChecksum(verification.verifiedChecksum());
        }
    }

    private String normalizeOptionalChecksum(String checksum) {
        return checksum != null && checksum.isBlank() ? null : checksum;
    }
}
