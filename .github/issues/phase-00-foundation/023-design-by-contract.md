---
title: "Set up design-by-contract and partial formal verification"
labels: [foundation, backend, quality, verification]
phase: 0
priority: P1
---

## Description

Establish a partial formal verification strategy using design-by-contract (DbC) decorators and SMT-based verification tools. This provides stronger correctness guarantees than testing alone for critical business logic like risk scoring, permission checks, and state machine transitions.

**Tools:**
- **icontract** — Runtime contract checking (preconditions, postconditions, invariants) with informative violation messages
- **CrossHair** — SMT solver-based concolic testing that finds counterexamples to contracts
- **Hypothesis** — Property-based testing with CrossHair as optional backend
- **deal** — Alternative DbC library with Z3-based formal verification support

## References

- PRD: Section 7 (Non-Functional Requirements — correctness, security)
- Architecture: Section 3.4 (Domain Services — business logic)
- [CrossHair docs](https://crosshair.readthedocs.io/)
- [icontract GitHub](https://github.com/Parquery/icontract)
- [deal docs](https://deal.readthedocs.io/)

## Acceptance Criteria

- [ ] Dependencies added: `icontract`, `crosshair-tool`, `hypothesis`, `deal`
- [ ] `backend/src/ground_control/contracts/` module:
  - `invariants.py` — shared invariants (e.g., score ranges, valid states)
  - `README.md` — guide on when and how to use contracts
- [ ] Contract patterns documented and applied to critical areas:
  - **Risk scoring:** `@icontract.require(lambda likelihood: 1 <= likelihood <= 5)`
  - **State machines:** `@icontract.require(lambda old, new: new in VALID_TRANSITIONS[old])`
  - **Permission checks:** `@icontract.ensure(lambda result: result.tenant_id == current_tenant_id)`
  - **Audit log:** `@icontract.ensure(lambda result: result.entry_hash is not None)`
- [ ] CrossHair integration:
  - `crosshair watch backend/src/ground_control/domain/` for IDE integration
  - `make verify` target runs CrossHair on annotated modules
  - CrossHair configured in `pyproject.toml` or `setup.cfg`
- [ ] Hypothesis property-based tests:
  - Test strategies for domain objects (risks, controls, etc.)
  - `@given` tests for scoring, state transitions, permission logic
  - CrossHair backend enabled for Hypothesis where applicable
- [ ] ADR: `architecture/adrs/010-design-by-contract.md` documenting:
  - Which code gets contracts (domain logic, NOT infrastructure)
  - Performance impact and production toggle strategy
  - Contract enforcement: development=strict, production=configurable

## Technical Notes

- Contracts add runtime overhead — use `ICONTRACT_SLOW` env var to disable in production or use `icontract.SLOW` for expensive checks
- CrossHair is best at finding counterexamples (proving something CAN fail) rather than proving correctness
- Focus contracts on pure functions and domain logic — avoid on I/O-heavy code
- icontract integrates with CrossHair and FastAPI (via `fastapi-icontract`)
- deal's Z3 solver can formally verify simple functions — use for critical math (scoring formulas)
- Property-based tests with Hypothesis complement contracts by generating edge cases
