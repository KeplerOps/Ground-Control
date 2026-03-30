package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.*;

import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.SyncStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@SuppressWarnings("java:S2187") // Tests are in @Nested inner classes
class TraceabilityLinkTest {

    private static final Project TEST_PROJECT = createTestProject();

    private static Project createTestProject() {
        var project = new Project("test-project", "Test Project");
        try {
            var field = Project.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(project, UUID.fromString("00000000-0000-0000-0000-000000000001"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return project;
    }

    private static Requirement createRequirement() {
        return new Requirement(TEST_PROJECT, "REQ-001", "Title", "Statement");
    }

    private static TraceabilityLink createLink() {
        return new TraceabilityLink(
                createRequirement(), ArtifactType.CODE_FILE, "file:src/Main.java", LinkType.IMPLEMENTS);
    }

    @Nested
    class Defaults {

        @Test
        void syncStatusDefaultsToSynced() {
            var link = createLink();
            assertThat(link.getSyncStatus()).isEqualTo(SyncStatus.SYNCED);
        }

        @Test
        void artifactUrlDefaultsToEmpty() {
            var link = createLink();
            assertThat(link.getArtifactUrl()).isEmpty();
        }

        @Test
        void artifactTitleDefaultsToEmpty() {
            var link = createLink();
            assertThat(link.getArtifactTitle()).isEmpty();
        }

        @Test
        void timestampsAreNullBeforePersist() {
            var link = createLink();
            assertThat(link.getCreatedAt()).isNull();
            assertThat(link.getUpdatedAt()).isNull();
            assertThat(link.getLastSyncedAt()).isNull();
        }
    }

    @Nested
    class Construction {

        @Test
        void requiredFieldsSetCorrectly() {
            var req = createRequirement();
            var link = new TraceabilityLink(req, ArtifactType.ADR, "adr:011", LinkType.DOCUMENTS);

            assertThat(link.getRequirement()).isEqualTo(req);
            assertThat(link.getArtifactType()).isEqualTo(ArtifactType.ADR);
            assertThat(link.getArtifactIdentifier()).isEqualTo("adr:011");
            assertThat(link.getLinkType()).isEqualTo(LinkType.DOCUMENTS);
        }

        @ParameterizedTest
        @EnumSource(ArtifactType.class)
        void canBeConstructedWithAnyArtifactType(ArtifactType type) {
            var req = createRequirement();
            var link = new TraceabilityLink(req, type, "artifact:" + type.name(), LinkType.IMPLEMENTS);

            assertThat(link.getArtifactType()).isEqualTo(type);
            assertThat(link.getArtifactIdentifier()).isEqualTo("artifact:" + type.name());
        }
    }

    @Nested
    class Accessors {

        @Test
        void syncStatusSetterWorks() {
            var link = createLink();
            link.setSyncStatus(SyncStatus.STALE);
            assertThat(link.getSyncStatus()).isEqualTo(SyncStatus.STALE);
        }

        @Test
        void artifactUrlSetterWorks() {
            var link = createLink();
            link.setArtifactUrl("https://example.com");
            assertThat(link.getArtifactUrl()).isEqualTo("https://example.com");
        }

        @Test
        void artifactTitleSetterWorks() {
            var link = createLink();
            link.setArtifactTitle("Some artifact");
            assertThat(link.getArtifactTitle()).isEqualTo("Some artifact");
        }

        @Test
        void lastSyncedAtSetterWorks() {
            var link = createLink();
            var now = Instant.now();
            link.setLastSyncedAt(now);
            assertThat(link.getLastSyncedAt()).isEqualTo(now);
        }

        @Test
        void idIsNullBeforePersist() {
            var link = createLink();
            assertThat(link.getId()).isNull();
        }
    }
}
