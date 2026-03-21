package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import com.keplerops.groundcontrol.domain.requirements.service.AuditService;
import com.keplerops.groundcontrol.domain.requirements.service.FieldChange;
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

            var diff = AuditService.computeDiff(previous, current);

            assertThat(diff).isNotNull();
            assertThat(diff).containsOnlyKeys("title");
            assertThat(diff.get("title")).isEqualTo(new FieldChange("Old Title", "New Title"));
        }

        @Test
        void returnsNullWhenNoChanges() {
            var snapshot = Map.<String, Object>of("title", "Same", "status", "DRAFT");

            var diff = AuditService.computeDiff(snapshot, snapshot);

            assertThat(diff).isNull();
        }

        @Test
        void detectsMultipleChanges() {
            var previous = Map.<String, Object>of("title", "Old", "status", "DRAFT", "priority", "SHOULD");
            var current = Map.<String, Object>of("title", "New", "status", "ACTIVE", "priority", "SHOULD");

            var diff = AuditService.computeDiff(previous, current);

            assertThat(diff).isNotNull();
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

            var diff = AuditService.computeDiff(previous, current);

            assertThat(diff).isNotNull();
            assertThat(diff).containsOnlyKeys("wave");
            assertThat(diff.get("wave")).isEqualTo(new FieldChange(null, 2));
        }

        @Test
        void handlesNullToNull() {
            var previous = new LinkedHashMap<String, Object>();
            previous.put("wave", null);

            var current = new LinkedHashMap<String, Object>();
            current.put("wave", null);

            var diff = AuditService.computeDiff(previous, current);

            assertThat(diff).isNull();
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

            var snapshot = AuditService.requirementToSnapshot(req);

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

            var snapshot = AuditService.relationToSnapshot(rel);

            assertThat(snapshot)
                    .containsEntry("sourceId", source.getId().toString())
                    .containsEntry("targetId", target.getId().toString())
                    .containsEntry("relationType", "DEPENDS_ON")
                    .containsEntry("description", "");
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

            var snapshot = AuditService.traceabilityLinkToSnapshot(link);

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
