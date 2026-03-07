---
title: "Implement background job processing"
labels: [backend, infrastructure, events]
phase: 5
priority: P0
---

## Description

Set up background job processing for async tasks: report generation, email delivery, evidence collection, search indexing, overdue detection.

## References

- Architecture: Section 7 (ARQ or Celery)
- Deployment: Section 2.3 (worker service)

## Acceptance Criteria

- [ ] Job framework setup (ARQ recommended — lightweight, Redis-backed, async-native):
  - Worker process: `gc-worker` command
  - Job registration and discovery
  - Retry with exponential backoff
  - Dead letter queue for failed jobs
  - Job status tracking
- [ ] Job types:
  - `send_email` — email notifications
  - `generate_report` — PDF/PPTX generation
  - `sync_search_index` — update Meilisearch
  - `check_overdue` — find overdue evidence requests, treatments, remediations
  - `collect_evidence` — run evidence collection plugins
- [ ] Scheduled jobs (cron-like):
  - Overdue check: every hour
  - Search index sync: every 5 minutes (or event-driven)
- [ ] Job monitoring: queue depth, processing time, failure rate (Prometheus metrics)
- [ ] Graceful shutdown: finish current job before stopping
- [ ] Unit tests with mock Redis
