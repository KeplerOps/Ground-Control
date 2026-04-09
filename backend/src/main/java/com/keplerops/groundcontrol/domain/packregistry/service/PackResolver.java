package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.packregistry.model.PackDependency;
import com.keplerops.groundcontrol.domain.packregistry.model.PackRegistryEntry;
import com.keplerops.groundcontrol.domain.packregistry.repository.PackRegistryEntryRepository;
import com.keplerops.groundcontrol.domain.packregistry.state.CatalogStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PackResolver {

    private static final Logger log = LoggerFactory.getLogger(PackResolver.class);
    private static final int MAX_DEPENDENCY_DEPTH = 10;
    private static final Pattern SEMVER_PATTERN = Pattern.compile(
            "^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z][0-9A-Za-z.-]*))?(?:\\+[0-9A-Za-z][0-9A-Za-z.]*)?$");

    private final PackRegistryEntryRepository registryRepository;
    private final String platformVersion;

    public PackResolver(
            PackRegistryEntryRepository registryRepository,
            @Value("${ground-control.version:0.109.0}") String platformVersion) {
        this.registryRepository = registryRepository;
        this.platformVersion = platformVersion;
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

        var resolutionPath = new HashSet<String>();
        resolutionPath.add(selected.getPackId());
        var resolvedDeps = resolveDependencies(projectId, selected, resolutionPath, 0);
        log.info(
                "pack_resolved: pack_id={}, version={}, dependencies={}",
                packId,
                selected.getVersion(),
                resolvedDeps.size());

        return new ResolvedPack(
                selected, selected.getVersion(), selected.getSourceUrl(), selected.getChecksum(), resolvedDeps);
    }

    public ResolvedPack resolveLatestCompatible(UUID projectId, String packId) {
        var availableVersions = registryRepository.findByProjectIdAndPackIdAndCatalogStatusOrderByRegisteredAtDesc(
                projectId, packId, CatalogStatus.AVAILABLE);

        if (availableVersions.isEmpty()) {
            throw new NotFoundException(String.format("No available versions found for pack '%s'", packId));
        }

        return availableVersions.stream()
                .sorted(Comparator.comparing((PackRegistryEntry entry) -> parseSemver(entry.getVersion()))
                        .reversed())
                .map(entry -> resolve(projectId, packId, entry.getVersion()))
                .filter(this::checkCompatibility)
                .findFirst()
                .orElseThrow(() ->
                        new NotFoundException(String.format("No compatible versions found for pack '%s'", packId)));
    }

    public boolean checkCompatibility(ResolvedPack resolvedPack) {
        if (!checkEntryCompatibility(resolvedPack.entry())) {
            return false;
        }

        if (resolvedPack.resolvedDependencies() == null) {
            return true;
        }
        for (var dependency : resolvedPack.resolvedDependencies()) {
            if (!checkCompatibility(dependency)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkEntryCompatibility(PackRegistryEntry entry) {
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
        for (PackDependency dep : deps) {
            var depPackId = dep.packId();
            var depConstraint = dep.versionConstraint();

            if (depPackId == null || depPackId.isBlank()) {
                throw new DomainValidationException(
                        "Invalid dependency: missing packId in pack '" + entry.getPackId() + "'");
            }

            var addedToPath = visited.add(depPackId);
            if (!addedToPath) {
                throw new DomainValidationException(
                        "Circular dependency detected: pack '" + depPackId + "' already in resolution chain");
            }

            try {
                var depVersions = registryRepository.findByProjectIdAndPackIdAndCatalogStatusOrderByRegisteredAtDesc(
                        projectId, depPackId, CatalogStatus.AVAILABLE);
                if (depVersions.isEmpty()) {
                    throw new NotFoundException(String.format(
                            "Dependency pack '%s' not found for pack '%s'", depPackId, entry.getPackId()));
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
            } finally {
                visited.remove(depPackId);
            }
        }
        return resolved;
    }

    PackRegistryEntry selectLatest(List<PackRegistryEntry> versions) {
        return versions.stream()
                .max(Comparator.comparing(e -> parseSemver(e.getVersion())))
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
                .max(Comparator.comparing(e -> parseSemver(e.getVersion())))
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
            return v.major() == t.major() && v.compareTo(t) >= 0;
        } else if (constraint.startsWith("~")) {
            // Tilde: same major.minor, >= specified
            var target = constraint.substring(1).trim();
            var v = parseSemver(version);
            var t = parseSemver(target);
            return v.major() == t.major() && v.minor() == t.minor() && v.compareTo(t) >= 0;
        }
        return version.equals(constraint);
    }

    static SemanticVersion parseSemver(String version) {
        var matcher = SEMVER_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new DomainValidationException("Invalid semantic version: '" + version + "'");
        }

        return new SemanticVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)),
                matcher.group(4) == null ? List.of() : List.of(matcher.group(4).split("\\.")));
    }

    static int compareSemver(String a, String b) {
        return parseSemver(a).compareTo(parseSemver(b));
    }

    private String getPlatformVersion() {
        return platformVersion;
    }

    record SemanticVersion(int major, int minor, int patch, List<String> prerelease)
            implements Comparable<SemanticVersion> {

        @Override
        public int compareTo(SemanticVersion other) {
            int majorComparison = Integer.compare(major, other.major);
            if (majorComparison != 0) {
                return majorComparison;
            }

            int minorComparison = Integer.compare(minor, other.minor);
            if (minorComparison != 0) {
                return minorComparison;
            }

            int patchComparison = Integer.compare(patch, other.patch);
            if (patchComparison != 0) {
                return patchComparison;
            }

            if (prerelease.isEmpty() && other.prerelease.isEmpty()) {
                return 0;
            }
            if (prerelease.isEmpty()) {
                return 1;
            }
            if (other.prerelease.isEmpty()) {
                return -1;
            }

            int maxIdentifiers = Math.min(prerelease.size(), other.prerelease.size());
            for (int i = 0; i < maxIdentifiers; i++) {
                int comparison = comparePrereleaseIdentifier(prerelease.get(i), other.prerelease.get(i));
                if (comparison != 0) {
                    return comparison;
                }
            }

            return Integer.compare(prerelease.size(), other.prerelease.size());
        }

        private int comparePrereleaseIdentifier(String left, String right) {
            boolean leftNumeric = left.chars().allMatch(Character::isDigit);
            boolean rightNumeric = right.chars().allMatch(Character::isDigit);

            if (leftNumeric && rightNumeric) {
                return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
            }
            if (leftNumeric) {
                return -1;
            }
            if (rightNumeric) {
                return 1;
            }
            return left.compareTo(right);
        }
    }
}
