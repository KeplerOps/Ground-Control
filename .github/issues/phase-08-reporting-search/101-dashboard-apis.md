---
title: "Implement dashboard data aggregation APIs"
labels: [backend, reporting, api]
phase: 8
priority: P0
---

## Description

Build the dashboard data APIs that power executive dashboards with pre-aggregated data.

## References

- API Spec: Section 4.12 (Reports — dashboard endpoints)
- User Stories: US-1.5 (View Risk Dashboard), US-6.1 (Generate Executive Reports)

## Acceptance Criteria

- [ ] Dashboard endpoints:
  - `GET /api/v1/reports/dashboards/risk-posture` — heat map data, top risks, trends, appetite breaches
  - `GET /api/v1/reports/dashboards/control-health` — effectiveness distribution, test coverage, aging
  - `GET /api/v1/reports/dashboards/assessment-progress` — campaign status, completion %, overdue
  - `GET /api/v1/reports/dashboards/findings-summary` — open findings, severity distribution, aging
- [ ] All dashboards filterable by: business_unit, date range, framework
- [ ] Response caching (Redis, 60s TTL) for performance
- [ ] Heat map data: likelihood × impact matrix with risk counts and IDs
- [ ] Trend data: score changes over last 4 quarters
- [ ] Unit tests
