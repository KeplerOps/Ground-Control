package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackEntryDefinition;
import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackService;
import com.keplerops.groundcontrol.domain.controlpacks.service.InstallControlPackCommand;
import com.keplerops.groundcontrol.domain.controlpacks.service.UpgradeControlPackCommand;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.packregistry.model.PackRegistryEntry;
import com.keplerops.groundcontrol.domain.packregistry.model.RegisteredControlPackEntry;
import com.keplerops.groundcontrol.domain.packregistry.state.PackType;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ControlPackTypeHandler implements PackTypeHandler {

    private final ControlPackService controlPackService;

    public ControlPackTypeHandler(ControlPackService controlPackService) {
        this.controlPackService = controlPackService;
    }

    @Override
    public PackType packType() {
        return PackType.CONTROL_PACK;
    }

    @Override
    public void applyRegistrationContent(PackRegistryEntry entry, PackRegistrationContent content) {
        var controlContent = requireControlPackContent(content, "registry");
        if (controlContent.entries().isEmpty()) {
            throw new DomainValidationException("CONTROL_PACK registry entries must include controlPackEntries");
        }
        entry.setControlPackEntries(toRegisteredControlPackEntries(controlContent.entries()));
    }

    @Override
    public PackOperationResult install(PackOperationContext context) {
        var entry = context.entry();
        var result = controlPackService.install(new InstallControlPackCommand(
                context.projectId(),
                entry.getPackId(),
                context.resolvedPack().resolvedVersion(),
                entry.getPublisher(),
                entry.getDescription(),
                entry.getSourceUrl(),
                context.integrityVerification().verifiedChecksum(),
                entry.getCompatibility(),
                entry.getRegistryMetadata(),
                toControlPackEntryDefinitions(requiredStoredEntries(entry, "installation"))));
        return new PackOperationResult(result.controlPack().getId());
    }

    @Override
    public PackOperationResult upgrade(PackOperationContext context) {
        var entry = context.entry();
        var result = controlPackService.upgrade(new UpgradeControlPackCommand(
                context.projectId(),
                entry.getPackId(),
                context.resolvedPack().resolvedVersion(),
                entry.getPublisher(),
                entry.getDescription(),
                entry.getSourceUrl(),
                context.integrityVerification().verifiedChecksum(),
                entry.getCompatibility(),
                entry.getRegistryMetadata(),
                toControlPackEntryDefinitions(requiredStoredEntries(entry, "upgrade"))));
        return new PackOperationResult(result.controlPack().getId());
    }

    private ControlPackRegistrationContent requireControlPackContent(
            PackRegistrationContent content, String operation) {
        if (content instanceof ControlPackRegistrationContent controlContent) {
            return controlContent;
        }
        throw new DomainValidationException("CONTROL_PACK " + operation + " content must include controlPackEntries");
    }

    private List<RegisteredControlPackEntry> requiredStoredEntries(PackRegistryEntry entry, String operation) {
        if (entry.getControlPackEntries() == null
                || entry.getControlPackEntries().isEmpty()) {
            throw new DomainValidationException("Resolved control pack is missing controlPackEntries for " + operation);
        }
        return entry.getControlPackEntries();
    }

    private List<RegisteredControlPackEntry> toRegisteredControlPackEntries(List<ControlPackEntryDefinition> entries) {
        return entries.stream()
                .map(entry -> new RegisteredControlPackEntry(
                        entry.uid(),
                        entry.title(),
                        entry.controlFunction(),
                        entry.description(),
                        entry.objective(),
                        entry.owner(),
                        entry.implementationScope(),
                        entry.methodologyFactors(),
                        entry.effectiveness(),
                        entry.category(),
                        entry.source(),
                        entry.implementationGuidance(),
                        entry.expectedEvidence(),
                        entry.frameworkMappings()))
                .toList();
    }

    private List<ControlPackEntryDefinition> toControlPackEntryDefinitions(
            List<RegisteredControlPackEntry> registeredEntries) {
        return registeredEntries.stream()
                .map(entry -> new ControlPackEntryDefinition(
                        entry.uid(),
                        entry.title(),
                        entry.description(),
                        entry.objective(),
                        entry.controlFunction(),
                        entry.owner(),
                        entry.implementationScope(),
                        entry.methodologyFactors(),
                        entry.effectiveness(),
                        entry.category(),
                        entry.source(),
                        entry.implementationGuidance(),
                        entry.expectedEvidence(),
                        entry.frameworkMappings()))
                .toList();
    }
}
