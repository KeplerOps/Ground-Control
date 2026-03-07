---
title: "Implement health endpoints, Prometheus metrics, and Grafana dashboards"
labels: [backend, observability, production]
phase: 11
priority: P0
---

## Description

Build comprehensive health checks, Prometheus metrics exposition, and Grafana dashboard templates.

## References

- Deployment: Section 8 (Monitoring & Observability)

## Acceptance Criteria

- [ ] Health endpoints:
  - `/health` — all dependencies (DB, Redis, S3, Search)
  - `/health/ready` — ready to serve traffic
  - `/health/live` — process is alive
- [ ] Prometheus metrics at `/metrics`:
  - `gc_api_requests_total{method, path, status}`
  - `gc_api_request_duration_seconds{method, path}`
  - `gc_active_users_total{tenant}`
  - `gc_artifacts_stored_bytes{tenant}`
  - `gc_assessments_active_total{tenant}`
  - `gc_agent_results_total{agent_id, status}`
  - `gc_background_jobs_total{queue, status}`
  - `gc_background_job_duration_seconds{queue}`
- [ ] Grafana dashboard JSON templates:
  - API performance (request rate, latency, error rate)
  - Business metrics (active assessments, findings, evidence)
  - Infrastructure (DB connections, Redis, job queue)
- [ ] Alerting rules (Prometheus AlertManager format)
