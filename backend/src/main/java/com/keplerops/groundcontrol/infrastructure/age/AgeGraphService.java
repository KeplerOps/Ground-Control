package com.keplerops.groundcontrol.infrastructure.age;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * AGE adapter. Owns Cypher/SQL construction for the {@code ag_catalog.cypher(...)} surface.
 *
 * <p>Per ADR-032, every user-controlled value reaches AGE through one of two paths:
 *
 * <ul>
 *   <li>Cypher parameters bound via the third argument of {@code cypher(graph, query, params)} —
 *       used for all string/JSON-shaped values (UIDs, project identifiers, free-form properties
 *       like requirement titles and statements).
 *   <li>Allowlist-validated identifiers — used only for tokens AGE cannot parameterize: graph
 *       names, node labels, relationship types, and Cypher property keys.
 * </ul>
 *
 * <p>String concatenation of user-supplied data into the SQL or Cypher text is not allowed. The
 * {@link #SAFE_IDENTIFIER} allowlist and {@link #SAFE_PARAM_NAME} allowlist are defense in depth;
 * the primary mitigation is parameter binding.
 *
 * <p>Class-level {@link Transactional} pins every public AGE call to a single connection. AGE's
 * {@code LOAD 'age'} and {@code SET search_path} commands are connection-local, so without a
 * transaction those settings could land on one pooled connection and the subsequent
 * {@code ag_catalog.cypher(...)} call could land on another — failing with "function
 * ag_catalog.cypher does not exist" or returning empty results.
 */
@Component
@Transactional
public class AgeGraphService implements GraphClient, MixedGraphClient {

    private static final Logger log = LoggerFactory.getLogger(AgeGraphService.class);
    private static final java.util.regex.Pattern SAFE_IDENTIFIER = java.util.regex.Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final java.util.regex.Pattern SAFE_PARAM_NAME =
            java.util.regex.Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    // Jackson mapper used for both the agtype params payload and for parsing AGE rows back. We
    // explicitly disable WRITE_DATES_AS_TIMESTAMPS so that Instant / LocalDate properties are
    // bound as ISO-8601 strings (matching the previous String.format-based path that called
    // instant.toString()). Without this, JavaTimeModule would default to Jackson's
    // big-decimal-seconds shape, and getVisualization() would silently emit numeric timestamps
    // when AGE is enabled while still emitting ISO strings when AGE is disabled — a
    // configuration-dependent API regression.
    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    /**
     * Maximum allowed traversal depth for {@link #getAncestors}/{@link #getDescendants}/{@link
     * #findPaths}. AGE 1.6 cannot parameterize variable-length-path bounds, so we enforce a hard
     * cap before constructing the cypher; otherwise a caller could request {@code depth=10_000}
     * and trigger an unbounded graph expansion. Cap chosen to fit any realistic requirement
     * dependency tree while keeping latency bounded.
     */
    static final int MAX_GRAPH_TRAVERSAL_DEPTH = 20;

    /**
     * Result-row cap on {@link #findPaths}. Even with a depth bound, a dense or cyclic graph
     * can produce an exponential number of distinct paths between two requirements; this cap
     * keeps the response size bounded and the per-call latency predictable.
     */
    static final int MAX_FIND_PATHS_RESULTS = 50;

    /** Maximum allowed length for a UID arriving at the AGE adapter (matches the column width). */
    static final int MAX_UID_LENGTH = 50;

    /**
     * Approved AGE property keys. Per ADR-032, dynamic Cypher tokens (labels, relationship
     * names, property keys) must come from a fixed allowlist — not just satisfy a syntactic
     * pattern — so that a future {@link com.keplerops.groundcontrol.domain.graph.service
     * .GraphProjectionContributor} cannot silently grow the AGE schema by emitting unknown
     * property keys. The set covers both the implicit keys this adapter writes
     * ({@code id}, {@code domain_id}, {@code entity_type}, etc.) and every key currently
     * emitted by {@code *GraphProjectionContributor} implementations. New contributors must
     * add their keys here and ship a regression test; that's intentional friction.
     */
    public static final Set<String> APPROVED_PROPERTY_KEYS = Set.of(
            // Adapter-emitted (set in executeCreateNode / executeCreateEdge).
            "id",
            "domain_id",
            "entity_type",
            "project_identifier",
            "uid",
            "label",
            "edge_type",
            "source_id",
            "target_id",
            "source_entity_type",
            "target_entity_type",
            // Requirement projection.
            "title",
            "statement",
            "priority",
            "status",
            "requirementType",
            "wave",
            "archivedAt",
            "createdAt",
            "createdBy",
            "sourceUid",
            "targetUid",
            // Asset projection.
            "name",
            "description",
            "assetType",
            "assetScopeSummary",
            "owner",
            "categoryTags",
            "expiresAt",
            "observedAt",
            // Observation projection.
            "narrative",
            "observationDate",
            "observationKey",
            "observationValue",
            "evidenceRef",
            "analystIdentity",
            "confidence",
            // Risk-scenario projection.
            "category",
            "threatSource",
            "threatEvent",
            "vulnerability",
            "consequence",
            "affectedObject",
            "stride",
            "timeHorizon",
            "effect",
            "property",
            "strategy",
            // Treatment / control / verification.
            "result",
            "verifiedAt",
            "prover",
            "source",
            "reviewCadence",
            "nextReviewAt",
            "dueDate",
            "assessmentAt",
            "assuranceLevel",
            // Methodology profile.
            "family",
            "version");
    // AGE's ag_catalog.cypher() function takes cstring/cstring/agtype. Its first two arguments
    // are parsed at SQL parse time by AGE's parser hook, so they cannot be JDBC bind parameters
    // — they must be SQL literals. The third argument (params agtype) is the user-data carrier
    // and IS bound through JDBC, so all user-controlled values (UIDs, project identifiers,
    // free-form requirement properties) flow exclusively through the bound params payload and
    // are referenced from the cypher template via $paramName (AGE-internal substitution).
    //
    // Graph names and cypher templates are constructed solely from allowlisted identifiers
    // (graph name, entity-type labels, edge-type labels, property keys — all validated by
    // validateGraphName) and Cypher template syntax. No user value ever reaches the SQL string.

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
        executeCypher(graph, "MATCH (n) DETACH DELETE n", "{}");

        var projection = graphProjectionRegistryService.buildProjection();
        for (GraphNode node : projection.nodes()) {
            executeCreateNode(graph, node);
        }
        for (GraphEdge edge : projection.edges()) {
            executeCreateEdge(graph, edge);
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
        validateDepth(depth);
        String projectIdentifier = getProjectIdentifier(projectId);
        String graph = validateGraphName(ageProperties.graphName());
        setupSearchPath();

        // PARENT edges are materialized source→target and the domain convention is
        // child→parent (RequirementRelation(child, parent, PARENT)), so ancestors of n are
        // reachable by following OUTGOING PARENT edges from n. The depth bound is an int
        // already validated against MAX_GRAPH_TRAVERSAL_DEPTH; AGE 1.6 does not parameterize
        // variable-length-path bounds, but integer formatting is not injectable.
        String cypher = "MATCH (n:REQUIREMENT {uid: $uid, project_identifier: $project_identifier})"
                + "-[:PARENT*1.." + depth + "]->(a:REQUIREMENT {project_identifier: $project_identifier}) "
                + "RETURN a.uid";
        String params = encodeParams(Map.of("uid", uid, "project_identifier", projectIdentifier));

        return queryUids(graph, cypher, params);
    }

    @Override
    public List<String> getDescendants(UUID projectId, String uid, int depth) {
        if (!ageProperties.enabled()) {
            return List.of();
        }
        validateUid(uid);
        validateDepth(depth);
        String projectIdentifier = getProjectIdentifier(projectId);
        String graph = validateGraphName(ageProperties.graphName());
        setupSearchPath();

        // Inverse of getAncestors: descendants of n are reachable by following INCOMING PARENT
        // edges to n.
        String cypher = "MATCH (n:REQUIREMENT {uid: $uid, project_identifier: $project_identifier})"
                + "<-[:PARENT*1.." + depth + "]-(d:REQUIREMENT {project_identifier: $project_identifier}) "
                + "RETURN d.uid";
        String params = encodeParams(Map.of("uid", uid, "project_identifier", projectIdentifier));

        return queryUids(graph, cypher, params);
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

        // findPaths has no depth in its API contract, so we hard-cap variable-length traversal
        // at MAX_GRAPH_TRAVERSAL_DEPTH and apply a result LIMIT INSIDE the Cypher block to
        // bound the work AGE itself does (the outer SQL LIMIT only truncates rows after AGE
        // has materialized them, which doesn't bound expansion on a cyclic graph).
        //
        // Returning `nodes(path)` and `relationships(path)` directly — rather than through a
        // list comprehension like `[n IN nodes(path) | n.uid]` — works around an AGE 1.6 plan
        // error ("could not find properties for n") triggered when the planner mixes path
        // accessors with subsequent property lookups. We iterate the returned vertex/edge
        // arrays in Java and pull the UID/label from each element.
        String cypher = "MATCH path = (s:REQUIREMENT {uid: $source_uid, project_identifier: $project_identifier})"
                + "-[*1.." + MAX_GRAPH_TRAVERSAL_DEPTH
                + "]->(t:REQUIREMENT {uid: $target_uid, project_identifier: $project_identifier}) "
                + "RETURN nodes(path), relationships(path) LIMIT " + MAX_FIND_PATHS_RESULTS;
        String params = encodeParams(Map.of(
                "source_uid", sourceUid,
                "target_uid", targetUid,
                "project_identifier", projectIdentifier));

        List<PathResult> paths = new ArrayList<>();
        jdbcTemplate.query(buildCypherPathSql(graph, cypher), bindAgtypeParams(params), (RowCallbackHandler) rs -> {
            List<String> nodeUids = extractPathNodeUids(rs.getString(1));
            List<String> edgeLabels = extractPathEdgeLabels(rs.getString(2));
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
        String nodeCypher = "MATCH (n {project_identifier: $project_identifier}) RETURN properties(n)";
        String nodeParams = encodeParams(Map.of("project_identifier", projectIdentifier));
        jdbcTemplate.query(buildCypherSql(graph, nodeCypher), bindAgtypeParams(nodeParams), (RowCallbackHandler)
                rs -> nodes.add(toGraphNode(parseAgtypeMap(rs.getString(1)))));

        List<GraphEdge> edges = new ArrayList<>();
        String edgeCypher = "MATCH (s {project_identifier: $project_identifier})"
                + "-[r]->(t {project_identifier: $project_identifier}) RETURN properties(r)";
        String edgeParams = encodeParams(Map.of("project_identifier", projectIdentifier));
        jdbcTemplate.query(buildCypherSql(graph, edgeCypher), bindAgtypeParams(edgeParams), (RowCallbackHandler)
                rs -> edges.add(toGraphEdge(parseAgtypeMap(rs.getString(1)))));

        return new GraphProjection(nodes, edges);
    }

    private void executeCreateNode(String graph, GraphNode node) {
        String label = validateGraphName(node.entityType().name());
        Map<String, Object> nodeProps = new LinkedHashMap<>();
        nodeProps.put("id", node.id());
        nodeProps.put("domain_id", node.domainId());
        nodeProps.put("entity_type", node.entityType().name());
        nodeProps.put("project_identifier", node.projectIdentifier());
        nodeProps.put("uid", node.uid());
        nodeProps.put("label", node.label());
        nodeProps.putAll(node.properties());

        ParamBuilder builder = new ParamBuilder("p_");
        String propClause = renderPropertyClause(nodeProps, builder);
        String cypher = "CREATE (:" + label + " " + propClause + ")";
        executeCypher(graph, cypher, builder.toJson());
    }

    private void executeCreateEdge(String graph, GraphEdge edge) {
        String edgeType = validateGraphName(edge.edgeType());
        Map<String, Object> edgeProps = new LinkedHashMap<>();
        edgeProps.put("id", edge.id());
        edgeProps.put("edge_type", edge.edgeType());
        edgeProps.put("source_id", edge.sourceId());
        edgeProps.put("target_id", edge.targetId());
        edgeProps.put("source_entity_type", edge.sourceEntityType().name());
        edgeProps.put("target_entity_type", edge.targetEntityType().name());
        edgeProps.putAll(edge.properties());

        ParamBuilder builder = new ParamBuilder("p_");
        builder.put("source_id", edge.sourceId());
        builder.put("target_id", edge.targetId());
        // Re-use the same builder so all values share the single agtype params payload.
        ParamBuilder propBuilder = new ParamBuilder(builder, "pp_");
        String propClause = renderPropertyClause(edgeProps, propBuilder);
        String cypher = "MATCH (s {id: $p_source_id}), (t {id: $p_target_id}) " + "CREATE (s)-[:" + edgeType + " "
                + propClause + "]->(t)";
        executeCypher(graph, cypher, propBuilder.toJson());
    }

    /**
     * Build the SQL for a single-column-cypher() call. Graph name and cypher template are
     * concatenated as SQL literals — both come from constants and allowlisted identifiers, never
     * from user input. The third {@code agtype} parameter (carrying every user value) is bound
     * via JDBC as a bare {@code ?} placeholder, with the parameter typed as {@code agtype} via
     * a {@link PGobject} (AGE rejects {@code ?::agtype} SQL casts because its third-arg check
     * requires a bare {@code Param} parser node, not a wrapping {@code TypeCast}).
     */
    private static String buildCypherSql(String graph, String cypher) {
        return "SELECT * FROM ag_catalog.cypher('" + graph + "', $gc$" + cypher + "$gc$, ?) AS (v agtype)";
    }

    /** Same as {@link #buildCypherSql} but for path queries that return two agtype columns. */
    private static String buildCypherPathSql(String graph, String cypher) {
        return "SELECT * FROM ag_catalog.cypher('" + graph + "', $gc$" + cypher
                + "$gc$, ?) AS (nodes agtype, rels agtype)";
    }

    private void executeCypher(String graph, String cypher, String params) {
        // ag_catalog.cypher(...) always returns a SETOF agtype, even for write statements like
        // DETACH DELETE or CREATE. JdbcTemplate.update() rejects that with "A result was returned
        // when none was expected"; route through query() with a no-op handler instead.
        jdbcTemplate.query(buildCypherSql(graph, cypher), bindAgtypeParams(params), (RowCallbackHandler) rs -> {});
    }

    private List<String> queryUids(String graph, String cypher, String params) {
        List<String> results = new ArrayList<>();
        jdbcTemplate.query(buildCypherSql(graph, cypher), bindAgtypeParams(params), (RowCallbackHandler)
                rs -> results.add(stringValue(parseAgtypeValue(rs.getString(1)))));
        return results;
    }

    /**
     * Bind {@code paramsJson} as a single positional parameter typed as the AGE {@code agtype}
     * pseudotype. We can't use a SQL-level {@code ?::agtype} cast because AGE checks that the
     * third argument of {@code ag_catalog.cypher(...)} is a bare {@code Param} parser node;
     * wrapping it in a {@code TypeCast} fails that check with "third argument of cypher function
     * must be a parameter". Setting the parameter type via {@link PGobject} ensures PostgreSQL
     * knows the type at PREPARE time without rewriting the SQL.
     */
    private static PreparedStatementSetter bindAgtypeParams(String paramsJson) {
        return ps -> {
            PGobject obj = new PGobject();
            try {
                obj.setType("agtype");
                obj.setValue(paramsJson);
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to bind agtype parameter", e);
            }
            ps.setObject(1, obj);
        };
    }

    /**
     * Render a Cypher property map clause where every value is bound through {@code builder}
     * rather than inlined as a Cypher literal. Property keys are validated against the strict
     * Cypher-property-key grammar (no hyphens, no leading digits) — AGE does not parameterize
     * property keys, so they must be safe SQL/Cypher tokens.
     */
    private static String renderPropertyClause(Map<String, Object> properties, ParamBuilder builder) {
        StringBuilder out = new StringBuilder("{");
        boolean first = true;
        for (var entry : properties.entrySet()) {
            String key = validateCypherKey(entry.getKey());
            String paramName = builder.put(entry.getValue());
            if (!first) {
                out.append(", ");
            }
            first = false;
            out.append(key).append(": $").append(paramName);
        }
        out.append("}");
        return out.toString();
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

    /**
     * AGE returns vertex objects with a top-level {@code properties} map; pull the {@code uid}
     * out of each element of an agtype list. Used by {@link #findPaths} to extract the UID
     * sequence from a path's {@code nodes(path)} return.
     */
    @SuppressWarnings("unchecked")
    private static List<String> extractPathNodeUids(String agtypeValue) {
        Object parsed = parseAgtypeValue(agtypeValue);
        if (!(parsed instanceof List<?> values)) {
            return List.of();
        }
        List<String> uids = new ArrayList<>(values.size());
        for (Object element : values) {
            if (element instanceof Map<?, ?> vertex) {
                Object props = vertex.get("properties");
                if (props instanceof Map<?, ?> propMap) {
                    Object uid = propMap.get("uid");
                    if (uid != null) {
                        uids.add(uid.toString());
                    }
                }
            }
        }
        return uids;
    }

    /**
     * AGE edge objects carry the relationship label at the top level. Pull each label from
     * {@code relationships(path)}.
     */
    @SuppressWarnings("unchecked")
    private static List<String> extractPathEdgeLabels(String agtypeValue) {
        Object parsed = parseAgtypeValue(agtypeValue);
        if (!(parsed instanceof List<?> values)) {
            return List.of();
        }
        List<String> labels = new ArrayList<>(values.size());
        for (Object element : values) {
            if (element instanceof Map<?, ?> edge) {
                Object label = edge.get("label");
                if (label != null) {
                    labels.add(label.toString());
                }
            }
        }
        return labels;
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
            return OBJECT_MAPPER.readValue(stripAgtypeTypeTags(agtypeValue), new TypeReference<>() {});
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to parse AGE agtype value: " + agtypeValue, exception);
        }
    }

    /**
     * AGE serializes vertex/edge/path values with a {@code ::vertex} / {@code ::edge} /
     * {@code ::path} type tag suffix that standard JSON parsers do not understand. Strip those
     * tags so the surrounding object literal parses as plain JSON.
     *
     * <p>A naive {@code String.replace("}::vertex", "}")} would also corrupt user-controlled
     * string values (a requirement title containing the literal sequence {@code }::vertex}
     * would lose its tag suffix). This walker tracks JSON-string state — including {@code \"}
     * escapes — and only rewrites type tags that appear in structural positions outside any
     * quoted string. Package-private for unit-test verification.
     */
    public static String stripAgtypeTypeTags(String agtypeValue) {
        StringBuilder out = new StringBuilder(agtypeValue.length());
        boolean inString = false;
        boolean escape = false;
        int i = 0;
        while (i < agtypeValue.length()) {
            char c = agtypeValue.charAt(i);
            if (inString) {
                out.append(c);
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                i++;
                continue;
            }
            if (c == '"') {
                inString = true;
                out.append(c);
                i++;
                continue;
            }
            if (c == '}' && matchesTypeTagAt(agtypeValue, i + 1)) {
                out.append('}');
                i = i + 1 + lengthOfTypeTagAt(agtypeValue, i + 1);
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static boolean matchesTypeTagAt(String s, int pos) {
        return s.startsWith("::vertex", pos) || s.startsWith("::edge", pos) || s.startsWith("::path", pos);
    }

    private static int lengthOfTypeTagAt(String s, int pos) {
        if (s.startsWith("::vertex", pos)) {
            return "::vertex".length();
        }
        if (s.startsWith("::edge", pos)) {
            return "::edge".length();
        }
        return "::path".length();
    }

    private static String encodeParams(Map<String, Object> params) {
        try {
            return OBJECT_MAPPER.writeValueAsString(params);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to encode AGE Cypher params", exception);
        }
    }

    private String getProjectIdentifier(UUID projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId))
                .getIdentifier();
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Validate a token that will be embedded in the SQL/Cypher text as an identifier (graph
     * name, node label, edge type). Allows alphanumerics plus `_` and `-` because some entity
     * type names use hyphens. Identifiers reach AGE as part of a SQL literal — they cannot be
     * parameter-bound — so the allowlist is a hard requirement, not defense in depth.
     */
    private static String validateGraphName(String name) {
        if (name == null || !SAFE_IDENTIFIER.matcher(name).matches()) {
            throw new DomainValidationException("Invalid graph identifier: " + name);
        }
        return name;
    }

    /**
     * Validate a Cypher property key. Two layers: (1) {@link #APPROVED_PROPERTY_KEYS} —
     * per ADR-032, keys must come from a fixed allowlist, not just match a syntactic pattern,
     * so contributors can't silently grow the AGE schema; (2) {@link #SAFE_PARAM_NAME} —
     * defense-in-depth syntactic check that the registry entry itself is a safe Cypher
     * identifier (no hyphens, no leading digits). The registry check is the primary contract.
     */
    private static String validateCypherKey(String key) {
        if (key == null || !APPROVED_PROPERTY_KEYS.contains(key)) {
            throw new DomainValidationException("Cypher property key not in approved AGE schema registry: " + key);
        }
        if (!SAFE_PARAM_NAME.matcher(key).matches()) {
            throw new DomainValidationException("Invalid Cypher property key syntax: " + key);
        }
        return key;
    }

    /**
     * Validate a requirement UID arriving at the AGE adapter. UIDs are now bound through the
     * agtype params payload, so injection is structurally impossible — this validator only
     * enforces the operational bounds the rest of the domain enforces (length matches the
     * {@code requirement.uid} column width; no control characters that would corrupt logs or
     * confuse downstream tooling). It deliberately does NOT enforce the
     * {@link #SAFE_IDENTIFIER} grammar because importers (StrictDoc, ReqIF) accept richer UID
     * shapes and persisted requirements with such UIDs must remain queryable.
     */
    private static void validateUid(String uid) {
        if (uid == null || uid.isBlank()) {
            throw new DomainValidationException("Invalid UID for graph query: blank or null");
        }
        if (uid.length() > MAX_UID_LENGTH) {
            throw new DomainValidationException(
                    "Invalid UID for graph query: length " + uid.length() + " exceeds " + MAX_UID_LENGTH);
        }
        for (int i = 0; i < uid.length(); i++) {
            char c = uid.charAt(i);
            if (Character.isISOControl(c)) {
                throw new DomainValidationException("Invalid UID for graph query: contains control character");
            }
        }
    }

    private static void validateDepth(int depth) {
        if (depth < 1 || depth > MAX_GRAPH_TRAVERSAL_DEPTH) {
            throw new DomainValidationException(
                    "Invalid graph traversal depth: " + depth + " (must be 1.." + MAX_GRAPH_TRAVERSAL_DEPTH + ")");
        }
    }

    /**
     * Builder that emits unique Cypher parameter names AND collects the values into a single
     * agtype JSON payload. Parameter names are generated positionally (e.g., {@code p_0},
     * {@code p_1}) so they're decoupled from the property keys themselves. This means a future
     * graph contributor adding a hyphenated property key cannot accidentally produce an
     * invalid Cypher parameter name; the property-key validator catches the bad key separately.
     */
    private static final class ParamBuilder {
        private final Map<String, Object> values;
        private final String prefix;

        ParamBuilder(String prefix) {
            this.values = new LinkedHashMap<>();
            this.prefix = prefix;
        }

        ParamBuilder(ParamBuilder shared, String prefix) {
            this.values = shared.values;
            this.prefix = prefix;
        }

        String put(Object value) {
            String name = prefix + values.size();
            values.put(name, value);
            return name;
        }

        /**
         * Reserve a fixed-name parameter (used when the cypher template references a known
         * name like {@code $p_source_id}). The full parameter name is {@code prefix +
         * reservedName} and must satisfy {@link #SAFE_PARAM_NAME}.
         */
        String put(String reservedName, Object value) {
            String fullName = prefix + reservedName;
            if (!SAFE_PARAM_NAME.matcher(fullName).matches()) {
                throw new DomainValidationException("Invalid reserved Cypher parameter name: " + fullName);
            }
            values.put(fullName, value);
            return fullName;
        }

        String toJson() {
            return encodeParams(values);
        }
    }
}
