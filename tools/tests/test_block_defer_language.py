"""Tests for .claude/hooks/block-defer-language.py.

Drives the hook as a subprocess with synthetic Claude PreToolUse JSON on
stdin, against the shared golden-case set in tools/policy/deferral_cases.json
(the same file tools/tests/test_policy.py loads) so the hook's standalone
classifier and the bin/policy classifier cannot drift without one suite
failing. Exit 2 = blocked; exit 0 = allowed.
"""
import json
import shlex
import subprocess
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
HOOK = REPO_ROOT / ".claude" / "hooks" / "block-defer-language.py"
CASES = json.loads((REPO_ROOT / "tools" / "policy" / "deferral_cases.json").read_text(encoding="utf-8"))["cases"]

# Map a classifier surface to a representative `gh` command. The classifier's
# `pr-body` surface corresponds to the `gh pr create` command shape (both get
# Tier-1-only treatment), so pr-body cases are exercised via `gh pr create`.
_SURFACE_TO_CMD = {
    "issue-create": lambda body: f"gh issue create --title T --body {shlex.quote(body)}",
    "issue-edit": lambda body: f"gh issue edit 830 --body {shlex.quote(body)}",
    "issue-close": lambda body: f"gh issue close 830 --comment {shlex.quote(body)}",
    "issue-comment": lambda body: f"gh issue comment 830 --body {shlex.quote(body)}",
    "pr-create": lambda body: f"gh pr create --title T --body {shlex.quote(body)}",
    "pr-edit": lambda body: f"gh pr edit 5 --body {shlex.quote(body)}",
    "pr-comment": lambda body: f"gh pr comment 5 --body {shlex.quote(body)}",
    "pr-body": lambda body: f"gh pr create --title T --body {shlex.quote(body)}",
}


def _run_hook(command: str) -> int:
    payload = json.dumps({"tool_input": {"command": command}})
    proc = subprocess.run(
        ["python3", str(HOOK)],
        input=payload,
        capture_output=True,
        text=True,
        timeout=10,
    )
    return proc.returncode


class BlockDeferLanguageHookTest(unittest.TestCase):
    def test_golden_cases(self):
        self.assertTrue(HOOK.exists(), f"hook not found at {HOOK}")
        failures = []
        for case in CASES:
            surface = case["surface"]
            builder = _SURFACE_TO_CMD.get(surface)
            if builder is None:
                failures.append(f"{case['id']}: no command mapping for surface {surface}")
                continue
            command = builder(case["text"])
            rc = _run_hook(command)
            expected_rc = 2 if case["expect"] == "deny" else 0
            if rc != expected_rc:
                failures.append(
                    f"{case['id']}: surface={surface} expected rc={expected_rc} got rc={rc} — {case['why']}"
                )
        self.assertEqual(failures, [], "\n".join(failures))

    def test_passes_through_non_gh_commands(self):
        self.assertEqual(_run_hook("echo 'deferred to a follow-up PR'"), 0)
        self.assertEqual(_run_hook("git commit -m 'fixed in a subsequent PR'"), 0)

    def test_passes_through_unrelated_gh_commands(self):
        self.assertEqual(_run_hook("gh pr view 5 --json body"), 0)
        self.assertEqual(_run_hook("gh issue list --label bug"), 0)

    def test_blocks_heredoc_body_on_comment_surface(self):
        # The common `gh issue comment N --body "$(cat <<'EOF' … EOF)"` shape:
        # shlex cannot extract the body, so the heredoc-extraction path must.
        command = (
            "gh issue comment 830 --body \"$(cat <<'EOF'\n"
            "## Final report\n\nAll done. SonarCloud findings deferred to a follow-up PR.\n"
            "EOF\n)\""
        )
        self.assertEqual(_run_hook(command), 2)

    def test_allows_clean_heredoc_body(self):
        command = (
            "gh issue comment 830 --body \"$(cat <<'EOF'\n"
            "## Final report\n\nAll review findings fixed; CI green; PR ready for merge.\n"
            "EOF\n)\""
        )
        self.assertEqual(_run_hook(command), 0)

    def test_allows_out_of_scope_heredoc_on_issue_create(self):
        command = (
            "gh issue create --title 'New work' --body \"$(cat <<'EOF'\n"
            "## Out of scope\n\n- Retroactive cleanup of past issues.\nEOF\n)\""
        )
        self.assertEqual(_run_hook(command), 0)

    def test_blocks_body_file(self):
        import tempfile

        with tempfile.NamedTemporaryFile("w", suffix=".md", delete=False) as f:
            f.write("Finding 3 is to be done later.\n")
            path = f.name
        try:
            self.assertEqual(_run_hook(f"gh issue close 830 --comment-file {path}"), 0)  # --comment-file is not a recognized flag → not scanned
            self.assertEqual(_run_hook(f"gh issue comment 830 --body-file {path}"), 2)
        finally:
            Path(path).unlink()

    def test_blocks_short_comment_flag_on_issue_close(self):
        # `gh issue close 830 -c "..."` — `-c` is the close-comment short form.
        self.assertEqual(_run_hook("gh issue close 830 -c 'remaining findings deferred'"), 2)
        self.assertEqual(_run_hook("gh issue close 830 -c 'all findings fixed'"), 0)

    def test_fails_closed_on_body_file_stdin_for_report_surfaces(self):
        # --body-file - reads stdin; the hook cannot inspect it. On report
        # surfaces (issue comment/close, pr comment) there is no completion-gate
        # backstop, so the hook fails closed.
        self.assertEqual(_run_hook("gh issue comment 830 --body-file -"), 2)
        self.assertEqual(_run_hook("gh issue close 830 --comment-file -"), 0)  # --comment-file is not a recognized flag → not inspected
        # Definition surfaces (issue/pr create) are not fail-closed — the PR-body
        # policy backstops PR creation, and a new issue's scope text is benign.
        self.assertEqual(_run_hook("gh issue create --title T --body-file -"), 0)
        self.assertEqual(_run_hook("gh pr create --title T --body-file -"), 0)

    def test_handles_gh_global_flags_before_subcommand(self):
        # `gh -R owner/repo issue comment ...` and `gh --repo=x pr edit ...`
        # must still be inspected.
        self.assertEqual(_run_hook("gh -R KeplerOps/Ground-Control issue comment 830 --body 'deferred to a follow-up PR'"), 2)
        self.assertEqual(_run_hook("gh --repo=KeplerOps/Ground-Control pr edit 5 --body 'in a subsequent PR'"), 2)
        self.assertEqual(_run_hook("gh -R KeplerOps/Ground-Control issue comment 830 --body 'all findings fixed'"), 0)
        # Heredoc surface detection also accounts for global flags.
        cmd = (
            "gh -R KeplerOps/Ground-Control issue comment 830 --body \"$(cat <<'EOF'\n"
            "Closing. The rest deferred to a follow-up issue.\nEOF\n)\""
        )
        self.assertEqual(_run_hook(cmd), 2)

    def test_malformed_hook_input_does_not_block(self):
        proc = subprocess.run(
            ["python3", str(HOOK)],
            input="not json at all",
            capture_output=True,
            text=True,
            timeout=10,
        )
        self.assertEqual(proc.returncode, 0)


if __name__ == "__main__":
    unittest.main()
