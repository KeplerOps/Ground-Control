package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import com.keplerops.groundcontrol.domain.requirements.service.FieldChange;
import com.keplerops.groundcontrol.domain.requirements.service.SnapshotMapper;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AuditServiceTest {

    private static void setField(Object obj, String fieldName, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class ComputeDiff {

        @Test
        void detectsChangedFields() {
            var previous = Map.<String, Object>of("title", "Old Title", "status", "DRAFT", "wave", 1);
            var current = Map.<String, Object>of("title", "New Title", "status", "DRAFT", "wave", 1);

            var diff = SnapshotMapper.computeDiff(previous, current);

            assertThat(diff).containsOnlyKeys("title");
            assertThat(diff.get("title")).isEqualTo(new FieldChange("Old Title", "New Title"));
        }

        @Test
        void returnsEmptyMapWhenNoChanges() {
            var snapshot = Map.<String, Object>of("title", "Same", "status", "DRAFT");

            var diff = SnapshotMapper.computeDiff(snapshot, snapshot);

            assertThat(diff).isEmpty();
        }

        @Test
        void detectsMultipleChanges() {
            var previous = Map.<String, Object>of("title", "Old", "status", "DRAFT", "priority", "SHOULD");
            var current = Map.<String, Object>of("title", "New", "status", "ACTIVE", "priority", "SHOULD");

            var diff = SnapshotMapper.computeDiff(previous, current);

            assertThat(diff).hasSize(2);
            assertThat(diff).containsKeys("title", "status");
        }

        @Test
        void handlesNullValues() {
            var previous = new LinkedHashMap<String, Object>();
            previous.put("wave", null);
            previous.put("title", "Title");

            var current = new LinkedHashMap<String, Object>();
            current.put("wave", 2);
            current.put("title", "Title");

            var diff = SnapshotMapper.computeDiff(previous, current);

            assertThat(diff).containsOnlyKeys("wave");
            assertThat(diff.get("wave")).isEqualTo(new FieldChange(null, 2));
        }

        @Test
        void handlesNullToNull() {
            var previous = new LinkedHashMap<String, Object>();
            previous.put("wave", null);

            var current = new LinkedHashMap<String, Object>();
            current.put("wave", null);

            var diff = SnapshotMapper.computeDiff(previous, current);

            assertThat(diff).isEmpty();
        }
    }

    @Nested
    class RequirementSnapshot {

        @Test
        void extractsAllFields() {
            var project = new Project("test", "Test");
            setField(project, "id", UUID.randomUUID());
            var req = new Requirement(project, "REQ-001", "My Title", "My Statement");
            req.setRationale("My Rationale");

            var snapshot = SnapshotMapper.fromRequirement(req);

            assertThat(snapshot)
                    .containsEntry("uid", "REQ-001")
                    .containsEntry("title", "My Title")
                    .containsEntry("statement", "My Statement")
                    .containsEntry("rationale", "My Rationale")
                    .containsEntry("requirementType", "FUNCTIONAL")
                    .containsEntry("priority", "MUST")
                    .containsEntry("status", "DRAFT");
        }
    }

    @Nested
    class RelationSnapshot {

        @Test
        void extractsAllFields() {
            var project = new Project("test", "Test");
            setField(project, "id", UUID.randomUUID());
            var source = new Requirement(project, "A", "A", "A");
            var target = new Requirement(project, "B", "B", "B");
            setField(source, "id", UUID.randomUUID());
            setField(target, "id", UUID.randomUUID());
            var rel = new RequirementRelation(source, target, RelationType.DEPENDS_ON);

            var snapshot = SnapshotMapper.fromRelation(rel);

            assertThat(snapshot)
                    .containsEntry("sourceId", source.getId().toString())
                    .containsEntry("targetId", target.getId().toString())
                    .containsEntry("relationType", "DEPENDS_ON")
                    .containsEntry("description", "");
        }
    }

    @Nested
    class ComputeRelationChanges {

        @Test
        void detectsAddedRelation() {
            var id = UUID.randomUUID();
            Map<UUID, Map<String, Object>> fromMap = Map.of();
            Map<UUID, Map<String, Object>> toMap = Map.of(id, Map.of("relationType", "DEPENDS_ON"));

            var changes = SnapshotMapper.computeRelationChanges(fromMap, toMap);

            assertThat(changes).hasSize(1);
            assertThat(changes.get(0).changeType()).isEqualTo("ADDED");
            assertThat(changes.get(0).relationId()).isEqualTo(id);
            assertThat(changes.get(0).snapshot()).containsEntry("relationType", "DEPENDS_ON");
            assertThat(changes.get(0).fieldChanges()).isEmpty();
        }

        @Test
        void detectsRemovedRelation() {
            var id = UUID.randomUUID();
            Map<UUID, Map<String, Object>> fromMap = Map.of(id, Map.of("relationType", "DEPENDS_ON"));
            Map<UUID, Map<String, Object>> toMap = Map.of();

            var changes = SnapshotMapper.computeRelationChanges(fromMap, toMap);

            assertThat(changes).hasSize(1);
            assertThat(changes.get(0).changeType()).isEqualTo("REMOVED");
            assertThat(changes.get(0).relationId()).isEqualTo(id);
        }

        @Test
        void detectsModifiedRelation() {
            var id = UUID.randomUUID();
            Map<UUID, Map<String, Object>> fromMap = Map.of(id, Map.of("description", "old desc"));
            Map<UUID, Map<String, Object>> toMap = Map.of(id, Map.of("description", "new desc"));

            var changes = SnapshotMapper.computeRelationChanges(fromMap, toMap);

            assertThat(changes).hasSize(1);
            assertThat(changes.get(0).changeType()).isEqualTo("MODIFIED");
            assertThat(changes.get(0).fieldChanges()).containsKey("description");
        }

        @Test
        void excludesUnchangedRelations() {
            var id = UUID.randomUUID();
            var snapshot = Map.<String, Object>of("relationType", "DEPENDS_ON");
            Map<UUID, Map<String, Object>> fromMap = Map.of(id, snapshot);
            Map<UUID, Map<String, Object>> toMap = Map.of(id, snapshot);

            var changes = SnapshotMapper.computeRelationChanges(fromMap, toMap);

            assertThat(changes).isEmpty();
        }

        @Test
        void emptyMapsProduceNoChanges() {
            var changes = SnapshotMapper.computeRelationChanges(Map.of(), Map.of());
            assertThat(changes).isEmpty();
        }
    }

    @Nested
    class ComputeTraceabilityLinkChanges {

        @Test
        void detectsAddedLink() {
            var id = UUID.randomUUID();
            Map<UUID, Map<String, Object>> fromMap = Map.of();
            Map<UUID, Map<String, Object>> toMap = Map.of(id, Map.of("artifactType", "CODE_FILE"));

            var changes = SnapshotMapper.computeTraceabilityLinkChanges(fromMap, toMap);

            assertThat(changes).hasSize(1);
            assertThat(changes.get(0).changeType()).isEqualTo("ADDED");
            assertThat(changes.get(0).linkId()).isEqualTo(id);
        }

        @Test
        void detectsRemovedLink() {
            var id = UUID.randomUUID();
            Map<UUID, Map<String, Object>> fromMap = Map.of(id, Map.of("artifactType", "CODE_FILE"));
            Map<UUID, Map<String, Object>> toMap = Map.of();

            var changes = SnapshotMapper.computeTraceabilityLinkChanges(fromMap, toMap);

            assertThat(changes).hasSize(1);
            assertThat(changes.get(0).changeType()).isEqualTo("REMOVED");
        }

        @Test
        void detectsModifiedLink() {
            var id = UUID.randomUUID();
            Map<UUID, Map<String, Object>> fromMap = Map.of(id, Map.of("syncStatus", "SYNCED"));
            Map<UUID, Map<String, Object>> toMap = Map.of(id, Map.of("syncStatus", "STALE"));

            var changes = SnapshotMapper.computeTraceabilityLinkChanges(fromMap, toMap);

            assertThat(changes).hasSize(1);
            assertThat(changes.get(0).changeType()).isEqualTo("MODIFIED");
            assertThat(changes.get(0).fieldChanges()).containsKey("syncStatus");
        }
    }

    @Nested
    class TraceabilityLinkSnapshot {

        @Test
        void extractsAllFields() {
            var project = new Project("test", "Test");
            setField(project, "id", UUID.randomUUID());
            var req = new Requirement(project, "REQ-001", "Title", "Statement");
            setField(req, "id", UUID.randomUUID());
            var link = new TraceabilityLink(req, ArtifactType.CODE_FILE, "src/Foo.java", LinkType.IMPLEMENTS);
            link.setArtifactUrl("https://example.com");
            link.setArtifactTitle("Foo implementation");

            var snapshot = SnapshotMapper.fromTraceabilityLink(link);

            assertThat(snapshot)
                    .containsEntry("artifactType", "CODE_FILE")
                    .containsEntry("artifactIdentifier", "src/Foo.java")
                    .containsEntry("artifactUrl", "https://example.com")
                    .containsEntry("artifactTitle", "Foo implementation")
                    .containsEntry("linkType", "IMPLEMENTS")
                    .containsEntry("syncStatus", "SYNCED");
        }
    }
}
