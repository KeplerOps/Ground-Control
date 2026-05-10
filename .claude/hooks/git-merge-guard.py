#!/usr/bin/env python3
"""
Git Merge Guard Hook (PreToolUse, Bash matcher)

The user owns every actual merge. Claude may commit and push, and — so it
can update a PR after rebasing its feature branch onto the base — Claude may
run `git push --force-with-lease <remote> <feature-branch>`. Everything that
is a real merge or a destructive history rewrite of a protected branch stays
blocked.

Blocked unconditionally:
  - `git merge`
  - `gh pr merge`
  - `git reset --hard`
  - `git push` with a plain `--force` / `--force=<...>` / `-f` (any non-lease
    force, including short-option clusters such as `-fu` / `-uf`)

Blocked even for `--force-with-lease`:
  - a push whose explicit destination refspec is `main` or `dev`
  - a push whose refspec contains a wildcard `*` (could match protected refs)
  - a push whose refspec destination is `HEAD` (ambiguous; may be on a
    protected branch)
  - a `--force-with-lease` push with no explicit `<remote> <refspec>` (so the
    target is unambiguous — the agent must name the feature branch)

Allowed:
  - `git push --force-with-lease[=<...>] <remote> <feature-branch>` — the
    rebase-then-update-PR flow

Parsing notes:

  - The raw command string is normalized so shell control operators (`;`,
    `&&`, `||`, `|`, `&`, `( )`) become standalone tokens even when the bash
    form is `a;b` or `a&&b`. Tokenization then uses `shlex` and segments are
    inspected one at a time.
  - For each segment we strip leading `VAR=value` assignments and command
    wrappers (`env`, `sudo`, …), then for `git`/`gh` we also strip global
    options that consume a value (`git -C <dir>`, `gh --repo <o/r>`, …)
    before identifying the subcommand. That stops `git -C /tmp merge dev`
    and `gh --repo o/r pr merge 1` from sneaking past the subcommand match.

Exit codes:
  0 = allow command
  2 = block command (message goes to the user on stderr)
"""
import json
import shlex
import sys

PROTECTED_BRANCHES = {"main", "dev"}

# Shell control operators we split sub-commands at. Order matters: the
# longest match must come first so `&&` isn't mis-tokenized as `&` `&`.
SHELL_OPERATORS = ("|&", "&&", "||", ";;", ";", "|", "&", "(", ")", "{", "}", "\n")
SHELL_OPERATOR_SET = set(SHELL_OPERATORS)
# Leading command wrappers we look past to find the real git/gh invocation.
COMMAND_WRAPPERS = {"env", "sudo", "command", "nice", "nohup", "time", "stdbuf", "xargs"}

# Global options before the `git` subcommand that consume the following argv
# token as their value (the `--foo=value` form is handled separately).
GIT_GLOBAL_OPTS_WITH_VALUE = {
    "-C", "-c", "--exec-path", "--git-dir", "--work-tree",
    "--namespace", "--super-prefix", "--list-cmds",
}
# Same idea for `gh`.
GH_GLOBAL_OPTS_WITH_VALUE = {"-R", "--repo", "--cwd"}

# `git push` options that consume the following argv token as their value.
# `--force-with-lease` is intentionally NOT here — its value form is
# `--force-with-lease=<ref>`; a bare `--force-with-lease` is a standalone flag
# and the next argv token is the next push positional.
PUSH_OPTS_WITH_VALUE = {"--repo", "-o", "--push-option", "--receive-pack", "--exec"}

# Substrings dangerous enough to block even if the command can't be parsed.
UNPARSEABLE_FALLBACK_DENY = (
    "git merge",
    "git reset --hard",
    "gh pr merge",
    "git push --force",
    "git push -f",
)


def deny(message):
    print(f"ERROR: {message}", file=sys.stderr)
    sys.exit(2)


def normalize_operators(cmd):
    """Insert spaces around shell control operators that aren't inside quotes
    or escaped, so `shlex.split` returns them as their own tokens. Not a full
    shell parser; just enough that `a;b`, `a&&b`, `a|b` become `a ; b`,
    `a && b`, `a | b`."""
    out = []
    i = 0
    n = len(cmd)
    in_single = False
    in_double = False
    while i < n:
        ch = cmd[i]
        if in_single:
            out.append(ch)
            if ch == "'":
                in_single = False
            i += 1
            continue
        if in_double:
            out.append(ch)
            if ch == "\\" and i + 1 < n:
                out.append(cmd[i + 1])
                i += 2
                continue
            if ch == '"':
                in_double = False
            i += 1
            continue
        if ch == "\\" and i + 1 < n:
            out.append(ch)
            out.append(cmd[i + 1])
            i += 2
            continue
        if ch == "'":
            in_single = True
            out.append(ch)
            i += 1
            continue
        if ch == '"':
            in_double = True
            out.append(ch)
            i += 1
            continue
        # Match a (longest) shell operator at this position.
        matched = None
        for op in SHELL_OPERATORS:
            if cmd.startswith(op, i):
                matched = op
                break
        if matched is not None:
            out.append(" ")
            out.append(matched)
            out.append(" ")
            i += len(matched)
            continue
        out.append(ch)
        i += 1
    return "".join(out)


def split_segments(tokens):
    segments, current = [], []
    for token in tokens:
        if token in SHELL_OPERATOR_SET:
            if current:
                segments.append(current)
                current = []
        else:
            current.append(token)
    if current:
        segments.append(current)
    return segments


def strip_wrappers(argv):
    """Drop leading `VAR=value` assignments and `env`/`sudo`/… wrappers."""
    i = 0
    while i < len(argv):
        tok = argv[i]
        if "=" in tok and not tok.startswith("-") and "/" not in tok.split("=", 1)[0]:
            i += 1
            continue
        if tok.rsplit("/", 1)[-1] in COMMAND_WRAPPERS:
            i += 1
            # `env` and friends may be followed by VAR=value pairs.
            while i < len(argv) and "=" in argv[i] and not argv[i].startswith("-"):
                i += 1
            continue
        break
    return argv[i:]


def strip_global_opts(argv, opts_with_value):
    """Drop leading global options before the subcommand, including their
    values when the option is separated from its value by whitespace
    (e.g. `git -C <dir>`). The `--foo=value` form is self-contained."""
    i = 0
    while i < len(argv):
        tok = argv[i]
        if not tok.startswith("-"):
            break
        if "=" in tok:
            i += 1  # `--foo=bar`: value is attached
            continue
        if tok in opts_with_value:
            i += 2  # skip the option and its separate value
            continue
        i += 1  # boolean global flag (e.g. `--help`, `--version`, `-p`)
    return argv[i:]


def looks_protected(refspec):
    """True when a push refspec resolves to (or could resolve to) a protected
    destination."""
    if not refspec:
        return False
    if "*" in refspec:
        # Wildcard / matching refspec — could match protected branches.
        return True
    ref = refspec.lstrip("+")
    src, _, dst = ref.partition(":")
    target = dst if dst else src
    target = target.removeprefix("refs/heads/")
    if target == "HEAD":
        return True
    return target in PROTECTED_BRANCHES


def has_short_force_flag(tok):
    """True for short-option clusters that include `f` (e.g. `-f`, `-fu`,
    `-uf`). Excludes long options (`--foo`)."""
    return (
        tok.startswith("-")
        and not tok.startswith("--")
        and len(tok) >= 2
        and "f" in tok[1:]
    )


def check_gh(args):
    rest = strip_global_opts(args, GH_GLOBAL_OPTS_WITH_VALUE)
    positional = [t for t in rest if not t.startswith("-")]
    if positional[:2] == ["pr", "merge"]:
        deny("Claude may not run 'gh pr merge'. The user handles all merges.")


def check_git(args):
    rest = strip_global_opts(args, GIT_GLOBAL_OPTS_WITH_VALUE)
    subcommand = None
    sub_index = None
    for index, tok in enumerate(rest):
        if not tok.startswith("-"):
            subcommand = tok
            sub_index = index
            break
    if subcommand is None:
        return
    after = rest[sub_index + 1:]

    if subcommand == "merge":
        deny("Claude may not run 'git merge'. The user handles all merges.")
    if subcommand == "reset" and "--hard" in after:
        deny("Claude may not run 'git reset --hard'. The user handles destructive history rewrites.")
    if subcommand == "push":
        check_git_push(after)


def check_git_push(args):
    plain_force = False
    lease_force = False
    for tok in args:
        if tok == "--force" or tok.startswith("--force="):
            plain_force = True
        elif tok == "--force-with-lease" or tok.startswith("--force-with-lease="):
            lease_force = True
        elif has_short_force_flag(tok):
            plain_force = True

    if plain_force:
        deny(
            "Claude may not run a plain 'git push --force' / 'git push -f'. "
            "Use '--force-with-lease <remote> <feature-branch>' after rebasing onto the base. "
            "The user handles anything riskier."
        )
    if not lease_force:
        return  # ordinary push — allowed

    positionals = []
    skip_next = False
    for tok in args:
        if skip_next:
            skip_next = False
            continue
        if tok.startswith("-"):
            if tok in PUSH_OPTS_WITH_VALUE and "=" not in tok:
                skip_next = True
            continue
        positionals.append(tok)
    refspecs = positionals[1:] if len(positionals) >= 2 else []

    if not refspecs:
        deny(
            "Claude may not run 'git push --force-with-lease' without an explicit "
            "'<remote> <feature-branch>'. Name the feature branch so the target is unambiguous; "
            "force-pushing the resolved upstream could hit a protected branch."
        )
    if any(looks_protected(r) for r in refspecs):
        deny(
            "Claude may not force-push to 'main', 'dev', 'HEAD', or a wildcard refspec. "
            "The user handles protected-branch history."
        )


def main():
    data = json.load(sys.stdin)
    command = data.get("tool_input", {}).get("command", "") or ""

    try:
        normalized = normalize_operators(command)
        tokens = shlex.split(normalized, comments=True)
    except ValueError:
        for needle in UNPARSEABLE_FALLBACK_DENY:
            if needle in command:
                deny(f"Claude may not run '{needle}'. The user handles merges and destructive history rewrites.")
        sys.exit(0)

    for segment in split_segments(tokens):
        argv = strip_wrappers(segment)
        if not argv:
            continue
        program = argv[0].rsplit("/", 1)[-1]
        if program == "git":
            check_git(argv[1:])
        elif program == "gh":
            check_gh(argv[1:])

    sys.exit(0)


if __name__ == "__main__":
    main()
