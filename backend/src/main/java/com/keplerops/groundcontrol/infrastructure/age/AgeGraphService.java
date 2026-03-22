package com.keplerops.groundcontrol.infrastructure.age;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.service.GraphClient;
import com.keplerops.groundcontrol.domain.requirements.service.PathResult;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AgeGraphService implements GraphClient {

    private static final Logger log = LoggerFactory.getLogger(AgeGraphService.class);

    /**
     * Maximum allowed traversal depth to prevent DoS via expensive graph queries.
     * Requests with depth > MAX_DEPTH are rejected with IllegalArgumentException.
     */
    static final int MAX_DEPTH = 20;

    private final JdbcTemplate jdbcTemplate;
    private final AgeProperties ageProperties;
    private final RequirementRepository requirementRepository;
    private final RequirementRelationRepository relationRepository;

    public AgeGraphService(
            JdbcTemplate jdbcTemplate,
            AgeProperties ageProperties,
            RequirementRepository requirementRepository,
            RequirementRelationRepository relationRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.ageProperties = ageProperties;
        this.requirementRepository = requirementRepository;
        this.relationRepository = relationRepository;
    }

    @Override
    public void materializeGraph() {
        if (!ageProperties.enabled()) {
            log.debug("graph_materialization_skipped: reason=disabled");
            return;
        }

        String graph = ageProperties.graphName();
        jdbcTemplate.execute("LOAD 'age'");
        jdbcTemplate.execute("SET search_path = ag_catalog, \"$user\", public");

        // Clear existing graph data
        jdbcTemplate.execute(
                "SELECT * FROM ag_catalog.cypher('" + graph + "', $$ MATCH (n) DETACH DELETE n $$) AS (v agtype)");

        // Create nodes for all requirements.
        // AGE does not support parameterized CREATE with multiple properties via the agtype params
        // argument in a single call, so property values are interpolated using escapeCypher().
        // Data originates from the database (not direct user input), reducing but not eliminating risk.
        // See: https://age.apache.org/age-manual/master/intro/cypher_queries.html
        List<Requirement> requirements = requirementRepository.findAll();
        for (Requirement req : requirements) {
            String cypher = String.format(
                    "SELECT * FROM ag_catalog.cypher('%s', $$ CREATE (:Requirement {uid: '%s', title: '%s', status: '%s', wave: %s, requirement_type: '%s', priority: '%s'}) $$) AS (v agtype)",
                    graph,
                    escapeCypher(req.getUid()),
                    escapeCypher(req.getTitle()),
                    req.getStatus().name(),
                    req.getWave() != null ? req.getWave().toString() : "null",
                    req.getRequirementType().name(),
                    req.getPriority().name());
            jdbcTemplate.execute(cypher);
        }

        // Create edges for all relations
        List<RequirementRelation> relations = relationRepository.findAllWithSourceAndTarget();
        for (RequirementRelation rel : relations) {
            String cypher = String.format(
                    "SELECT * FROM ag_catalog.cypher('%s', $$ MATCH (s:Requirement {uid: '%s'}), (t:Requirement {uid: '%s'}) CREATE (s)-[:%s]->(t) $$) AS (v agtype)",
                    graph,
                    escapeCypher(rel.getSource().getUid()),
                    escapeCypher(rel.getTarget().getUid()),
                    rel.getRelationType().name());
            jdbcTemplate.execute(cypher);
        }

        log.info("graph_materialized: nodes={} edges={} graph={}", requirements.size(), relations.size(), graph);
    }

    @Override
    public List<String> getAncestors(String uid, int depth) {
        if (!ageProperties.enabled()) {
            return List.of();
        }
        validateDepth(depth);

        String graph = ageProperties.graphName();
        setupSearchPath();

        // Graph name comes from server config (trusted). UID is passed via AGE agtype params to
        // prevent Cypher injection from user-supplied values.
        String sql =
                "SELECT * FROM ag_catalog.cypher(?, $$ MATCH (n:Requirement {uid: $uid})<-[:PARENT*1.."
                        + depth + "]-(a) RETURN a.uid $$, ?::agtype) AS (v agtype)";
        return extractUidResults(sql, graph, ageParams("uid", uid));
    }

    @Override
    public List<String> getDescendants(String uid, int depth) {
        if (!ageProperties.enabled()) {
            return List.of();
        }
        validateDepth(depth);

        String graph = ageProperties.graphName();
        setupSearchPath();

        String sql =
                "SELECT * FROM ag_catalog.cypher(?, $$ MATCH (n:Requirement {uid: $uid})-[:PARENT*1.."
                        + depth + "]->(d) RETURN d.uid $$, ?::agtype) AS (v agtype)";
        return extractUidResults(sql, graph, ageParams("uid", uid));
    }

    @Override
    public List<PathResult> findPaths(String sourceUid, String targetUid) {
        if (!ageProperties.enabled()) {
            return List.of();
        }

        String graph = ageProperties.graphName();
        setupSearchPath();

        String sql =
                "SELECT * FROM ag_catalog.cypher(?, $$ MATCH path = (s:Requirement {uid: $sourceUid})-[*]->(t:Requirement {uid: $targetUid}) RETURN [n IN nodes(path) | n.uid], [r IN relationships(path) | label(r)] $$, ?::agtype) AS (nodes agtype, rels agtype)";
        String params = "{\"sourceUid\": \"" + escapeJson(sourceUid) + "\", \"targetUid\": \"" + escapeJson(targetUid) + "\"}";

        List<PathResult> paths = new ArrayList<>();
        jdbcTemplate.query(sql, rs -> {
            List<String> nodeUids = parseAgtypeArray(rs.getString(1));
            List<String> edgeLabels = parseAgtypeArray(rs.getString(2));
            paths.add(new PathResult(nodeUids, edgeLabels));
        }, graph, params);
        return paths;
    }

    private void setupSearchPath() {
        jdbcTemplate.execute("LOAD 'age'");
        jdbcTemplate.execute("SET search_path = ag_catalog, \"$user\", public");
    }

    private List<String> extractUidResults(String sql, String graph, String params) {
        List<String> results = new ArrayList<>();
        jdbcTemplate.query(sql, rs -> {
            String agtypeValue = rs.getString(1);
            // agtype string values are returned as "value" (with quotes)
            String uid = agtypeValue.replaceAll("^\"|\"$", "");
            results.add(uid);
        }, graph, params);
        return results;
    }

    private static List<String> parseAgtypeArray(String agtypeValue) {
        // agtype arrays look like: ["uid1", "uid2", "uid3"]
        List<String> uids = new ArrayList<>();
        String stripped = agtypeValue.replaceAll("[\\[\\]]", "");
        for (String part : stripped.split(",", -1)) {
            String trimmed = part.trim().replaceAll("^\"|\"$", "");
            if (!trimmed.isEmpty()) {
                uids.add(trimmed);
            }
        }
        return uids;
    }

    /**
     * Escapes a string for interpolation into a Cypher string literal.
     * Used only for materializeGraph() where AGE does not support multi-property parameterization.
     * Data flowing through this path originates from the database, not direct user input.
     */
    static String escapeCypher(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\0", "")       // strip null bytes
                .replace("`", "\\`");    // backtick used for identifier escaping in Cypher
    }

    /**
     * Escapes a string value for embedding in a JSON string literal.
     * Used when building AGE agtype parameter maps.
     */
    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\0", "");      // strip null bytes
    }

    /**
     * Builds an AGE agtype parameter map JSON string with a single string key/value pair.
     */
    private static String ageParams(String key, String value) {
        return "{\"" + escapeJson(key) + "\": \"" + escapeJson(value) + "\"}";
    }

    /**
     * Validates that depth is within the allowed range [1, MAX_DEPTH].
     * Prevents DoS via unbounded graph traversals.
     */
    private static void validateDepth(int depth) {
        if (depth < 1 || depth > MAX_DEPTH) {
            throw new IllegalArgumentException(
                    "depth must be between 1 and " + MAX_DEPTH + ", got: " + depth);
        }
    }
}
