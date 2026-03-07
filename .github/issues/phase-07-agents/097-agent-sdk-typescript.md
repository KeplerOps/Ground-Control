---
title: "Build Agent SDK — TypeScript package"
labels: [sdk, agents, typescript]
phase: 7
priority: P1
---

## Description

Build the TypeScript Agent SDK as an npm package (`@ground-control/agent-sdk`).

## References

- API Spec: Section 9.2 (TypeScript Agent SDK)
- PRD: Section 10 (v0.4 — Agent SDK TypeScript)

## Acceptance Criteria

- [ ] Package: `sdks/typescript/` with `package.json`, `tsconfig.json`
- [ ] Published as: `@ground-control/agent-sdk`
- [ ] `AgentClient` class matching Python SDK functionality
- [ ] Full TypeScript types for all request/response payloads
- [ ] Works in Node.js (ESM and CJS)
- [ ] Unit tests (vitest)
- [ ] README with examples
