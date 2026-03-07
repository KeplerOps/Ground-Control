---
title: "Create Kubernetes Helm chart"
labels: [deployment, kubernetes, production]
phase: 11
priority: P0
---

## Description

Create a production-grade Helm chart for deploying Ground Control on Kubernetes.

## References

- Deployment: Section 3 (Kubernetes Deployment — Helm)

## Acceptance Criteria

- [ ] `deploy/helm/ground-control/` Helm chart:
  - Deployments: gc-app (N replicas), gc-worker
  - Services: ClusterIP for app
  - Ingress with TLS
  - ConfigMaps and Secrets
  - PVCs for persistence
  - HPA (autoscaling)
  - PodDisruptionBudget
  - NetworkPolicy
  - ServiceMonitor (Prometheus)
- [ ] Sub-charts or dependency charts: PostgreSQL, Redis, MinIO, Meilisearch
- [ ] External database/redis support (for managed services)
- [ ] Migration Job (pre-upgrade hook)
- [ ] values.yaml with comprehensive documentation
- [ ] Helm chart tests (`helm test`)
- [ ] Chart published to GitHub Pages or OCI registry
