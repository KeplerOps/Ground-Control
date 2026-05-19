package com.keplerops.groundcontrol.domain.graph;

/**
 * Canonical traversal-bound policy for graph endpoints and graph clients (per ADR-032).
 *
 * <p>Every API DTO, domain service, and infrastructure adapter that accepts caller-controlled graph
 * depth, root sets, entity-type filters, or path queries enforces these bounds. Holding them here
 * means DTO bean-validation, service-level defensive checks, and the AGE adapter's pre-Cypher caps
 * all reference the same numbers — there is no class of regression where one layer's literal drifts
 * away from another's. Mechanically: AGE 1.6 cannot parameter-bind variable-length path bounds, so
 * the adapter MUST cap depth before constructing {@code [*1..N]}; capping only at the HTTP DTO is
 * insufficient because internal callers can reach domain services directly.
 *
 * <p>If a future caller needs operator-tunable limits, replace the constants here with a
 * {@code @ConfigurationProperties} record bound in {@code GroundControlApplication}; until then,
 * static finals keep the policy unambiguous and free of runtime configuration risk.
 */
public final class GraphTraversalLimits {

    /**
     * Maximum traversal depth. Capped at 20 to fit any realistic requirement-dependency tree while
     * keeping AGE variable-length expansion bounded; the same cap applies to the in-memory mixed
     * graph BFS so behaviour is consistent across adapters.
     */
    public static final int MAX_DEPTH = 20;

    /** Maximum number of root nodes accepted in a neighborhood / traversal request. */
    public static final int MAX_ROOT_NODES = 50;

    /**
     * Maximum number of path results returned by enumerative path queries (AGE adapter). The
     * mixed-graph BFS returns at most one shortest path, so the cap is structurally unreachable
     * there, but it is shared so the AGE and mixed paths cannot drift apart.
     */
    public static final int MAX_PATH_RESULTS = 50;

    /** Maximum length of any entity-type filter list. */
    public static final int MAX_ENTITY_TYPE_FILTER = 32;

    /**
     * Maximum length of any caller-supplied node identifier (UUID strings, requirement UIDs, mixed
     * graph node ids of the form {@code ENTITY_TYPE:domain-id}). Wide enough to admit every shape
     * the codebase currently emits, narrow enough that an attacker cannot push megabytes of bound
     * data through a single field.
     */
    public static final int MAX_NODE_IDENTIFIER_LENGTH = 256;

    /**
     * Hard cap on the node count of any projection (visualization or neighborhood) returned by the
     * mixed graph surface. A materialized graph that exceeds this cap forces the caller to apply
     * an entity-type filter; the alternative — silent truncation — would hide the cap from
     * downstream consumers and make response sizes non-deterministic.
     */
    public static final int MAX_PROJECTION_NODES = 5_000;

    /** Hard cap on the edge count of any projection returned by the mixed graph surface. */
    public static final int MAX_PROJECTION_EDGES = 20_000;

    private GraphTraversalLimits() {}
}
