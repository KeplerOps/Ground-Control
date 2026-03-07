# ADR-004: Plugin Architecture for Extensibility

## Status

Accepted

## Date

2026-03-07

## Context

Ground Control must support diverse compliance frameworks (SOX, SOC 2, ISO 27001, NIST, PCI-DSS, HIPAA, etc.), integrations (Jira, ServiceNow, Slack), and custom workflows. Hardcoding all of these into the core would make the codebase unwieldy and force all users to carry code they don't need.

## Decision

Implement a plugin architecture where frameworks, integrations, evidence collectors, and custom workflows are plugins.

- Plugins are Python packages with a declared manifest (name, version, permissions, hooks)
- Plugin runtime provides process-level sandboxing
- Plugins interact with core via a Plugin SDK (scoped API access, event hooks, UI extension points)
- Built-in framework definitions (SOX ITGC, SOC 2) ship as plugins, not core code
- Plugin registry tracks installed plugins, versions, and signatures

Plugin types: Framework, Integration, Evidence Collector, Workflow, Report, Agent.

## Consequences

### Positive

- Core stays lean and focused on domain logic
- New frameworks can be added without core changes
- Community and customers can build custom plugins
- Each plugin declares its permission scope — principle of least privilege

### Negative

- Plugin API is a public contract — breaking changes require careful versioning
- Sandboxing adds runtime overhead
- Plugin quality varies — need review/signing process for trust

### Risks

- Plugin SDK must be designed carefully upfront — changing it later breaks the ecosystem
- Security: malicious plugins could exfiltrate data (mitigated: sandboxing + permission scopes + audit logging)
