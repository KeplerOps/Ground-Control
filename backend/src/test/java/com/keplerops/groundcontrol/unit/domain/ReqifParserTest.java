package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.requirements.service.ReqifParseResult;
import com.keplerops.groundcontrol.domain.requirements.service.ReqifParser;
import com.keplerops.groundcontrol.domain.requirements.service.ReqifRelation;
import com.keplerops.groundcontrol.domain.requirements.service.ReqifRequirement;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReqifParserTest {

    @Nested
    class ParseSpecObjects {

        @Test
        void parsesSpecObjectsWithStandardAttributes() throws IOException {
            String xml = readFixture();
            ReqifParseResult result = ReqifParser.parse(xml);

            assertThat(result.requirements()).hasSize(2);

            ReqifRequirement req1 = result.requirements().stream()
                    .filter(r -> r.identifier().equals("REQ-001"))
                    .findFirst()
                    .orElseThrow();
            assertThat(req1.title()).isEqualTo("Login Feature");
            assertThat(req1.statement()).contains("secure");
            assertThat(req1.statement()).contains("login mechanism");

            ReqifRequirement req2 = result.requirements().stream()
                    .filter(r -> r.identifier().equals("REQ-002"))
                    .findFirst()
                    .orElseThrow();
            assertThat(req2.title()).isEqualTo("Password Policy");
            assertThat(req2.statement()).contains("12 characters");
        }

        @Test
        void parsesSpecRelations() throws IOException {
            String xml = readFixture();
            ReqifParseResult result = ReqifParser.parse(xml);

            assertThat(result.relations()).hasSize(1);
            ReqifRelation rel = result.relations().get(0);
            assertThat(rel.sourceIdentifier()).isEqualTo("REQ-002");
            assertThat(rel.targetIdentifier()).isEqualTo("REQ-001");
            assertThat(rel.relationType()).isEqualTo(RelationType.PARENT);
        }

        @Test
        void parsesSpecificationHierarchy() throws IOException {
            String xml = readFixture();
            ReqifParseResult result = ReqifParser.parse(xml);

            ReqifRequirement req2 = result.requirements().stream()
                    .filter(r -> r.identifier().equals("REQ-002"))
                    .findFirst()
                    .orElseThrow();
            assertThat(req2.parentIdentifiers()).contains("REQ-001");

            // REQ-001 is the root, no parent from hierarchy
            ReqifRequirement req1 = result.requirements().stream()
                    .filter(r -> r.identifier().equals("REQ-001"))
                    .findFirst()
                    .orElseThrow();
            assertThat(req1.parentIdentifiers()).isEmpty();
        }

        @Test
        void handlesXhtmlAttributeValues() throws IOException {
            String xml = readFixture();
            ReqifParseResult result = ReqifParser.parse(xml);

            ReqifRequirement req1 = result.requirements().stream()
                    .filter(r -> r.identifier().equals("REQ-001"))
                    .findFirst()
                    .orElseThrow();
            // XHTML should be stripped to plain text
            assertThat(req1.statement()).doesNotContain("<xhtml:");
            assertThat(req1.statement()).doesNotContain("<b>");
        }
    }

    @Nested
    class EmptyAndEdgeCases {

        @Test
        void handlesEmptyReqifFile() {
            String xml =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <REQ-IF xmlns="http://www.omg.org/spec/ReqIF/20110401/reqif.xsd">
                      <THE-HEADER>
                        <REQ-IF-HEADER IDENTIFIER="h1"><TITLE>Empty</TITLE></REQ-IF-HEADER>
                      </THE-HEADER>
                      <CORE-CONTENT>
                        <REQ-IF-CONTENT>
                          <DATATYPES/>
                          <SPEC-TYPES/>
                          <SPEC-OBJECTS/>
                          <SPEC-RELATIONS/>
                          <SPECIFICATIONS/>
                        </REQ-IF-CONTENT>
                      </CORE-CONTENT>
                    </REQ-IF>
                    """;

            ReqifParseResult result = ReqifParser.parse(xml);

            assertThat(result.requirements()).isEmpty();
            assertThat(result.relations()).isEmpty();
        }

        @Test
        void skipsSpecObjectsWithoutIdentifier() {
            String xml =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <REQ-IF xmlns="http://www.omg.org/spec/ReqIF/20110401/reqif.xsd">
                      <THE-HEADER>
                        <REQ-IF-HEADER IDENTIFIER="h1"><TITLE>Test</TITLE></REQ-IF-HEADER>
                      </THE-HEADER>
                      <CORE-CONTENT>
                        <REQ-IF-CONTENT>
                          <DATATYPES/>
                          <SPEC-TYPES/>
                          <SPEC-OBJECTS>
                            <SPEC-OBJECT IDENTIFIER="" LONG-NAME="No ID"/>
                            <SPEC-OBJECT IDENTIFIER="VALID-001" LONG-NAME="Valid"/>
                          </SPEC-OBJECTS>
                          <SPEC-RELATIONS/>
                          <SPECIFICATIONS/>
                        </REQ-IF-CONTENT>
                      </CORE-CONTENT>
                    </REQ-IF>
                    """;

            ReqifParseResult result = ReqifParser.parse(xml);

            assertThat(result.requirements()).hasSize(1);
            assertThat(result.requirements().get(0).identifier()).isEqualTo("VALID-001");
        }
    }

    @Nested
    class RelationTypeMapping {

        @Test
        void mapsRelationTypesByNamingConvention() {
            assertThat(ReqifParser.mapRelationType("Parent Relationship")).isEqualTo(RelationType.PARENT);
            assertThat(ReqifParser.mapRelationType("child-of")).isEqualTo(RelationType.PARENT);
            assertThat(ReqifParser.mapRelationType("hierarchy")).isEqualTo(RelationType.PARENT);
            assertThat(ReqifParser.mapRelationType("depends on")).isEqualTo(RelationType.DEPENDS_ON);
            assertThat(ReqifParser.mapRelationType("dependency")).isEqualTo(RelationType.DEPENDS_ON);
            assertThat(ReqifParser.mapRelationType("conflicts with")).isEqualTo(RelationType.CONFLICTS_WITH);
            assertThat(ReqifParser.mapRelationType("refines")).isEqualTo(RelationType.REFINES);
            assertThat(ReqifParser.mapRelationType("refinement")).isEqualTo(RelationType.REFINES);
            assertThat(ReqifParser.mapRelationType("supersedes")).isEqualTo(RelationType.SUPERSEDES);
            assertThat(ReqifParser.mapRelationType("replaces")).isEqualTo(RelationType.SUPERSEDES);
            assertThat(ReqifParser.mapRelationType("some unknown type")).isEqualTo(RelationType.RELATED);
            assertThat(ReqifParser.mapRelationType("")).isEqualTo(RelationType.RELATED);
            assertThat(ReqifParser.mapRelationType(null)).isEqualTo(RelationType.RELATED);
        }
    }

    @Nested
    class XxePrevention {

        @Test
        void rejectsDoctypeDeclaration() {
            String xml =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                    <REQ-IF xmlns="http://www.omg.org/spec/ReqIF/20110401/reqif.xsd">
                      <THE-HEADER>
                        <REQ-IF-HEADER IDENTIFIER="h1"><TITLE>&xxe;</TITLE></REQ-IF-HEADER>
                      </THE-HEADER>
                    </REQ-IF>
                    """;

            assertThatThrownBy(() -> ReqifParser.parse(xml))
                    .isInstanceOf(ReqifParser.ReqifParseException.class)
                    .hasMessageContaining("Failed to parse ReqIF XML");
        }
    }

    @Nested
    class UidSanitization {

        @Test
        void shortIdentifiersUnchanged() {
            assertThat(ReqifParser.sanitizeUid("REQ-001")).isEqualTo("REQ-001");
            assertThat(ReqifParser.sanitizeUid("a".repeat(50))).isEqualTo("a".repeat(50));
        }

        @Test
        void longIdentifiersTruncatedDeterministically() {
            String longId = "a".repeat(60);
            String result = ReqifParser.sanitizeUid(longId);

            assertThat(result).hasSize(50);
            assertThat(result).startsWith("a".repeat(42) + "-");
            // Deterministic: same input -> same output
            assertThat(ReqifParser.sanitizeUid(longId)).isEqualTo(result);
        }
    }

    private static String readFixture() throws IOException {
        return Files.readString(Path.of("src/test/resources/fixtures/test-requirements.reqif"));
    }
}
