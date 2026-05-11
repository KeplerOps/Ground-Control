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

## Consequences

### Positive

- Keeps the SQL/Cypher trust boundary in one adapter instead of spreading escaping rules through controllers, services,
  or projection contributors.
- Preserves the existing `GraphProjection`, `GraphNode`, `GraphEdge`, and `GraphEntityType` model rather than creating a
  parallel AGE-specific graph schema.
- Keeps API error handling aligned with the existing `GlobalExceptionHandler` and domain exception hierarchy.

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
