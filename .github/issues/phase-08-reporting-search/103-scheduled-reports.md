---
title: "Implement scheduled report delivery and GraphQL API"
labels: [backend, reporting, api]
phase: 8
priority: P1
---

## Description

Build scheduled report delivery (email on configurable schedule) and the optional GraphQL API for complex relational queries.

## References

- PRD: Section 4.6 (Scheduled Reports — email delivery)
- Architecture: Section 3.3 (GraphQL Server — Strawberry)
- API Spec: Section 5 (GraphQL API)
- User Stories: US-6.2 (schedule saved reports), US-6.3 (API-Driven Analytics)

## Acceptance Criteria

- [ ] Scheduled reports:
  - Cron-like schedule configuration per saved report
  - Email delivery with attached report (PDF/Excel)
  - Background job: `scheduled_report_delivery`
  - Admin UI to manage schedules
- [ ] GraphQL API at `POST /graphql`:
  - Strawberry-based schema
  - Queries for: risks, controls, assessments, findings (with nested relations)
  - Risk dashboard query (heat map, top risks, trends)
  - Authentication/authorization via same JWT
  - Rate limiting
- [ ] GraphQL schema auto-generated from domain models
- [ ] Integration tests for both features
