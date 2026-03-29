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

@SuppressWarnings("java:S2187") // Tests are in @Nested inner classes
class TraceabilityLinkTest {

    private static final Project TEST_PROJECT = createTestProject();

    private static Project createTestProject() {
        return createTestProject("test-project", UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    private static Project createTestProject(String identifier, UUID id) {
        var project = new Project(identifier, "Test Project");
        try {
            var field = Project.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(project, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return project;
    }

    private static Requirement createRequirement() {
        return createRequirement(TEST_PROJECT, "REQ-001");
    }

    private static Requirement createRequirement(Project project, String uid) {
        return new Requirement(project, uid, "Title", "Statement");
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
    }

    @Nested
    class Equality {

        private static Requirement createRequirementWithId(Project project, String uid, UUID id) {
            var req = new Requirement(project, uid, "Title", "Statement");
            try {
                var f = Requirement.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(req, id);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            return req;
        }

        @Test
        void linksWithSameNaturalKeyAreEqual() {
            var req1 = createRequirement();
            var req2 = createRequirement(TEST_PROJECT, "req-001");
            var link1 = new TraceabilityLink(req1, ArtifactType.CODE_FILE, "file:src/Main.java", LinkType.IMPLEMENTS);
            var link2 = new TraceabilityLink(req2, ArtifactType.CODE_FILE, "file:src/Main.java", LinkType.IMPLEMENTS);
            assertThat(link1).isEqualTo(link2);
        }

        @Test
        void differentLinkTypeIsNotEqual() {
            var reqId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            var req1 = createRequirementWithId(TEST_PROJECT, "REQ-001", reqId);
            var req2 = createRequirementWithId(TEST_PROJECT, "REQ-001", reqId);
            var link1 = new TraceabilityLink(req1, ArtifactType.CODE_FILE, "file:src/Main.java", LinkType.IMPLEMENTS);
            var link2 = new TraceabilityLink(req2, ArtifactType.CODE_FILE, "file:src/Main.java", LinkType.TESTS);
            assertThat(link1).isNotEqualTo(link2);
        }

        @Test
        void equalLinksHaveSameHashCode() {
            var req1 = createRequirement();
            var req2 = createRequirement(TEST_PROJECT, "req-001");
            var link1 = new TraceabilityLink(req1, ArtifactType.CODE_FILE, "file:src/Main.java", LinkType.IMPLEMENTS);
            var link2 = new TraceabilityLink(req2, ArtifactType.CODE_FILE, "file:src/Main.java", LinkType.IMPLEMENTS);
            assertThat(link1.hashCode()).isEqualTo(link2.hashCode());
        }

        @Test
        void sameNaturalKeyInSetDeduplicates() {
            var req1 = createRequirement();
            var req2 = createRequirement(TEST_PROJECT, "req-001");
            var link1 = new TraceabilityLink(req1, ArtifactType.CODE_FILE, "file:src/Main.java", LinkType.IMPLEMENTS);
            var link2 = new TraceabilityLink(req2, ArtifactType.CODE_FILE, "file:src/Main.java", LinkType.IMPLEMENTS);
            var set = new java.util.HashSet<TraceabilityLink>();
            set.add(link1);
            set.add(link2);
            assertThat(set).hasSize(1);
        }

        @Test
        void linksForDifferentTransientRequirementsAreNotEqual() {
            var otherProject =
                    createTestProject("other-project", UUID.fromString("00000000-0000-0000-0000-000000000002"));
            var link1 = new TraceabilityLink(
                    createRequirement(TEST_PROJECT, "REQ-001"),
                    ArtifactType.CODE_FILE,
                    "file:src/Main.java",
                    LinkType.IMPLEMENTS);
            var link2 = new TraceabilityLink(
                    createRequirement(otherProject, "REQ-001"),
                    ArtifactType.CODE_FILE,
                    "file:src/Main.java",
                    LinkType.IMPLEMENTS);
            assertThat(link1).isNotEqualTo(link2);
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
