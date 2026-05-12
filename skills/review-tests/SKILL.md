---
name: review-tests
description: Review test files for quality — catches shallow coverage tests, integration-as-unit, inline mock abuse, missing parameterization, and tests that can't detect regressions.
---

# Test Quality Review

You are reviewing test files changed in the current branch. Your job is to identify **tests that provide false assurance** — tests that pass but would still pass if the implementation were broken.

This skill is invoked by the `implement` workflow at Step 13. It is agent-neutral: Claude Code drivers call it via the `Skill` tool with `skill="review-tests"`; Codex drivers invoke it via `~/.codex/prompts/review-tests.md`. Both targets are populated by `bin/install-skills.sh` from the canonical source at `skills/review-tests/SKILL.md`.

## How to Find Changed Test Files

The base branch is configurable per-repo via `.ground-control.yaml` under `workflow.base_branch` (default: `dev`). This skill reads the YAML itself rather than expecting a caller to resolve a placeholder for it, because it can be invoked standalone (from `Skill` or a Codex prompt) without an enclosing workflow.

1. Resolve the absolute repo root with `pwd`.
2. Call `gc_get_repo_ground_control_context` with `repo_path: <pwd>`. Read `workflow.base_branch` from the response; treat `null`/missing as the literal string `dev`.
3. Cache the resolved value as `BASE_REF`.
4. Run:

   ```bash
   git diff --name-only origin/${BASE_REF}...HEAD | grep -iE '(test_|_test\.|tests/|Test\.)'
   ```

   If `origin/${BASE_REF}` is not available (no remote, fetch needed), retry with the local ref `${BASE_REF}`. If neither resolves, run `git fetch origin ${BASE_REF}` and retry; if the fetch fails, STOP and surface a clear error.
5. Read every changed test file. For each, also read the source file it tests so you understand what behavior should be verified.

## What to Flag

### Critical (must fix)

1. **Assertion-free tests** — tests that call code but never assert on the result. A test that only checks "no exception was raised" is not a test.

2. **Mock-only assertions** — tests where the only assertion is that a mock was called. This verifies wiring, not behavior. The test must also assert on the **return value, side effect, or state change** produced by the code under test.

3. **Integration masquerading as unit** — tests that hit a real database, make real HTTP calls, touch the filesystem, or spawn subprocesses without being explicitly marked as integration tests. Unit tests must be isolated.

4. **Per-test resource setup** — creating a database, connection pool, or heavy resource inside each test method instead of using shared fixtures or setup methods. This causes OOM at scale.

5. **Mocking language/framework internals** — mocking subprocess, os.path, datetime.now, or equivalent framework internals. If you need to mock these, the code under test needs restructuring, not more mocks.

6. **Tests that can't detect regressions** — if you could replace the function under test with a no-op and the test would still pass, the test is worthless. Check: does the test assert on something that **only the correct implementation** would produce?

### Warnings (should fix)

7. **Inline mock/stub abuse** — excessive mock/stub/spy instantiation inside a single test method instead of shared fixtures or setup.

8. **Missing parameterization** — multiple near-identical test methods that differ only in input/expected output. These should use parameterized tests.

9. **Overly broad exception catching** — catching generic Exception types in assertions instead of the specific exception.

10. **No negative test cases** — only happy-path tests with no error/edge case coverage.

## How to Review

For each test file:

1. Read the test file.
2. Read the source file it tests.
3. For each test method, answer: "If I broke the implementation, would this test catch it?"
4. If the answer is no, flag it.

## Output Format

For each finding:
```
**[CRITICAL/WARNING] test_file::TestClass::test_method**
Problem: <what's wrong>
Why it matters: <what regression this would miss>
Fix: <specific fix, not vague advice>
```

## After Review

This skill is **pure review output**. Report findings in the documented format above and return control to the parent. Do NOT fix findings, do NOT commit, do NOT push, do NOT consult the user about scope. The parent `/implement` workflow owns the full Review-loop rules: it constructs the `gc_post_decision_record(reviewer: "test-quality", findings: [...])` payload from the findings returned here, records `fix` / `wontfix` / `not-applicable` dispositions, applies the structural fix for `class` findings, commits and pushes, re-invokes this skill, and advances into Step 14 only after a clean cycle's decision-record post returns `ok: true`. Returning fix-applied or partial-fix state to the parent would let the durable `gc_post_decision_record` payload be reconstructed *after* fixing, losing the per-cycle findings-and-decisions trail that ADR-029 requires.

If the tests are solid: report "Test quality review: no issues found" and return control. The human-readable line is a transcript convenience; the durable workflow signal is the parent's `gc_post_decision_record` call with `findings: []`, and only after that call returns `ok: true` does the parent advance into Step 14. This skill does not advance Step 14; the parent owns phase progression (issue #884).
