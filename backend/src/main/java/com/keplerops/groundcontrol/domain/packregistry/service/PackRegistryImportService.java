package com.keplerops.groundcontrol.domain.packregistry.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackEntryDefinition;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.packregistry.model.PackDependency;
import com.keplerops.groundcontrol.domain.packregistry.model.PackRegistryEntry;
import com.keplerops.groundcontrol.domain.packregistry.state.PackType;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PackRegistryImportService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS_TYPE = new TypeReference<>() {};
    private static final String FIELD_CATALOG = "catalog";
    private static final String FIELD_CONTROLS = "controls";
    private static final String FIELD_GROUPS = "groups";
    private static final String FIELD_PACK_ID = "packId";
    private static final String FIELD_PARTS = "parts";
    private static final String FIELD_TITLE = "title";

    private final ObjectMapper objectMapper;
    private final PackRegistryService packRegistryService;

    public PackRegistryImportService(ObjectMapper objectMapper, PackRegistryService packRegistryService) {
        this.objectMapper = objectMapper;
        this.packRegistryService = packRegistryService;
    }

    public PackRegistryEntry importEntry(
            UUID projectId, String filename, byte[] content, PackRegistryImportOptions options) {
        return packRegistryService.registerEntry(toRegisterCommand(projectId, filename, content, options));
    }

    public RegisterPackCommand toRegisterCommand(
            UUID projectId, String filename, byte[] content, PackRegistryImportOptions options) {
        var root = parseJson(content);
        var format = detectFormat(root, options.format());
        return switch (format) {
            case OSCAL_JSON -> toOscalRegisterCommand(projectId, filename, root, options);
            case GC_MANIFEST -> toManifestRegisterCommand(projectId, root, options);
            case AUTO -> throw new DomainValidationException("AUTO format must be resolved before conversion");
        };
    }

    private JsonNode parseJson(byte[] content) {
        try {
            return objectMapper.readTree(content);
        } catch (IOException e) {
            throw new DomainValidationException("Import file must be valid JSON: " + e.getMessage());
        }
    }

    private PackRegistryImportFormat detectFormat(JsonNode root, PackRegistryImportFormat requestedFormat) {
        if (requestedFormat != null && requestedFormat != PackRegistryImportFormat.AUTO) {
            return requestedFormat;
        }
        if (root.has(FIELD_CATALOG) && root.path(FIELD_CATALOG).isObject()) {
            return PackRegistryImportFormat.OSCAL_JSON;
        }
        if (root.has(FIELD_PACK_ID) && root.has("packType")) {
            return PackRegistryImportFormat.GC_MANIFEST;
        }
        throw new DomainValidationException(
                "Could not detect import format. Provide options.format as OSCAL_JSON or GC_MANIFEST.");
    }

    private RegisterPackCommand toManifestRegisterCommand(
            UUID projectId, JsonNode root, PackRegistryImportOptions options) {
        var packId = firstNonBlank(options.packId(), text(root, FIELD_PACK_ID));
        var packType = parsePackType(firstNonBlank(null, text(root, "packType")));
        var version = firstNonBlank(options.version(), text(root, "version"));
        var publisher = firstNonBlank(options.publisher(), text(root, "publisher"));
        var description = firstNonBlank(options.description(), text(root, "description"));
        var sourceUrl = firstNonBlank(options.sourceUrl(), text(root, "sourceUrl"));
        var checksum = firstNonBlank(options.checksum(), text(root, "checksum"));
        var signatureInfo = mergeMaps(asMap(root.get("signatureInfo")), options.signatureInfo());
        var compatibility = mergeMaps(asMap(root.get("compatibility")), options.compatibility());
        var dependencies =
                options.dependencies() != null ? options.dependencies() : parseDependencies(root.get("dependencies"));
        var provenance = mergeMaps(asMap(root.get("provenance")), options.provenance());
        var registryMetadata = mergeMaps(asMap(root.get("registryMetadata")), options.registryMetadata());

        if (packId == null) {
            throw new DomainValidationException("Imported manifest is missing packId");
        }
        if (version == null) {
            throw new DomainValidationException("Imported manifest is missing version");
        }

        return new RegisterPackCommand(
                projectId,
                packId,
                packType,
                version,
                publisher,
                description,
                sourceUrl,
                checksum,
                signatureInfo,
                compatibility,
                dependencies,
                parseRegistrationContent(packType, root.get("controlPackEntries"), options.defaultControlFunction()),
                provenance,
                registryMetadata);
    }

    private RegisterPackCommand toOscalRegisterCommand(
            UUID projectId, String filename, JsonNode root, PackRegistryImportOptions options) {
        var catalog = root.path(FIELD_CATALOG);
        if (!catalog.isObject()) {
            throw new DomainValidationException("OSCAL import requires a top-level catalog object");
        }

        var metadata = catalog.path("metadata");
        var frameworkTitle = firstNonBlank(text(metadata, FIELD_TITLE), "OSCAL Catalog");
        var version = firstNonBlank(options.version(), text(metadata, "version"));
        if (version == null) {
            throw new DomainValidationException("OSCAL catalog is missing metadata.version; provide an override.");
        }

        var packId = firstNonBlank(
                options.packId(), slugify(firstNonBlank(text(metadata, FIELD_TITLE), stripExtension(filename))));
        var publisher = firstNonBlank(options.publisher(), inferPublisher(metadata));
        var description = firstNonBlank(options.description(), frameworkTitle);
        var sourceUrl = firstNonBlank(options.sourceUrl(), inferSourceUrl(metadata));
        var entries = new ArrayList<ControlPackEntryDefinition>();

        if (catalog.has(FIELD_GROUPS)) {
            collectGroupControls(
                    catalog.path(FIELD_GROUPS), frameworkTitle, null, null, options.defaultControlFunction(), entries);
        }
        if (catalog.has(FIELD_CONTROLS)) {
            collectControls(
                    catalog.path(FIELD_CONTROLS),
                    frameworkTitle,
                    null,
                    null,
                    options.defaultControlFunction(),
                    entries);
        }
        if (entries.isEmpty()) {
            throw new DomainValidationException("OSCAL catalog does not contain any controls");
        }

        var generatedProvenance = new LinkedHashMap<String, Object>();
        generatedProvenance.put("sourceFormat", "OSCAL_JSON");
        generatedProvenance.put("inputFile", filename);
        putIfPresent(generatedProvenance, "catalogUuid", text(catalog, "uuid"));
        putIfPresent(generatedProvenance, "oscalVersion", text(metadata, "oscal-version"));
        putIfPresent(generatedProvenance, "importedFrameworkTitle", frameworkTitle);

        var generatedRegistryMetadata = new LinkedHashMap<String, Object>();
        generatedRegistryMetadata.put("sourceFormat", "OSCAL_JSON");
        generatedRegistryMetadata.put("importedControlCount", entries.size());

        return new RegisterPackCommand(
                projectId,
                packId,
                PackType.CONTROL_PACK,
                version,
                publisher,
                description,
                sourceUrl,
                options.checksum(),
                options.signatureInfo(),
                options.compatibility(),
                options.dependencies(),
                new ControlPackRegistrationContent(entries),
                mergeMaps(generatedProvenance, options.provenance()),
                mergeMaps(generatedRegistryMetadata, options.registryMetadata()));
    }

    private void collectGroupControls(
            JsonNode groups,
            String frameworkTitle,
            String inheritedFamilyId,
            String inheritedFamilyTitle,
            ControlFunction defaultControlFunction,
            List<ControlPackEntryDefinition> accumulator) {
        for (var group : iterable(groups)) {
            var familyId = firstNonBlank(text(group, "id"), inheritedFamilyId);
            var familyTitle = firstNonBlank(text(group, FIELD_TITLE), inheritedFamilyTitle);
            collectControls(
                    group.path(FIELD_CONTROLS),
                    frameworkTitle,
                    familyId,
                    familyTitle,
                    defaultControlFunction,
                    accumulator);
            collectGroupControls(
                    group.path(FIELD_GROUPS),
                    frameworkTitle,
                    familyId,
                    familyTitle,
                    defaultControlFunction,
                    accumulator);
        }
    }

    private void collectControls(
            JsonNode controls,
            String frameworkTitle,
            String familyId,
            String familyTitle,
            ControlFunction defaultControlFunction,
            List<ControlPackEntryDefinition> accumulator) {
        for (var control : iterable(controls)) {
            accumulator.add(toControlPackEntry(control, frameworkTitle, familyId, familyTitle, defaultControlFunction));
            collectControls(
                    control.path(FIELD_CONTROLS),
                    frameworkTitle,
                    familyId,
                    familyTitle,
                    defaultControlFunction,
                    accumulator);
        }
    }

    private ControlPackEntryDefinition toControlPackEntry(
            JsonNode control,
            String frameworkTitle,
            String familyId,
            String familyTitle,
            ControlFunction defaultControlFunction) {
        var label = findPropValue(control.path("props"), "label");
        var identifier = firstNonBlank(label, text(control, "id"));
        var uid = sanitizeUid(firstNonBlank(identifier, text(control, FIELD_TITLE), "UNNAMED-CONTROL"));
        var title = firstNonBlank(text(control, FIELD_TITLE), uid);

        return new ControlPackEntryDefinition(
                uid,
                title,
                joinPartText(control.path(FIELD_PARTS), Set.of("statement")),
                joinPartText(control.path(FIELD_PARTS), Set.of("objective", "assessment-objective")),
                defaultControlFunction,
                null,
                null,
                null,
                null,
                firstNonBlank(familyTitle, familyId),
                frameworkTitle,
                joinPartText(control.path(FIELD_PARTS), Set.of("guidance")),
                null,
                List.of(frameworkMapping(frameworkTitle, firstNonBlank(identifier, uid), title)));
    }

    private Map<String, Object> frameworkMapping(String framework, String identifier, String title) {
        var mapping = new LinkedHashMap<String, Object>();
        putIfPresent(mapping, "framework", framework);
        putIfPresent(mapping, "identifier", identifier);
        putIfPresent(mapping, FIELD_TITLE, title);
        return mapping;
    }

    private String joinPartText(JsonNode parts, Set<String> names) {
        var values = new LinkedHashSet<String>();
        collectPartText(parts, names, false, values);
        if (values.isEmpty()) {
            return null;
        }
        return String.join("\n\n", values);
    }

    private void collectPartText(JsonNode parts, Set<String> names, boolean inheritedMatch, Set<String> values) {
        for (var part : iterable(parts)) {
            var name = text(part, "name");
            var matches = inheritedMatch || (name != null && names.contains(name));
            var prose = text(part, "prose");
            if (matches && prose != null) {
                values.add(prose);
            }
            collectPartText(part.path(FIELD_PARTS), names, matches, values);
        }
    }

    private String inferPublisher(JsonNode metadata) {
        for (var party : iterable(metadata.path("parties"))) {
            if ("organization".equals(text(party, "type"))) {
                var name = text(party, "name");
                if (name != null) {
                    return name;
                }
            }
        }
        return null;
    }

    private String inferSourceUrl(JsonNode metadata) {
        for (var link : iterable(metadata.path("links"))) {
            var href = text(link, "href");
            if (href != null) {
                return href;
            }
        }
        return null;
    }

    private PackRegistrationContent parseRegistrationContent(
            PackType packType, JsonNode controlPackEntriesNode, ControlFunction defaultControlFunction) {
        if (packType != PackType.CONTROL_PACK) {
            return EmptyPackRegistrationContent.INSTANCE;
        }
        var entries = parseControlPackEntries(controlPackEntriesNode, defaultControlFunction);
        if (entries.isEmpty()) {
            return EmptyPackRegistrationContent.INSTANCE;
        }
        return new ControlPackRegistrationContent(entries);
    }

    private List<ControlPackEntryDefinition> parseControlPackEntries(
            JsonNode controlPackEntriesNode, ControlFunction defaultControlFunction) {
        if (controlPackEntriesNode == null
                || controlPackEntriesNode.isMissingNode()
                || controlPackEntriesNode.isNull()) {
            return List.of();
        }
        if (!controlPackEntriesNode.isArray()) {
            throw new DomainValidationException("controlPackEntries must be an array");
        }
        var entries = new ArrayList<ControlPackEntryDefinition>();
        for (var entry : controlPackEntriesNode) {
            var uid = text(entry, "uid");
            var title = text(entry, FIELD_TITLE);
            var controlFunction =
                    parseControlFunction(firstNonBlank(text(entry, "controlFunction"), null), defaultControlFunction);
            if (uid == null || title == null) {
                throw new DomainValidationException("Each controlPackEntries item must include uid and title");
            }
            entries.add(new ControlPackEntryDefinition(
                    uid,
                    title,
                    text(entry, "description"),
                    text(entry, "objective"),
                    controlFunction,
                    text(entry, "owner"),
                    text(entry, "implementationScope"),
                    asMap(entry.get("methodologyFactors")),
                    asMap(entry.get("effectiveness")),
                    text(entry, "category"),
                    text(entry, "source"),
                    text(entry, "implementationGuidance"),
                    asListOfMaps(entry.get("expectedEvidence")),
                    asListOfMaps(entry.get("frameworkMappings"))));
        }
        return entries;
    }

    private ControlFunction parseControlFunction(String raw, ControlFunction defaultControlFunction) {
        if (raw == null) {
            return defaultControlFunction;
        }
        try {
            return ControlFunction.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException("Unsupported control function: " + raw);
        }
    }

    private PackType parsePackType(String raw) {
        if (raw == null) {
            throw new DomainValidationException("Imported manifest is missing packType");
        }
        try {
            return PackType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException("Unsupported packType: " + raw);
        }
    }

    private List<PackDependency> parseDependencies(JsonNode dependenciesNode) {
        if (dependenciesNode == null || dependenciesNode.isMissingNode() || dependenciesNode.isNull()) {
            return null;
        }
        if (!dependenciesNode.isArray()) {
            throw new DomainValidationException("dependencies must be an array");
        }
        var dependencies = new ArrayList<PackDependency>();
        for (var dependency : dependenciesNode) {
            var packId = text(dependency, FIELD_PACK_ID);
            if (packId == null) {
                throw new DomainValidationException("Each dependency must include packId");
            }
            dependencies.add(new PackDependency(packId, text(dependency, "versionConstraint")));
        }
        return dependencies;
    }

    private Iterable<JsonNode> iterable(JsonNode node) {
        return node != null && node.isArray() ? node::elements : List.<JsonNode>of();
    }

    private Map<String, Object> asMap(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isObject()) {
            throw new DomainValidationException("Expected a JSON object");
        }
        return objectMapper.convertValue(node, MAP_TYPE);
    }

    private List<Map<String, Object>> asListOfMaps(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isArray()) {
            throw new DomainValidationException("Expected a JSON array");
        }
        return objectMapper.convertValue(node, LIST_OF_MAPS_TYPE);
    }

    private String findPropValue(JsonNode props, String name) {
        for (var prop : iterable(props)) {
            if (name.equals(text(prop, "name"))) {
                var value = text(prop, "value");
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        var value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            return value.toString();
        }
        return normalizeWhitespace(value.asText());
    }

    private String firstNonBlank(String... values) {
        for (var value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalizeWhitespace(String value) {
        if (value == null) {
            return null;
        }
        var normalized = trimTrailingHorizontalWhitespaceBeforeNewlines(value.replace("\r\n", "\n"))
                .trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String sanitizeUid(String value) {
        return normalizeWhitespace(value).replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private String stripExtension(String filename) {
        var dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private String slugify(String value) {
        var normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-");
        var trimmed = trimBoundaryDashes(normalized);
        return trimmed.isBlank() ? "imported-pack" : trimmed;
    }

    private Map<String, Object> mergeMaps(Map<String, Object> base, Map<String, Object> overrides) {
        if ((base == null || base.isEmpty()) && (overrides == null || overrides.isEmpty())) {
            return null;
        }
        var merged = new LinkedHashMap<String, Object>();
        if (base != null) {
            merged.putAll(base);
        }
        if (overrides != null) {
            merged.putAll(overrides);
        }
        return merged;
    }

    private void putIfPresent(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    private String trimTrailingHorizontalWhitespaceBeforeNewlines(String value) {
        var normalized = new StringBuilder(value.length());
        int index = 0;
        while (index < value.length()) {
            char current = value.charAt(index);
            if (!isHorizontalWhitespace(current)) {
                normalized.append(current);
                index++;
            } else {
                int whitespaceStart = index;
                while (index < value.length() && isHorizontalWhitespace(value.charAt(index))) {
                    index++;
                }
                if (index >= value.length() || value.charAt(index) != '\n') {
                    normalized.append(value, whitespaceStart, index);
                }
            }
        }
        return normalized.toString();
    }

    private String trimBoundaryDashes(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '-') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == '-') {
            end--;
        }
        return value.substring(start, end);
    }

    private boolean isHorizontalWhitespace(char value) {
        return value == ' ' || value == '\t';
    }
}
