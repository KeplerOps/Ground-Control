# ADR-022: Content Pack Distribution Architecture

## Status

Accepted

## Date

2026-04-05

## Context

Ground Control now has a general plugin architecture requirement (GC-P005) and new draft requirements for requirements packs (GC-P014), control packs (GC-P015), and pack registry / trust semantics (GC-P016). Those requirements capture the need, but without an architectural decision the likely implementation path remains fragmented.

Today the system supports:

- Ad hoc content import paths such as StrictDoc and ReqIF
- Domain-specific seeding in limited cases such as methodology profiles
- First-class requirement and control entities, but no portable packaging model for distributing curated content between repositories or organizations

That is not enough for reusable repo bootstrapping. A portable content model needs more than "import some records":

- A pack must have stable identity, versioning, provenance, and compatibility semantics
- Pack installation must be idempotent and auditable
- Local projects must be able to tailor imported content without copying the entire upstream baseline
- Trust policy must distinguish curated packs from arbitrary external imports

If requirements packs and control packs are treated as one-off import formats, every content type will reinvent installation, upgrade, provenance, and override behavior separately.

## Decision

Adopt a three-layer content pack architecture for installable reusable content.

### 1. Shared Pack Substrate

Ground Control will treat installable packs as first-class distribution units with common metadata and lifecycle semantics:

- Pack identity and publisher
- Semantic version
- Compatibility constraints
- Dependency declarations
- Provenance metadata
- Integrity material such as checksums or signatures
- Auditable installation records

This shared substrate is the architectural realization of GC-P016 and is the foundation for any installable content pack type.

### 2. Typed Content Packs

Content packs remain type-specific above the shared substrate rather than being flattened into one generic blob format.

**Requirements packs** package:

- documents
- sections
- requirements
- requirement relations
- pack metadata

This is the architectural realization of GC-P014.

**Control packs** package:

- control definitions
- control metadata
- framework mappings
- implementation guidance
- expected evidence patterns
- pack metadata

This is the architectural realization of GC-P015 and extends the GC-I001 control catalog model.

Separate pack types are intentional. Requirements and controls have different structure, lifecycle, and downstream analysis semantics even though they share installation and trust concerns.

### 3. Install / Upgrade / Tailor Model

Pack installation is not modeled as a raw import. Instead, Ground Control will use pack-aware operations with these semantics:

- **Idempotent install**: reapplying the same pack version does not duplicate content
- **Version-aware upgrade**: moving between pack versions preserves source identity and install history
- **Provenance preservation**: installed content retains source pack and version metadata
- **Local tailoring**: projects may extend or override installed content without forking the originating pack wholesale

Local tailoring is constrained to project-local overlays, not mutation of the upstream pack definition. This preserves upgradeability and makes divergence explicit.

### 4. Plugin Boundary

The plugin architecture (GC-P005) provides the extension boundary for new pack handlers, registry backends, validators, and install-time policy hooks. Pack distribution is therefore built on the plugin substrate, but content packs are a higher-level abstraction than generic plugins.

In other words:

- Plugins extend the platform's executable behavior
- Content packs distribute reusable domain content through the platform

These are related but not interchangeable concerns.

## Consequences

**Positive:**

- Requirements packs and control packs share one trust and installation model instead of duplicating infrastructure
- Portable curated content becomes a first-class capability rather than a side effect of import formats
- Local repo or organization customization remains compatible with upstream pack evolution
- Provenance and auditability become intrinsic to reusable content distribution

**Negative:**

- Adds architectural surface area before the first concrete pack type is implemented
- Requires explicit decisions about override precedence, conflict handling, and upgrade reconciliation

**Risks:**

- If local overrides are too unconstrained, upgrades will become unsafe and pack lineage will blur
- If the shared substrate is over-generalized, pack handlers may collapse into an opaque import mechanism with weak domain semantics
- Trust policy that is too weak turns packs into a supply-chain liability; trust policy that is too rigid makes curated content difficult to adopt

## Related Requirements

- GC-P005 Plugin Architecture
- GC-P014 Requirements Pack Distribution and Installation
- GC-P015 Control Pack Distribution and Installation
- GC-P016 Pack Registry, Resolution, and Trust Model
- GC-I001 Control Catalog
