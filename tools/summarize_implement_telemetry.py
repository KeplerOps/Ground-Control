#!/usr/bin/env python3
# /implement step-telemetry summarizer (ADR-036).
#
# Reads one or more JSONL files written by `gc_log_step_telemetry` under
# `.gc/telemetry/<issue>-<sanitized-branch>.jsonl` and prints per-step and
# per-model totals (wall-time and token sums when available).
#
# Operational measurement only — this script never writes back, never gates a
# phase, never feeds the cycle-cap counter (per ADR-036).
#
# Usage:
#   python3 tools/summarize_implement_telemetry.py [PATH ...]
#
# Default PATH is .gc/telemetry/ (scans every *.jsonl). Each PATH may be a
# file or a directory. Unknown record shapes (missing or malformed fields)
# are skipped with a warning to stderr; valid records are aggregated.

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

SCHEMA = "gc.implement.telemetry/v1"


def iter_records(paths: list[Path]) -> "tuple[list[dict[str, Any]], int]":
    """Read every JSONL line under the supplied paths. Returns (records,
    skipped_count). Files that don't exist are skipped with a warning."""
    records: list[dict[str, Any]] = []
    skipped = 0
    for top in paths:
        if not top.exists():
            print(f"warning: path does not exist: {top}", file=sys.stderr)
            continue
        files = [top] if top.is_file() else sorted(top.glob("*.jsonl"))
        for file in files:
            try:
                text = file.read_text(encoding="utf-8")
            except OSError as e:
                print(f"warning: could not read {file}: {e}", file=sys.stderr)
                continue
            for lineno, raw in enumerate(text.splitlines(), start=1):
                stripped = raw.strip()
                if not stripped:
                    continue
                try:
                    record = json.loads(stripped)
                except json.JSONDecodeError as e:
                    print(
                        f"warning: skipping malformed line {file}:{lineno}: {e}",
                        file=sys.stderr,
                    )
                    skipped += 1
                    continue
                if not isinstance(record, dict):
                    skipped += 1
                    continue
                if record.get("schema") != SCHEMA:
                    skipped += 1
                    continue
                records.append(record)
    return records, skipped


def aggregate(records: list[dict[str, Any]]) -> "tuple[dict[str, Any], int]":
    """Group by step + model and sum wall-time / token counts.

    Returns (summary, malformed_count). Malformed records — non-int
    `wall_time_ms`, non-int token counts, missing `step` / `model` — are
    skipped with a stderr warning rather than crashing, so a single bad line
    cannot abort `make implement-cost-summary`.
    """
    by_step: dict[str, dict[str, Any]] = {}
    by_model: dict[str, dict[str, Any]] = {}
    total_wall = 0
    total_in = 0
    total_out = 0
    token_coverage = 0  # records that supplied token counts
    accepted = 0
    malformed = 0
    for idx, r in enumerate(records):
        # Strict type-check every numeric field before aggregating. Anything
        # that would have caused `int(...)` to raise is rejected up front so
        # `aggregate` cannot crash on a malformed record.
        wall_raw = r.get("wall_time_ms")
        if not isinstance(wall_raw, int) or wall_raw < 0:
            print(
                f"warning: record {idx}: wall_time_ms is not a non-negative int "
                f"({wall_raw!r}); skipping",
                file=sys.stderr,
            )
            malformed += 1
            continue
        in_tok = r.get("input_tokens")
        if in_tok is not None and (not isinstance(in_tok, int) or in_tok < 0):
            print(
                f"warning: record {idx}: input_tokens is not int|null ({in_tok!r}); "
                "skipping",
                file=sys.stderr,
            )
            malformed += 1
            continue
        out_tok = r.get("output_tokens")
        if out_tok is not None and (not isinstance(out_tok, int) or out_tok < 0):
            print(
                f"warning: record {idx}: output_tokens is not int|null ({out_tok!r}); "
                "skipping",
                file=sys.stderr,
            )
            malformed += 1
            continue
        step = r.get("step")
        model = r.get("model")
        if not isinstance(step, str) or step == "":
            print(
                f"warning: record {idx}: step missing or not a string; skipping",
                file=sys.stderr,
            )
            malformed += 1
            continue
        if not isinstance(model, str) or model == "":
            print(
                f"warning: record {idx}: model missing or not a string; skipping",
                file=sys.stderr,
            )
            malformed += 1
            continue
        accepted += 1
        total_wall += wall_raw
        if isinstance(in_tok, int):
            total_in += in_tok
        if isinstance(out_tok, int):
            total_out += out_tok
        if isinstance(in_tok, int) and isinstance(out_tok, int):
            token_coverage += 1

        step_row = by_step.setdefault(
            step,
            # `token_calls` counts records in this group that supplied token
            # counts (codex cycle-4 F5). Without it, a row with 10 calls and
            # tokens on 1 was indistinguishable from 10-call complete coverage
            # with near-zero usage — undermining cost measurement.
            {"calls": 0, "wall_time_ms": 0, "input_tokens": 0, "output_tokens": 0, "token_calls": 0},
        )
        step_row["calls"] += 1
        step_row["wall_time_ms"] += wall_raw
        if isinstance(in_tok, int):
            step_row["input_tokens"] += in_tok
        if isinstance(out_tok, int):
            step_row["output_tokens"] += out_tok
        if isinstance(in_tok, int) and isinstance(out_tok, int):
            step_row["token_calls"] += 1

        model_row = by_model.setdefault(
            model,
            {"calls": 0, "wall_time_ms": 0, "input_tokens": 0, "output_tokens": 0, "token_calls": 0},
        )
        model_row["calls"] += 1
        model_row["wall_time_ms"] += wall_raw
        if isinstance(in_tok, int):
            model_row["input_tokens"] += in_tok
        if isinstance(out_tok, int):
            model_row["output_tokens"] += out_tok
        if isinstance(in_tok, int) and isinstance(out_tok, int):
            model_row["token_calls"] += 1

    return (
        {
            "record_count": accepted,
            "total_wall_time_ms": total_wall,
            "total_input_tokens": total_in,
            "total_output_tokens": total_out,
            "token_coverage": token_coverage,
            "by_step": by_step,
            "by_model": by_model,
        },
        malformed,
    )


def render(summary: dict[str, Any]) -> str:
    lines: list[str] = []
    lines.append("# /implement telemetry summary")
    lines.append("")
    lines.append(f"Records aggregated: {summary['record_count']}")
    lines.append(f"Total wall time:    {summary['total_wall_time_ms']} ms")
    if summary["token_coverage"] > 0:
        lines.append(
            f"Tokens (in/out):    "
            f"{summary['total_input_tokens']} / {summary['total_output_tokens']} "
            f"({summary['token_coverage']} of {summary['record_count']} "
            f"records carried token counts)"
        )
    else:
        lines.append(
            "Tokens (in/out):    n/a — no record carried token counts "
            "(Claude Code Agent results do not surface per-call counts today)"
        )
    lines.append("")
    def fmt_tokens(group_row):
        # Render token totals with explicit per-group coverage. Codex cycle-4 F5:
        # a row with partial coverage (e.g. 1 of 10 calls had tokens) was
        # rendered as the full total, making partial sums indistinguishable
        # from complete totals. Now: when coverage < calls, the cell shows
        # "<sum> (n of N)"; when zero coverage, "n/a"; when full, just the
        # sum.
        tk = group_row.get("token_calls", 0)
        calls = group_row["calls"]
        if tk == 0:
            return ("n/a", "n/a")
        if tk < calls:
            return (
                f"{group_row['input_tokens']} ({tk} of {calls})",
                f"{group_row['output_tokens']} ({tk} of {calls})",
            )
        return (str(group_row["input_tokens"]), str(group_row["output_tokens"]))

    lines.append("## By step")
    lines.append("")
    lines.append("| Step | Calls | Wall ms | In tokens | Out tokens |")
    lines.append("|------|------:|--------:|----------:|-----------:|")
    for step in sorted(summary["by_step"]):
        row = summary["by_step"][step]
        in_s, out_s = fmt_tokens(row)
        lines.append(
            f"| {step} | {row['calls']} | {row['wall_time_ms']} | {in_s} | {out_s} |"
        )
    lines.append("")
    lines.append("## By model")
    lines.append("")
    lines.append("| Model | Calls | Wall ms | In tokens | Out tokens |")
    lines.append("|-------|------:|--------:|----------:|-----------:|")
    for model in sorted(summary["by_model"]):
        row = summary["by_model"][model]
        in_s, out_s = fmt_tokens(row)
        lines.append(
            f"| {model} | {row['calls']} | {row['wall_time_ms']} | {in_s} | {out_s} |"
        )
    return "\n".join(lines) + "\n"


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(
        description="Summarize /implement step telemetry (ADR-036).",
    )
    parser.add_argument(
        "paths",
        nargs="*",
        type=Path,
        help="files or directories to scan (default: .gc/telemetry/)",
    )
    parser.add_argument(
        "--json",
        action="store_true",
        help="emit raw aggregate JSON instead of human-readable Markdown",
    )
    args = parser.parse_args(argv)
    paths = args.paths or [Path(".gc/telemetry")]
    records, json_skipped = iter_records(paths)
    if json_skipped:
        print(
            f"warning: skipped {json_skipped} malformed/unknown JSONL line(s)",
            file=sys.stderr,
        )
    if not records:
        print("(no telemetry records found)", file=sys.stderr)
        return 0
    summary, agg_skipped = aggregate(records)
    if agg_skipped:
        print(
            f"warning: skipped {agg_skipped} record(s) with malformed field types",
            file=sys.stderr,
        )
    if summary["record_count"] == 0:
        print("(no well-formed telemetry records found)", file=sys.stderr)
        return 0
    if args.json:
        print(json.dumps(summary, indent=2, sort_keys=True))
    else:
        print(render(summary), end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
