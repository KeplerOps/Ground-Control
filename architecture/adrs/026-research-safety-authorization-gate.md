# ADR-026: Safety and Authorization Gate for Adversarial Research

## Status

Accepted

## Date

2026-05-05

## Context

The research workflow (ADR-024) explicitly supports adversarial / purple-team
lab research — driving offensive techniques, detection rules, and defensive
posture changes against an asset topology. Examples include using a range
like the APTL purple-team lab (<https://github.com/Brad-Edwards/aptl>) or
similar in-house labs.

Adversarial work has consequences software-feature work does not:

- An offensive technique aimed at the wrong asset — even by accident — can
  damage production systems, violate contracts, or break laws.
- Authorization is real: a named party authorizes specific actions
  against specific scope for a specific window. "Approved by management"
  is not authorization.
- Blast radius matters even inside a lab. Lateral movement, identity
  abuse, and persistence techniques can leak outside the intended
  segment if isolation is not verified.
- Evidence handling rules vary by data classification.

Without a hard gate, the agent can be talked into running offensive
tooling on the basis of conversational consent. That is unacceptable for
a system that touches a lab range — let alone any system that an operator
might point at production.

## Decision

Introduce a **Safety / Authorization Preflight** phase as a hard gate in
the research workflow. The gate has the following structure.

### When the gate is required

The gate is mandatory when **any** of the following is true:

1. The charter mode is `ADVERSARIAL_LAB`.
2. The protocol references techniques in a configurable
   `high-blast-radius` list (default: lateral movement, credential
   theft, persistence implantation, destructive actions, exploitation
   of unpatched vulnerabilities, social engineering of real humans).
3. The protocol references third-party systems, regulated data, human
   subjects, or production assets.
4. The operator explicitly opts in via the skill argument
   `--require-safety-preflight`.

The gate is recommended for `EXPERIMENTAL` mode and skippable for pure
`LITERATURE` mode.

### Required preflight artifact

The Safety Preflight produces a checklist artifact (Document with
prescribed sections, or an ADR with prescribed sections, operator
choice). The artifact must include all of:

- **Authorizing party**: a named role and a named individual, dated.
  Generic statements are rejected by the preflight prompt.
- **Authorization basis**: contract, statement of work, internal policy
  reference, or research ethics approval — by name, with date.
- **Scope window**: explicit start and end of the authorization window.
- **In-scope assets**: enumerated list of `Asset` UIDs (or external
  identifiers when assets are not yet modeled). Wildcards are not
  allowed.
- **Out-of-scope assets / classes**: enumerated. Statements like "do not
  touch production" must be expressed as the asset list of production
  systems explicitly excluded, not as a free-text exhortation.
- **Blast-radius bound**: the worst credible outcome and how it is
  contained. Containment must reference concrete isolation artifacts
  (e.g., network segment, dedicated identities, snapshot rollback,
  air-gapped lab).
- **Data handling**: classification, retention, redaction rules for
  captured artifacts.
- **Abort conditions**: enumerated triggers that stop execution and the
  rollback procedure.
- **Sign-off**: dated sign-off by the authorizing party.

### Hard gate semantics

- The Execution phase tooling (the part of the `/research` skill or
  follow-up automation that actually runs the protocol) shall refuse to
  proceed when:
  - The charter mode requires the gate (RW-F050) and no Safety Preflight
    artifact is linked to the research question with
    `link_type=DOCUMENTS` and a recognizable identifier (matching
    `safety-preflight` in title or filename, or carrying an
    `ArtifactType.POLICY` link); or
  - The Safety Preflight artifact lacks any required section above; or
  - The Safety Preflight artifact's sign-off date is outside the scope
    window.
- "Refuse" means the helper that runs the execution prompt returns an
  error result describing the missing precondition. It does not
  fabricate a sign-off and does not "best-effort" past the gate.
- The skill must surface the missing precondition to the human for
  resolution. The agent does not author the sign-off itself.

### Codex preflight prompt

The Codex Safety Preflight prompt (`buildResearchSafetyPreflightPrompt`)
must:

- Reject vague authorization language and demand named roles and named
  assets.
- Demand concrete isolation references, not free-text reassurance.
- Enumerate the high-blast-radius technique list and call out any that
  appear in the protocol.
- Refuse to add or update the sign-off block on behalf of the operator.

### Persistence

The Safety Preflight artifact is a regular Document (preferred) or ADR
(when the decision is significant and lasting). It is linked to the
research question via TraceabilityLink as in ADR-025. Sign-off is
expressed as a status transition the human performs (Document review or
ADR `ACCEPTED`), not as a magic field set by the agent.

## Consequences

### Positive

- Adversarial research has the same level of explicit authorization
  bookkeeping as a real engagement, recorded in the same graph as the
  rest of the work.
- The gate is hard: the helper functions return errors when the
  artifact is missing or malformed, so a future skill edit cannot
  silently bypass it.
- No new entity type — the gate is enforced by helper logic over
  existing artifacts (Documents / ADRs / TraceabilityLinks /
  AssetLinks).

### Negative

- Adds friction to small lab experiments that an operator might feel
  do not need a full preflight. The mode selector lets pure
  `LITERATURE` work skip the gate, and `EXPERIMENTAL` is a soft
  recommendation rather than a hard requirement.
- Sign-off is an out-of-band human action; the workflow cannot be
  fully autonomous for adversarial work. This is intentional.

### Risks

- **Operator pressure to bypass.** A determined operator can edit a
  Document to add a fake sign-off. Mitigation: Document edits are
  audited, sign-off is a status transition with an actor field, and
  the cross-model peer review in ADR-024 examines the preflight
  artifact for credible authorization.
- **Drift in the high-blast-radius technique list.** As the lab toolkit
  grows the default list will be incomplete. Mitigation: the helper
  exposes the list as a configurable constant and the requirements doc
  records that the list is expected to evolve.
