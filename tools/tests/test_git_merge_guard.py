"""Tests for .claude/hooks/git-merge-guard.py.

Drives the hook as a subprocess with synthetic Claude PreToolUse JSON on
stdin and asserts the exit code: 2 = blocked, 0 = allowed. The hook tokenizes
the command line with shlex and inspects parsed argv (force flags, push
refspecs) rather than matching substrings, so these cases pin that contract —
in particular: a plain `--force`/`-f` is blocked even when `--force-with-lease`
also appears, a lease-force needs an explicit `<remote> <feature-branch>`, a
lease-force whose destination refspec is `main`/`dev` is blocked, and a branch
that merely contains the substring `dev` (e.g. `feature/dev-rebase`) is allowed.
"""
import json
import subprocess
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
HOOK = REPO_ROOT / ".claude" / "hooks" / "git-merge-guard.py"

# (description, command, expected_exit)
ALLOW_CASES = [
    ("plain push", "git push"),
    ("push -u feature branch", "git push -u origin 823-verify-gc-t001"),
    ("commit", "git commit -m 'x'"),
    ("status then log", "git status && git log --oneline"),
    ("commit message that mentions merge", "git commit -m 'tidy git merge guard'"),
    ("lease-force to a feature branch", "git push --force-with-lease origin 823-verify-gc-t001"),
    ("lease-force to a feature/* branch containing 'dev'", "git push --force-with-lease origin feature/dev-rebase"),
    ("lease-force with =<ref> plus an explicit refspec", "git push --force-with-lease=origin/823 origin 823"),
    ("lease-force after a push option that takes a value", "git push -o ci.skip --force-with-lease origin 823"),
    ("gh pr view (not merge)", "gh pr view 123 --json number"),
    ("git -C with a non-blocked subcommand", "git -C /tmp status"),
    ("git -c key=value with a non-blocked subcommand", "git -c color.ui=true log --oneline -1"),
    ("gh --repo with a non-blocked subcommand", "gh --repo o/r pr view 1"),
]

BLOCK_CASES = [
    ("git merge", "git merge dev"),
    ("git merge via fetch chain", "git fetch && git merge origin/dev"),
    ("gh pr merge", "gh pr merge 123 --squash"),
    ("git reset --hard", "git reset --hard HEAD~1"),
    ("git reset --hard in a pipeline", "echo x | git reset --hard"),
    ("plain --force", "git push --force origin 823"),
    ("plain -f", "git push -f origin 823"),
    ("plain --force alongside --force-with-lease", "git push --force --force-with-lease origin 823"),
    ("-f alongside --force-with-lease", "git push -f --force-with-lease origin 823"),
    ("lease-force with no explicit refspec", "git push --force-with-lease"),
    ("lease-force with only a remote", "git push --force-with-lease origin"),
    ("lease-force destination main", "git push --force-with-lease origin main"),
    ("lease-force destination HEAD:dev", "git push --force-with-lease origin HEAD:dev"),
    ("lease-force destination +main refspec", "git push --force-with-lease origin +main"),
    ("git merge behind an env wrapper", "env GIT_PAGER=cat git merge dev"),
    ("git merge via an absolute path", "/usr/bin/git merge dev"),
    # Cycle-3 fixes — these all have to stay blocked even though they sit on a
    # less-obvious surface (global opts, short-option clusters, no-space shell
    # operators, wildcard / HEAD refspecs):
    ("git -C path merge", "git -C /tmp merge dev"),
    ("git -c key=value merge", "git -c advice.detachedHead=false merge dev"),
    ("git --git-dir <path> merge", "git --git-dir /tmp/.git merge dev"),
    ("gh --repo o/r pr merge", "gh --repo owner/repo pr merge 123 --squash"),
    ("gh -R o/r pr merge", "gh -R owner/repo pr merge 123"),
    ("short-option cluster -fu", "git push -fu origin 823"),
    ("short-option cluster -uf", "git push -uf origin 823"),
    ("--force=value form", "git push --force=origin/main origin 823"),
    ("no-space ; operator hides merge", "echo ok;git merge dev"),
    ("no-space && operator hides merge", "true&&git merge dev"),
    ("no-space ; chain to gh pr merge", "git status;gh pr merge 123 --squash"),
    ("no-space && chain to merge", "git status&&git merge dev"),
    ("no-space pipe to reset --hard", "git status|git reset --hard"),
    ("lease-force wildcard refspec", "git push --force-with-lease origin 'refs/heads/*:refs/heads/*'"),
    ("lease-force bare HEAD refspec", "git push --force-with-lease origin HEAD"),
    ("lease-force refs/heads/main", "git push --force-with-lease origin refs/heads/main"),
]


def _run(command):
    payload = json.dumps({"tool_input": {"command": command}})
    proc = subprocess.run(
        ["python3", str(HOOK)],
        input=payload,
        capture_output=True,
        text=True,
        timeout=10,
        check=False,
    )
    return proc.returncode


class GitMergeGuardTest(unittest.TestCase):
    def test_allowed_commands_exit_zero(self):
        for desc, command in ALLOW_CASES:
            with self.subTest(case=desc):
                self.assertEqual(_run(command), 0, f"{desc!r} should be allowed: {command!r}")

    def test_blocked_commands_exit_two(self):
        for desc, command in BLOCK_CASES:
            with self.subTest(case=desc):
                self.assertEqual(_run(command), 2, f"{desc!r} should be blocked: {command!r}")

    def test_unparseable_command_with_dangerous_substring_is_blocked(self):
        # Unbalanced quote -> shlex.split raises -> conservative substring fallback.
        self.assertEqual(_run("git merge 'oops"), 2)

    def test_unparseable_command_without_dangerous_substring_is_allowed(self):
        self.assertEqual(_run("echo 'oops"), 0)


if __name__ == "__main__":
    unittest.main()
