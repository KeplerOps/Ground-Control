package com.keplerops.groundcontrol.domain.assets.validation;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    // Detail-map keys (Sonar S1192).
    private static final String K_REASON = "reason";
    private static final String K_LIMIT = "limit";
    private static final String K_ACTUAL = "actual";
    private static final String K_FIELD = "field";
    private static final String K_KEYWORD = "keyword";
    private static final String K_ACTUAL_TYPE = "actualType";
    private static final String K_EXPECTED_TYPE = "expectedType";
    private static final String K_MINIMUM = "minimum";
    private static final String K_MAXIMUM = "maximum";

    // Detail-map reason values (Sonar S1192).
    private static final String R_INVALID_SCHEMA_SHAPE = "invalid_schema_shape";
    private static final String R_SCHEMA_BODY_REQUIRED = "schema_body_required";

    // Schema-language keywords.
    private static final String KW_FIELDS = "fields";
    private static final String KW_ALLOW_ADDITIONAL = "allowAdditional";
    private static final String KW_REQUIRED = "required";
    private static final String KW_MAX_LENGTH = "maxLength";
    private static final String KW_MINIMUM = "minimum";
    private static final String KW_MAXIMUM = "maximum";
    private static final String KW_VALUES = "values";
    private static final String KW_TYPE = "type";

    // Message-construction fragments.
    private static final String SCHEMA_FIELD_PREFIX = "Subtype schema field '";
    private static final String ASSET_FIELD_PREFIX = "Asset metadata field '";
    private static final String EXCEEDS = "' exceeds ";
    private static final String CHARACTERS = " characters";
    private static final String ROOT_PATH = "<root>";

    private static final Set<String> ROOT_KEYWORDS = Set.of(KW_FIELDS, KW_ALLOW_ADDITIONAL);

    private static final Map<FieldType, Set<String>> FIELD_KEYWORDS_BY_TYPE = Map.of(
            FieldType.STRING, Set.of(KW_TYPE, KW_REQUIRED, KW_MAX_LENGTH),
            FieldType.INTEGER, Set.of(KW_TYPE, KW_REQUIRED, KW_MINIMUM, KW_MAXIMUM),
            FieldType.NUMBER, Set.of(KW_TYPE, KW_REQUIRED, KW_MINIMUM, KW_MAXIMUM),
            FieldType.BOOLEAN, Set.of(KW_TYPE, KW_REQUIRED),
            FieldType.ENUM, Set.of(KW_TYPE, KW_REQUIRED, KW_VALUES));

    public void validateMetadataBounds(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return;
        }
        if (metadata.size() > MAX_METADATA_KEYS) {
            throw fail(
                    "Asset metadata exceeds maximum of " + MAX_METADATA_KEYS + " keys",
                    detail(K_REASON, "too_many_keys", K_LIMIT, MAX_METADATA_KEYS, K_ACTUAL, metadata.size()));
        }
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                throw fail("Asset metadata key must not be blank", detail(K_REASON, "blank_key"));
            }
            if (key.length() > MAX_KEY_LENGTH) {
                throw fail(
                        "Asset metadata key '" + truncate(key) + EXCEEDS + MAX_KEY_LENGTH + CHARACTERS,
                        detail(K_REASON, "key_too_long", K_FIELD, truncate(key), K_LIMIT, MAX_KEY_LENGTH));
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
        boolean allowAdditional = readBooleanKeyword(schemaBody, KW_ALLOW_ADDITIONAL, ROOT_PATH, false);

        Map<String, Object> effective = metadata == null ? Map.of() : metadata;
        rejectUndeclaredMetadataKeys(effective, fields, allowAdditional);
        for (Map.Entry<String, Object> fieldEntry : fields.entrySet()) {
            validateMetadataAgainstField(fieldEntry, effective);
        }
    }

    private void rejectUndeclaredMetadataKeys(
            Map<String, Object> metadata, Map<String, Object> fields, boolean allowAdditional) {
        if (allowAdditional) {
            return;
        }
        for (String key : metadata.keySet()) {
            if (!fields.containsKey(key)) {
                throw fail(
                        "Asset metadata key '" + key + "' is not declared by the subtype schema",
                        detail(K_REASON, "unknown_field", K_FIELD, key));
            }
        }
    }

    private void validateMetadataAgainstField(Map.Entry<String, Object> fieldEntry, Map<String, Object> metadata) {
        String name = fieldEntry.getKey();
        Map<String, Object> def = castStringObjectMap(KW_FIELDS + "." + name, fieldEntry.getValue());
        FieldType type = parseFieldType(name, def.get(KW_TYPE));
        boolean required = readBooleanKeyword(def, KW_REQUIRED, name, false);
        Object value = metadata.get(name);
        if (!metadata.containsKey(name) || value == null) {
            if (required) {
                throw fail(
                        "Required asset metadata field '" + name + "' is missing",
                        detail(K_REASON, "required_field_missing", K_FIELD, name, K_EXPECTED_TYPE, type.name()));
            }
            return;
        }
        validateTyped(name, value, type, def);
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
                        "Subtype schema body is required",
                        detail(K_REASON, R_SCHEMA_BODY_REQUIRED, K_FIELD, ROOT_PATH));
            }
            return;
        }
        rejectUnknownKeywords(ROOT_PATH, schemaBody, ROOT_KEYWORDS);
        Map<String, Object> fields = readRequiredFields(schemaBody, requireFields);
        readBooleanKeyword(schemaBody, KW_ALLOW_ADDITIONAL, ROOT_PATH, false);
        int requiredCount = walkAndValidateSchemaFields(fields);
        rejectImpossibleRequiredCount(requiredCount);
    }

    private Map<String, Object> readRequiredFields(Map<String, Object> schemaBody, boolean requireFields) {
        Object rawFields = schemaBody.get(KW_FIELDS);
        if (requireFields && rawFields == null) {
            throw fail(
                    "Subtype schema body must declare a 'fields' object",
                    detail(K_REASON, R_SCHEMA_BODY_REQUIRED, K_FIELD, KW_FIELDS));
        }
        Map<String, Object> fields = readFieldsMap(schemaBody);
        if (requireFields && fields.isEmpty()) {
            throw fail(
                    "Subtype schema 'fields' object must declare at least one field",
                    detail(K_REASON, R_SCHEMA_BODY_REQUIRED, K_FIELD, KW_FIELDS));
        }
        return fields;
    }

    private int walkAndValidateSchemaFields(Map<String, Object> fields) {
        int requiredCount = 0;
        for (Map.Entry<String, Object> fieldEntry : fields.entrySet()) {
            if (validateSchemaField(fieldEntry)) {
                requiredCount++;
            }
        }
        return requiredCount;
    }

    /** Returns true if the field is required, so the caller can tally. */
    private boolean validateSchemaField(Map.Entry<String, Object> fieldEntry) {
        String name = fieldEntry.getKey();
        validateSchemaFieldName(name);
        Map<String, Object> def = castStringObjectMap(KW_FIELDS + "." + name, fieldEntry.getValue());
        FieldType type = parseFieldType(name, def.get(KW_TYPE));
        rejectUnknownKeywords(name, def, FIELD_KEYWORDS_BY_TYPE.get(type));
        boolean required = readBooleanKeyword(def, KW_REQUIRED, name, false);
        validateFieldTypeBounds(name, def, type);
        return required;
    }

    private void validateSchemaFieldName(String name) {
        if (name == null || name.isBlank()) {
            throw fail(
                    "Subtype schema field name must not be blank",
                    detail(K_REASON, R_INVALID_SCHEMA_SHAPE, K_FIELD, KW_FIELDS));
        }
        if (name.length() > MAX_KEY_LENGTH) {
            throw fail(
                    "Subtype schema field name '" + name + EXCEEDS + MAX_KEY_LENGTH + CHARACTERS,
                    detail(K_REASON, R_INVALID_SCHEMA_SHAPE, K_FIELD, name, K_LIMIT, MAX_KEY_LENGTH));
        }
    }

    private void validateFieldTypeBounds(String name, Map<String, Object> def, FieldType type) {
        switch (type) {
            case STRING -> validateStringFieldBounds(name, def);
            case INTEGER -> validateIntegerBounds(name, def);
            case NUMBER -> validateNumericRange(name, def);
            case ENUM -> validateEnumBounds(name, def);
            case BOOLEAN -> {
                // No bounds for BOOLEAN.
            }
            default -> throw new IllegalStateException("Unhandled field type: " + type);
        }
    }

    private void validateStringFieldBounds(String name, Map<String, Object> def) {
        Integer maxLength = readIntBound(def, KW_MAX_LENGTH, name);
        // STRING values are also bounded by the universal MAX_STRING_VALUE_LENGTH;
        // a maxLength larger than that can never reject a real overrun, so reject
        // it as foot-gun-prone schema authoring.
        if (maxLength != null && maxLength > MAX_STRING_VALUE_LENGTH) {
            throw fail(
                    SCHEMA_FIELD_PREFIX + name + "' has maxLength " + maxLength + " exceeding universal limit "
                            + MAX_STRING_VALUE_LENGTH,
                    detail(
                            K_REASON, R_INVALID_SCHEMA_SHAPE,
                            K_FIELD, name,
                            K_KEYWORD, KW_MAX_LENGTH,
                            K_LIMIT, MAX_STRING_VALUE_LENGTH));
        }
    }

    private void validateNumericRange(String name, Map<String, Object> def) {
        BigDecimal min = readNumberKeyword(def, KW_MINIMUM, name);
        BigDecimal max = readNumberKeyword(def, KW_MAXIMUM, name);
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw unsatisfiableRange(name, min, max);
        }
    }

    private void validateEnumBounds(String name, Map<String, Object> def) {
        for (String v : readEnumValues(def, name)) {
            if (v.length() > MAX_STRING_VALUE_LENGTH) {
                throw fail(
                        SCHEMA_FIELD_PREFIX + name + "' ENUM value exceeds universal limit " + MAX_STRING_VALUE_LENGTH,
                        detail(
                                K_REASON, R_INVALID_SCHEMA_SHAPE,
                                K_FIELD, name,
                                K_KEYWORD, KW_VALUES,
                                K_LIMIT, MAX_STRING_VALUE_LENGTH));
            }
        }
    }

    private void rejectImpossibleRequiredCount(int requiredCount) {
        if (requiredCount > MAX_METADATA_KEYS) {
            throw fail(
                    "Subtype schema declares " + requiredCount + " required fields, exceeding the universal "
                            + "metadata-key limit of " + MAX_METADATA_KEYS,
                    detail(
                            K_REASON, R_INVALID_SCHEMA_SHAPE,
                            K_FIELD, KW_FIELDS,
                            K_LIMIT, MAX_METADATA_KEYS,
                            K_ACTUAL, requiredCount));
        }
    }

    private void validateIntegerBounds(String name, Map<String, Object> def) {
        BigDecimal min = readNumberKeyword(def, KW_MINIMUM, name);
        BigDecimal max = readNumberKeyword(def, KW_MAXIMUM, name);
        // INTEGER min/max must themselves be whole numbers — fractional bounds
        // are nonsensical for an integer field and a pair like (0.1, 0.2) is
        // unsatisfiable while passing a naive min<=max check.
        if (min != null && hasFractionalPart(min)) {
            throw fail(
                    "Subtype schema INTEGER field '" + name + "' has fractional minimum",
                    detail(K_REASON, R_INVALID_SCHEMA_SHAPE, K_FIELD, name, K_KEYWORD, KW_MINIMUM));
        }
        if (max != null && hasFractionalPart(max)) {
            throw fail(
                    "Subtype schema INTEGER field '" + name + "' has fractional maximum",
                    detail(K_REASON, R_INVALID_SCHEMA_SHAPE, K_FIELD, name, K_KEYWORD, KW_MAXIMUM));
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw unsatisfiableRange(name, min, max);
        }
    }

    private DomainValidationException unsatisfiableRange(String name, BigDecimal min, BigDecimal max) {
        return fail(
                SCHEMA_FIELD_PREFIX + name + "' has minimum > maximum (unsatisfiable)",
                detail(
                        K_REASON,
                        R_INVALID_SCHEMA_SHAPE,
                        K_FIELD,
                        name,
                        K_MINIMUM,
                        min.toPlainString(),
                        K_MAXIMUM,
                        max.toPlainString()));
    }

    private static boolean hasFractionalPart(BigDecimal value) {
        return value.scale() > 0 && value.stripTrailingZeros().scale() > 0;
    }

    private void rejectUnknownKeywords(String fieldName, Map<String, Object> map, Set<String> allowed) {
        if (allowed == null) {
            return;
        }
        for (String key : map.keySet()) {
            if (!allowed.contains(key)) {
                throw fail(
                        SCHEMA_FIELD_PREFIX + fieldName + "' has unsupported keyword '" + key + "'",
                        detail(K_REASON, R_INVALID_SCHEMA_SHAPE, K_FIELD, fieldName, K_KEYWORD, key));
            }
        }
    }

    private Map<String, Object> readFieldsMap(Map<String, Object> schemaBody) {
        Object rawFields = schemaBody.get(KW_FIELDS);
        if (rawFields == null) {
            return Map.of();
        }
        if (!(rawFields instanceof Map<?, ?>)) {
            throw fail(
                    "Subtype schema 'fields' must be an object",
                    detail(K_REASON, R_INVALID_SCHEMA_SHAPE, K_FIELD, KW_FIELDS));
        }
        return castStringObjectMap(KW_FIELDS, rawFields);
    }

    private void validateScalarValue(String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String s) {
            if (s.length() > MAX_STRING_VALUE_LENGTH) {
                throw fail(
                        "Asset metadata value for '" + key + EXCEEDS + MAX_STRING_VALUE_LENGTH + CHARACTERS,
                        detail(K_REASON, "string_value_too_long", K_FIELD, key, K_LIMIT, MAX_STRING_VALUE_LENGTH));
            }
            return;
        }
        if (value instanceof Boolean || value instanceof Number) {
            return;
        }
        throw fail(
                "Asset metadata value for '" + key + "' must be a string, number, boolean, or null",
                detail(
                        K_REASON,
                        "unsupported_value_type",
                        K_FIELD,
                        key,
                        K_ACTUAL_TYPE,
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
        Integer maxLength = readIntBound(def, KW_MAX_LENGTH, name);
        if (maxLength != null && s.length() > maxLength) {
            throw fail(
                    ASSET_FIELD_PREFIX + name + "' exceeds maxLength " + maxLength,
                    detail(K_REASON, "string_too_long", K_FIELD, name, K_LIMIT, maxLength));
        }
    }

    private boolean readBooleanKeyword(Map<String, Object> def, String key, String fieldName, boolean defaultValue) {
        if (!def.containsKey(key)) {
            return defaultValue;
        }
        Object raw = def.get(key);
        // Distinguish "key absent" (default) from "key present with null value"
        // (malformed — reject). Without this guard a schema with `required: null`
        // or `allowAdditional: null` silently weakens its own contract.
        if (!(raw instanceof Boolean b)) {
            throw fail(
                    SCHEMA_FIELD_PREFIX + fieldName + "' has non-boolean '" + key + "'",
                    detail(K_REASON, R_INVALID_SCHEMA_SHAPE, K_FIELD, fieldName, K_KEYWORD, key));
        }
        return b;
    }

    private BigDecimal readNumberKeyword(Map<String, Object> def, String key, String fieldName) {
        if (!def.containsKey(key)) {
            return null;
        }
        Object raw = def.get(key);
        // Boolean is not a subclass of Number — pattern check alone excludes it.
        // Present-null is rejected (see readBooleanKeyword rationale).
        if (!(raw instanceof Number n)) {
            throw fail(
                    SCHEMA_FIELD_PREFIX + fieldName + "' has non-numeric '" + key + "'",
                    detail(K_REASON, R_INVALID_SCHEMA_SHAPE, K_FIELD, fieldName, K_KEYWORD, key));
        }
        return toBigDecimal(n);
    }

    private List<String> readEnumValues(Map<String, Object> def, String fieldName) {
        Object raw = def.get(KW_VALUES);
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            throw fail(
                    "Subtype schema ENUM field '" + fieldName + "' must declare a non-empty 'values' array",
                    detail(K_REASON, R_INVALID_SCHEMA_SHAPE, K_FIELD, fieldName, K_KEYWORD, KW_VALUES));
        }
        List<String> out = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String s)) {
                throw fail(
                        "Subtype schema ENUM field '" + fieldName + "' 'values' must be strings",
                        detail(K_REASON, R_INVALID_SCHEMA_SHAPE, K_FIELD, fieldName, K_KEYWORD, KW_VALUES));
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
                    ASSET_FIELD_PREFIX + name + "' must be one of the allowed enum values",
                    detail(K_REASON, "enum_value_not_allowed", K_FIELD, name, K_ACTUAL, s));
        }
    }

    private void checkRange(String name, BigDecimal value, Map<String, Object> def) {
        BigDecimal min = readNumberKeyword(def, KW_MINIMUM, name);
        if (min != null && value.compareTo(min) < 0) {
            throw fail(
                    ASSET_FIELD_PREFIX + name + "' is below minimum",
                    detail(K_REASON, "below_minimum", K_FIELD, name, K_MINIMUM, min.toPlainString()));
        }
        BigDecimal max = readNumberKeyword(def, KW_MAXIMUM, name);
        if (max != null && value.compareTo(max) > 0) {
            throw fail(
                    ASSET_FIELD_PREFIX + name + "' is above maximum",
                    detail(K_REASON, "above_maximum", K_FIELD, name, K_MAXIMUM, max.toPlainString()));
        }
    }

    private FieldType parseFieldType(String fieldName, Object raw) {
        if (!(raw instanceof String s)) {
            throw fail(
                    SCHEMA_FIELD_PREFIX + fieldName + "' must declare a string 'type'",
                    detail(K_REASON, R_INVALID_SCHEMA_SHAPE, K_FIELD, fieldName));
        }
        try {
            return FieldType.valueOf(s);
        } catch (IllegalArgumentException ex) {
            throw fail(
                    SCHEMA_FIELD_PREFIX + fieldName + "' has unsupported type '" + s + "'",
                    detail(K_REASON, R_INVALID_SCHEMA_SHAPE, K_FIELD, fieldName, K_ACTUAL_TYPE, s));
        }
    }

    private Integer readIntBound(Map<String, Object> def, String key, String fieldName) {
        if (!def.containsKey(key)) {
            return null;
        }
        Object raw = def.get(key);
        // Boolean is not a subclass of Number — pattern check alone excludes it.
        // Present-null is rejected (see readBooleanKeyword rationale).
        if (!(raw instanceof Number n)) {
            throw fail(
                    SCHEMA_FIELD_PREFIX + fieldName + "' has non-numeric '" + key + "'",
                    detail(K_REASON, R_INVALID_SCHEMA_SHAPE, K_FIELD, fieldName, K_KEYWORD, key));
        }
        BigDecimal value = toBigDecimal(n);
        if (value.scale() > 0 && value.stripTrailingZeros().scale() > 0) {
            throw fail(
                    SCHEMA_FIELD_PREFIX + fieldName + "' '" + key + "' must be an integer",
                    detail(K_REASON, R_INVALID_SCHEMA_SHAPE, K_FIELD, fieldName, K_KEYWORD, key));
        }
        try {
            int as = value.intValueExact();
            if (as < 0) {
                throw fail(
                        SCHEMA_FIELD_PREFIX + fieldName + "' '" + key + "' must be non-negative",
                        detail(K_REASON, R_INVALID_SCHEMA_SHAPE, K_FIELD, fieldName, K_KEYWORD, key));
            }
            return as;
        } catch (ArithmeticException ex) {
            throw fail(
                    SCHEMA_FIELD_PREFIX + fieldName + "' '" + key + "' is out of integer range",
                    detail(K_REASON, R_INVALID_SCHEMA_SHAPE, K_FIELD, fieldName, K_KEYWORD, key));
        }
    }

    private DomainValidationException typeMismatch(String name, Object value, FieldType expected) {
        return fail(
                ASSET_FIELD_PREFIX + name + "' expected type " + expected,
                detail(
                        K_REASON,
                        "type_mismatch",
                        K_FIELD,
                        name,
                        K_EXPECTED_TYPE,
                        expected.name(),
                        K_ACTUAL_TYPE,
                        classNameOrNull(value)));
    }

    private static String classNameOrNull(Object value) {
        if (value == null) {
            return "null";
        }
        return value.getClass().getSimpleName();
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
                    detail(K_REASON, R_INVALID_SCHEMA_SHAPE, K_FIELD, path));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!(e.getKey() instanceof String key)) {
                throw new DomainValidationException(
                        "Subtype schema '" + path + "' keys must be strings",
                        ERROR_CODE,
                        detail(K_REASON, R_INVALID_SCHEMA_SHAPE, K_FIELD, path));
            }
            out.put(key, e.getValue());
        }
        return out;
    }

    private static Map<String, Serializable> detail(Object... pairs) {
        Map<String, Serializable> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            String key = (String) pairs[i];
            out.put(key, asSerializable(pairs[i + 1]));
        }
        return out;
    }

    private static Serializable asSerializable(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Serializable s) {
            return s;
        }
        return value.toString();
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
