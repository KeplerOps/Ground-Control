---
title: "Configure MCP servers for AI-assisted development (Rocq, AWS, ops)"
labels: [foundation, devex, tooling]
phase: 0
priority: P0
---

## Description

Set up Model Context Protocol (MCP) servers that accelerate AI-assisted development. These give the AI developer (Claude) direct tool access to proof assistants, cloud infrastructure, and operational tooling — reducing hallucination and token waste on tasks outside core training data.

## References

- Coding Standards: Section 11 (Formal Methods — Coq/Rocq proofs in `proofs/`)
- Issue #023b (Formal proof infrastructure)
- Architecture: Section 5 (Deployment Architecture — AWS, Docker Compose, Kubernetes)
- Architecture: Section 3.6 (Data & Storage — PostgreSQL, Redis, S3)

## MCP Servers

### 1. rocq-mcp (Rocq/Coq Proof Assistant)

**Purpose:** Direct interaction with the Rocq/Coq proof assistant via coq-lsp and Pytanque. Enables type checking, tactic execution, and proof feedback without guessing syntax.

**Source:** https://github.com/LLM4Rocq/rocq-mcp

**Setup:**
```bash
# Install coq-lsp
opam install lwt logs coq-lsp

# Install rocq-mcp
pip install git+https://github.com/LLM4Rocq/rocq-mcp.git
```

**Used for:** All work in `proofs/` — audit log integrity, RBAC policy correctness, state machine transitions, tenant isolation proofs.

### 2. AWS MCP (Official)

**Purpose:** Authenticated access to AWS APIs for infrastructure management, deployment, and evidence collection plugin development.

**Source:** https://github.com/awslabs/mcp

**Used for:** Deployment to AWS (ECS/EKS, RDS, S3, ElastiCache), CloudWatch log access, Secrets Manager, infrastructure validation.

### 3. Ground Control Ops MCP (Custom, later phase)

**Purpose:** Project-specific operational tooling — local Docker Compose management, database migrations, health checks, log tailing, test data seeding. Modeled on the Shifter ops MCP pattern.

**Deferred to:** Phase 11 (Production Readiness), when the application is running and needs operational tooling. Skeleton can be created earlier if useful.

**Planned tools:**
- `gc_health` — Check health of all services (app, db, redis, search)
- `gc_migrate` — Run/rollback Alembic migrations
- `gc_logs` — Tail application logs with structured filtering
- `gc_seed` — Seed test data for development
- `gc_db_query` — Read-only SQL queries against local dev database
- `gc_docker` — Start/stop/restart Docker Compose services

## Acceptance Criteria

- [ ] rocq-mcp installed and configured in Claude Code MCP settings
- [ ] Verify rocq-mcp works: create a trivial Coq file in `proofs/`, type-check it via MCP
- [ ] AWS MCP configured (requires AWS credentials — document setup in README)
- [ ] `proofs/README.md` includes MCP setup instructions for contributors
- [ ] Claude Code `.claude/settings.json` or project config documents MCP server configurations
- [ ] Issue #023b updated to note rocq-mcp as the development interface for proofs

## Notes

- rocq-mcp is the highest priority — formal proofs are the area most prone to AI errors without tool feedback.
- AWS MCP requires IAM credentials. For local dev, use AWS SSO profiles. Document the required permissions.
- The custom ops MCP is deferred but the pattern is proven (see Shifter ops MCP). Create it when there's an app to operate.
- MCP servers already available in the dev environment (GitHub, Playwright, Serena, Context7, DigitalOcean, Neo4j, Figma) do not need additional setup.
