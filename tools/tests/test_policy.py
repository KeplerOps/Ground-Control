import json
import tempfile
import unittest
from pathlib import Path

from tools.policy.checks import (
    DEFERRAL_CASES_PATH,
    REPO_ROOT,
    check_pr_body,
    classify_deferral_language,
    parse_args,
    run_adr_guard,
    run_controller_contracts,
    run_deploy_compose_credential_passthrough,
    run_migration_policy,
    run_no_deferral_disposition_check,
    run_pr_body_check,
)


class PolicyChecksTest(unittest.TestCase):
    def test_adr_guard_requires_workflow_docs(self):
        violations = run_adr_guard([".claude/skills/implement/SKILL.md"])
        self.assertTrue(any(item.code == "workflow-guardrail-sync" for item in violations))

    def test_adr_guard_fires_on_canonical_implement_skill_path(self):
        violations = run_adr_guard(["skills/implement/SKILL.md"])
        self.assertTrue(any(item.code == "workflow-guardrail-sync" for item in violations))

    def test_controller_contracts_require_docs_mcp_and_webmvctest(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            test_file = root / "backend/src/test/java/com/keplerops/groundcontrol/unit/api/FooControllerTest.java"
            test_file.parent.mkdir(parents=True, exist_ok=True)
            test_file.write_text(
                "@WebMvcTest(FooController.class)\nclass FooControllerTest {}\n",
                encoding="utf-8",
            )
            violations = run_controller_contracts(
                ["backend/src/main/java/com/keplerops/groundcontrol/api/foo/FooController.java"],
                root=root,
            )
            codes = {item.code for item in violations}
            self.assertIn("controller-parity", codes)
            self.assertIn("controller-webmvctest-update", codes)

    def test_migration_policy_requires_smoke_and_e2e_updates(self):
        violations = run_migration_policy(
            ["backend/src/main/resources/db/migration/V999__example.sql"],
            root=REPO_ROOT,
        )
        self.assertTrue(any(item.code == "migration-smoke-sync" for item in violations))

    def test_pr_body_requires_new_sections(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            event_path = Path(tmp_dir) / "event.json"
            event_path.write_text(json.dumps({"pull_request": {"body": "## Summary\n\nMissing policy sections"}}))
            violations = run_pr_body_check(event_path)
            self.assertTrue(any(item.code == "pr-template-sections" for item in violations))

    def test_check_pr_body_accepts_string_directly(self):
        violations = check_pr_body("## Summary\n\nMissing policy sections")
        codes = {item.code for item in violations}
        self.assertIn("pr-template-sections", codes)

    def test_check_pr_body_passes_for_well_formed_body(self):
        body = (
            "## Summary\n\nFix.\n"
            "## Requirement UIDs\n\n- GC-X001\n"
            "## ADR Impact\n\nADR-026 added.\n"
            "## Ground Control Checks\n\n"
            "- [x] `make policy` passes\n"
            "- [x] `gc_evaluate_quality_gates` passes or is unchanged by this repo-only change\n"
            "- [x] `gc_run_sweep` reviewed; findings fixed or recorded with rationale\n"
            "## Traceability\n\n- IMPLEMENTS: foo\n- TESTS: bar\n"
        )
        self.assertEqual(check_pr_body(body), [])

    def test_deploy_compose_credential_passthrough_passes_on_committed_file(self):
        # The committed deploy/docker/docker-compose.prod.yml must enumerate the
        # ADR-026 credential and IP-allowlist env vars on the backend service so
        # an operator who fills .env still has them propagate into the container.
        # #828 was triggered by exactly this gap. Run the check against the live
        # repo file as the post-condition assertion.
        violations = run_deploy_compose_credential_passthrough(root=REPO_ROOT)
        self.assertEqual(violations, [], msg=f"unexpected violations: {[v.render() for v in violations]}")

    def test_deploy_compose_credential_passthrough_fires_when_keys_missing(self):
        # If the backend service's environment block stops enumerating the
        # ADR-026 keys, the check must fail loudly so the regression is caught
        # in `make policy` before it ships.
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            compose_path = root / "deploy/docker/docker-compose.prod.yml"
            compose_path.parent.mkdir(parents=True, exist_ok=True)
            compose_path.write_text(
                "services:\n"
                "  backend:\n"
                "    image: ghcr.io/keplerops/ground-control:latest\n"
                "    environment:\n"
                "      - GC_DATABASE_URL=${GC_DATABASE_URL}\n",
                encoding="utf-8",
            )
            violations = run_deploy_compose_credential_passthrough(root=root)
            codes = {item.code for item in violations}
            self.assertIn("deploy-compose-adr026-passthrough", codes)
            # The violation must enumerate the missing keys so the operator's
            # fix is unambiguous.
            details = " ".join(detail for v in violations for detail in v.details)
            self.assertIn("GC_SECURITY_ENABLED", details)
            self.assertIn("GROUNDCONTROL_SECURITY_CREDENTIALS_0_TOKEN", details)
            self.assertIn("GROUNDCONTROL_SECURITY_IP_ALLOWLIST_0", details)

    def test_deploy_compose_credential_passthrough_requires_all_five_slots(self):
        # The runbook tells operators they have five reserved slots; the policy
        # gate must enforce all five so a future diff stripping slot 4 fails
        # `make policy` rather than silently regressing the documented headroom.
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            compose_path = root / "deploy/docker/docker-compose.prod.yml"
            compose_path.parent.mkdir(parents=True, exist_ok=True)
            compose_path.write_text(
                "services:\n"
                "  backend:\n"
                "    environment:\n"
                "      - GC_SECURITY_ENABLED=${GC_SECURITY_ENABLED:-true}\n"
                "      - GC_SECURITY_OPENAPI_PUBLIC=${GC_SECURITY_OPENAPI_PUBLIC:-false}\n"
                # Only slots 0..3 declared — slot 4 missing.
                + "".join(
                    f"      - GROUNDCONTROL_SECURITY_CREDENTIALS_{i}_PRINCIPAL_NAME\n"
                    f"      - GROUNDCONTROL_SECURITY_CREDENTIALS_{i}_TOKEN\n"
                    f"      - GROUNDCONTROL_SECURITY_CREDENTIALS_{i}_ROLE\n"
                    for i in range(4)
                )
                + "".join(
                    f"      - GROUNDCONTROL_SECURITY_IP_ALLOWLIST_{i}\n"
                    for i in range(4)
                ),
                encoding="utf-8",
            )
            violations = run_deploy_compose_credential_passthrough(root=root)
            codes = {item.code for item in violations}
            self.assertIn("deploy-compose-adr026-passthrough", codes)
            details = " ".join(detail for v in violations for detail in v.details)
            self.assertIn("GROUNDCONTROL_SECURITY_CREDENTIALS_4_TOKEN", details)
            self.assertIn("GROUNDCONTROL_SECURITY_IP_ALLOWLIST_4", details)

    def test_deploy_compose_credential_passthrough_rejects_map_form_for_indexed_slots(self):
        # Map form with `${VAR:-}` defaults injects the variable into the
        # container as an empty string when the host variable is unset, which
        # SecurityProperties.validate() rejects (the failure mode that
        # codex flagged in #828 cycle 2). The policy gate must reject any
        # indexed slot in map form, even if the typed config keys remain in
        # map form (they have non-empty defaults so they are safe either way).
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            compose_path = root / "deploy/docker/docker-compose.prod.yml"
            compose_path.parent.mkdir(parents=True, exist_ok=True)
            body = "services:\n  backend:\n    environment:\n"
            body += "      GC_SECURITY_ENABLED: ${GC_SECURITY_ENABLED:-true}\n"
            body += "      GC_SECURITY_OPENAPI_PUBLIC: ${GC_SECURITY_OPENAPI_PUBLIC:-false}\n"
            # Indexed slots in map form — NOT acceptable.
            for i in range(5):
                body += f"      GROUNDCONTROL_SECURITY_CREDENTIALS_{i}_PRINCIPAL_NAME: ${{GROUNDCONTROL_SECURITY_CREDENTIALS_{i}_PRINCIPAL_NAME:-}}\n"
                body += f"      GROUNDCONTROL_SECURITY_CREDENTIALS_{i}_TOKEN: ${{GROUNDCONTROL_SECURITY_CREDENTIALS_{i}_TOKEN:-}}\n"
                body += f"      GROUNDCONTROL_SECURITY_CREDENTIALS_{i}_ROLE: ${{GROUNDCONTROL_SECURITY_CREDENTIALS_{i}_ROLE:-}}\n"
            for i in range(5):
                body += f"      GROUNDCONTROL_SECURITY_IP_ALLOWLIST_{i}: ${{GROUNDCONTROL_SECURITY_IP_ALLOWLIST_{i}:-}}\n"
            compose_path.write_text(body, encoding="utf-8")
            violations = run_deploy_compose_credential_passthrough(root=root)
            codes = {item.code for item in violations}
            self.assertIn("deploy-compose-adr026-inherit-only", codes)
            details = " ".join(detail for v in violations for detail in v.details)
            self.assertIn("GROUNDCONTROL_SECURITY_CREDENTIALS_0_TOKEN", details)
            self.assertIn("GROUNDCONTROL_SECURITY_IP_ALLOWLIST_4", details)

    def test_deploy_compose_credential_passthrough_rejects_list_form_with_default(self):
        # `- KEY=${VAR:-}` is also unsafe: it injects empty string when the
        # host variable is unset. Only bare `- KEY` (inherit-only) form is
        # acceptable for the indexed slots.
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            compose_path = root / "deploy/docker/docker-compose.prod.yml"
            compose_path.parent.mkdir(parents=True, exist_ok=True)
            body = "services:\n  backend:\n    environment:\n"
            body += "      - GC_SECURITY_ENABLED=${GC_SECURITY_ENABLED:-true}\n"
            body += "      - GC_SECURITY_OPENAPI_PUBLIC=${GC_SECURITY_OPENAPI_PUBLIC:-false}\n"
            for i in range(5):
                body += f"      - GROUNDCONTROL_SECURITY_CREDENTIALS_{i}_PRINCIPAL_NAME=${{GROUNDCONTROL_SECURITY_CREDENTIALS_{i}_PRINCIPAL_NAME:-}}\n"
                body += f"      - GROUNDCONTROL_SECURITY_CREDENTIALS_{i}_TOKEN=${{GROUNDCONTROL_SECURITY_CREDENTIALS_{i}_TOKEN:-}}\n"
                body += f"      - GROUNDCONTROL_SECURITY_CREDENTIALS_{i}_ROLE=${{GROUNDCONTROL_SECURITY_CREDENTIALS_{i}_ROLE:-}}\n"
            for i in range(5):
                body += f"      - GROUNDCONTROL_SECURITY_IP_ALLOWLIST_{i}=${{GROUNDCONTROL_SECURITY_IP_ALLOWLIST_{i}:-}}\n"
            compose_path.write_text(body, encoding="utf-8")
            violations = run_deploy_compose_credential_passthrough(root=root)
            codes = {item.code for item in violations}
            self.assertIn("deploy-compose-adr026-inherit-only", codes)

    def test_deploy_compose_credential_passthrough_handles_missing_file(self):
        # If the canonical compose file disappears entirely the check must fail
        # rather than silently pass — the absence is itself a regression worth
        # surfacing in `make policy`.
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            violations = run_deploy_compose_credential_passthrough(root=root)
            codes = {item.code for item in violations}
            self.assertIn("deploy-compose-missing", codes)

    def test_deferral_classifier_matches_golden_cases(self):
        # The shared golden-case file is the single source of truth for what
        # text, on what surface, gets flagged. The hook test
        # (tools/tests/test_block_defer_language.py) loads the same file, so
        # the hook's standalone classifier and this one cannot drift without
        # one of the two suites failing.
        cases = json.loads(DEFERRAL_CASES_PATH.read_text(encoding="utf-8"))["cases"]
        self.assertGreater(len(cases), 10, "deferral_cases.json should have a substantive case set")
        failures = []
        for case in cases:
            decision, pattern = classify_deferral_language(case["text"], case["surface"])
            if decision != case["expect"]:
                failures.append(
                    f"{case['id']}: surface={case['surface']} expected {case['expect']} "
                    f"got {decision} (pattern={pattern}) — {case['why']}"
                )
        self.assertEqual(failures, [], "\n".join(failures))

    def test_no_deferral_disposition_check_flags_tier1_in_pr_body(self):
        violations = run_no_deferral_disposition_check(
            pr_body="## Summary\n\nFixed the gate. SonarCloud findings deferred to a follow-up PR.\n"
        )
        codes = {v.code for v in violations}
        self.assertIn("pr-body-deferral-disposition", codes)
        details = " ".join(d for v in violations for d in v.details)
        self.assertIn("tier1:", details)

    def test_no_deferral_disposition_check_allows_out_of_scope_section(self):
        # A PR body legitimately scope-bounds its own work; bare "out of scope"
        # with no deferral verb is not flagged on the pr-body surface.
        body = (
            "## Summary\n\nImplements the gate.\n\n"
            "## Out of scope\n\n- Retroactive cleanup of past issues.\n"
            "- Changing the existing hard cap behavior.\n"
        )
        self.assertEqual(run_no_deferral_disposition_check(pr_body=body), [])

    def test_no_deferral_disposition_check_allows_amended_gc_run_sweep_line(self):
        # After the A4 wording fix, the Ground Control Checks line no longer
        # carries "deferred"; the scanner must not flag the template line.
        body = "## Summary\n\nx\n- [x] `gc_run_sweep` reviewed; findings fixed or recorded with rationale\n"
        self.assertEqual(run_no_deferral_disposition_check(pr_body=body), [])

    def test_no_deferral_disposition_check_noop_when_no_body(self):
        self.assertEqual(run_no_deferral_disposition_check(pr_body=None), [])

    def test_parse_args_accepts_pre_commit_positional_files(self):
        args = parse_args(["--skip-pr-body", "docs/WORKFLOW.md", "mcp/ground-control/lib.js"])
        self.assertEqual(args.paths, ["docs/WORKFLOW.md", "mcp/ground-control/lib.js"])
        self.assertIsNone(args.files)

    def test_parse_args_accepts_pr_body_file(self):
        args = parse_args(["--pr-body-file", "/tmp/pr-body.md"])
        self.assertEqual(args.pr_body_file, "/tmp/pr-body.md")

    def test_parse_args_accepts_pr_number(self):
        args = parse_args(["--pr-number", "790"])
        self.assertEqual(args.pr_number, 790)


if __name__ == "__main__":
    unittest.main()
