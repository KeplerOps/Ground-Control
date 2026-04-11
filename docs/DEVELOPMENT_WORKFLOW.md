# Development Workflow

This documents the automated development workflow using Claude Code with the `/implement` skill. The workflow takes a Ground Control requirement from plan through PR-ready with a single skill invocation.

## Prerequisites

### GPG Signing
- GPG key `B47C8B1F62CC2B54` has no passphrase (removed 2026-03-31)
- Commits are signed non-interactively by Claude Code
- Global deny rules and blocking hooks were removed to enable this

### OpenTelemetry Observability
- OTEL collector runs as a Docker container at `~/.claude/telemetry/`
- Config: `~/.claude/telemetry/otel-collector-config.yaml`
- Compose: `~/.claude/telemetry/docker-compose.yml`
- Output: `~/.claude/telemetry/data/claude-code.jsonl`
- Rotation: 100MB max, 90-day retention, 10 backups
- Start: `cd ~/.claude/telemetry && docker compose up -d`
- Analyze: `~/.claude/telemetry/claude-metrics`

Env vars in `~/.claude/settings.json`:
```
OTEL_LOGS_EXPORTER=otlp
OTEL_LOG_TOOL_DETAILS=1
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
```

### Codex CLI
- OpenAI Codex CLI (`codex-cli`) installed at `~/.nvm/versions/node/v25.8.1/bin/codex`
- Used for architecture preflight and cross-model code review via Ground Control MCP workflow tools

## Workflow: `/implement <requirement-uid>`

Pass the full requirement UID exactly as it exists in Ground Control. Do not synthesize or rewrite a project prefix.

Repo-local Ground Control project context should come from the current repository's `AGENTS.md`, not from hardcoded assumptions in the skill. The workflow validates this via `gc_get_repo_ground_control_context` before it starts implementation. It should:
- use the repo's configured Ground Control `project` when present
- treat inputs like `OBS-001`, `DSL-101`, `API-412`, or `GC-J001` as already-complete UIDs
- avoid guessing a prefix from the repository name

Recommended `AGENTS.md` convention:

## Ground Control Context

```yaml
ground_control:
  project: aces-sdl
```

### User Touchpoints
1. **Plan approval** — user reviews and approves the implementation plan
2. **PR review and merge** — user reviews the final PR and merges to dev

Everything between these two checkpoints is automated.

### Phase A: Plan & Implement
1. Resolve repo-local Ground Control context via `gc_get_repo_ground_control_context`
2. Fetch requirement from Ground Control, create GitHub issue if needed
3. Checkout feature branch via `gh issue develop`
4. Read the GitHub issue
5. **Codex architecture preflight** — via `gc_codex_architecture_preflight`, including ADR/design guidance updates when needed
6. Explore codebase for existing coverage using the preflight guardrails
7. **Enter plan mode — user approves**
8. Implement, clause-by-clause verification
9. Create traceability links (IMPLEMENTS, TESTS), transition to ACTIVE

### Phase B: Quality Gate
10. Run `pre-commit run --all-files`
11. Completion gate: `make policy`, `make check`, CHANGELOG, traceability, requirement status

### Phase C: Stage, Commit, Push
12. Stage files, pre-commit loop (fix failures, max 5 iterations)
13. Commit (no attribution) and push

### Phase D: Ship
14. Create PR to dev
15. Monitor CI (`gh run watch`)
16. Check SonarCloud quality gate
17. **Codex review** — cross-model review by ChatGPT via `gc_codex_review`
18. **Code review** — Claude built-in `/review` skill via Skill tool
19. **Security review** — Claude built-in `/security-review` skill via Skill tool
20. Fix ALL findings from steps 17-19, commit, push, re-run CI
21. **Report to user** — PR URL, summary of findings/fixes, CI/SonarCloud status

Claude does NOT merge. The user reviews the PR and merges.

## Review Pipeline

The workflow now has one mandatory pre-implementation architecture pass and three reviewers before the user sees the PR.

| Stage | What it catches | How it runs |
|-------|-----------------|-------------|
| Codex architecture preflight | Cross-cutting concerns, reuse opportunities, abstraction/concept confusion, need for ADR/design guidance before coding | `gc_codex_architecture_preflight` |
| SonarCloud | Coverage, code smells, duplication, security hotspots | CI job, quality gate must pass |
| Codex (ChatGPT-5.4) review | Design, abstractions, concept conflation, maintainability, reliability, security, consistency | `gc_codex_review` |
| `/review` (Claude built-in) | Code quality, conventions, correctness, performance | `Skill("review")` |
| `/security-review` (Claude built-in) | OWASP Top 10, input validation, auth, injection, data exposure | `Skill("security-review")` |

All preflight/review stages operate under the same rule: **fix everything, defer nothing.** The only reason to escalate to the user is if a fix requires architectural changes touching 5+ files outside the current feature scope.

## Guardrails

### Deny Rules (`~/.claude/settings.json`)
- `Bash(gh pr merge*)` — Claude cannot merge PRs
- `Bash(gh api */merge*)` — Claude cannot merge via API
- `Bash(git merge *)` — Claude cannot merge branches

### Attribution (`~/.claude/settings.json`)
```json
"attribution": { "commit": "", "pr": "" }
```
No Co-Authored-By, no "Generated with Claude Code", no AI attribution anywhere.

### Stop Hook (`~/.claude/hooks/verify-implementation.sh`) — User Level
Blocks Claude from completing, but **only when `/implement` was invoked in the current session**. Scoped by process ID (`$PPID`) so concurrent Claude windows on the same branch don't interfere. Uses timestamps to require fresh reviews for each `/implement` loop within a session.

Universal checks (all repos):
- CHANGELOG not updated (when source files changed)
- `/review` skill was not invoked after the last `/implement`
- `/security-review` skill was not invoked after the last `/implement`

Project-specific checks (`.claude/hooks/verify-extra.sh`, sourced if present):
- shared repo-native policy script (`bin/policy`) over the changed-file set

### Repo-Native Policy Layer

- `architecture/policies/adr-policy.json` defines machine-readable ADR guardrails
- `python3 bin/policy` enforces ADR/workflow, controller/MCP/docs, migration, and PR-body policy
- `make policy` is the common path for Claude, Codex, pre-commit, and CI
- `make sync-ground-control-policy` and `make policy-live` keep Ground Control quality gates and ADR metadata aligned when a live GC instance is available

### Skill Call Logging (`~/.claude/hooks/log-skill-call.sh`) — User Level
PostToolUse hook on `Skill` — writes JSONL to `/tmp/claude-skill-log/<PID>.jsonl` (per-session, not per-branch). The Stop hook reads this log to verify reviews were actually invoked (not just claimed). Stale logs (>24h) are auto-pruned.

### Git Merge Guard (`~/.claude/hooks/git-merge-guard.py`) — User Level
PreToolUse hook on `Bash` — blocks `git merge`, `git push --force`, `git reset --hard`, and `gh pr merge`. The user handles all merges.

## Standalone Skills

| Skill | Purpose |
|-------|---------|
| `/implement <uid>` | Full end-to-end: plan through PR-ready |
| `/ship` | Ship an already-committed branch (CI, reviews, fix, report) |
| `/stage` | Stage files + pre-commit loop |
| `/gh-workflow-monitor` | Monitor GitHub Actions workflow runs |

## Key Lessons (from GC-J001 first run)

- **Write `@WebMvcTest` controller tests**, not just integration tests. SonarCloud CI doesn't run Testcontainers.
- **Update `MigrationSmokeTest` and `RequirementsE2EIntegrationTest`** version lists when adding migrations.
- **Add `@NotAudited` to `@ManyToOne` references** to non-audited entities when using `@Audited`.
- **Add `_audit` table migration** when adding `@Audited` entities.
- **Default durable mutable entities to `BaseEntity`**. Only keep standalone lifecycle fields for intentionally append-only, snapshot, cache, or import/audit records.
- **Use the scaffold commands** (`make scaffold-controller`, `make scaffold-audited-entity`, `make scaffold-l2-state-machine`) to start from a compliant shape.
