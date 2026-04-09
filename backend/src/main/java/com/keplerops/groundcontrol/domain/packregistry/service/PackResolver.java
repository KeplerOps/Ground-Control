package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.packregistry.model.PackRegistryEntry;
import com.keplerops.groundcontrol.domain.packregistry.repository.PackRegistryEntryRepository;
import com.keplerops.groundcontrol.domain.packregistry.state.CatalogStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PackResolver {

    private static final Logger log = LoggerFactory.getLogger(PackResolver.class);
    private static final int MAX_DEPENDENCY_DEPTH = 10;

    private final PackRegistryEntryRepository registryRepository;

    public PackResolver(PackRegistryEntryRepository registryRepository) {
        this.registryRepository = registryRepository;
    }

    public ResolvedPack resolve(UUID projectId, String packId, String versionConstraint) {
        var availableVersions = registryRepository.findByProjectIdAndPackIdAndCatalogStatusOrderByRegisteredAtDesc(
                projectId, packId, CatalogStatus.AVAILABLE);

        if (availableVersions.isEmpty()) {
            throw new NotFoundException(String.format("No available versions found for pack '%s'", packId));
        }

        PackRegistryEntry selected;
        if (versionConstraint == null || versionConstraint.isBlank()) {
            selected = selectLatest(availableVersions);
        } else {
            selected = selectByConstraint(availableVersions, versionConstraint);
        }

        if (selected == null) {
            throw new NotFoundException(String.format(
                    "No version matching constraint '%s' found for pack '%s'", versionConstraint, packId));
        }

        var resolvedDeps = resolveDependencies(projectId, selected, new HashSet<>(), 0);
        log.info(
                "pack_resolved: pack_id={}, version={}, dependencies={}",
                packId,
                selected.getVersion(),
                resolvedDeps.size());

        return new ResolvedPack(
                selected, selected.getVersion(), selected.getSourceUrl(), selected.getChecksum(), resolvedDeps);
    }

    public ResolvedPack resolveLatestCompatible(UUID projectId, String packId) {
        return resolve(projectId, packId, null);
    }

    public boolean checkCompatibility(ResolvedPack resolvedPack) {
        var entry = resolvedPack.entry();
        var compatibility = entry.getCompatibility();
        if (compatibility == null || compatibility.isEmpty()) {
            return true;
        }

        var minVersion = compatibility.get("minVersion");
        var maxVersion = compatibility.get("maxVersion");
        var platformVersion = getPlatformVersion();

        if (minVersion instanceof String min && !min.isBlank()) {
            if (compareSemver(platformVersion, min) < 0) {
                return false;
            }
        }
        if (maxVersion instanceof String max && !max.isBlank()) {
            if (compareSemver(platformVersion, max) > 0) {
                return false;
            }
        }
        return true;
    }

    List<ResolvedPack> resolveDependencies(UUID projectId, PackRegistryEntry entry, Set<String> visited, int depth) {
        var deps = entry.getDependencies();
        if (deps == null || deps.isEmpty()) {
            return List.of();
        }

        if (depth > MAX_DEPENDENCY_DEPTH) {
            throw new DomainValidationException("Dependency depth exceeds maximum of " + MAX_DEPENDENCY_DEPTH
                    + " for pack '" + entry.getPackId() + "'");
        }

        List<ResolvedPack> resolved = new ArrayList<>();
        for (var dep : deps) {
            var depPackId = (String) dep.get("packId");
            var depConstraint = (String) dep.get("versionConstraint");

            if (depPackId == null || depPackId.isBlank()) {
                throw new DomainValidationException(
                        "Invalid dependency: missing packId in pack '" + entry.getPackId() + "'");
            }

            if (!visited.add(depPackId)) {
                throw new DomainValidationException(
                        "Circular dependency detected: pack '" + depPackId + "' already in resolution chain");
            }

            var depVersions = registryRepository.findByProjectIdAndPackIdAndCatalogStatusOrderByRegisteredAtDesc(
                    projectId, depPackId, CatalogStatus.AVAILABLE);
            if (depVersions.isEmpty()) {
                throw new NotFoundException(
                        String.format("Dependency pack '%s' not found for pack '%s'", depPackId, entry.getPackId()));
            }

            PackRegistryEntry selected;
            if (depConstraint == null || depConstraint.isBlank()) {
                selected = selectLatest(depVersions);
            } else {
                selected = selectByConstraint(depVersions, depConstraint);
            }
            if (selected == null) {
                throw new NotFoundException(
                        String.format("No version matching '%s' for dependency '%s'", depConstraint, depPackId));
            }

            var nestedDeps = resolveDependencies(projectId, selected, visited, depth + 1);
            resolved.add(new ResolvedPack(
                    selected, selected.getVersion(), selected.getSourceUrl(), selected.getChecksum(), nestedDeps));
        }
        return resolved;
    }

    PackRegistryEntry selectLatest(List<PackRegistryEntry> versions) {
        return versions.stream()
                .max(Comparator.comparing(e -> parseSemver(e.getVersion()), PackResolver::compareSemverArrays))
                .orElse(null);
    }

    PackRegistryEntry selectByConstraint(List<PackRegistryEntry> versions, String constraint) {
        if (!constraint.startsWith(">=")
                && !constraint.startsWith("<=")
                && !constraint.startsWith("^")
                && !constraint.startsWith("~")) {
            // Exact match
            return versions.stream()
                    .filter(e -> e.getVersion().equals(constraint))
                    .findFirst()
                    .orElse(null);
        }

        return versions.stream()
                .filter(e -> matchesConstraint(e.getVersion(), constraint))
                .max(Comparator.comparing(e -> parseSemver(e.getVersion()), PackResolver::compareSemverArrays))
                .orElse(null);
    }

    static boolean matchesConstraint(String version, String constraint) {
        if (constraint.startsWith(">=")) {
            return compareSemver(version, constraint.substring(2).trim()) >= 0;
        } else if (constraint.startsWith("<=")) {
            return compareSemver(version, constraint.substring(2).trim()) <= 0;
        } else if (constraint.startsWith("^")) {
            // Caret: same major, >= specified
            var target = constraint.substring(1).trim();
            var v = parseSemver(version);
            var t = parseSemver(target);
            return v[0] == t[0] && compareSemver(version, target) >= 0;
        } else if (constraint.startsWith("~")) {
            // Tilde: same major.minor, >= specified
            var target = constraint.substring(1).trim();
            var v = parseSemver(version);
            var t = parseSemver(target);
            return v[0] == t[0] && v[1] == t[1] && compareSemver(version, target) >= 0;
        }
        return version.equals(constraint);
    }

    static int[] parseSemver(String version) {
        var parts = version.split("\\.");
        int major = parts.length > 0 ? parseIntSafe(parts[0]) : 0;
        int minor = parts.length > 1 ? parseIntSafe(parts[1]) : 0;
        int patch = parts.length > 2 ? parseIntSafe(parts[2]) : 0;
        return new int[] {major, minor, patch};
    }

    static int compareSemver(String a, String b) {
        return compareSemverArrays(parseSemver(a), parseSemver(b));
    }

    static int compareSemverArrays(int[] a, int[] b) {
        for (int i = 0; i < 3; i++) {
            int cmp = Integer.compare(a[i], b[i]);
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getPlatformVersion() {
        // Current platform version for compatibility checks
        return "0.1.0";
    }
}
