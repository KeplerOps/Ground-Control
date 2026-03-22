package com.keplerops.groundcontrol.infrastructure.age;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.service.GraphClient;
import com.keplerops.groundcontrol.domain.requirements.service.PathResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AgeGraphService implements GraphClient {

    private static final Logger log = LoggerFactory.getLogger(AgeGraphService.class);

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

        // Batch-create nodes for all requirements in a single Cypher statement
        List<Requirement> requirements = requirementRepository.findAll();
        if (!requirements.isEmpty()) {
            StringBuilder nodesCypher = new StringBuilder("SELECT * FROM ag_catalog.cypher('")
                    .append(graph)
                    .append("', $$ CREATE ");
            for (int i = 0; i < requirements.size(); i++) {
                if (i > 0) nodesCypher.append(", ");
                Requirement req = requirements.get(i);
                nodesCypher
                        .append("(:Requirement {uid: '")
                        .append(escapeCypher(req.getUid()))
                        .append("', title: '")
                        .append(escapeCypher(req.getTitle()))
                        .append("', status: '")
                        .append(req.getStatus().name())
                        .append("', wave: ")
                        .append(req.getWave() != null ? req.getWave().toString() : "null")
                        .append(", requirement_type: '")
                        .append(req.getRequirementType().name())
                        .append("', priority: '")
                        .append(req.getPriority().name())
                        .append("'})");
            }
            nodesCypher.append(" $$) AS (v agtype)");
            jdbcTemplate.execute(nodesCypher.toString());
        }

        // Batch-create edges grouped by relation type: one UNWIND query per type
        List<RequirementRelation> relations = relationRepository.findAllWithSourceAndTarget();
        Map<String, List<RequirementRelation>> byType = relations.stream()
                .collect(Collectors.groupingBy(r -> r.getRelationType().name()));
        for (Map.Entry<String, List<RequirementRelation>> entry : byType.entrySet()) {
            String relType = entry.getKey();
            StringBuilder pairList = new StringBuilder("[");
            List<RequirementRelation> relList = entry.getValue();
            for (int i = 0; i < relList.size(); i++) {
                if (i > 0) pairList.append(", ");
                RequirementRelation rel = relList.get(i);
                pairList
                        .append("['")
                        .append(escapeCypher(rel.getSource().getUid()))
                        .append("', '")
                        .append(escapeCypher(rel.getTarget().getUid()))
                        .append("']");
            }
            pairList.append("]");
            String edgeCypher = String.format(
                    "SELECT * FROM ag_catalog.cypher('%s', $$ UNWIND %s AS pair MATCH (s:Requirement {uid: pair[0]}), (t:Requirement {uid: pair[1]}) CREATE (s)-[:%s]->(t) $$) AS (v agtype)",
                    graph, pairList, relType);
            jdbcTemplate.execute(edgeCypher);
        }

        log.info("graph_materialized: nodes={} edges={} graph={}", requirements.size(), relations.size(), graph);
    }

    @Override
    public List<String> getAncestors(String uid, int depth) {
        if (!ageProperties.enabled()) {
            return List.of();
        }

        String graph = ageProperties.graphName();
        setupSearchPath();

        String sql = String.format(
                "SELECT * FROM ag_catalog.cypher('%s', $$ MATCH (n:Requirement {uid: '%s'})<-[:PARENT*1..%d]-(a) RETURN a.uid $$) AS (v agtype)",
                graph, escapeCypher(uid), depth);

        return extractUidResults(sql);
    }

    @Override
    public List<String> getDescendants(String uid, int depth) {
        if (!ageProperties.enabled()) {
            return List.of();
        }

        String graph = ageProperties.graphName();
        setupSearchPath();

        String sql = String.format(
                "SELECT * FROM ag_catalog.cypher('%s', $$ MATCH (n:Requirement {uid: '%s'})-[:PARENT*1..%d]->(d) RETURN d.uid $$) AS (v agtype)",
                graph, escapeCypher(uid), depth);

        return extractUidResults(sql);
    }

    @Override
    public List<PathResult> findPaths(String sourceUid, String targetUid) {
        if (!ageProperties.enabled()) {
            return List.of();
        }

        String graph = ageProperties.graphName();
        setupSearchPath();

        String sql = String.format(
                "SELECT * FROM ag_catalog.cypher('%s', $$ MATCH path = (s:Requirement {uid: '%s'})-[*]->(t:Requirement {uid: '%s'}) RETURN [n IN nodes(path) | n.uid], [r IN relationships(path) | label(r)] $$) AS (nodes agtype, rels agtype)",
                graph, escapeCypher(sourceUid), escapeCypher(targetUid));

        List<PathResult> paths = new ArrayList<>();
        jdbcTemplate.query(sql, rs -> {
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
            String uid = agtypeValue.replaceAll("^\"|\"$", "");
            results.add(uid);
        });
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

    private static String escapeCypher(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
