package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.*;

import com.keplerops.groundcontrol.domain.requirements.model.RequirementImport;
import com.keplerops.groundcontrol.domain.requirements.state.ImportSourceType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S2187") // Tests are in @Nested inner classes
class RequirementImportTest {

    private static RequirementImport createImport() {
        return new RequirementImport(ImportSourceType.GITHUB);
    }

    @Nested
    class Defaults {

        @Test
        void sourceFileDefaultsToEmpty() {
            var imp = createImport();
            assertThat(imp.getSourceFile()).isEmpty();
        }

        @Test
        void statsDefaultsToEmptyMap() {
            var imp = createImport();
            assertThat(imp.getStats()).isEmpty();
        }

        @Test
        void errorsDefaultsToEmptyList() {
            var imp = createImport();
            assertThat(imp.getErrors()).isEmpty();
        }
    }

    @Nested
    class Construction {

        @Test
        void sourceTypeSetCorrectly() {
            var imp = new RequirementImport(ImportSourceType.STRICTDOC);
            assertThat(imp.getSourceType()).isEqualTo(ImportSourceType.STRICTDOC);
        }
    }

    @Nested
    class Accessors {

        @Test
        void sourceFileSetterWorks() {
            var imp = createImport();
            imp.setSourceFile("requirements.sdoc");
            assertThat(imp.getSourceFile()).isEqualTo("requirements.sdoc");
        }

        @Test
        void statsSetterWorks() {
            var imp = createImport();
            imp.setStats(Map.of("total", 42, "imported", 40));
            assertThat(imp.getStats()).containsEntry("total", 42).containsEntry("imported", 40);
        }

        @Test
        void errorsSetterWorks() {
            var imp = createImport();
            imp.setErrors(List.of(Map.of("line", 5, "message", "parse error")));
            assertThat(imp.getErrors()).hasSize(1);
            assertThat(imp.getErrors().get(0)).containsEntry("message", "parse error");
        }

        @Test
        void idIsNullBeforePersist() {
            var imp = createImport();
            assertThat(imp.getId()).isNull();
        }

        @Test
        void importedAtIsNullBeforePersist() {
            var imp = createImport();
            assertThat(imp.getImportedAt()).isNull();
        }
    }
}
