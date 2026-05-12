# ADR-032: AGE Query Construction Boundary

## Status

Accepted

## Date

2026-05-09

## Context

Apache AGE queries are built inside the `infrastructure/age` adapter, but the graph projection data includes
requirement fields and request parameters that originate at API boundaries. AGE calls wrap Cypher in SQL
(`ag_catalog.cypher(...)`), so escaping only Cypher string contents is not a complete security control: values can
also affect the surrounding SQL literal and AGE graph argument.

ADR-005 already limits the product to application-owned Cypher and no user-supplied query language. Issue 244 clarifies
that the implementation must also make query construction itself a security boundary.

## Decision

AGE SQL/Cypher construction belongs inside the AGE infrastructure adapter. Callers pass typed domain or API values
through the existing `GraphClient`, `MixedGraphClient`, and graph projection contracts; they must not assemble SQL,
Cypher fragments, labels, relationship types, or property maps for AGE directly.

The AGE adapter must prefer Spring JDBC parameter binding for SQL arguments and projected values. Any dynamic Cypher
tokens that AGE cannot parameterize, such as labels, relationship names, property keys, or graph identifiers, must come
from fixed configuration, enums, or explicit allowlists and must be rejected before query execution when they do not
match that contract.

Validation at controllers or DTOs remains useful for request quality, but it is defense in depth. It must not be the
only injection mitigation, because materialization also consumes persisted requirement data and projection contributors.

Graph traversal cost is part of the same boundary. Any API or domain service that accepts a caller-controlled graph
depth, root set, entity-type filter, or path query must validate bounded inputs before invoking a graph client, and the
AGE adapter must keep its own defensive caps before constructing variable-length Cypher patterns. Path queries must not
use unbounded `[*]` expansions; they need an explicit maximum depth and an in-Cypher result limit so AGE does not
materialize an unbounded path set before Java truncates the response. Limits should live behind the existing graph
service/client contracts and domain validation envelope, not in endpoint-specific ad hoc checks.

## Consequences

### Positive

- Keeps the SQL/Cypher trust boundary in one adapter instead of spreading escaping rules through controllers, services,
  or projection contributors.
- Preserves the existing `GraphProjection`, `GraphNode`, `GraphEdge`, and `GraphEntityType` model rather than creating a
  parallel AGE-specific graph schema.
- Keeps API error handling aligned with the existing `GlobalExceptionHandler` and domain exception hierarchy.
- Keeps graph DoS protections attached to the graph contracts themselves, so browser/session callers, bearer callers,
  MCP/tool callers, and future graph-analysis endpoints inherit the same bounded traversal behavior.

### Negative

- AGE's `ag_catalog.cypher(...)` interface does not make every Cypher token parameterizable, so the adapter must keep a
  small, well-tested allowlist path for identifiers and relationship labels.

### Risks

- Treating a custom escape helper as equivalent to parameter binding can reintroduce SQL-dollar-quote or Cypher
  delimiter bugs.
- Rejecting unsafe data only at new write endpoints can miss existing persisted data, imports, or future projection
  contributors; the adapter must validate before execution as well.
- Broadening the requirement UID grammar as part of this fix can create product and import compatibility changes that
  are separate from the injection repair.
- Capping only HTTP request DTOs is insufficient: internal callers can still reach the graph service/client ports, and
  AGE cannot parameterize variable-length path bounds, so adapter-side depth checks remain mandatory.
- Applying `LIMIT` outside `ag_catalog.cypher(...)` does not bound AGE path expansion. The limit must be inside the
  Cypher text for path enumeration queries.
