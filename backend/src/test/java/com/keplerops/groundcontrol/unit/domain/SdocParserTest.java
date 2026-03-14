package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.keplerops.groundcontrol.domain.requirements.service.SdocParser;
import com.keplerops.groundcontrol.domain.requirements.service.SdocRequirement;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SdocParserTest {

    @Nested
    class ParseRequirements {

        @Test
        void parsesRequirementWithAllFields() {
            String sdoc =
                    """
                    [[SECTION]]
                    TITLE: Wave 2 — Testing

                    [REQUIREMENT]
                    UID: REQ-001
                    TITLE: Test requirement
                    STATEMENT: >>>
                    This is a test statement.
                    <<<
                    COMMENT: GitHub issues: #42

                    [[/SECTION]]
                    """;

            List<SdocRequirement> result = SdocParser.parse(sdoc);

            assertThat(result).hasSize(1);
            SdocRequirement req = result.get(0);
            assertThat(req.uid()).isEqualTo("REQ-001");
            assertThat(req.title()).isEqualTo("Test requirement");
            assertThat(req.statement()).isEqualTo("This is a test statement.");
            assertThat(req.comment()).isEqualTo("GitHub issues: #42");
            assertThat(req.wave()).isEqualTo(2);
        }

        @Test
        void extractsMultilineStatement() {
            String sdoc =
                    """
                    [REQUIREMENT]
                    UID: REQ-ML
                    TITLE: Multiline test
                    STATEMENT: >>>
                    Line one of the statement.
                    Line two of the statement.
                    Line three of the statement.
                    <<<
                    """;

            List<SdocRequirement> result = SdocParser.parse(sdoc);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).statement())
                    .contains("Line one")
                    .contains("Line two")
                    .contains("Line three");
        }

        @Test
        void extractsParentRelations() {
            String sdoc =
                    """
                    [REQUIREMENT]
                    UID: REQ-CHILD
                    TITLE: Child requirement
                    STATEMENT: >>>
                    Child statement.
                    <<<
                    RELATIONS:
                    - TYPE: Parent
                      VALUE: REQ-PARENT-1
                    - TYPE: Parent
                      VALUE: REQ-PARENT-2
                    """;

            List<SdocRequirement> result = SdocParser.parse(sdoc);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).parentUids()).containsExactly("REQ-PARENT-1", "REQ-PARENT-2");
        }

        @Test
        void extractsIssueRefsFromComment() {
            String sdoc =
                    """
                    [REQUIREMENT]
                    UID: REQ-ISSUES
                    TITLE: Issue refs test
                    STATEMENT: >>>
                    Statement.
                    <<<
                    COMMENT: GitHub issues: #19, #20, #32
                    """;

            List<SdocRequirement> result = SdocParser.parse(sdoc);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).issueRefs()).containsExactly(19, 20, 32);
        }

        @Test
        void extractsWaveFromSectionTitle() {
            String sdoc =
                    """
                    [[SECTION]]
                    TITLE: Wave 3 — Security and Auth

                    [REQUIREMENT]
                    UID: REQ-WAVE
                    TITLE: Wave test
                    STATEMENT: >>>
                    Statement.
                    <<<

                    [[/SECTION]]
                    """;

            List<SdocRequirement> result = SdocParser.parse(sdoc);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).wave()).isEqualTo(3);
        }

        @Test
        void skipsRequirementWithoutUid() {
            String sdoc =
                    """
                    [REQUIREMENT]
                    TITLE: No UID requirement
                    STATEMENT: >>>
                    Statement without UID.
                    <<<

                    [REQUIREMENT]
                    UID: REQ-VALID
                    TITLE: Valid requirement
                    STATEMENT: >>>
                    Valid statement.
                    <<<
                    """;

            List<SdocRequirement> result = SdocParser.parse(sdoc);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).uid()).isEqualTo("REQ-VALID");
        }

        @Test
        void handlesEmptyComment() {
            String sdoc =
                    """
                    [REQUIREMENT]
                    UID: REQ-NOCOMMENT
                    TITLE: No comment test
                    STATEMENT: >>>
                    Statement.
                    <<<
                    """;

            List<SdocRequirement> result = SdocParser.parse(sdoc);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).comment()).isEmpty();
            assertThat(result.get(0).issueRefs()).isEmpty();
        }

        @Test
        void parsesRealProjectSdoc() throws IOException {
            Path sdocPath = Path.of("../archive/docs/requirements/project.sdoc");
            if (!Files.exists(sdocPath)) {
                sdocPath = Path.of("/home/atomik/src/Ground-Control/archive/docs/requirements/project.sdoc");
            }
            assumeTrue(Files.exists(sdocPath), "Skipped: project.sdoc not present");
            String content = Files.readString(sdocPath);

            List<SdocRequirement> result = SdocParser.parse(content);

            assertThat(result).hasSizeGreaterThanOrEqualTo(80);
            // Verify some known requirements exist
            assertThat(result.stream().map(SdocRequirement::uid)).contains("W0-DEV-ENV", "W0-ADR");
            // Verify wave extraction works
            assertThat(result.stream().filter(r -> r.wave() != null && r.wave() == 0))
                    .isNotEmpty();
        }
    }
}
