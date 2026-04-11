# TLA+ Specs

This directory holds design-level specifications adopted by ADR-014.

Current use:

- model state machines and graph invariants before adding or changing L2 logic
- keep design-level verification artifacts versioned with code
- use scaffolds from `bin/scaffold-l2-state-machine` for new stateful workflows

TLC is not yet wired into CI. Until it is, treat specs here as required design artifacts for
L2 workflows and keep them in sync with the corresponding Java implementation and ADRs.
