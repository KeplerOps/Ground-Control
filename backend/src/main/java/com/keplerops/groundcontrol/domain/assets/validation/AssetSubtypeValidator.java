package com.keplerops.groundcontrol.domain.assets.validation;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Enforces GC-M011 bounds and (optional) per-subtype schemas on asset metadata.
 *
 * <p>Universal bounds always apply; a registered ACTIVE schema for the asset's
 * {@code (project, assetType, subtype)} triple layers structural validation on
 * top. The validator is the single component behind {@code AssetService} that
 * owns these checks — controllers, MCP handlers, and migrations do not
 * duplicate the rules.
 */
@Component
public class AssetSubtypeValidator {

    public static final int MAX_METADATA_KEYS = 50;
    public static final int MAX_KEY_LENGTH = 100;
    public static final int MAX_STRING_VALUE_LENGTH = 4096;

    private static final String ERROR_CODE = "asset_metadata_invalid";

    public void validateMetadataBounds(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return;
        }
        if (metadata.size() > MAX_METADATA_KEYS) {
            throw fail(
                    "Asset metadata exceeds maximum of " + MAX_METADATA_KEYS + " keys",
                    detail("reason", "too_many_keys", "limit", MAX_METADATA_KEYS, "actual", metadata.size()));
        }
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                throw fail("Asset metadata key must not be blank", detail("reason", "blank_key"));
            }
            if (key.length() > MAX_KEY_LENGTH) {
                throw fail(
                        "Asset metadata key '" + truncate(key) + "' exceeds " + MAX_KEY_LENGTH + " characters",
                        detail("reason", "key_too_long", "field", truncate(key), "limit", MAX_KEY_LENGTH));
            }
            validateScalarValue(key, entry.getValue());
        }
    }

    public void validateAgainstSchema(Map<String, Object> metadata, Map<String, Object> schemaBody) {
        validateMetadataBounds(metadata);
        if (schemaBody == null) {
            return;
        }
        Map<String, Object> fields = readFieldsMap(schemaBody);
        boolean allowAdditional = readBooleanKeyword(schemaBody, "allowAdditional", "<root>", false);

        Map<String, Object> effective = metadata == null ? Map.of() : metadata;
        for (Map.Entry<String, Object> e : effective.entrySet()) {
            if (!fields.containsKey(e.getKey()) && !allowAdditional) {
                throw fail(
                        "Asset metadata key '" + e.getKey() + "' is not declared by the subtype schema",
                        detail("reason", "unknown_field", "field", e.getKey()));
            }
        }
        for (Map.Entry<String, Object> fieldEntry : fields.entrySet()) {
            String name = fieldEntry.getKey();
            Map<String, Object> def = castStringObjectMap("fields." + name, fieldEntry.getValue());
            FieldType type = parseFieldType(name, def.get("type"));
            boolean required = readBooleanKeyword(def, "required", name, false);
            boolean present = effective.containsKey(name);
            Object value = effective.get(name);
            if (!present || value == null) {
                if (required) {
                    throw fail(
                            "Required asset metadata field '" + name + "' is missing",
                            detail("reason", "required_field_missing", "field", name, "expectedType", type.name()));
                }
                continue;
            }
            validateTyped(name, value, type, def);
        }
    }

    /**
     * Validate a schema body's shape *without* validating any metadata against
     * it. Used by the registry write path so a malformed schema cannot be
     * installed and only fail later at asset-write time. {@code requireFields}
     * controls whether a body with no {@code fields} object (or an empty one)
     * is acceptable: ACTIVE registry rows must declare at least one field —
     * otherwise the registry would advertise "schema layering" while
     * enforcing nothing.
     */
    public void validateSchemaBody(Map<String, Object> schemaBody, boolean requireFields) {
        if (schemaBody == null) {
            if (requireFields) {
                throw fail(
                        "Subtype schema body is required", detail("reason", "schema_body_required", "field", "<root>"));
            }
            return;
        }
        rejectUnknownKeywords("<root>", schemaBody, ROOT_KEYWORDS);
        Object rawFields = schemaBody.get("fields");
        if (requireFields && rawFields == null) {
            throw fail(
                    "Subtype schema body must declare a 'fields' object",
                    detail("reason", "schema_body_required", "field", "fields"));
        }
        Map<String, Object> fields = readFieldsMap(schemaBody);
        if (requireFields && fields.isEmpty()) {
            throw fail(
                    "Subtype schema 'fields' object must declare at least one field",
                    detail("reason", "schema_body_required", "field", "fields"));
        }
        readBooleanKeyword(schemaBody, "allowAdditional", "<root>", false);
        // Cap required-field count at the universal metadata-key limit; a
        // schema that demands more required fields than any payload can
        // legally carry is unsatisfiable by construction.
        int requiredCount = 0;
        for (Map.Entry<String, Object> fieldEntry : fields.entrySet()) {
            String name = fieldEntry.getKey();
            // Field names are persisted as metadata keys; enforce the same
            // bounds the metadata validator applies so a schema cannot
            // declare a field that no metadata payload could legally carry.
            if (name == null || name.isBlank()) {
                throw fail(
                        "Subtype schema field name must not be blank",
                        detail("reason", "invalid_schema_shape", "field", "fields"));
            }
            if (name.length() > MAX_KEY_LENGTH) {
                throw fail(
                        "Subtype schema field name '" + name + "' exceeds " + MAX_KEY_LENGTH + " characters",
                        detail("reason", "invalid_schema_shape", "field", name, "limit", MAX_KEY_LENGTH));
            }
            Map<String, Object> def = castStringObjectMap("fields." + name, fieldEntry.getValue());
            FieldType type = parseFieldType(name, def.get("type"));
            rejectUnknownKeywords(name, def, FIELD_KEYWORDS_BY_TYPE.get(type));
            boolean required = readBooleanKeyword(def, "required", name, false);
            if (required) {
                requiredCount++;
            }
            switch (type) {
                case STRING -> {
                    Integer maxLength = readIntBound(def, "maxLength", name);
                    // STRING values are also bounded by the universal
                    // MAX_STRING_VALUE_LENGTH; a maxLength larger than that
                    // can never reject a real overrun before bounds do, so
                    // it is functionally dead but allowed.
                    if (maxLength != null && maxLength > MAX_STRING_VALUE_LENGTH) {
                        throw fail(
                                "Subtype schema field '" + name + "' has maxLength " + maxLength
                                        + " exceeding universal limit " + MAX_STRING_VALUE_LENGTH,
                                detail(
                                        "reason",
                                        "invalid_schema_shape",
                                        "field",
                                        name,
                                        "keyword",
                                        "maxLength",
                                        "limit",
                                        MAX_STRING_VALUE_LENGTH));
                    }
                }
                case INTEGER -> validateIntegerBounds(name, def);
                case NUMBER -> {
                    java.math.BigDecimal min = readNumberKeyword(def, "minimum", name);
                    java.math.BigDecimal max = readNumberKeyword(def, "maximum", name);
                    if (min != null && max != null && min.compareTo(max) > 0) {
                        throw unsatisfiableRange(name, min, max);
                    }
                }
                case ENUM -> {
                    List<String> values = readEnumValues(def, name);
                    for (String v : values) {
                        if (v.length() > MAX_STRING_VALUE_LENGTH) {
                            throw fail(
                                    "Subtype schema field '" + name + "' ENUM value exceeds universal limit "
                                            + MAX_STRING_VALUE_LENGTH,
                                    detail(
                                            "reason",
                                            "invalid_schema_shape",
                                            "field",
                                            name,
                                            "keyword",
                                            "values",
                                            "limit",
                                            MAX_STRING_VALUE_LENGTH));
                        }
                    }
                }
                case BOOLEAN -> {
                    /* no bounds */
                }
                default -> throw new IllegalStateException("Unhandled field type: " + type);
            }
        }
        if (requiredCount > MAX_METADATA_KEYS) {
            throw fail(
                    "Subtype schema declares " + requiredCount + " required fields, exceeding the universal "
                            + "metadata-key limit of " + MAX_METADATA_KEYS,
                    detail(
                            "reason",
                            "invalid_schema_shape",
                            "field",
                            "fields",
                            "limit",
                            MAX_METADATA_KEYS,
                            "actual",
                            requiredCount));
        }
    }

    private void validateIntegerBounds(String name, Map<String, Object> def) {
        java.math.BigDecimal min = readNumberKeyword(def, "minimum", name);
        java.math.BigDecimal max = readNumberKeyword(def, "maximum", name);
        // INTEGER min/max must themselves be whole numbers — fractional
        // bounds are nonsensical for an integer field and a pair like
        // (0.1, 0.2) is unsatisfiable while passing a naive min<=max check.
        if (min != null && hasFractionalPart(min)) {
            throw fail(
                    "Subtype schema INTEGER field '" + name + "' has fractional minimum",
                    detail("reason", "invalid_schema_shape", "field", name, "keyword", "minimum"));
        }
        if (max != null && hasFractionalPart(max)) {
            throw fail(
                    "Subtype schema INTEGER field '" + name + "' has fractional maximum",
                    detail("reason", "invalid_schema_shape", "field", name, "keyword", "maximum"));
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw unsatisfiableRange(name, min, max);
        }
    }

    private DomainValidationException unsatisfiableRange(
            String name, java.math.BigDecimal min, java.math.BigDecimal max) {
        return fail(
                "Subtype schema field '" + name + "' has minimum > maximum (unsatisfiable)",
                detail(
                        "reason",
                        "invalid_schema_shape",
                        "field",
                        name,
                        "minimum",
                        min.toPlainString(),
                        "maximum",
                        max.toPlainString()));
    }

    private static boolean hasFractionalPart(java.math.BigDecimal value) {
        return value.scale() > 0 && value.stripTrailingZeros().scale() > 0;
    }

    private void rejectUnknownKeywords(String fieldName, Map<String, Object> map, java.util.Set<String> allowed) {
        if (allowed == null) {
            return;
        }
        for (String key : map.keySet()) {
            if (!allowed.contains(key)) {
                throw fail(
                        "Subtype schema field '" + fieldName + "' has unsupported keyword '" + key + "'",
                        detail("reason", "invalid_schema_shape", "field", fieldName, "keyword", key));
            }
        }
    }

    private static final java.util.Set<String> ROOT_KEYWORDS = java.util.Set.of("fields", "allowAdditional");

    private static final Map<FieldType, java.util.Set<String>> FIELD_KEYWORDS_BY_TYPE = Map.of(
            FieldType.STRING, java.util.Set.of("type", "required", "maxLength"),
            FieldType.INTEGER, java.util.Set.of("type", "required", "minimum", "maximum"),
            FieldType.NUMBER, java.util.Set.of("type", "required", "minimum", "maximum"),
            FieldType.BOOLEAN, java.util.Set.of("type", "required"),
            FieldType.ENUM, java.util.Set.of("type", "required", "values"));

    private Map<String, Object> readFieldsMap(Map<String, Object> schemaBody) {
        Object rawFields = schemaBody.get("fields");
        if (rawFields == null) {
            return Map.of();
        }
        if (!(rawFields instanceof Map<?, ?>)) {
            throw fail(
                    "Subtype schema 'fields' must be an object",
                    detail("reason", "invalid_schema_shape", "field", "fields"));
        }
        return castStringObjectMap("fields", rawFields);
    }

    private void validateScalarValue(String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String s) {
            if (s.length() > MAX_STRING_VALUE_LENGTH) {
                throw fail(
                        "Asset metadata value for '" + key + "' exceeds " + MAX_STRING_VALUE_LENGTH + " characters",
                        detail("reason", "string_value_too_long", "field", key, "limit", MAX_STRING_VALUE_LENGTH));
            }
            return;
        }
        if (value instanceof Boolean || value instanceof Number) {
            return;
        }
        throw fail(
                "Asset metadata value for '" + key + "' must be a string, number, boolean, or null",
                detail(
                        "reason",
                        "unsupported_value_type",
                        "field",
                        key,
                        "actualType",
                        value.getClass().getSimpleName()));
    }

    private void validateTyped(String name, Object value, FieldType type, Map<String, Object> def) {
        switch (type) {
            case STRING -> validateString(name, value, def);
            case INTEGER -> validateInteger(name, value, def);
            case NUMBER -> validateNumber(name, value, def);
            case BOOLEAN -> validateBoolean(name, value);
            case ENUM -> validateEnum(name, value, def);
            default -> throw new IllegalStateException("Unhandled field type: " + type);
        }
    }

    private void validateString(String name, Object value, Map<String, Object> def) {
        if (!(value instanceof String s)) {
            throw typeMismatch(name, value, FieldType.STRING);
        }
        Integer maxLength = readIntBound(def, "maxLength", name);
        if (maxLength != null && s.length() > maxLength) {
            throw fail(
                    "Asset metadata field '" + name + "' exceeds maxLength " + maxLength,
                    detail("reason", "string_too_long", "field", name, "limit", maxLength));
        }
    }

    private boolean readBooleanKeyword(Map<String, Object> def, String key, String fieldName, boolean defaultValue) {
        if (!def.containsKey(key)) {
            return defaultValue;
        }
        Object raw = def.get(key);
        // Distinguish "key absent" (default) from "key present with null value"
        // (malformed — reject). Without this guard a schema with `required: null`
        // or `allowAdditional: null` silently weakens its own contract
        // (codex cycle-5 finding 1).
        if (raw == null || !(raw instanceof Boolean b)) {
            throw fail(
                    "Subtype schema field '" + fieldName + "' has non-boolean '" + key + "'",
                    detail("reason", "invalid_schema_shape", "field", fieldName, "keyword", key));
        }
        return b;
    }

    private java.math.BigDecimal readNumberKeyword(Map<String, Object> def, String key, String fieldName) {
        if (!def.containsKey(key)) {
            return null;
        }
        Object raw = def.get(key);
        // Boolean is not a subclass of Number — pattern check alone excludes it.
        // Present-null is rejected (see readBooleanKeyword rationale).
        if (raw == null || !(raw instanceof Number n)) {
            throw fail(
                    "Subtype schema field '" + fieldName + "' has non-numeric '" + key + "'",
                    detail("reason", "invalid_schema_shape", "field", fieldName, "keyword", key));
        }
        return toBigDecimal(n);
    }

    private List<String> readEnumValues(Map<String, Object> def, String fieldName) {
        Object raw = def.get("values");
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            throw fail(
                    "Subtype schema ENUM field '" + fieldName + "' must declare a non-empty 'values' array",
                    detail("reason", "invalid_schema_shape", "field", fieldName, "keyword", "values"));
        }
        List<String> out = new java.util.ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String s)) {
                throw fail(
                        "Subtype schema ENUM field '" + fieldName + "' 'values' must be strings",
                        detail("reason", "invalid_schema_shape", "field", fieldName, "keyword", "values"));
            }
            out.add(s);
        }
        return out;
    }

    private void validateInteger(String name, Object value, Map<String, Object> def) {
        // Boolean is not a subclass of Number in Java, so the pattern check
        // alone rejects Boolean values — no explicit Boolean guard needed.
        if (!(value instanceof Number n)) {
            throw typeMismatch(name, value, FieldType.INTEGER);
        }
        BigDecimal decimal = toBigDecimal(n);
        if (decimal.scale() > 0 && decimal.stripTrailingZeros().scale() > 0) {
            throw typeMismatch(name, value, FieldType.INTEGER);
        }
        checkRange(name, decimal, def);
    }

    private void validateNumber(String name, Object value, Map<String, Object> def) {
        if (!(value instanceof Number n)) {
            throw typeMismatch(name, value, FieldType.NUMBER);
        }
        checkRange(name, toBigDecimal(n), def);
    }

    private void validateBoolean(String name, Object value) {
        if (!(value instanceof Boolean)) {
            throw typeMismatch(name, value, FieldType.BOOLEAN);
        }
    }

    private void validateEnum(String name, Object value, Map<String, Object> def) {
        List<String> allowed = readEnumValues(def, name);
        if (!(value instanceof String s)) {
            throw typeMismatch(name, value, FieldType.ENUM);
        }
        if (!allowed.contains(s)) {
            throw fail(
                    "Asset metadata field '" + name + "' must be one of the allowed enum values",
                    detail("reason", "enum_value_not_allowed", "field", name, "actual", s));
        }
    }

    private void checkRange(String name, BigDecimal value, Map<String, Object> def) {
        BigDecimal min = readNumberKeyword(def, "minimum", name);
        if (min != null && value.compareTo(min) < 0) {
            throw fail(
                    "Asset metadata field '" + name + "' is below minimum",
                    detail("reason", "below_minimum", "field", name, "minimum", min.toPlainString()));
        }
        BigDecimal max = readNumberKeyword(def, "maximum", name);
        if (max != null && value.compareTo(max) > 0) {
            throw fail(
                    "Asset metadata field '" + name + "' is above maximum",
                    detail("reason", "above_maximum", "field", name, "maximum", max.toPlainString()));
        }
    }

    private FieldType parseFieldType(String fieldName, Object raw) {
        if (!(raw instanceof String s)) {
            throw fail(
                    "Subtype schema field '" + fieldName + "' must declare a string 'type'",
                    detail("reason", "invalid_schema_shape", "field", fieldName));
        }
        try {
            return FieldType.valueOf(s);
        } catch (IllegalArgumentException ex) {
            throw fail(
                    "Subtype schema field '" + fieldName + "' has unsupported type '" + s + "'",
                    detail("reason", "invalid_schema_shape", "field", fieldName, "actualType", s));
        }
    }

    private Integer readIntBound(Map<String, Object> def, String key, String fieldName) {
        if (!def.containsKey(key)) {
            return null;
        }
        Object raw = def.get(key);
        // Boolean is not a subclass of Number — pattern check alone excludes it.
        // Present-null is rejected (see readBooleanKeyword rationale).
        if (raw == null || !(raw instanceof Number n)) {
            throw fail(
                    "Subtype schema field '" + fieldName + "' has non-numeric '" + key + "'",
                    detail("reason", "invalid_schema_shape", "field", fieldName, "keyword", key));
        }
        BigDecimal value = toBigDecimal(n);
        if (value.scale() > 0 && value.stripTrailingZeros().scale() > 0) {
            throw fail(
                    "Subtype schema field '" + fieldName + "' '" + key + "' must be an integer",
                    detail("reason", "invalid_schema_shape", "field", fieldName, "keyword", key));
        }
        try {
            int as = value.intValueExact();
            if (as < 0) {
                throw fail(
                        "Subtype schema field '" + fieldName + "' '" + key + "' must be non-negative",
                        detail("reason", "invalid_schema_shape", "field", fieldName, "keyword", key));
            }
            return as;
        } catch (ArithmeticException ex) {
            throw fail(
                    "Subtype schema field '" + fieldName + "' '" + key + "' is out of integer range",
                    detail("reason", "invalid_schema_shape", "field", fieldName, "keyword", key));
        }
    }

    private DomainValidationException typeMismatch(String name, Object value, FieldType expected) {
        return fail(
                "Asset metadata field '" + name + "' expected type " + expected,
                detail(
                        "reason",
                        "type_mismatch",
                        "field",
                        name,
                        "expectedType",
                        expected.name(),
                        "actualType",
                        value == null ? "null" : value.getClass().getSimpleName()));
    }

    private DomainValidationException fail(String message, Map<String, Serializable> detail) {
        return new DomainValidationException(message, ERROR_CODE, detail);
    }

    private static BigDecimal toBigDecimal(Number n) {
        if (n instanceof BigDecimal bd) {
            return bd;
        }
        if (n instanceof BigInteger bi) {
            return new BigDecimal(bi);
        }
        if (n instanceof Long l) {
            return BigDecimal.valueOf(l);
        }
        if (n instanceof Integer i) {
            return BigDecimal.valueOf(i);
        }
        return BigDecimal.valueOf(n.doubleValue());
    }

    private static Map<String, Object> castStringObjectMap(String path, Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            throw new DomainValidationException(
                    "Subtype schema '" + path + "' must be an object",
                    ERROR_CODE,
                    detail("reason", "invalid_schema_shape", "field", path));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!(e.getKey() instanceof String key)) {
                throw new DomainValidationException(
                        "Subtype schema '" + path + "' keys must be strings",
                        ERROR_CODE,
                        detail("reason", "invalid_schema_shape", "field", path));
            }
            out.put(key, e.getValue());
        }
        return out;
    }

    private static Map<String, Serializable> detail(Object... pairs) {
        Map<String, Serializable> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            String key = (String) pairs[i];
            Object value = pairs[i + 1];
            out.put(key, value instanceof Serializable s ? s : value == null ? null : value.toString());
        }
        return out;
    }

    private static String truncate(String s) {
        if (s.length() <= MAX_KEY_LENGTH) {
            return s;
        }
        return s.substring(0, MAX_KEY_LENGTH) + "…";
    }

    private enum FieldType {
        STRING,
        INTEGER,
        NUMBER,
        BOOLEAN,
        ENUM
    }
}
