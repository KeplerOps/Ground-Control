package com.keplerops.groundcontrol.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.TestUtil;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.graph.model.GraphEdge;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphNode;
import com.keplerops.groundcontrol.domain.graph.model.GraphProjection;
import com.keplerops.groundcontrol.domain.graph.service.GraphProjectionRegistryService;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.infrastructure.age.AgeGraphService;
import com.keplerops.groundcontrol.infrastructure.age.AgeProperties;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;

@ExtendWith(MockitoExtension.class)
class AgeGraphServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private GraphProjectionRegistryService graphProjectionRegistryService;

    @Mock
    private ProjectRepository projectRepository;

    private AgeGraphService service;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Project TEST_PROJECT = createTestProject();

    private static Project createTestProject() {
        var project = new Project("test-project", "Test Project");
        TestUtil.setField(project, "id", PROJECT_ID);
        return project;
    }

    @BeforeEach
    void setUp() {
        var disabledProperties = new AgeProperties(false, "requirements");
        service = new AgeGraphService(
                jdbcTemplate, disabledProperties, graphProjectionRegistryService, projectRepository);
    }

    @Nested
    class WhenDisabled {

        @Test
        void materializeGraph_isNoOp() {
            service.materializeGraph();

            verifyNoInteractions(jdbcTemplate, graphProjectionRegistryService, projectRepository);
        }

        @Test
        void getAncestors_returnsEmpty() {
            var result = service.getAncestors(PROJECT_ID, "REQ-001", 10);

            assertThat(result).isEmpty();
            verifyNoInteractions(jdbcTemplate);
        }

        @Test
        void getDescendants_returnsEmpty() {
            var result = service.getDescendants(PROJECT_ID, "REQ-001", 10);

            assertThat(result).isEmpty();
            verifyNoInteractions(jdbcTemplate);
        }

        @Test
        void findPaths_returnsEmpty() {
            var result = service.findPaths(PROJECT_ID, "REQ-001", "REQ-002");

            assertThat(result).isEmpty();
            verifyNoInteractions(jdbcTemplate);
        }
    }

    @Nested
    class WhenEnabled {

        private AgeGraphService enabledService;

        @BeforeEach
        void setUp() {
            var enabledProperties = new AgeProperties(true, "test_graph");
            enabledService = new AgeGraphService(
                    jdbcTemplate, enabledProperties, graphProjectionRegistryService, projectRepository);
        }

        @Test
        void materializeGraph_createsNodesAndEdges() {
            when(graphProjectionRegistryService.buildProjection())
                    .thenReturn(new GraphProjection(List.of(), List.of()));

            enabledService.materializeGraph();

            // setupSearchPath: LOAD + SET via execute(String)
            verify(jdbcTemplate).execute("LOAD 'age'");
            verify(jdbcTemplate).execute("SET search_path = ag_catalog, \"$user\", public");
            // DETACH DELETE is a parameterized cypher() call — the cypher text is concatenated
            // into the SQL literal (AGE parses it at SQL parse time), and only the agtype params
            // payload is bound through a PreparedStatementSetter. AGE's cypher() always returns
            // SETOF agtype even for write statements, so we route through query(), not update().
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate, atLeast(1))
                    .query(sqlCaptor.capture(), any(PreparedStatementSetter.class), any(RowCallbackHandler.class));
            assertThat(sqlCaptor.getAllValues().stream().anyMatch(sql -> sql.contains("DETACH DELETE")))
                    .isTrue();
        }

        @Test
        void materializeGraph_withRequirements_createsNodes() {
            var req = new Requirement(TEST_PROJECT, "GC-A001", "Test Req", "Statement");
            when(graphProjectionRegistryService.buildProjection())
                    .thenReturn(new GraphProjection(
                            List.of(new GraphNode(
                                    "REQUIREMENT:" + UUID.randomUUID(),
                                    req.getId() != null
                                            ? req.getId().toString()
                                            : UUID.randomUUID().toString(),
                                    GraphEntityType.REQUIREMENT,
                                    TEST_PROJECT.getIdentifier(),
                                    req.getUid(),
                                    req.getUid(),
                                    Map.of("title", req.getTitle()))),
                            List.of()));

            enabledService.materializeGraph();

            // setupSearchPath issues LOAD + SET via execute(String); DETACH DELETE + CREATE go
            // through query(sql, pss, callback) — AGE's cypher() always returns SETOF agtype.
            verify(jdbcTemplate, times(2)).execute(anyString());
            verify(jdbcTemplate, atLeast(2))
                    .query(anyString(), any(PreparedStatementSetter.class), any(RowCallbackHandler.class));
        }

        @Test
        void getAncestors_queriesGraph() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(TEST_PROJECT));
            enabledService.getAncestors(PROJECT_ID, "REQ-001", 5);

            // setupSearchPath: LOAD + SET
            verify(jdbcTemplate, times(2)).execute(anyString());
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate)
                    .query(sqlCaptor.capture(), any(PreparedStatementSetter.class), any(RowCallbackHandler.class));
            assertThat(sqlCaptor.getValue()).contains("PARENT");
        }

        @Test
        void getDescendants_queriesGraph() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(TEST_PROJECT));
            enabledService.getDescendants(PROJECT_ID, "REQ-001", 5);

            verify(jdbcTemplate, times(2)).execute(anyString());
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate)
                    .query(sqlCaptor.capture(), any(PreparedStatementSetter.class), any(RowCallbackHandler.class));
            assertThat(sqlCaptor.getValue()).contains("PARENT");
        }

        @Test
        void findPaths_queriesGraphWithRelationships() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(TEST_PROJECT));
            enabledService.findPaths(PROJECT_ID, "REQ-001", "REQ-002");

            verify(jdbcTemplate, times(2)).execute(anyString());
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate)
                    .query(sqlCaptor.capture(), any(PreparedStatementSetter.class), any(RowCallbackHandler.class));
            assertThat(sqlCaptor.getValue()).contains("relationships(path)");
            assertThat(sqlCaptor.getValue()).contains("nodes(path)");
        }
    }

    @Nested
    class Sanitization {

        private static final ObjectMapper TEST_OBJECT_MAPPER = new ObjectMapper();
        private AgeGraphService enabledService;

        @BeforeEach
        void setUp() {
            var enabledProperties = new AgeProperties(true, "test_graph");
            enabledService = new AgeGraphService(
                    jdbcTemplate, enabledProperties, graphProjectionRegistryService, projectRepository);
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> parseParams(String json) {
            try {
                return TEST_OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Drive a captured {@link PreparedStatementSetter} against a mock {@link PreparedStatement}
         * to extract the bound agtype payload. This is how the production code's bind path is
         * exercised in unit tests without a real database.
         */
        private static String capturedAgtypeParam(PreparedStatementSetter pss) {
            try {
                PreparedStatement mockPs = mock(PreparedStatement.class);
                pss.setValues(mockPs);
                ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
                verify(mockPs).setObject(eq(1), captor.capture());
                Object obj = captor.getValue();
                return obj instanceof PGobject pgo ? pgo.getValue() : (obj == null ? null : obj.toString());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        void getAncestors_userValuesAreParameterizedNotInlined() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(TEST_PROJECT));

            enabledService.getAncestors(PROJECT_ID, "REQ-001", 5);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<PreparedStatementSetter> pssCaptor = ArgumentCaptor.forClass(PreparedStatementSetter.class);
            verify(jdbcTemplate).query(sqlCaptor.capture(), pssCaptor.capture(), any(RowCallbackHandler.class));

            String sql = sqlCaptor.getValue();
            // User-controlled values must NOT appear in the SQL string (which AGE parses at SQL
            // parse time and which embeds the cypher template). They must appear in the bound
            // agtype params payload, set via the PreparedStatementSetter.
            assertThat(sql).doesNotContain("REQ-001");
            assertThat(sql).doesNotContain("test-project");
            String paramsJson = capturedAgtypeParam(pssCaptor.getValue());
            assertThat(paramsJson).contains("REQ-001");
            assertThat(paramsJson).contains("test-project");
        }

        @Test
        void getDescendants_userValuesAreParameterizedNotInlined() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(TEST_PROJECT));

            enabledService.getDescendants(PROJECT_ID, "REQ-001", 5);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<PreparedStatementSetter> pssCaptor = ArgumentCaptor.forClass(PreparedStatementSetter.class);
            verify(jdbcTemplate).query(sqlCaptor.capture(), pssCaptor.capture(), any(RowCallbackHandler.class));

            String sql = sqlCaptor.getValue();
            assertThat(sql).doesNotContain("REQ-001");
            assertThat(sql).doesNotContain("test-project");
            String paramsJson = capturedAgtypeParam(pssCaptor.getValue());
            assertThat(paramsJson).contains("REQ-001");
            assertThat(paramsJson).contains("test-project");
        }

        @Test
        void findPaths_userValuesAreParameterizedNotInlined() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(TEST_PROJECT));

            enabledService.findPaths(PROJECT_ID, "REQ-001", "REQ-002");

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<PreparedStatementSetter> pssCaptor = ArgumentCaptor.forClass(PreparedStatementSetter.class);
            verify(jdbcTemplate).query(sqlCaptor.capture(), pssCaptor.capture(), any(RowCallbackHandler.class));

            String sql = sqlCaptor.getValue();
            assertThat(sql).doesNotContain("REQ-001");
            assertThat(sql).doesNotContain("REQ-002");
            assertThat(sql).doesNotContain("test-project");
            String paramsJson = capturedAgtypeParam(pssCaptor.getValue());
            assertThat(paramsJson).contains("REQ-001");
            assertThat(paramsJson).contains("REQ-002");
            assertThat(paramsJson).contains("test-project");
        }

        @Test
        void getVisualization_userValuesAreParameterizedNotInlined() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(TEST_PROJECT));

            enabledService.getVisualization(PROJECT_ID);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<PreparedStatementSetter> pssCaptor = ArgumentCaptor.forClass(PreparedStatementSetter.class);
            verify(jdbcTemplate, times(2))
                    .query(sqlCaptor.capture(), pssCaptor.capture(), any(RowCallbackHandler.class));

            for (int i = 0; i < sqlCaptor.getAllValues().size(); i++) {
                String sql = sqlCaptor.getAllValues().get(i);
                assertThat(sql).doesNotContain("test-project");
                String paramsJson = capturedAgtypeParam(pssCaptor.getAllValues().get(i));
                assertThat(paramsJson).contains("test-project");
            }
        }

        @Test
        void materializeGraph_freeFormPropertyValuesAreParameterizedNotInlined() {
            // Adversarial title with $gc$ delimiter, single quotes, backslashes, and SQL keywords.
            // None of these may appear in the SQL string sent to JdbcTemplate; they must appear
            // only in the bound agtype params payload (round-tripped through Jackson, since the
            // JSON encoding escapes backslashes/quotes).
            String adversarialTitle = "Evil $gc$); DROP TABLE requirement; --";
            String adversarialStatement = "Stmt with 'quotes' and \\backslashes\\ and $$delimiters$$";
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("title", adversarialTitle);
            properties.put("statement", adversarialStatement);

            var node = new GraphNode(
                    "REQUIREMENT:" + UUID.randomUUID(),
                    UUID.randomUUID().toString(),
                    GraphEntityType.REQUIREMENT,
                    TEST_PROJECT.getIdentifier(),
                    "GC-A001",
                    "GC-A001",
                    properties);
            when(graphProjectionRegistryService.buildProjection())
                    .thenReturn(new GraphProjection(List.of(node), List.of()));

            enabledService.materializeGraph();

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<PreparedStatementSetter> pssCaptor = ArgumentCaptor.forClass(PreparedStatementSetter.class);
            verify(jdbcTemplate, atLeast(1))
                    .query(sqlCaptor.capture(), pssCaptor.capture(), any(RowCallbackHandler.class));

            boolean foundCreate = false;
            for (int i = 0; i < sqlCaptor.getAllValues().size(); i++) {
                String sql = sqlCaptor.getAllValues().get(i);
                if (sql.contains("CREATE (:")) {
                    foundCreate = true;
                    assertThat(sql).doesNotContain(adversarialTitle);
                    assertThat(sql).doesNotContain(adversarialStatement);
                    assertThat(sql).doesNotContain("DROP TABLE");
                    String paramsJson =
                            capturedAgtypeParam(pssCaptor.getAllValues().get(i));
                    Map<String, Object> params = parseParams(paramsJson);
                    assertThat(params.values()).contains(adversarialTitle, adversarialStatement);
                }
            }
            assertThat(foundCreate)
                    .as("CREATE statement should be issued for the requirement")
                    .isTrue();
        }

        @Test
        void materializeGraph_edgePropertyValuesAreParameterizedNotInlined() {
            String adversarialSourceUid = "REQ-EVIL$gc$);DROP--";
            var edge = new GraphEdge(
                    UUID.randomUUID().toString(),
                    "PARENT",
                    "REQUIREMENT:" + UUID.randomUUID(),
                    "REQUIREMENT:" + UUID.randomUUID(),
                    GraphEntityType.REQUIREMENT,
                    GraphEntityType.REQUIREMENT,
                    Map.of("sourceUid", adversarialSourceUid));
            when(graphProjectionRegistryService.buildProjection())
                    .thenReturn(new GraphProjection(List.of(), List.of(edge)));

            enabledService.materializeGraph();

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<PreparedStatementSetter> pssCaptor = ArgumentCaptor.forClass(PreparedStatementSetter.class);
            verify(jdbcTemplate, atLeast(1))
                    .query(sqlCaptor.capture(), pssCaptor.capture(), any(RowCallbackHandler.class));

            boolean foundEdgeCreate = false;
            for (int i = 0; i < sqlCaptor.getAllValues().size(); i++) {
                String sql = sqlCaptor.getAllValues().get(i);
                if (sql.contains("MATCH") && sql.contains("CREATE")) {
                    foundEdgeCreate = true;
                    assertThat(sql).doesNotContain(adversarialSourceUid);
                    String paramsJson =
                            capturedAgtypeParam(pssCaptor.getAllValues().get(i));
                    Map<String, Object> params = parseParams(paramsJson);
                    assertThat(params.values()).contains(adversarialSourceUid);
                }
            }
            assertThat(foundEdgeCreate)
                    .as("MATCH/CREATE edge statement should be issued")
                    .isTrue();
        }

        @Test
        void validateUid_rejectsBlankInput() {
            assertThatThrownBy(() -> enabledService.getAncestors(PROJECT_ID, "", 5))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void validateUid_rejectsControlCharacters() {
            assertThatThrownBy(() -> enabledService.getAncestors(PROJECT_ID, "REQ\n001", 5))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void validateUid_rejectsOverlongValues() {
            String overlong = "X".repeat(51);
            assertThatThrownBy(() -> enabledService.getAncestors(PROJECT_ID, overlong, 5))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void getAncestors_acceptsAdversarialUidThroughParameterization() {
            // UIDs containing $gc$, single quotes, and backslashes are no longer rejected at
            // the AGE-adapter validator — parameter binding makes them structurally safe — but
            // they MUST still be bound through the params payload, never inlined into the SQL.
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(TEST_PROJECT));
            String adversarialUid = "REQ$gc$';DROP";

            enabledService.getAncestors(PROJECT_ID, adversarialUid, 5);

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<PreparedStatementSetter> pssCaptor = ArgumentCaptor.forClass(PreparedStatementSetter.class);
            verify(jdbcTemplate).query(sqlCaptor.capture(), pssCaptor.capture(), any(RowCallbackHandler.class));
            assertThat(sqlCaptor.getValue()).doesNotContain(adversarialUid);
            String paramsJson = capturedAgtypeParam(pssCaptor.getValue());
            Map<String, Object> params = parseParams(paramsJson);
            assertThat(params).containsEntry("uid", adversarialUid);
        }

        @Test
        void validateGraphName_rejectsPayloadsContainingDollarSigns() {
            var dangerousProps = new AgeProperties(true, "graph$gc$");
            var dangerousService = new AgeGraphService(
                    jdbcTemplate, dangerousProps, graphProjectionRegistryService, projectRepository);
            // No buildProjection stub: validateGraphName fails before the projection is read.

            assertThatThrownBy(dangerousService::materializeGraph).isInstanceOf(DomainValidationException.class);
        }

        @Test
        void getAncestors_rejectsDepthBelowMin() {
            assertThatThrownBy(() -> enabledService.getAncestors(PROJECT_ID, "REQ-001", 0))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void getAncestors_rejectsDepthAboveMax() {
            // MAX_GRAPH_TRAVERSAL_DEPTH = 20; anything above must be rejected before the
            // variable-length-path bound is interpolated.
            assertThatThrownBy(() -> enabledService.getAncestors(PROJECT_ID, "REQ-001", 21))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void getAncestors_rejectsNegativeDepth() {
            assertThatThrownBy(() -> enabledService.getAncestors(PROJECT_ID, "REQ-001", -1))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void getDescendants_rejectsDepthBelowMin() {
            assertThatThrownBy(() -> enabledService.getDescendants(PROJECT_ID, "REQ-001", 0))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void getDescendants_rejectsDepthAboveMax() {
            assertThatThrownBy(() -> enabledService.getDescendants(PROJECT_ID, "REQ-001", 21))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void findPaths_cypherIncludesHardDepthCap() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(TEST_PROJECT));

            enabledService.findPaths(PROJECT_ID, "REQ-001", "REQ-002");

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate)
                    .query(sqlCaptor.capture(), any(PreparedStatementSetter.class), any(RowCallbackHandler.class));
            // Confirm the variable-length path is bounded; an unbounded `[*]->` would let a single
            // findPaths call enumerate every path in a cyclic graph.
            assertThat(sqlCaptor.getValue()).contains("[*1..20]");
            assertThat(sqlCaptor.getValue()).doesNotContain("[*]->");
        }

        @Test
        void findPaths_cypherIncludesResultLimitInsideCypherBlock() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(TEST_PROJECT));

            enabledService.findPaths(PROJECT_ID, "REQ-001", "REQ-002");

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate)
                    .query(sqlCaptor.capture(), any(PreparedStatementSetter.class), any(RowCallbackHandler.class));
            // The LIMIT must live inside the $gc$...$gc$ Cypher block so AGE itself bounds path
            // enumeration. An outer-SQL LIMIT only truncates rows after AGE materializes them,
            // which doesn't bound expansion on a cyclic graph.
            String sql = sqlCaptor.getValue();
            int gcStart = sql.indexOf("$gc$");
            int gcEnd = sql.lastIndexOf("$gc$");
            assertThat(gcStart).isPositive();
            assertThat(gcEnd).isGreaterThan(gcStart);
            String cypherBlock = sql.substring(gcStart, gcEnd);
            assertThat(cypherBlock).contains("LIMIT 50");
        }
    }

    @Nested
    class PropertyKeyRegistry {

        private AgeGraphService enabledService;

        @BeforeEach
        void setUp() {
            var enabledProperties = new AgeProperties(true, "test_graph");
            enabledService = new AgeGraphService(
                    jdbcTemplate, enabledProperties, graphProjectionRegistryService, projectRepository);
        }

        @Test
        void materializeGraph_rejectsUnknownPropertyKey() {
            // Per ADR-032, AGE property keys must come from APPROVED_PROPERTY_KEYS — a future
            // contributor cannot silently introduce a new key just by satisfying the syntactic
            // allowlist.
            var node = new GraphNode(
                    "REQUIREMENT:" + UUID.randomUUID(),
                    UUID.randomUUID().toString(),
                    GraphEntityType.REQUIREMENT,
                    "test-project",
                    "GC-A001",
                    "GC-A001",
                    Map.of("unknown_property_not_in_registry", "value"));
            when(graphProjectionRegistryService.buildProjection())
                    .thenReturn(new GraphProjection(List.of(node), List.of()));

            assertThatThrownBy(enabledService::materializeGraph)
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("not in approved AGE schema registry");
        }

        @Test
        void approvedPropertyKeysSetIsImmutable() {
            // Defense in depth: the registry must be a Set.of(...) that throws on mutation,
            // not a mutable HashSet a future caller could grow at runtime.
            assertThatThrownBy(() -> AgeGraphService.APPROVED_PROPERTY_KEYS.add("evil"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class AgtypeParsing {

        @Test
        void stripAgtypeTypeTags_stripsTagsInStructuralPositions() {
            String input = "[{\"id\": 1}::vertex, {\"id\": 2}::vertex]";
            assertThat(AgeGraphService.stripAgtypeTypeTags(input)).isEqualTo("[{\"id\": 1}, {\"id\": 2}]");
        }

        @Test
        void stripAgtypeTypeTags_leavesTagsInsideStringsAlone() {
            // A user-controlled property value containing the literal "}::vertex" must NOT be
            // mutated. Without quote-aware processing, a naive replace would corrupt the value.
            String input = "{\"properties\": {\"title\": \"Evil }::vertex marker\"}}::vertex";
            String expected = "{\"properties\": {\"title\": \"Evil }::vertex marker\"}}";
            assertThat(AgeGraphService.stripAgtypeTypeTags(input)).isEqualTo(expected);
        }

        @Test
        void stripAgtypeTypeTags_handlesEscapedQuotesInsideStrings() {
            // A string value containing an escaped quote followed by }::vertex must not split
            // the string-state tracking.
            String input = "{\"title\": \"weird \\\"quoted\\\" }::vertex stuff\"}::vertex";
            String expected = "{\"title\": \"weird \\\"quoted\\\" }::vertex stuff\"}";
            assertThat(AgeGraphService.stripAgtypeTypeTags(input)).isEqualTo(expected);
        }

        @Test
        void stripAgtypeTypeTags_handlesEdgeAndPathTags() {
            String input = "[{\"label\": \"R\"}::edge, {\"length\": 2}::path]";
            assertThat(AgeGraphService.stripAgtypeTypeTags(input)).isEqualTo("[{\"label\": \"R\"}, {\"length\": 2}]");
        }
    }
}
