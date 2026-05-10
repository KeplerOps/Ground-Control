from __future__ import annotations

import argparse
import fnmatch
import json
import os
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


REPO_ROOT = Path(__file__).resolve().parents[2]
ADR_POLICY_PATH = REPO_ROOT / "architecture" / "policies" / "adr-policy.json"
CONTROLLER_PATH_RE = re.compile(
    r"^backend/src/main/java/com/keplerops/groundcontrol/api/.+Controller\.java$"
)
MIGRATION_PATH_RE = re.compile(r"^backend/src/main/resources/db/migration/V\d+__.+\.sql$")
PR_REQUIREMENT_RE = re.compile(r"\b[A-Z][A-Z0-9]+-[A-Z0-9]+(?:-\d+|\d+)\b")

DEPLOY_COMPOSE_PROD_PATH = Path("deploy/docker/docker-compose.prod.yml")
REQUIRED_ADR026_CREDENTIAL_SLOT_COUNT = 5
REQUIRED_ADR026_ALLOWLIST_SLOT_COUNT = 5


def _required_adr026_backend_env_keys() -> tuple[tuple[str, ...], tuple[str, ...]]:
    """Return the (always-set, inherit-only) ADR-026 keys.

    Always-set keys are typed config (security toggles); list or map form is
    fine for them because their defaults are guaranteed non-empty. Inherit-only
    keys are the optional indexed credential / allowlist slots — those MUST
    use bare list form (``- KEY`` with no ``=``) so unset host variables
    are NOT injected as empty strings (Spring's
    ``SecurityProperties.validate()`` rejects blank principal/token/role/CIDR
    entries and the container fails startup).
    """
    always_set: list[str] = ["GC_SECURITY_ENABLED", "GC_SECURITY_OPENAPI_PUBLIC"]
    inherit_only: list[str] = []
    for index in range(REQUIRED_ADR026_CREDENTIAL_SLOT_COUNT):
        inherit_only.append(f"GROUNDCONTROL_SECURITY_CREDENTIALS_{index}_PRINCIPAL_NAME")
        inherit_only.append(f"GROUNDCONTROL_SECURITY_CREDENTIALS_{index}_TOKEN")
        inherit_only.append(f"GROUNDCONTROL_SECURITY_CREDENTIALS_{index}_ROLE")
    for index in range(REQUIRED_ADR026_ALLOWLIST_SLOT_COUNT):
        inherit_only.append(f"GROUNDCONTROL_SECURITY_IP_ALLOWLIST_{index}")
    return tuple(always_set), tuple(inherit_only)


(
    REQUIRED_ADR026_ALWAYS_SET_KEYS,
    REQUIRED_ADR026_INHERIT_ONLY_KEYS,
) = _required_adr026_backend_env_keys()
REQUIRED_ADR026_BACKEND_ENV_KEYS: tuple[str, ...] = (
    *REQUIRED_ADR026_ALWAYS_SET_KEYS,
    *REQUIRED_ADR026_INHERIT_ONLY_KEYS,
)
COMPOSE_ENV_KEY_RE = re.compile(r"^\s*-\s*([A-Z][A-Z0-9_]*)(?:=.*)?\s*$|^\s*([A-Z][A-Z0-9_]*)\s*:.*$")
COMPOSE_ENV_INHERIT_FORM_RE = re.compile(r"^\s*-\s*([A-Z][A-Z0-9_]*)\s*$")


@dataclass
class Violation:
    code: str
    message: str
    details: list[str]

    def render(self) -> str:
        if not self.details:
            return f"[{self.code}] {self.message}"
        formatted = "\n".join(f"  - {detail}" for detail in self.details)
        return f"[{self.code}] {self.message}\n{formatted}"


def normalize_path(path: str) -> str:
    return Path(path).as_posix().lstrip("./")


def run_git(args: list[str], root: Path = REPO_ROOT) -> str:
    result = subprocess.run(
        ["git", *args],
        cwd=root,
        check=True,
        capture_output=True,
        text=True,
    )
    return result.stdout


def read_changed_files(
    *,
    files: Iterable[str] | None = None,
    base: str | None = None,
    staged: bool = False,
    env_var: str | None = None,
    root: Path = REPO_ROOT,
) -> list[str]:
    if files:
        return sorted({normalize_path(path) for path in files if path})
    if env_var:
        raw = os.getenv(env_var, "")
        return sorted({normalize_path(path) for path in raw.splitlines() if path.strip()})
    if base:
        output = run_git(["diff", "--name-only", "--diff-filter=ACMRTUXB", base, "--"], root=root)
        return sorted({normalize_path(path) for path in output.splitlines() if path.strip()})
    if staged:
        output = run_git(["diff", "--cached", "--name-only", "--diff-filter=ACMRTUXB", "--"], root=root)
        return sorted({normalize_path(path) for path in output.splitlines() if path.strip()})

    tracked = run_git(["diff", "--name-only", "--diff-filter=ACMRTUXB", "HEAD", "--"], root=root)
    untracked = run_git(["ls-files", "--others", "--exclude-standard"], root=root)
    combined = tracked.splitlines() + untracked.splitlines()
    return sorted({normalize_path(path) for path in combined if path.strip()})


def matches_any(path: str, patterns: Iterable[str]) -> bool:
    return any(fnmatch.fnmatch(path, pattern) for pattern in patterns)


def filter_matches(paths: Iterable[str], patterns: Iterable[str]) -> list[str]:
    return sorted({path for path in paths if matches_any(path, patterns)})


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def get_repo_relative_files(root: Path, glob_pattern: str) -> list[str]:
    return sorted(
        normalize_path(str(path.relative_to(root)))
        for path in root.glob(glob_pattern)
        if path.is_file()
    )


def run_adr_guard(changed_files: list[str], root: Path = REPO_ROOT) -> list[Violation]:
    policy = load_json(ADR_POLICY_PATH)
    violations: list[Violation] = []

    for policy_entry in policy["policies"]:
        for rule in policy_entry.get("rules", []):
            triggers = filter_matches(changed_files, rule.get("whenAny", []))
            if not triggers:
                continue

            missing_all = [
                required
                for required in rule.get("requireAll", [])
                if required not in changed_files
            ]
            missing_any = []
            require_any = rule.get("requireAny", [])
            if require_any and not any(required in changed_files for required in require_any):
                missing_any.append(f"one of: {', '.join(require_any)}")

            if missing_all or missing_any:
                details = [f"triggered by: {', '.join(triggers)}"]
                if missing_all:
                    details.append(f"missing required file updates: {', '.join(missing_all)}")
                details.extend(missing_any)
                violations.append(
                    Violation(
                        code=rule["id"],
                        message=rule["message"],
                        details=details,
                    )
                )

    return violations


def run_controller_contracts(changed_files: list[str], root: Path = REPO_ROOT) -> list[Violation]:
    controllers = [path for path in changed_files if CONTROLLER_PATH_RE.match(path)]
    if not controllers:
        return []

    violations: list[Violation] = []
    required_changed = []
    for required in ("docs/API.md", "mcp/ground-control/lib.js", "mcp/ground-control/index.js"):
        if required not in changed_files:
            required_changed.append(required)
    if required_changed:
        violations.append(
            Violation(
                code="controller-parity",
                message="Controller changes require API docs and MCP parity updates.",
                details=[
                    f"controllers changed: {', '.join(controllers)}",
                    f"missing companion updates: {', '.join(required_changed)}",
                ],
            )
        )

    for controller in controllers:
        test_name = f"{Path(controller).stem}Test.java"
        matching_test_updates = [path for path in changed_files if path.endswith(f"/{test_name}")]
        if not matching_test_updates:
            violations.append(
                Violation(
                    code="controller-webmvctest-update",
                    message="Controller changes require a matching @WebMvcTest update.",
                    details=[f"missing changed test for {controller}: expected {test_name}"],
                )
            )
            continue

        test_paths = get_repo_relative_files(root, f"backend/src/test/java/**/{test_name}")
        if not test_paths:
            violations.append(
                Violation(
                    code="controller-webmvctest-missing",
                    message="Controller is missing a matching @WebMvcTest class.",
                    details=[f"expected test file {test_name} for {controller}"],
                )
            )
            continue

        for test_path in test_paths:
            content = (root / test_path).read_text(encoding="utf-8")
            if "@WebMvcTest(" not in content:
                violations.append(
                    Violation(
                        code="controller-webmvctest-annotation",
                        message="Controller test exists but is not a @WebMvcTest.",
                        details=[f"{test_path} must use @WebMvcTest for {controller}"],
                    )
                )

    return violations


def git_diff_for_paths(paths: Iterable[str], root: Path = REPO_ROOT) -> str:
    path_list = [normalize_path(path) for path in paths]
    if not path_list:
        return ""
    return run_git(["diff", "--unified=0", "HEAD", "--", *path_list], root=root)


def run_migration_policy(changed_files: list[str], root: Path = REPO_ROOT) -> list[Violation]:
    migrations = [path for path in changed_files if MIGRATION_PATH_RE.match(path)]
    violations: list[Violation] = []

    if migrations:
        required = [
            "backend/src/test/java/com/keplerops/groundcontrol/integration/MigrationSmokeTest.java",
            "backend/src/test/java/com/keplerops/groundcontrol/integration/RequirementsE2EIntegrationTest.java",
        ]
        missing = [path for path in required if path not in changed_files]
        if missing:
            violations.append(
                Violation(
                    code="migration-smoke-sync",
                    message="Migration changes require the hardcoded integration version lists to be updated.",
                    details=[
                        f"migrations changed: {', '.join(migrations)}",
                        f"missing companion updates: {', '.join(missing)}",
                    ],
                )
            )

    java_files = [path for path in changed_files if path.endswith(".java")]
    diff = git_diff_for_paths(java_files, root=root)
    audited_files: set[str] = set()
    current_file = ""
    for line in diff.splitlines():
        if line.startswith("+++ b/"):
            current_file = normalize_path(line.removeprefix("+++ b/"))
        elif line.startswith("+") and not line.startswith("+++") and "@Audited" in line and current_file:
            audited_files.add(current_file)

    if audited_files and not migrations:
        violations.append(
            Violation(
                code="audited-entity-migration",
                message="Adding @Audited requires matching Flyway migration updates.",
                details=[
                    f"@Audited added in: {', '.join(sorted(audited_files))}",
                    "expected at least one db/migration/V*.sql change in the same diff",
                ],
            )
        )

    return violations


def _extract_compose_backend_env_entries(text: str) -> dict[str, str]:
    """Extract environment entries declared on the `backend` service.

    Returns a mapping of key → form, where form is one of:
      ``"inherit"`` — list shorthand ``- KEY`` (no value, host-inheritance only).
      ``"list-value"`` — list form with explicit value ``- KEY=...``.
      ``"map"``    — map form ``KEY: ...``.

    Compose allows both list-form (``- KEY=VALUE`` / ``- KEY``) and map-form
    (``KEY: VALUE``) under ``environment:``; honor either. A handwritten
    indentation walker is intentional here — adding a PyYAML dependency for
    one check would make ``make policy`` fail with ``ModuleNotFoundError`` on
    a clean Python installation, since the rest of ``tools/policy/`` is
    stdlib-only.
    """
    found: dict[str, str] = {}
    in_backend = False
    backend_indent = -1
    in_environment = False
    env_indent = -1
    for raw_line in text.splitlines():
        stripped = raw_line.lstrip()
        if not stripped or stripped.startswith("#"):
            continue
        indent = len(raw_line) - len(stripped)

        # Track sibling-out-of-block exits before recognizing new starts.
        if in_environment and indent <= env_indent:
            in_environment = False
        if in_backend and indent <= backend_indent and not stripped.startswith("backend:"):
            in_backend = False

        if stripped.startswith("backend:"):
            in_backend = True
            backend_indent = indent
            in_environment = False
            continue
        if in_backend and stripped.startswith("environment:") and not in_environment:
            in_environment = True
            env_indent = indent
            continue
        if in_environment:
            inherit_match = COMPOSE_ENV_INHERIT_FORM_RE.match(raw_line)
            if inherit_match:
                found.setdefault(inherit_match.group(1), "inherit")
                continue
            match = COMPOSE_ENV_KEY_RE.match(raw_line)
            if match:
                key = match.group(1) or match.group(2)
                form = "list-value" if match.group(1) is not None else "map"
                found.setdefault(key, form)
    return found


def run_deploy_compose_credential_passthrough(root: Path = REPO_ROOT) -> list[Violation]:
    """Assert the production compose file enumerates ADR-026 credential env vars.

    #828 was triggered because the operator filled `GROUNDCONTROL_SECURITY_*`
    values into `/opt/gc/.env` but `deploy/docker/docker-compose.prod.yml` did
    not list them on the backend service's `environment:` block, so docker
    compose never propagated them into the container. The first deploy of the
    ADR-026 image therefore 401'd every consumer. The check below is a static
    post-condition — independent of `changed_files` — so any future diff that
    silently strips one of the required keys fails `make policy`. All five
    documented credential slots and all five allowlist slots must remain
    enumerated; partial removal is itself the regression.
    """
    compose_path = root / DEPLOY_COMPOSE_PROD_PATH
    if not compose_path.exists():
        return [
            Violation(
                code="deploy-compose-missing",
                message=(
                    "Canonical production compose file is missing — ADR-026 "
                    "credential passthrough cannot be verified."
                ),
                details=[f"expected at {DEPLOY_COMPOSE_PROD_PATH.as_posix()}"],
            )
        ]

    text = compose_path.read_text(encoding="utf-8")
    entries = _extract_compose_backend_env_entries(text)

    violations: list[Violation] = []

    missing = [key for key in REQUIRED_ADR026_BACKEND_ENV_KEYS if key not in entries]
    if missing:
        violations.append(
            Violation(
                code="deploy-compose-adr026-passthrough",
                message=(
                    "deploy/docker/docker-compose.prod.yml backend service is "
                    "missing ADR-026 credential env-var passthroughs (GC-P011)."
                ),
                details=[f"missing keys: {', '.join(missing)}"],
            )
        )

    # Indexed credential / allowlist slots MUST use bare list form (- KEY).
    # Map form with ${VAR:-} or list-with-value form ${VAR:-} both inject the
    # variable into the container as an empty string when the host variable
    # is unset, which Spring's SecurityProperties.validate() then rejects —
    # exactly the brittle path #828 cycle 1 surfaced. Bare list form inherits
    # only when the host has the variable set.
    wrong_form = [
        key
        for key in REQUIRED_ADR026_INHERIT_ONLY_KEYS
        if key in entries and entries[key] != "inherit"
    ]
    if wrong_form:
        violations.append(
            Violation(
                code="deploy-compose-adr026-inherit-only",
                message=(
                    "Optional ADR-026 credential / allowlist slots in "
                    "deploy/docker/docker-compose.prod.yml must use bare list form "
                    "(- KEY) so unset host variables are not injected as blank "
                    "(GC-P011 / SecurityProperties.validate)."
                ),
                details=[f"keys not in inherit-only form: {', '.join(wrong_form)}"],
            )
        )

    return violations


def run_pr_body_check(event_path: Path) -> list[Violation]:
    """Backwards-compatible wrapper that loads the PR body from a GitHub event payload."""
    event = json.loads(event_path.read_text(encoding="utf-8"))
    pull_request = event.get("pull_request") or {}
    body = pull_request.get("body") or ""
    return check_pr_body(body)


def check_pr_body(body: str) -> list[Violation]:
    """Validate a PR body against the Ground Control template requirements.

    Pure function over the body string so it can be driven from GitHub event
    payloads (CI), a local draft file (pre-push hook), or `gh pr view --json
    body`. The CI path is `run_pr_body_check`; local tooling should call this
    directly.
    """
    violations: list[Violation] = []

    required_headers = [
        "## Requirement UIDs",
        "## ADR Impact",
        "## Ground Control Checks",
        "## Traceability",
    ]
    missing_headers = [header for header in required_headers if header not in body]
    if missing_headers:
        violations.append(
            Violation(
                code="pr-template-sections",
                message="PR body is missing required Ground Control sections.",
                details=[f"missing headers: {', '.join(missing_headers)}"],
            )
        )
        return violations

    if not PR_REQUIREMENT_RE.search(body):
        violations.append(
            Violation(
                code="pr-requirement-uid",
                message="PR body must name at least one requirement UID.",
                details=["expected a UID like GC-O007 in the Requirement UIDs section"],
            )
        )

    if "No ADR required" not in body and "ADR-" not in body:
        violations.append(
            Violation(
                code="pr-adr-impact",
                message="PR body must call out ADR impact or say 'No ADR required'.",
                details=[],
            )
        )

    required_checks = [
        "- [x] `make policy` passes",
        "- [x] `gc_evaluate_quality_gates` passes or is unchanged by this repo-only change",
        "- [x] `gc_run_sweep` reviewed or intentionally deferred with reason",
    ]
    missing_checks = [entry for entry in required_checks if entry not in body]
    if missing_checks:
        violations.append(
            Violation(
                code="pr-ground-control-checks",
                message="PR body must record the Ground Control verification checklist.",
                details=missing_checks,
            )
        )

    traceability_markers = ["- IMPLEMENTS:", "- TESTS:"]
    missing_traceability = [marker for marker in traceability_markers if marker not in body]
    if missing_traceability:
        violations.append(
            Violation(
                code="pr-traceability-summary",
                message="PR body must summarize IMPLEMENTS and TESTS traceability.",
                details=missing_traceability,
            )
        )

    return violations


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run repo policy checks.")
    parser.add_argument("--base", help="Git base ref to diff against.")
    parser.add_argument(
        "--files",
        nargs="*",
        default=None,
        help="Explicit repo-relative files to evaluate.",
    )
    parser.add_argument(
        "paths",
        nargs="*",
        help="Positional repo-relative files to evaluate. Used by pre-commit.",
    )
    parser.add_argument("--files-env", help="Read newline-delimited files from an env var.")
    parser.add_argument("--staged", action="store_true", help="Read staged files from git.")
    parser.add_argument(
        "--skip-pr-body",
        action="store_true",
        help="Do not evaluate the GitHub pull request body.",
    )
    parser.add_argument(
        "--event-path",
        help="Path to a GitHub event payload. Defaults to GITHUB_EVENT_PATH when present.",
    )
    parser.add_argument(
        "--pr-body-file",
        help=(
            "Path to a plain-text PR body draft. Use this in pre-push hooks to "
            "validate the PR body before push. Mutually exclusive with --event-path / "
            "--pr-number; --pr-body-file wins when supplied."
        ),
    )
    parser.add_argument(
        "--pr-number",
        type=int,
        help=(
            "GitHub PR number. When set (and neither --pr-body-file nor "
            "--event-path is supplied), the body is fetched via "
            "`gh pr view <n> --json body`."
        ),
    )
    return parser.parse_args(argv)


def render_and_exit(violations: list[Violation]) -> int:
    if not violations:
        print("Policy checks passed.")
        return 0

    print("Policy checks failed:")
    for violation in violations:
        print(violation.render())
    return 1


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    explicit_files = args.files if args.files is not None else args.paths
    if args.files and args.paths:
        explicit_files = [*args.files, *args.paths]
    changed_files = read_changed_files(
        files=explicit_files,
        base=args.base,
        staged=args.staged,
        env_var=args.files_env,
    )

    violations = []
    violations.extend(run_adr_guard(changed_files))
    violations.extend(run_controller_contracts(changed_files))
    violations.extend(run_migration_policy(changed_files))
    violations.extend(run_deploy_compose_credential_passthrough())

    if not args.skip_pr_body:
        body = _resolve_pr_body(args)
        if body is not None:
            violations.extend(check_pr_body(body))

    return render_and_exit(violations)


def _resolve_pr_body(args: argparse.Namespace) -> str | None:
    """Resolve the PR body string from CLI args / environment, in priority order.

    1. ``--pr-body-file`` — local pre-push hook driver.
    2. ``--pr-number`` — fetched via ``gh pr view <n> --json body``.
    3. ``--event-path`` or ``GITHUB_EVENT_PATH`` — CI driver.

    Returns ``None`` when no source is configured (the check is skipped).
    """
    if args.pr_body_file:
        return Path(args.pr_body_file).read_text(encoding="utf-8")
    if args.pr_number is not None:
        result = subprocess.run(
            ["gh", "pr", "view", str(args.pr_number), "--json", "body", "--jq", ".body"],
            check=True,
            capture_output=True,
            text=True,
        )
        return result.stdout
    event_path = args.event_path or os.getenv("GITHUB_EVENT_PATH")
    if event_path:
        event = json.loads(Path(event_path).read_text(encoding="utf-8"))
        pull_request = event.get("pull_request") or {}
        return pull_request.get("body") or ""
    return None


if __name__ == "__main__":
    raise SystemExit(main())
