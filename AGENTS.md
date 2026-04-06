# Agent Instructions

This repository uses Ground Control for requirements management and workflow automation.

## Ground Control Context

```yaml
ground_control:
  project: ground-control
```

Use this repo-local project identifier for Ground Control MCP calls when working in this repository.

## Workflow Notes

- Pass full requirement UIDs exactly as they exist in Ground Control.
- Do not synthesize or rewrite requirement prefixes.
- Run `make policy` before declaring repo work complete. This is the shared repo-native guardrail path for both Claude and Codex.
- When a reachable Ground Control instance is available, run `make sync-ground-control-policy` after changing repo policy or ADR workflow surfaces, then `make policy-live`.
- Do not rely on agent-specific user-level hooks as the only enforcement layer. Keep repo-native checks, docs, and Ground Control policy in sync.
- See `docs/DEVELOPMENT_WORKFLOW.md` for the full `/implement` and `/ship` workflow.
