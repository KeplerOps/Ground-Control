---
title: "Implement sampling methodology"
labels: [backend, assessment, domain-logic]
phase: 4
priority: P2
---

## Description

Build configurable sampling calculators for audit testing (statistical, judgmental, haphazard).

## References

- PRD: Section 4.3 (Sampling — configurable methodologies)
- Data Model: Section 2.9 (population_size, sample_size, sampling_method)

## Acceptance Criteria

- [ ] `SamplingCalculator` with strategies:
  - Statistical: confidence level + tolerable deviation → sample size
  - Judgmental: auditor-determined with justification
  - Haphazard: random selection from population
- [ ] Sample size recommendations based on population, frequency, and risk
- [ ] Random sample selection from population (seeded for reproducibility)
- [ ] Contracts: sample_size ≤ population_size, sample_size > 0
- [ ] Unit tests with known statistical tables
