package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.assets.validation.AssetSubtypeValidator;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AssetSubtypeValidatorTest {

    private final AssetSubtypeValidator validator = new AssetSubtypeValidator();

    @Nested
    class UniversalBounds {

        @Test
        void nullMetadataIsAllowed() {
            assertThatCode(() -> validator.validateMetadataBounds(null)).doesNotThrowAnyException();
        }

        @Test
        void emptyMetadataIsAllowed() {
            assertThatCode(() -> validator.validateMetadataBounds(Map.of())).doesNotThrowAnyException();
        }

        @Test
        void rejectsTooManyKeys() {
            Map<String, Object> tooMany = new LinkedHashMap<>();
            for (int i = 0; i < AssetSubtypeValidator.MAX_METADATA_KEYS + 1; i++) {
                tooMany.put("k" + i, "v");
            }
            assertThatThrownBy(() -> validator.validateMetadataBounds(tooMany))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("metadata")
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("too_many_keys");
        }

        @Test
        void rejectsKeyExceedingMaxLength() {
            String oversize = "x".repeat(AssetSubtypeValidator.MAX_KEY_LENGTH + 1);
            Map<String, Object> data = Map.of(oversize, "v");
            assertThatThrownBy(() -> validator.validateMetadataBounds(data))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("key_too_long");
        }

        @Test
        void rejectsBlankKey() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("  ", "v");
            assertThatThrownBy(() -> validator.validateMetadataBounds(data))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("blank_key");
        }

        @Test
        void rejectsStringValueExceedingMaxLength() {
            String oversize = "v".repeat(AssetSubtypeValidator.MAX_STRING_VALUE_LENGTH + 1);
            Map<String, Object> data = Map.of("note", oversize);
            assertThatThrownBy(() -> validator.validateMetadataBounds(data))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("string_value_too_long");
        }

        @Test
        void rejectsUnsupportedValueShapes() {
            Map<String, Object> nested = Map.of("inner", Map.of("a", "b"));
            assertThatThrownBy(() -> validator.validateMetadataBounds(nested))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("unsupported_value_type");
        }

        @Test
        void acceptsScalarTypes() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("string", "v");
            data.put("integer", 42);
            data.put("long", 9000000000L);
            data.put("big_integer", new BigInteger("12345"));
            data.put("double", 3.14d);
            data.put("big_decimal", new BigDecimal("1.234"));
            data.put("boolean", Boolean.TRUE);
            data.put("null_value", null);

            assertThatCode(() -> validator.validateMetadataBounds(data)).doesNotThrowAnyException();
        }
    }

    @Nested
    class SchemaEnforcement {

        @Test
        void noSchemaMeansBoundsOnly() {
            Map<String, Object> metadata = Map.of("anything", "goes");
            assertThatCode(() -> validator.validateAgainstSchema(metadata, null))
                    .doesNotThrowAnyException();
        }

        @Test
        void emptyFieldsMapWithDefaultDenyForbidsUnknownKey() {
            Map<String, Object> schema = Map.of("fields", Map.of());
            Map<String, Object> metadata = Map.of("rogue", "v");
            assertThatThrownBy(() -> validator.validateAgainstSchema(metadata, schema))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("unknown_field");
        }

        @Test
        void allowAdditionalLetsExtraKeysPass() {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("fields", Map.of());
            schema.put("allowAdditional", Boolean.TRUE);

            Map<String, Object> metadata = Map.of("anything", "v");

            assertThatCode(() -> validator.validateAgainstSchema(metadata, schema))
                    .doesNotThrowAnyException();
        }

        @Test
        void requiredFieldMissingFails() {
            Map<String, Object> fields =
                    Map.of("cloud_account_id", Map.of("type", "STRING", "required", Boolean.TRUE, "maxLength", 200));
            Map<String, Object> schema = Map.of("fields", fields);

            Map<String, Object> metadata = Map.of();

            assertThatThrownBy(() -> validator.validateAgainstSchema(metadata, schema))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("required_field_missing");
        }

        @Test
        void stringTypeMismatchFails() {
            Map<String, Object> fields = Map.of("name", Map.of("type", "STRING", "maxLength", 50));
            Map<String, Object> schema = Map.of("fields", fields);

            Map<String, Object> metadata = Map.of("name", 42);

            assertThatThrownBy(() -> validator.validateAgainstSchema(metadata, schema))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("type_mismatch");
        }

        @Test
        void stringMaxLengthEnforced() {
            Map<String, Object> fields = Map.of("note", Map.of("type", "STRING", "maxLength", 5));
            Map<String, Object> schema = Map.of("fields", fields);

            Map<String, Object> metadata = Map.of("note", "abcdef");

            assertThatThrownBy(() -> validator.validateAgainstSchema(metadata, schema))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("string_too_long");
        }

        @Test
        void integerRangeEnforced() {
            Map<String, Object> fields = Map.of("count", Map.of("type", "INTEGER", "minimum", 0, "maximum", 10));
            Map<String, Object> schema = Map.of("fields", fields);

            assertThatThrownBy(() -> validator.validateAgainstSchema(Map.of("count", -1), schema))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("below_minimum");

            assertThatThrownBy(() -> validator.validateAgainstSchema(Map.of("count", 11), schema))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("above_maximum");

            assertThatCode(() -> validator.validateAgainstSchema(Map.of("count", 5), schema))
                    .doesNotThrowAnyException();
        }

        @Test
        void integerRejectsFractionalValue() {
            Map<String, Object> fields = Map.of("count", Map.of("type", "INTEGER"));
            Map<String, Object> schema = Map.of("fields", fields);

            assertThatThrownBy(() -> validator.validateAgainstSchema(Map.of("count", 3.14d), schema))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("type_mismatch");
        }

        @Test
        void numberAcceptsIntegerAndDecimal() {
            Map<String, Object> fields = Map.of("ratio", Map.of("type", "NUMBER"));
            Map<String, Object> schema = Map.of("fields", fields);

            assertThatCode(() -> validator.validateAgainstSchema(Map.of("ratio", 42), schema))
                    .doesNotThrowAnyException();
            assertThatCode(() -> validator.validateAgainstSchema(Map.of("ratio", 0.5d), schema))
                    .doesNotThrowAnyException();
        }

        @Test
        void enumValueMustMatch() {
            Map<String, Object> fields =
                    Map.of("tier", Map.of("type", "ENUM", "values", List.of("BRONZE", "SILVER", "GOLD")));
            Map<String, Object> schema = Map.of("fields", fields);

            assertThatCode(() -> validator.validateAgainstSchema(Map.of("tier", "SILVER"), schema))
                    .doesNotThrowAnyException();

            assertThatThrownBy(() -> validator.validateAgainstSchema(Map.of("tier", "platinum"), schema))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("enum_value_not_allowed");
        }

        @Test
        void booleanType() {
            Map<String, Object> fields = Map.of("encrypted", Map.of("type", "BOOLEAN", "required", Boolean.TRUE));
            Map<String, Object> schema = Map.of("fields", fields);

            assertThatCode(() -> validator.validateAgainstSchema(Map.of("encrypted", Boolean.TRUE), schema))
                    .doesNotThrowAnyException();

            assertThatThrownBy(() -> validator.validateAgainstSchema(Map.of("encrypted", "yes"), schema))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("type_mismatch");
        }

        @Test
        void invalidSchemaShapeRejectedWithStableCode() {
            Map<String, Object> schema = Map.of("fields", "not-a-map");

            assertThatThrownBy(() -> validator.validateAgainstSchema(Map.of("k", "v"), schema))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void detailReportsFieldPathAndExpectedType() {
            Map<String, Object> fields = Map.of("name", Map.of("type", "STRING", "maxLength", 50));
            Map<String, Object> schema = Map.of("fields", fields);

            try {
                validator.validateAgainstSchema(Map.of("name", 42), schema);
            } catch (DomainValidationException ex) {
                assertThat(ex.getDetail()).containsEntry("field", "name").containsEntry("expectedType", "STRING");
                assertThat(ex.getErrorCode()).isEqualTo("asset_metadata_invalid");
                return;
            }
            throw new AssertionError("Expected DomainValidationException to be thrown");
        }

        @Test
        void rejectsNonBooleanAllowAdditional() {
            // Codex pre-push review: wrong-typed keyword must reject, not coerce.
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("fields", Map.of());
            schema.put("allowAdditional", "yes");

            assertThatThrownBy(() -> validator.validateAgainstSchema(Map.of(), schema))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void rejectsNonBooleanRequired() {
            Map<String, Object> fields = Map.of("name", Map.of("type", "STRING", "required", "yes"));
            Map<String, Object> schema = Map.of("fields", fields);

            assertThatThrownBy(() -> validator.validateAgainstSchema(Map.of("name", "x"), schema))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void rejectsNonNumericMinimum() {
            Map<String, Object> fields = Map.of("count", Map.of("type", "INTEGER", "minimum", "zero"));
            Map<String, Object> schema = Map.of("fields", fields);

            assertThatThrownBy(() -> validator.validateAgainstSchema(Map.of("count", 1), schema))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void rejectsNegativeMaxLength() {
            Map<String, Object> fields = Map.of("name", Map.of("type", "STRING", "maxLength", -1));
            Map<String, Object> schema = Map.of("fields", fields);

            assertThatThrownBy(() -> validator.validateAgainstSchema(Map.of("name", "x"), schema))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void rejectsNonStringEnumValues() {
            Map<String, Object> fields = Map.of("tier", Map.of("type", "ENUM", "values", List.of("OK", 42)));
            Map<String, Object> schema = Map.of("fields", fields);

            assertThatThrownBy(() -> validator.validateAgainstSchema(Map.of("tier", "OK"), schema))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void rejectsEmptyEnumValues() {
            Map<String, Object> fields = Map.of("tier", Map.of("type", "ENUM", "values", List.of()));
            Map<String, Object> schema = Map.of("fields", fields);

            assertThatThrownBy(() -> validator.validateAgainstSchema(Map.of("tier", "x"), schema))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }
    }

    @Nested
    class SchemaBodyValidation {

        @Test
        void nullSchemaBodyPassesWhenFieldsNotRequired() {
            // Lenient path (e.g. update on a DEPRECATED row): a null body is
            // the "no schema" sentinel and must not throw.
            assertThatCode(() -> validator.validateSchemaBody(null, false)).doesNotThrowAnyException();
        }

        @Test
        void nullSchemaBodyRejectedWhenFieldsRequired() {
            // ACTIVE registry path: a body with no fields means no enforceable
            // contract; codex over-cap finding 3 on #722.
            assertThatThrownBy(() -> validator.validateSchemaBody(null, true))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("schema_body_required");
        }

        @Test
        void emptyFieldsRejectedWhenFieldsRequired() {
            Map<String, Object> body = Map.of("fields", Map.of());
            assertThatThrownBy(() -> validator.validateSchemaBody(body, true))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("schema_body_required");
        }

        @Test
        void rejectsMalformedFieldsKeyword() {
            Map<String, Object> body = Map.of("fields", "not-a-map");

            assertThatThrownBy(() -> validator.validateSchemaBody(body, false))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void rejectsUnsupportedFieldType() {
            Map<String, Object> body = Map.of("fields", Map.of("x", Map.of("type", "WIDGET")));

            assertThatThrownBy(() -> validator.validateSchemaBody(body, false))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void rejectsUnknownRootKeyword() {
            // Codex cycle-3 finding 2: typo'd / unknown root keyword (e.g.
            // `allow_additional` snake_case mis-spelling) must NOT be
            // silently ignored.
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("fields", Map.of("name", Map.of("type", "STRING")));
            body.put("allow_additional", true);
            assertThatThrownBy(() -> validator.validateSchemaBody(body, true))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void rejectsTypeInapplicableKeyword() {
            // `minimum` on a STRING field is meaningless; must reject so the
            // author's intent surfaces.
            Map<String, Object> body = Map.of("fields", Map.of("name", Map.of("type", "STRING", "minimum", 0)));
            assertThatThrownBy(() -> validator.validateSchemaBody(body, true))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void rejectsFieldNameExceedingMaxKeyLength() {
            String oversize = "x".repeat(AssetSubtypeValidator.MAX_KEY_LENGTH + 1);
            Map<String, Object> body = Map.of("fields", Map.of(oversize, Map.of("type", "STRING")));
            assertThatThrownBy(() -> validator.validateSchemaBody(body, true))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void rejectsMinimumGreaterThanMaximum() {
            Map<String, Object> body =
                    Map.of("fields", Map.of("n", Map.of("type", "INTEGER", "minimum", 10, "maximum", 5)));
            assertThatThrownBy(() -> validator.validateSchemaBody(body, true))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void rejectsRequiredCountExceedingUniversalKeyCap() {
            // Codex cycle-4 finding 2: a schema demanding more required fields
            // than the universal MAX_METADATA_KEYS is unsatisfiable.
            Map<String, Object> fields = new LinkedHashMap<>();
            for (int i = 0; i < AssetSubtypeValidator.MAX_METADATA_KEYS + 1; i++) {
                fields.put("f" + i, Map.of("type", "STRING", "required", true));
            }
            Map<String, Object> body = Map.of("fields", fields);
            assertThatThrownBy(() -> validator.validateSchemaBody(body, true))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void rejectsIntegerWithFractionalMinimum() {
            Map<String, Object> body =
                    Map.of("fields", Map.of("n", Map.of("type", "INTEGER", "minimum", 0.1, "maximum", 0.2)));
            assertThatThrownBy(() -> validator.validateSchemaBody(body, true))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void rejectsStringMaxLengthExceedingUniversalLimit() {
            Map<String, Object> body = Map.of(
                    "fields",
                    Map.of(
                            "n",
                            Map.of("type", "STRING", "maxLength", AssetSubtypeValidator.MAX_STRING_VALUE_LENGTH + 1)));
            assertThatThrownBy(() -> validator.validateSchemaBody(body, true))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void rejectsEnumValueExceedingUniversalStringLimit() {
            String huge = "v".repeat(AssetSubtypeValidator.MAX_STRING_VALUE_LENGTH + 1);
            Map<String, Object> body = Map.of("fields", Map.of("t", Map.of("type", "ENUM", "values", List.of(huge))));
            assertThatThrownBy(() -> validator.validateSchemaBody(body, true))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void rejectsPresentNullRequired() {
            // Codex cycle-5 finding 1: a present-null keyword must NOT be
            // silently treated as absent — that would let a malformed schema
            // weaken its own contract.
            Map<String, Object> def = new LinkedHashMap<>();
            def.put("type", "STRING");
            def.put("required", null);
            Map<String, Object> body = Map.of("fields", Map.of("name", def));

            assertThatThrownBy(() -> validator.validateSchemaBody(body, true))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void rejectsPresentNullAllowAdditional() {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("fields", Map.of("name", Map.of("type", "STRING")));
            body.put("allowAdditional", null);

            assertThatThrownBy(() -> validator.validateSchemaBody(body, true))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void rejectsPresentNullMaxLength() {
            Map<String, Object> def = new LinkedHashMap<>();
            def.put("type", "STRING");
            def.put("maxLength", null);
            Map<String, Object> body = Map.of("fields", Map.of("name", def));

            assertThatThrownBy(() -> validator.validateSchemaBody(body, true))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void rejectsPresentNullMinimum() {
            Map<String, Object> def = new LinkedHashMap<>();
            def.put("type", "INTEGER");
            def.put("minimum", null);
            Map<String, Object> body = Map.of("fields", Map.of("count", def));

            assertThatThrownBy(() -> validator.validateSchemaBody(body, true))
                    .isInstanceOf(DomainValidationException.class)
                    .extracting(e -> ((DomainValidationException) e).getDetail().get("reason"))
                    .isEqualTo("invalid_schema_shape");
        }

        @Test
        void acceptsWellFormedSchema() {
            Map<String, Object> body = new LinkedHashMap<>();
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("cloud_account_id", Map.of("type", "STRING", "required", true, "maxLength", 64));
            fields.put("tier", Map.of("type", "ENUM", "values", List.of("BRONZE", "SILVER", "GOLD")));
            fields.put("count", Map.of("type", "INTEGER", "minimum", 0, "maximum", 100));
            body.put("fields", fields);
            body.put("allowAdditional", false);

            assertThatCode(() -> validator.validateSchemaBody(body, true)).doesNotThrowAnyException();
        }
    }
}
