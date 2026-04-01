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
- Used for cross-model code review (`codex review --base dev`)

## Workflow: `/implement <requirement-uid>`

### User Touchpoints
1. **Plan approval** — user reviews and approves the implementation plan
2. **PR review and merge** — user reviews the final PR and merges to dev

Everything between these two checkpoints is automated.

### Phase A: Plan & Implement
1. Fetch requirement from Ground Control, create GitHub issue if needed
2. Checkout feature branch via `gh issue develop`
3. Explore codebase for existing coverage
4. **Enter plan mode — user approves**
5. Implement, clause-by-clause verification
6. Create traceability links (IMPLEMENTS, TESTS), transition to ACTIVE

### Phase B: Quality Gate
7. Run `pre-commit run --all-files`
8. Completion gate: `make check`, CHANGELOG, traceability, requirement status

### Phase C: Stage, Commit, Push
9. Stage files, pre-commit loop (fix failures, max 5 iterations)
10. Commit (no attribution) and push

### Phase D: Ship
11. Create PR to dev
12. Monitor CI (`gh run watch`)
13. Check SonarCloud quality gate
14. **Codex review** — cross-model review by ChatGPT via `codex review --base dev`
15. **Code review** — Claude built-in `/review` skill via Skill tool
16. **Security review** — Claude built-in `/security-review` skill via Skill tool
17. Fix ALL findings from steps 14-16, commit, push, re-run CI
18. **Report to user** — PR URL, summary of findings/fixes, CI/SonarCloud status

Claude does NOT merge. The user reviews the PR and merges.

## Review Pipeline

Four reviewers run on every PR before the user sees it:

| Reviewer | What it catches | How it runs |
|----------|----------------|-------------|
| SonarCloud | Coverage, code smells, duplication, security hotspots | CI job, quality gate must pass |
| Codex (ChatGPT-5.4) | Design, abstractions, concept conflation, maintainability | `codex review --base dev` |
| `/review` (Claude built-in) | Code quality, conventions, correctness, performance | `Skill("review")` |
| `/security-review` (Claude built-in) | OWASP Top 10, input validation, auth, injection, data exposure | `Skill("security-review")` |

All reviewers operate under the same rule: **fix everything, defer nothing.** The only reason to escalate to the user is if a fix requires architectural changes touching 5+ files outside the current feature scope.

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
- API docs not updated (when controllers added)
- MCP tools not updated (when controllers added)

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
