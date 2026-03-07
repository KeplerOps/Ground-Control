---
title: "Security hardening and penetration test remediation"
labels: [security, production, quality]
phase: 11
priority: P0
---

## Description

Implement the security hardening checklist from the deployment guide and remediate findings from SAST/DAST/OpenANT scanning.

## References

- Deployment: Section 9 (Security Hardening Checklist)
- Architecture: Section 6 (Security Architecture)

## Acceptance Criteria

- [ ] All items from security hardening checklist verified:
  - All default secrets changed
  - TLS on all external endpoints
  - Database encryption at rest
  - Network policies / security groups
  - Audit logging forwarding to SIEM
  - SSO configured, local login disabled (if applicable)
  - MFA enabled for local accounts
  - API rate limits configured
  - Artifact encryption keys set
  - DB SSL connections
  - Backup encryption
  - CSP and HSTS headers
  - Plugin permissions reviewed
- [ ] SAST findings (Semgrep, Bandit) at zero High/Critical
- [ ] DAST findings (ZAP) at zero High
- [ ] Dependency vulnerabilities at zero High/Critical
- [ ] OpenANT verified findings addressed
- [ ] Security documentation for operators
