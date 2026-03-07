---
title: "CI pipeline: OpenANT AI vulnerability scanning"
labels: [foundation, ci-cd, security, ai]
phase: 0
priority: P1
---

## Description

Integrate [OpenANT](https://github.com/knostic/OpenAnt) — an open-source, LLM-based vulnerability discovery tool from Knostic — into the CI pipeline. OpenANT uses a two-stage approach: Stage 1 detects potential vulnerabilities, Stage 2 simulates attacks to verify them. Only findings that survive both stages are reported, significantly reducing false positives compared to traditional SAST.

## References

- Architecture: Section 6 (Security Architecture — supply chain, defense in depth)
- [OpenANT GitHub](https://github.com/knostic/OpenAnt)
- [OpenANT Documentation](https://openant.knostic.ai/)

## Acceptance Criteria

- [ ] `.github/workflows/openant.yml` workflow:
  - Triggers on: weekly schedule, manual dispatch (`workflow_dispatch`)
  - **Setup:**
    - Install Go 1.25+ (required to build OpenANT CLI)
    - Build `openant` binary from source or download release
    - Configure `ANTHROPIC_API_KEY` secret (requires Claude Opus 4.6 access)
  - **Scan:**
    - `openant init . -l python --name KeplerOps/Ground-Control`
    - `openant scan --verify` (runs full pipeline: parse → enhance → analyze → verify → build-output → report)
    - Alternatively run staged: `openant parse && openant enhance && openant analyze && openant verify && openant build-output`
  - **Report:**
    - `openant report -f summary` — generate human-readable summary
    - Save report as GitHub Actions artifact
    - Parse findings and create GitHub issues for verified vulnerabilities (optional)
- [ ] OpenANT configuration documented:
  - Supported languages: Python (stable), TypeScript (beta)
  - Token cost estimates documented for team budgeting
  - Scan frequency: weekly (or on-demand for releases)
- [ ] Results reviewed by a human before action is taken (AI findings are advisory)
- [ ] `make openant-scan` target for local scanning

## Technical Notes

- OpenANT requires an Anthropic API key with Claude Opus 4.6 access — store as `ANTHROPIC_API_KEY` secret
- OpenANT stores project data in `~/.openant/` — use GitHub Actions cache to avoid re-parsing unchanged files
- The verify stage performs simulated attacks — this is safe against source code (no network interaction)
- Run weekly rather than on every PR to manage API costs
- OpenANT is Apache 2.0 licensed — compatible with Ground Control's license
- Python and TypeScript are supported; scan both `backend/src/` and `frontend/src/`
