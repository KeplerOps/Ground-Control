#!/usr/bin/env python3
"""GitHub issue dependency graph analyzer.

Fetches issues from a GitHub repo via `gh` CLI, builds a NetworkX directed graph
of dependencies, and validates/analyzes the result.

Usage:
    python tools/issue_graph.py --fetch              # Fetch issues and show summary
    python tools/issue_graph.py --validate            # Detect cycles, orphans, bad deps
    python tools/issue_graph.py --critical-path       # Show longest dependency chain
    python tools/issue_graph.py --stale               # Find issues referencing dead tech
    python tools/issue_graph.py --export graph.json   # Export graph as JSON
    python tools/issue_graph.py --all                 # Run everything
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

import networkx as nx

# Tech that was removed in the FastAPI->Django migration (ADR-010)
STALE_TERMS = [
    "fastapi",
    "sqlalchemy",
    "alembic",
    "uvicorn",
    "python-jose",
    "passlib",
    "httpx",
    "starlette",
]

# Phase label pattern
PHASE_RE = re.compile(r"phase-(\d+)")

# Issue cross-reference pattern: #NNN in body text
# Matches "#123" but not URLs like "issues/123" (gh already resolves those)
ISSUE_REF_RE = re.compile(r"(?<!\w)#(\d+)\b")

# Priority label pattern
PRIORITY_RE = re.compile(r"^P(\d)$")

CACHE_FILE = Path(__file__).parent / ".issue_cache.json"
SDOC_FILE = Path(__file__).parent / "../../docs/requirements/project.sdoc"

# SDoc patterns
SDOC_UID_RE = re.compile(r"^UID:\s*(.+)$", re.MULTILINE)
SDOC_TITLE_RE = re.compile(r"^TITLE:\s*(.+)$", re.MULTILINE)
SDOC_COMMENT_RE = re.compile(r"^COMMENT:\s*(.+)$", re.MULTILINE)


def fetch_issues(*, use_cache: bool = False) -> list[dict]:
    """Fetch all open issues from the current repo via gh CLI."""
    if use_cache and CACHE_FILE.exists():
        print(f"Using cached issues from {CACHE_FILE}", file=sys.stderr)
        return json.loads(CACHE_FILE.read_text())

    print("Fetching issues from GitHub...", file=sys.stderr)
    result = subprocess.run(
        [
            "gh",
            "issue",
            "list",
            "--state",
            "all",
            "--limit",
            "500",
            "--json",
            "number,title,labels,body,state",
        ],
        capture_output=True,
        text=True,
        check=True,
    )
    issues = json.loads(result.stdout)

    # Cache for repeated runs
    CACHE_FILE.write_text(json.dumps(issues, indent=2))
    print(f"Fetched {len(issues)} issues, cached to {CACHE_FILE}", file=sys.stderr)
    return issues


def parse_phase(labels: list[dict]) -> int | None:
    """Extract phase number from issue labels."""
    for label in labels:
        m = PHASE_RE.match(label["name"])
        if m:
            return int(m.group(1))
    return None


def parse_priority(labels: list[dict]) -> str | None:
    """Extract priority (P0-P3) from issue labels."""
    for label in labels:
        m = PRIORITY_RE.match(label["name"])
        if m:
            return label["name"]
    return None


def parse_label_names(labels: list[dict]) -> list[str]:
    """Extract all label names."""
    return [label["name"] for label in labels]


def parse_dependencies(body: str, own_number: int) -> list[int]:
    """Extract issue numbers referenced in body text as dependencies."""
    if not body:
        return []
    refs = ISSUE_REF_RE.findall(body)
    # Filter out self-references and non-issue numbers (like PR refs)
    return [int(r) for r in refs if int(r) != own_number]


def find_stale_references(body: str, title: str) -> list[str]:
    """Check if issue text references removed/stale technology."""
    if not body:
        body = ""
    text = (title + " " + body).lower()
    return [term for term in STALE_TERMS if term in text]


def build_graph(issues: list[dict]) -> nx.DiGraph:
    """Build a directed graph from issues and their dependencies."""
    G = nx.DiGraph()

    # Index issues by number
    issue_map = {i["number"]: i for i in issues}

    # Add nodes
    for issue in issues:
        num = issue["number"]
        phase = parse_phase(issue["labels"])
        priority = parse_priority(issue["labels"])
        labels = parse_label_names(issue["labels"])
        stale = find_stale_references(issue.get("body", "") or "", issue["title"])

        G.add_node(
            num,
            title=issue["title"],
            phase=phase,
            priority=priority,
            labels=labels,
            state=issue["state"],
            stale_refs=stale,
        )

    # Add edges (dependency = referenced issue must come before this one)
    for issue in issues:
        num = issue["number"]
        deps = parse_dependencies(issue.get("body", "") or "", num)
        for dep in deps:
            if dep in issue_map:
                # Edge: dep -> num (dep must be done before num)
                G.add_edge(dep, num, type="explicit")

    return G


def validate_graph(G: nx.DiGraph) -> dict:
    """Validate the dependency graph for problems."""
    problems = {
        "cycles": [],
        "cross_phase_backward": [],
        "orphans": [],
        "stale_issues": [],
        "missing_phase": [],
    }

    # Detect cycles
    try:
        cycles = list(nx.simple_cycles(G))
        problems["cycles"] = cycles
    except nx.NetworkXNoCycle:
        pass

    # Find orphan issues (no edges at all)
    for node in G.nodes():
        if G.in_degree(node) == 0 and G.out_degree(node) == 0:
            problems["orphans"].append(node)

    # Find cross-phase backward dependencies
    for u, v in G.edges():
        u_phase = G.nodes[u].get("phase")
        v_phase = G.nodes[v].get("phase")
        if u_phase is not None and v_phase is not None:
            if u_phase > v_phase:
                problems["cross_phase_backward"].append(
                    {
                        "from": u,
                        "from_phase": u_phase,
                        "to": v,
                        "to_phase": v_phase,
                        "description": (
                            f"#{u} (phase-{u_phase}) is depended on by "
                            f"#{v} (phase-{v_phase}) — backward dependency"
                        ),
                    }
                )

    # Find stale issues
    for node, data in G.nodes(data=True):
        if data.get("stale_refs"):
            problems["stale_issues"].append(
                {
                    "number": node,
                    "title": data["title"],
                    "stale_refs": data["stale_refs"],
                    "state": data["state"],
                }
            )

    # Find issues without phase labels
    for node, data in G.nodes(data=True):
        if data.get("phase") is None:
            problems["missing_phase"].append(
                {
                    "number": node,
                    "title": data["title"],
                }
            )

    return problems


def find_critical_path(G: nx.DiGraph) -> list[int]:
    """Find the longest path through the dependency graph (critical path)."""
    if not nx.is_directed_acyclic_graph(G):
        print(
            "WARNING: Graph has cycles, cannot compute critical path.", file=sys.stderr
        )
        # Remove cycles by working on a copy
        H = G.copy()
        while not nx.is_directed_acyclic_graph(H):
            try:
                cycle = nx.find_cycle(H)
                # Remove the last edge in the cycle to break it
                H.remove_edge(*cycle[-1][:2])
            except nx.NetworkXNoCycle:
                break
        return nx.dag_longest_path(H)
    return nx.dag_longest_path(G)


def phase_summary(G: nx.DiGraph) -> dict[int, list[dict]]:
    """Group issues by phase with dependency info."""
    phases: dict[int, list[dict]] = {}
    for node, data in G.nodes(data=True):
        phase = data.get("phase")
        if phase is None:
            phase = -1  # Unphased
        if phase not in phases:
            phases[phase] = []
        phases[phase].append(
            {
                "number": node,
                "title": data["title"],
                "priority": data.get("priority"),
                "state": data["state"],
                "in_degree": G.in_degree(node),
                "out_degree": G.out_degree(node),
                "stale_refs": data.get("stale_refs", []),
                "depends_on": sorted(G.predecessors(node)),
                "blocks": sorted(G.successors(node)),
            }
        )
    return dict(sorted(phases.items()))


def blocking_issues(G: nx.DiGraph, top_n: int = 15) -> list[dict]:
    """Find issues that block the most other issues (highest out-degree)."""
    scored = []
    for node, data in G.nodes(data=True):
        descendants = len(nx.descendants(G, node))
        if descendants > 0:
            scored.append(
                {
                    "number": node,
                    "title": data["title"],
                    "phase": data.get("phase"),
                    "priority": data.get("priority"),
                    "state": data["state"],
                    "direct_blocks": G.out_degree(node),
                    "total_descendants": descendants,
                }
            )
    scored.sort(key=lambda x: x["total_descendants"], reverse=True)
    return scored[:top_n]


def print_summary(G: nx.DiGraph) -> None:
    """Print a human-readable summary of the graph."""
    print(f"\n{'=' * 70}")
    print("ISSUE DEPENDENCY GRAPH SUMMARY")
    print(f"{'=' * 70}")
    print(f"Total issues: {G.number_of_nodes()}")
    print(f"Total dependency edges: {G.number_of_edges()}")

    open_count = sum(1 for _, d in G.nodes(data=True) if d["state"] == "OPEN")
    closed_count = sum(1 for _, d in G.nodes(data=True) if d["state"] == "CLOSED")
    print(f"Open: {open_count}  Closed: {closed_count}")

    # Phase summary
    phases = phase_summary(G)
    print(f"\n{'─' * 70}")
    print("ISSUES BY PHASE")
    print(f"{'─' * 70}")
    for phase, issues in phases.items():
        phase_label = f"phase-{phase}" if phase >= 0 else "no-phase"
        open_issues = [i for i in issues if i["state"] == "OPEN"]
        closed_issues = [i for i in issues if i["state"] == "CLOSED"]
        print(
            f"\n  {phase_label} ({len(open_issues)} open, {len(closed_issues)} closed):"
        )
        for issue in sorted(issues, key=lambda x: x["number"]):
            state_mark = "x" if issue["state"] == "CLOSED" else " "
            priority = issue["priority"] or "  "
            stale_flag = " [STALE]" if issue["stale_refs"] else ""
            deps = (
                f" (depends on: {issue['depends_on']})" if issue["depends_on"] else ""
            )
            blocks = f" (blocks: {issue['blocks']})" if issue["blocks"] else ""
            print(
                f"    [{state_mark}] #{issue['number']:>3} {priority} "
                f"{issue['title'][:55]}{stale_flag}{deps}{blocks}"
            )


def print_validation(problems: dict) -> None:
    """Print validation results."""
    print(f"\n{'─' * 70}")
    print("VALIDATION RESULTS")
    print(f"{'─' * 70}")

    has_problems = False

    if problems["cycles"]:
        has_problems = True
        print(f"\n  CYCLES DETECTED ({len(problems['cycles'])}):")
        for cycle in problems["cycles"][:10]:
            print(f"    {' -> '.join(f'#{n}' for n in cycle)}")

    if problems["cross_phase_backward"]:
        has_problems = True
        print(
            f"\n  CROSS-PHASE BACKWARD DEPS ({len(problems['cross_phase_backward'])}):"
        )
        for dep in problems["cross_phase_backward"]:
            print(f"    {dep['description']}")

    if problems["stale_issues"]:
        has_problems = True
        print(f"\n  STALE ISSUES ({len(problems['stale_issues'])}):")
        for issue in problems["stale_issues"]:
            state = "CLOSED" if issue["state"] == "CLOSED" else "OPEN"
            print(
                f"    #{issue['number']} [{state}] {issue['title'][:50]} "
                f"— refs: {', '.join(issue['stale_refs'])}"
            )

    if problems["orphans"]:
        print(f"\n  ORPHAN ISSUES (no dependencies, {len(problems['orphans'])}):")
        for n in sorted(problems["orphans"])[:20]:
            print(f"    #{n}")

    if problems["missing_phase"]:
        print(f"\n  MISSING PHASE LABEL ({len(problems['missing_phase'])}):")
        for issue in problems["missing_phase"]:
            print(f"    #{issue['number']} {issue['title'][:55]}")

    if not has_problems:
        print("\n  No problems found.")


def print_critical_path(path: list[int], G: nx.DiGraph) -> None:
    """Print the critical path."""
    print(f"\n{'─' * 70}")
    print(f"CRITICAL PATH ({len(path)} issues)")
    print(f"{'─' * 70}")
    for i, node in enumerate(path):
        data = G.nodes[node]
        phase = (
            f"phase-{data['phase']}" if data.get("phase") is not None else "no-phase"
        )
        priority = data.get("priority", "  ") or "  "
        arrow = "  " if i == 0 else "→ "
        print(f"  {arrow}#{node:>3} {priority} [{phase}] {data['title'][:50]}")


def print_blocking(blockers: list[dict]) -> None:
    """Print top blocking issues."""
    print(f"\n{'─' * 70}")
    print("TOP BLOCKING ISSUES (by total downstream descendants)")
    print(f"{'─' * 70}")
    for b in blockers:
        phase = f"phase-{b['phase']}" if b.get("phase") is not None else "no-phase"
        state = "CLOSED" if b["state"] == "CLOSED" else "OPEN"
        print(
            f"  #{b['number']:>3} [{state}] [{phase}] {b['priority'] or '  '} "
            f"blocks {b['direct_blocks']} direct, {b['total_descendants']} total — "
            f"{b['title'][:45]}"
        )


SDOC_PARENT_RE = re.compile(r"- TYPE: Parent\s+VALUE:\s*(\S+)", re.MULTILINE)
SDOC_WAVE_RE = re.compile(r"Wave\s+(\d+)")


def parse_sdoc(sdoc_path: Path) -> list[dict]:
    """Parse requirements from an sdoc file.

    Returns a list of dicts with keys: uid, title, comment, issue_refs,
    parents (list of parent UIDs), wave (int or None).
    """
    text = sdoc_path.read_text()
    requirements = []

    # Track which wave (section) we're in by scanning section titles
    # Build a map of character offset -> wave number
    wave_ranges: list[tuple[int, int, int | None]] = []  # (start, end, wave)
    section_starts = [m.start() for m in re.finditer(r"\[\[SECTION\]\]", text)]
    section_ends = [m.start() for m in re.finditer(r"\[\[/SECTION\]\]", text)]

    for i, start in enumerate(section_starts):
        end = section_ends[i] if i < len(section_ends) else len(text)
        # Find the TITLE line after this section marker
        title_match = re.search(r"TITLE:\s*(.+)", text[start : start + 200])
        wave = None
        if title_match:
            wave_match = SDOC_WAVE_RE.search(title_match.group(1))
            if wave_match:
                wave = int(wave_match.group(1))
        wave_ranges.append((start, end, wave))

    def wave_for_offset(offset: int) -> int | None:
        for start, end, wave in wave_ranges:
            if start <= offset <= end:
                return wave
        return None

    # Split on [REQUIREMENT] blocks, tracking positions
    for m in re.finditer(r"\[REQUIREMENT\]", text):
        block_start = m.end()
        # Find end of this block
        block_end = len(text)
        for marker in ["[REQUIREMENT]", "[TEXT]", "[[SECTION]]", "[[/SECTION]]"]:
            idx = text.find(marker, block_start)
            if idx != -1 and idx < block_end:
                block_end = idx
        block = text[block_start:block_end]

        uid_match = SDOC_UID_RE.search(block)
        title_match = SDOC_TITLE_RE.search(block)
        comment_match = SDOC_COMMENT_RE.search(block)

        uid = uid_match.group(1).strip() if uid_match else None
        title = title_match.group(1).strip() if title_match else None
        comment = comment_match.group(1).strip() if comment_match else ""

        # Extract issue numbers from comment
        issue_refs = [int(n) for n in re.findall(r"#(\d+)", comment)]

        # Extract parent relations
        parents = SDOC_PARENT_RE.findall(block)

        # Determine wave from section context
        wave = wave_for_offset(m.start())

        if uid:
            requirements.append(
                {
                    "uid": uid,
                    "title": title or "",
                    "comment": comment,
                    "issue_refs": issue_refs,
                    "parents": parents,
                    "wave": wave,
                }
            )

    return requirements


def check_sdoc_gaps(issues: list[dict], sdoc_path: Path) -> dict:
    """Check traceability between sdoc requirements and GitHub issues.

    Returns dict with:
      - issues_without_reqs: open issues not referenced by any requirement
      - reqs_without_issues: requirements that reference no issues
      - refs_to_closed: requirements referencing closed issues
    """
    if not sdoc_path.exists():
        print(f"ERROR: sdoc not found at {sdoc_path}", file=sys.stderr)
        return {
            "issues_without_reqs": [],
            "reqs_without_issues": [],
            "refs_to_closed": [],
        }

    requirements = parse_sdoc(sdoc_path)
    issue_map = {i["number"]: i for i in issues}

    # All issue numbers referenced anywhere in sdoc
    all_sdoc_refs: set[int] = set()
    for req in requirements:
        all_sdoc_refs.update(req["issue_refs"])

    # Open issues not in sdoc
    open_numbers = {i["number"] for i in issues if i["state"] == "OPEN"}
    issues_without_reqs = sorted(open_numbers - all_sdoc_refs)

    # Requirements with no issue refs
    reqs_without_issues = [
        {"uid": r["uid"], "title": r["title"]}
        for r in requirements
        if not r["issue_refs"]
    ]

    # Requirements referencing closed issues
    refs_to_closed = []
    for req in requirements:
        for ref in req["issue_refs"]:
            issue = issue_map.get(ref)
            if issue and issue["state"] == "CLOSED":
                refs_to_closed.append(
                    {
                        "uid": req["uid"],
                        "title": req["title"],
                        "issue": ref,
                        "issue_title": issue["title"],
                    }
                )

    return {
        "issues_without_reqs": issues_without_reqs,
        "reqs_without_issues": reqs_without_issues,
        "refs_to_closed": refs_to_closed,
    }


def cross_check_sdoc_graph(
    requirements: list[dict],
    issues: list[dict],
    G: nx.DiGraph,
) -> dict:
    """Cross-check sdoc Parent relations against GitHub issue dependency graph.

    Returns dict with:
      - sdoc_parent_no_issue_dep: sdoc says A depends on B, but the
        underlying issues have no cross-reference
      - issue_dep_no_sdoc_parent: issues reference each other, but their
        sdoc requirements have no Parent relation
      - backward_wave_parents: sdoc Parent relation points from a lower
        wave to a higher wave (backward dependency)
    """
    # Build lookup maps
    uid_to_req = {r["uid"]: r for r in requirements}
    # Map issue number -> list of requirement UIDs that reference it
    issue_to_uids: dict[int, list[str]] = {}
    for req in requirements:
        for ref in req["issue_refs"]:
            issue_to_uids.setdefault(ref, []).append(req["uid"])

    # Check 0: self-referencing Parents (child and parent resolve to same issue)
    self_referencing = []
    for req in requirements:
        for parent_uid in req["parents"]:
            parent = uid_to_req.get(parent_uid)
            if not parent:
                continue
            overlap = set(req["issue_refs"]) & set(parent["issue_refs"])
            if overlap:
                self_referencing.append(
                    {
                        "child_uid": req["uid"],
                        "parent_uid": parent_uid,
                        "shared_issues": sorted(overlap),
                    }
                )

    # Check 1: sdoc Parent relation exists but no issue-level dependency
    sdoc_parent_no_issue_dep = []
    for req in requirements:
        for parent_uid in req["parents"]:
            parent = uid_to_req.get(parent_uid)
            if not parent:
                continue
            # Get all issues for child and parent requirements
            child_issues = set(req["issue_refs"])
            parent_issues = set(parent["issue_refs"])
            # Check if any parent issue -> child issue edge exists in G
            has_edge = False
            for pi in parent_issues:
                for ci in child_issues:
                    if G.has_edge(pi, ci) or G.has_edge(ci, pi):
                        has_edge = True
                        break
                if has_edge:
                    break
            if not has_edge and child_issues and parent_issues:
                sdoc_parent_no_issue_dep.append(
                    {
                        "child_uid": req["uid"],
                        "parent_uid": parent_uid,
                        "child_issues": sorted(child_issues),
                        "parent_issues": sorted(parent_issues),
                    }
                )

    # Check 2: issue-level dependency exists but no sdoc Parent relation
    # Build set of (parent_uid, child_uid) pairs from sdoc
    sdoc_parent_pairs: set[tuple[str, str]] = set()
    for req in requirements:
        for parent_uid in req["parents"]:
            sdoc_parent_pairs.add((parent_uid, req["uid"]))

    issue_dep_no_sdoc_parent = []
    for u, v in G.edges():
        u_data = G.nodes.get(u, {})
        v_data = G.nodes.get(v, {})
        # Only check open issues
        if u_data.get("state") != "OPEN" or v_data.get("state") != "OPEN":
            continue
        u_uids = issue_to_uids.get(u, [])
        v_uids = issue_to_uids.get(v, [])
        if not u_uids or not v_uids:
            continue
        # Check if any (parent=u_uid, child=v_uid) pair exists in sdoc
        has_sdoc_parent = False
        for u_uid in u_uids:
            for v_uid in v_uids:
                if (u_uid, v_uid) in sdoc_parent_pairs:
                    has_sdoc_parent = True
                    break
            if has_sdoc_parent:
                break
        if not has_sdoc_parent:
            issue_dep_no_sdoc_parent.append(
                {
                    "from_issue": u,
                    "from_title": u_data.get("title", ""),
                    "from_uids": u_uids,
                    "to_issue": v,
                    "to_title": v_data.get("title", ""),
                    "to_uids": v_uids,
                }
            )

    # Check 3: backward wave Parents (child in lower wave than parent)
    backward_wave_parents = []
    for req in requirements:
        if req["wave"] is None:
            continue
        for parent_uid in req["parents"]:
            parent = uid_to_req.get(parent_uid)
            if not parent or parent["wave"] is None:
                continue
            if parent["wave"] > req["wave"]:
                backward_wave_parents.append(
                    {
                        "child_uid": req["uid"],
                        "child_wave": req["wave"],
                        "parent_uid": parent_uid,
                        "parent_wave": parent["wave"],
                    }
                )

    return {
        "self_referencing": self_referencing,
        "sdoc_parent_no_issue_dep": sdoc_parent_no_issue_dep,
        "issue_dep_no_sdoc_parent": issue_dep_no_sdoc_parent,
        "backward_wave_parents": backward_wave_parents,
    }


def print_cross_check(cross: dict) -> None:
    """Print cross-check results."""
    print(f"\n{'─' * 70}")
    print("SDOC ↔ ISSUE GRAPH CROSS-CHECK")
    print(f"{'─' * 70}")

    has_problems = False

    if cross["self_referencing"]:
        has_problems = True
        print(
            f"\n  ERROR: SELF-REFERENCING PARENTS ({len(cross['self_referencing'])}):"
        )
        print(
            "  (Parent and child requirements resolve to the same issue — broken dependency)"
        )
        for item in cross["self_referencing"]:
            print(
                f"    {item['child_uid']} has parent {item['parent_uid']}, "
                f"but both map to issue(s) {['#' + str(i) for i in item['shared_issues']]}"
            )

    if cross["backward_wave_parents"]:
        has_problems = True
        print(f"\n  BACKWARD WAVE PARENTS ({len(cross['backward_wave_parents'])}):")
        for item in cross["backward_wave_parents"]:
            print(
                f"    {item['child_uid']} (Wave {item['child_wave']}) "
                f"has parent {item['parent_uid']} (Wave {item['parent_wave']})"
            )

    if cross["sdoc_parent_no_issue_dep"]:
        has_problems = True
        print(
            f"\n  SDOC PARENT WITHOUT ISSUE DEPENDENCY ({len(cross['sdoc_parent_no_issue_dep'])}):"
        )
        for item in cross["sdoc_parent_no_issue_dep"]:
            print(
                f"    {item['child_uid']} → parent {item['parent_uid']}: "
                f"issues {item['child_issues']} and {item['parent_issues']} "
                f"have no cross-reference"
            )

    if cross["issue_dep_no_sdoc_parent"]:
        has_problems = True
        print(
            f"\n  ISSUE DEPENDENCY WITHOUT SDOC PARENT ({len(cross['issue_dep_no_sdoc_parent'])}):"
        )
        for item in cross["issue_dep_no_sdoc_parent"]:
            print(
                f"    #{item['from_issue']} → #{item['to_issue']}: "
                f"reqs {item['from_uids']} and {item['to_uids']} "
                f"have no Parent relation"
            )

    if not has_problems:
        print("\n  No inconsistencies between sdoc and issue graph.")


def print_sdoc_gaps(gaps: dict, issue_map: dict[int, dict]) -> None:
    """Print sdoc gap analysis."""
    print(f"\n{'─' * 70}")
    print("SDOC ↔ GITHUB TRACEABILITY")
    print(f"{'─' * 70}")

    has_gaps = False

    if gaps["issues_without_reqs"]:
        has_gaps = True
        print(f"\n  OPEN ISSUES NOT IN SDOC ({len(gaps['issues_without_reqs'])}):")
        for num in gaps["issues_without_reqs"]:
            issue = issue_map.get(num, {})
            title = issue.get("title", "???")
            print(f"    #{num:>3} {title[:60]}")

    if gaps["reqs_without_issues"]:
        has_gaps = True
        print(
            f"\n  REQUIREMENTS WITH NO GITHUB ISSUE ({len(gaps['reqs_without_issues'])}):"
        )
        for req in gaps["reqs_without_issues"]:
            print(f"    {req['uid']}: {req['title'][:55]}")

    if gaps["refs_to_closed"]:
        has_gaps = True
        print(
            f"\n  REQUIREMENTS REFERENCING CLOSED ISSUES ({len(gaps['refs_to_closed'])}):"
        )
        for ref in gaps["refs_to_closed"]:
            print(
                f"    {ref['uid']} → #{ref['issue']} (CLOSED) {ref['issue_title'][:45]}"
            )

    if not has_gaps:
        print(
            "\n  No gaps. Every open issue has a requirement, every requirement has an issue."
        )


def export_graph(G: nx.DiGraph, path: str) -> None:
    """Export graph as JSON."""
    data = {
        "nodes": [],
        "edges": [],
        "summary": {
            "total_issues": G.number_of_nodes(),
            "total_edges": G.number_of_edges(),
            "is_dag": nx.is_directed_acyclic_graph(G),
        },
    }
    for node, attrs in G.nodes(data=True):
        data["nodes"].append({"number": node, **attrs})
    for u, v, attrs in G.edges(data=True):
        data["edges"].append({"from": u, "to": v, **attrs})

    Path(path).write_text(json.dumps(data, indent=2, default=str))
    print(f"Graph exported to {path}", file=sys.stderr)


def main() -> None:
    """CLI entry point."""
    parser = argparse.ArgumentParser(
        description="Analyze GitHub issue dependencies using NetworkX"
    )
    parser.add_argument("--fetch", action="store_true", help="Fetch issues from GitHub")
    parser.add_argument(
        "--cache", action="store_true", help="Use cached issues if available"
    )
    parser.add_argument(
        "--validate", action="store_true", help="Validate graph for problems"
    )
    parser.add_argument(
        "--critical-path", action="store_true", help="Show critical path"
    )
    parser.add_argument(
        "--stale", action="store_true", help="Find stale/obsolete issues"
    )
    parser.add_argument(
        "--blocking", action="store_true", help="Show top blocking issues"
    )
    parser.add_argument(
        "--export", type=str, metavar="FILE", help="Export graph as JSON"
    )
    parser.add_argument(
        "--sdoc-gaps",
        action="store_true",
        help="Check traceability between sdoc and GitHub issues",
    )
    parser.add_argument(
        "--cross-check",
        action="store_true",
        help="Cross-check sdoc Parent relations against issue dependencies",
    )
    parser.add_argument(
        "--sdoc",
        type=str,
        metavar="FILE",
        help="Path to sdoc file (default: docs/requirements/project.sdoc)",
    )
    parser.add_argument("--all", action="store_true", help="Run all analyses")

    args = parser.parse_args()

    # Default to --all if nothing specified
    if not any(
        [
            args.fetch,
            args.validate,
            args.critical_path,
            args.stale,
            args.blocking,
            args.export,
            args.sdoc_gaps,
            args.cross_check,
            args.all,
        ]
    ):
        args.all = True

    # Fetch or load issues
    issues = fetch_issues(use_cache=args.cache and not args.fetch)

    # Build graph
    G = build_graph(issues)

    if args.all or args.fetch:
        print_summary(G)

    if args.all or args.validate:
        problems = validate_graph(G)
        print_validation(problems)

    if args.all or args.critical_path:
        path = find_critical_path(G)
        print_critical_path(path, G)

    if args.all or args.blocking:
        blockers = blocking_issues(G)
        print_blocking(blockers)

    if args.all or args.stale:
        problems = validate_graph(G)
        if problems["stale_issues"]:
            # Already printed in validate, but if --stale alone, print here
            if not (args.all or args.validate):
                print(f"\n{'─' * 70}")
                print(f"STALE ISSUES ({len(problems['stale_issues'])})")
                print(f"{'─' * 70}")
                for issue in problems["stale_issues"]:
                    state = "CLOSED" if issue["state"] == "CLOSED" else "OPEN"
                    print(
                        f"  #{issue['number']} [{state}] {issue['title'][:50]} "
                        f"— refs: {', '.join(issue['stale_refs'])}"
                    )

    if args.all or args.sdoc_gaps or args.cross_check:
        sdoc_path = Path(args.sdoc) if args.sdoc else SDOC_FILE

    if args.all or args.sdoc_gaps:
        gaps = check_sdoc_gaps(issues, sdoc_path)
        issue_map = {i["number"]: i for i in issues}
        print_sdoc_gaps(gaps, issue_map)

    if args.all or args.cross_check:
        requirements = parse_sdoc(sdoc_path)
        cross = cross_check_sdoc_graph(requirements, issues, G)
        print_cross_check(cross)

    if args.export:
        export_graph(G, args.export)


if __name__ == "__main__":
    main()
