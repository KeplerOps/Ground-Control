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
    private static final java.util.regex.Pattern SAFE_IDENTIFIER = java.util.regex.Pattern.compile("^[a-zA-Z0-9_-]+$");

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

        String graph = validateGraphName(ageProperties.graphName());
        jdbcTemplate.execute("LOAD 'age'");
        jdbcTemplate.execute("SET search_path = ag_catalog, \"$user\", public");

        // Clear existing graph data — graph name is validated against allowlist pattern
        jdbcTemplate.execute( // NOSONAR — AGE Cypher does not support prepared statements; graph name is validated
                "SELECT * FROM ag_catalog.cypher('" + graph + "', $gc$MATCH (n) DETACH DELETE n $gc$) AS (v agtype)");

        // Create nodes for all requirements
        List<Requirement> requirements = requirementRepository.findAll();
        for (Requirement req : requirements) {
            String cypher = String.format(
                    "SELECT * FROM ag_catalog.cypher('%s', $gc$CREATE (:Requirement {uid: '%s', title: '%s', status: '%s', wave: %s, requirement_type: '%s', priority: '%s'}) $gc$) AS (v agtype)",
                    graph,
                    escapeCypher(req.getUid()),
                    escapeCypher(req.getTitle()),
                    req.getStatus().name(),
                    req.getWave() != null ? req.getWave().toString() : "null",
                    req.getRequirementType().name(),
                    req.getPriority().name());
            jdbcTemplate.execute(cypher); // NOSONAR — AGE Cypher; values escaped via escapeCypher
        }

        // Create edges for all relations
        List<RequirementRelation> relations = relationRepository.findAllWithSourceAndTarget();
        for (RequirementRelation rel : relations) {
            String cypher = String.format(
                    "SELECT * FROM ag_catalog.cypher('%s', $gc$MATCH (s:Requirement {uid: '%s'}), (t:Requirement {uid: '%s'}) CREATE (s)-[:%s]->(t) $gc$) AS (v agtype)",
                    graph,
                    escapeCypher(rel.getSource().getUid()),
                    escapeCypher(rel.getTarget().getUid()),
                    rel.getRelationType().name());
            jdbcTemplate.execute(cypher); // NOSONAR — AGE Cypher; UIDs escaped via escapeCypher
        }

        log.info("graph_materialized: nodes={} edges={} graph={}", requirements.size(), relations.size(), graph);
    }

    @Override
    public List<String> getAncestors(String uid, int depth) {
        if (!ageProperties.enabled()) {
            return List.of();
        }
        validateUid(uid);

        String graph = validateGraphName(ageProperties.graphName());
        setupSearchPath();

        String sql = String.format(
                "SELECT * FROM ag_catalog.cypher('%s', $gc$MATCH (n:Requirement {uid: '%s'})<-[:PARENT*1..%d]-(a) RETURN a.uid $gc$) AS (v agtype)",
                graph, escapeCypher(uid), depth);

        return extractUidResults(sql);
    }

    @Override
    public List<String> getDescendants(String uid, int depth) {
        if (!ageProperties.enabled()) {
            return List.of();
        }
        validateUid(uid);

        String graph = validateGraphName(ageProperties.graphName());
        setupSearchPath();

        String sql = String.format(
                "SELECT * FROM ag_catalog.cypher('%s', $gc$MATCH (n:Requirement {uid: '%s'})-[:PARENT*1..%d]->(d) RETURN d.uid $gc$) AS (v agtype)",
                graph, escapeCypher(uid), depth);

        return extractUidResults(sql);
    }

    @Override
    public List<PathResult> findPaths(String sourceUid, String targetUid) {
        if (!ageProperties.enabled()) {
            return List.of();
        }
        validateUid(sourceUid);
        validateUid(targetUid);

        String graph = validateGraphName(ageProperties.graphName());
        setupSearchPath();

        String sql = String.format(
                "SELECT * FROM ag_catalog.cypher('%s', $gc$MATCH path = (s:Requirement {uid: '%s'})-[*]->(t:Requirement {uid: '%s'}) RETURN [n IN nodes(path) | n.uid], [r IN relationships(path) | label(r)] $gc$) AS (nodes agtype, rels agtype)",
                graph, escapeCypher(sourceUid), escapeCypher(targetUid));

        List<PathResult> paths = new ArrayList<>();
        jdbcTemplate.query(
                sql,
                rs -> { // NOSONAR — AGE Cypher; UIDs validated and escaped
                    List<String> nodeUids = parseAgtypeArray(rs.getString(1));
                    List<String> edgeLabels = parseAgtypeArray(rs.getString(2));
                    paths.add(new PathResult(nodeUids, edgeLabels));
                });
        return paths;
    }

    private void setupSearchPath() {
        jdbcTemplate.execute("LOAD 'age'");
        jdbcTemplate.execute("SET search_path = ag_catalog, \"$user\", public");
    }

    private List<String> extractUidResults(String sql) {
        List<String> results = new ArrayList<>();
        jdbcTemplate.query(sql, rs -> {
            String agtypeValue = rs.getString(1);
            // agtype string values are returned as "value" (with quotes)
            String uid = agtypeValue.replaceAll("(^\")|(\"$)", "");
            results.add(uid);
        });
        return results;
    }

    private static List<String> parseAgtypeArray(String agtypeValue) {
        // agtype arrays look like: ["uid1", "uid2", "uid3"]
        List<String> uids = new ArrayList<>();
        String stripped = agtypeValue.replaceAll("[\\[\\]]", "");
        for (String part : stripped.split(",", -1)) {
            String trimmed = part.trim().replaceAll("(^\")|(\"$)", "");
            if (!trimmed.isEmpty()) {
                uids.add(trimmed);
            }
        }
        return uids;
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
            throw new IllegalArgumentException("Invalid graph name: " + name);
        }
        return name;
    }

    private static void validateUid(String uid) {
        if (uid == null || !SAFE_IDENTIFIER.matcher(uid).matches()) {
            throw new IllegalArgumentException("Invalid UID for graph query: " + uid);
        }
    }
}
