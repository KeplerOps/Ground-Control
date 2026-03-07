---
title: "Implement risk scoring engine with pluggable models"
labels: [backend, risk-management, domain-logic]
phase: 4
priority: P0
---

## Description

Build the risk scoring engine that calculates inherent and residual risk scores using configurable scoring methodologies. The engine must be pluggable to support different models (matrix, FAIR quantitative, custom).

## References

- PRD: Section 4.1 (Risk Scoring Engine — pluggable, 5x5 default, FAIR via plugin)
- User Stories: US-1.2 (update likelihood/impact scores with justification)
- Data Model: Section 2.4 (inherent/residual scores)

## Acceptance Criteria

- [ ] Scoring engine with strategy pattern:
  - `ScoringStrategy` protocol/interface
  - `MatrixScoringStrategy` — default 5x5 matrix (likelihood × impact)
  - `WeightedScoringStrategy` — weighted multi-factor scoring
  - Factory: `get_scoring_strategy(tenant_settings) → ScoringStrategy`
- [ ] Score calculation includes:
  - Inherent score (without controls)
  - Residual score (after control effectiveness considered)
  - Score delta and trend (compared to prior assessment)
- [ ] Risk appetite evaluation: flag risks exceeding threshold
- [ ] Heat map data generation from scored risks
- [ ] Contracts (formally verified with CrossHair/deal):
  - `@icontract.ensure(lambda result: 1 <= result.score <= 25)` (for 5x5)
  - Score is monotonically related to inputs
  - Residual ≤ inherent (when controls are effective)
- [ ] Hypothesis property-based tests for scoring edge cases
- [ ] Unit tests for all scoring strategies
