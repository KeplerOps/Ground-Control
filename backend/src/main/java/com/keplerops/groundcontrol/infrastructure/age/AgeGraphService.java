package com.keplerops.groundcontrol.infrastructure.age;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.domain.graph.model.GraphEdge;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphNode;
import com.keplerops.groundcontrol.domain.graph.model.GraphProjection;
import com.keplerops.groundcontrol.domain.graph.service.GraphProjectionRegistryService;
import com.keplerops.groundcontrol.domain.graph.service.MixedGraphClient;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import com.keplerops.groundcontrol.domain.requirements.service.GraphClient;
import com.keplerops.groundcontrol.domain.requirements.service.PathResult;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

@Component
public class AgeGraphService implements GraphClient, MixedGraphClient {

    private static final Logger log = LoggerFactory.getLogger(AgeGraphService.class);
    private static final java.util.regex.Pattern SAFE_IDENTIFIER = java.util.regex.Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private final JdbcTemplate jdbcTemplate;
    private final AgeProperties ageProperties;
    private final GraphProjectionRegistryService graphProjectionRegistryService;
    private final ProjectRepository projectRepository;

    public AgeGraphService(
            JdbcTemplate jdbcTemplate,
            AgeProperties ageProperties,
            GraphProjectionRegistryService graphProjectionRegistryService,
            ProjectRepository projectRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.ageProperties = ageProperties;
        this.graphProjectionRegistryService = graphProjectionRegistryService;
        this.projectRepository = projectRepository;
    }

    @Override
    public void materializeGraph() {
        if (!ageProperties.enabled()) {
            log.debug("graph_materialization_skipped: reason=disabled");
            return;
        }

        String graph = validateGraphName(ageProperties.graphName());
        setupSearchPath();
        jdbcTemplate.execute(
                "SELECT * FROM ag_catalog.cypher('" + graph + "', $gc$MATCH (n) DETACH DELETE n $gc$) AS (v agtype)");

        var projection = graphProjectionRegistryService.buildProjection();
        for (GraphNode node : projection.nodes()) {
            jdbcTemplate.execute(buildCreateNodeSql(graph, node));
        }
        for (GraphEdge edge : projection.edges()) {
            jdbcTemplate.execute(buildCreateEdgeSql(graph, edge));
        }

        log.info(
                "graph_materialized: nodes={} edges={} graph={}",
                projection.nodes().size(),
                projection.edges().size(),
                graph);
    }

    @Override
    public List<String> getAncestors(UUID projectId, String uid, int depth) {
        if (!ageProperties.enabled()) {
            return List.of();
        }
        validateUid(uid);
        String projectIdentifier = getProjectIdentifier(projectId);
        String graph = validateGraphName(ageProperties.graphName());
        setupSearchPath();

        String sql = String.format(
                "SELECT * FROM ag_catalog.cypher('%s', $gc$MATCH (n:REQUIREMENT {uid: '%s', project_identifier: '%s'})<-[:PARENT*1..%d]-(a:REQUIREMENT {project_identifier: '%s'}) RETURN a.uid $gc$) AS (v agtype)",
                graph, escapeCypher(uid), escapeCypher(projectIdentifier), depth, escapeCypher(projectIdentifier));

        return extractUidResults(sql);
    }

    @Override
    public List<String> getDescendants(UUID projectId, String uid, int depth) {
        if (!ageProperties.enabled()) {
            return List.of();
        }
        validateUid(uid);
        String projectIdentifier = getProjectIdentifier(projectId);
        String graph = validateGraphName(ageProperties.graphName());
        setupSearchPath();

        String sql = String.format(
                "SELECT * FROM ag_catalog.cypher('%s', $gc$MATCH (n:REQUIREMENT {uid: '%s', project_identifier: '%s'})-[:PARENT*1..%d]->(d:REQUIREMENT {project_identifier: '%s'}) RETURN d.uid $gc$) AS (v agtype)",
                graph, escapeCypher(uid), escapeCypher(projectIdentifier), depth, escapeCypher(projectIdentifier));

        return extractUidResults(sql);
    }

    @Override
    public List<PathResult> findPaths(UUID projectId, String sourceUid, String targetUid) {
        if (!ageProperties.enabled()) {
            return List.of();
        }
        validateUid(sourceUid);
        validateUid(targetUid);
        String projectIdentifier = getProjectIdentifier(projectId);
        String graph = validateGraphName(ageProperties.graphName());
        setupSearchPath();

        String sql = String.format(
                "SELECT * FROM ag_catalog.cypher('%s', $gc$MATCH path = (s:REQUIREMENT {uid: '%s', project_identifier: '%s'})-[*]->(t:REQUIREMENT {uid: '%s', project_identifier: '%s'}) RETURN [n IN nodes(path) | n.uid], [r IN relationships(path) | label(r)] $gc$) AS (nodes agtype, rels agtype)",
                graph,
                escapeCypher(sourceUid),
                escapeCypher(projectIdentifier),
                escapeCypher(targetUid),
                escapeCypher(projectIdentifier));

        List<PathResult> paths = new ArrayList<>();
        jdbcTemplate.query(sql, rs -> {
            List<String> nodeUids = parseAgtypeArray(rs.getString(1));
            List<String> edgeLabels = parseAgtypeArray(rs.getString(2));
            paths.add(new PathResult(nodeUids, edgeLabels));
        });
        return paths;
    }

    @Override
    public GraphProjection getVisualization(UUID projectId) {
        if (!ageProperties.enabled()) {
            return graphProjectionRegistryService.buildProjectionForProject(projectId);
        }
        String graph = validateGraphName(ageProperties.graphName());
        String projectIdentifier = getProjectIdentifier(projectId);
        setupSearchPath();

        List<GraphNode> nodes = new ArrayList<>();
        String nodeSql = String.format(
                "SELECT * FROM ag_catalog.cypher('%s', $gc$MATCH (n {project_identifier: '%s'}) RETURN properties(n) $gc$) AS (props agtype)",
                graph, escapeCypher(projectIdentifier));
        jdbcTemplate.query(nodeSql, (RowCallbackHandler) rs -> nodes.add(toGraphNode(parseAgtypeMap(rs.getString(1)))));

        List<GraphEdge> edges = new ArrayList<>();
        String edgeSql = String.format(
                "SELECT * FROM ag_catalog.cypher('%s', $gc$MATCH (s {project_identifier: '%s'})-[r]->(t {project_identifier: '%s'}) RETURN properties(r) $gc$) AS (props agtype)",
                graph, escapeCypher(projectIdentifier), escapeCypher(projectIdentifier));
        jdbcTemplate.query(edgeSql, (RowCallbackHandler) rs -> edges.add(toGraphEdge(parseAgtypeMap(rs.getString(1)))));

        return new GraphProjection(nodes, edges);
    }

    private String buildCreateNodeSql(String graph, GraphNode node) {
        String label = validateGraphName(node.entityType().name());
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", node.id());
        props.put("domain_id", node.domainId());
        props.put("entity_type", node.entityType().name());
        props.put("project_identifier", node.projectIdentifier());
        props.put("uid", node.uid());
        props.put("label", node.label());
        props.putAll(node.properties());
        return String.format(
                "SELECT * FROM ag_catalog.cypher('%s', $gc$CREATE (:%s %s) $gc$) AS (v agtype)",
                graph, label, toCypherMap(props));
    }

    private String buildCreateEdgeSql(String graph, GraphEdge edge) {
        String edgeType = validateGraphName(edge.edgeType());
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", edge.id());
        props.put("edge_type", edge.edgeType());
        props.put("source_id", edge.sourceId());
        props.put("target_id", edge.targetId());
        props.put("source_entity_type", edge.sourceEntityType().name());
        props.put("target_entity_type", edge.targetEntityType().name());
        props.putAll(edge.properties());
        return String.format(
                "SELECT * FROM ag_catalog.cypher('%s', $gc$MATCH (s {id: '%s'}), (t {id: '%s'}) CREATE (s)-[:%s %s]->(t) $gc$) AS (v agtype)",
                graph, escapeCypher(edge.sourceId()), escapeCypher(edge.targetId()), edgeType, toCypherMap(props));
    }

    private GraphNode toGraphNode(Map<String, Object> props) {
        String id = stringValue(props.remove("id"));
        String domainId = stringValue(props.remove("domain_id"));
        GraphEntityType entityType = GraphEntityType.valueOf(stringValue(props.remove("entity_type")));
        String projectIdentifier = stringValue(props.remove("project_identifier"));
        String uid = stringValue(props.remove("uid"));
        String label = stringValue(props.remove("label"));
        return new GraphNode(id, domainId, entityType, projectIdentifier, uid, label, props);
    }

    private GraphEdge toGraphEdge(Map<String, Object> props) {
        String id = stringValue(props.remove("id"));
        String edgeType = stringValue(props.remove("edge_type"));
        String sourceId = stringValue(props.remove("source_id"));
        String targetId = stringValue(props.remove("target_id"));
        GraphEntityType sourceEntityType = GraphEntityType.valueOf(stringValue(props.remove("source_entity_type")));
        GraphEntityType targetEntityType = GraphEntityType.valueOf(stringValue(props.remove("target_entity_type")));
        return new GraphEdge(id, edgeType, sourceId, targetId, sourceEntityType, targetEntityType, props);
    }

    private void setupSearchPath() {
        jdbcTemplate.execute("LOAD 'age'");
        jdbcTemplate.execute("SET search_path = ag_catalog, \"$user\", public");
    }

    private List<String> extractUidResults(String sql) {
        List<String> results = new ArrayList<>();
        jdbcTemplate.query(sql, (RowCallbackHandler) rs -> results.add(stringValue(parseAgtypeValue(rs.getString(1)))));
        return results;
    }

    private static List<String> parseAgtypeArray(String agtypeValue) {
        Object parsed = parseAgtypeValue(agtypeValue);
        if (!(parsed instanceof List<?> values)) {
            return List.of();
        }
        return values.stream().map(AgeGraphService::stringValue).toList();
    }

    private static Map<String, Object> parseAgtypeMap(String agtypeValue) {
        Object parsed = parseAgtypeValue(agtypeValue);
        if (parsed instanceof Map<?, ?> values) {
            Map<String, Object> map = new LinkedHashMap<>();
            values.forEach((key, value) -> map.put(String.valueOf(key), value));
            return map;
        }
        return Map.of();
    }

    private static Object parseAgtypeValue(String agtypeValue) {
        if (agtypeValue == null || agtypeValue.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(agtypeValue, new TypeReference<>() {});
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to parse AGE agtype value: " + agtypeValue, exception);
        }
    }

    private static String toCypherMap(Map<String, Object> value) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (var entry : value.entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append(validateGraphName(entry.getKey())).append(": ").append(toCypherValue(entry.getValue()));
        }
        builder.append("}");
        return builder.toString();
    }

    private static String toCypherValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return "'" + escapeCypher(stringValue) + "'";
        }
        if (value instanceof UUID uuid) {
            return "'" + escapeCypher(uuid.toString()) + "'";
        }
        if (value instanceof Instant instant) {
            return "'" + escapeCypher(instant.toString()) + "'";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Enum<?> enumValue) {
            return "'" + escapeCypher(enumValue.name()) + "'";
        }
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> converted = new LinkedHashMap<>();
            mapValue.forEach((key, innerValue) -> converted.put(String.valueOf(key), innerValue));
            return toCypherMap(converted);
        }
        if (value instanceof Collection<?> collection) {
            return "["
                    + collection.stream()
                            .map(AgeGraphService::toCypherValue)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("") + "]";
        }
        return "'" + escapeCypher(value.toString()) + "'";
    }

    private String getProjectIdentifier(UUID projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId))
                .getIdentifier();
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String escapeCypher(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("$", "\\u0024")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String validateGraphName(String name) {
        if (name == null || !SAFE_IDENTIFIER.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid graph identifier: " + name);
        }
        return name;
    }

    private static void validateUid(String uid) {
        if (uid == null || !SAFE_IDENTIFIER.matcher(uid).matches()) {
            throw new IllegalArgumentException("Invalid UID for graph query: " + uid);
        }
    }
}
