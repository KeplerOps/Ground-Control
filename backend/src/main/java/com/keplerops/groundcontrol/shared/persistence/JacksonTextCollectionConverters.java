package com.keplerops.groundcontrol.shared.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JacksonTextCollectionConverters {

    private JacksonTextCollectionConverters() {}

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private abstract static class AbstractJsonTextConverter<T> implements AttributeConverter<T, String> {

        private final TypeReference<T> typeReference;

        protected AbstractJsonTextConverter(TypeReference<T> typeReference) {
            this.typeReference = typeReference;
        }

        @Override
        public String convertToDatabaseColumn(T attribute) {
            if (attribute == null) {
                return null;
            }
            try {
                return OBJECT_MAPPER.writeValueAsString(attribute);
            } catch (JsonProcessingException exception) {
                throw new IllegalArgumentException("Unable to serialize JSON column", exception);
            }
        }

        @Override
        public T convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) {
                return null;
            }
            try {
                return OBJECT_MAPPER.readValue(dbData, typeReference);
            } catch (IOException exception) {
                throw new IllegalArgumentException("Unable to deserialize JSON column", exception);
            }
        }
    }

    @Converter
    public static class StringListConverter extends AbstractJsonTextConverter<List<String>> {

        public StringListConverter() {
            super(new TypeReference<>() {});
        }
    }

    @Converter
    public static class StringObjectMapConverter extends AbstractJsonTextConverter<Map<String, Object>> {

        public StringObjectMapConverter() {
            super(new TypeReference<>() {});
        }
    }

    @Converter
    public static class MapListConverter extends AbstractJsonTextConverter<List<Map<String, Object>>> {

        public MapListConverter() {
            super(new TypeReference<>() {});
        }
    }

    @Converter
    public static class StringSetConverter extends AbstractJsonTextConverter<Set<String>> {

        public StringSetConverter() {
            super(new TypeReference<>() {});
        }
    }
}
