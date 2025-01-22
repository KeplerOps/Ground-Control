#!/usr/bin/env python3

"""Ground Control main module for syncing JIRA tickets to local filesystem.

This module provides functionality to:
- Connect to JIRA using API tokens
- Download tickets and their relationships
- Create a local file structure mirroring the ticket hierarchy
- Save ticket content and metadata locally
"""

import argparse
import json
import os

from jira import JIRA

JIRA_URL = os.environ.get("JIRA_URL", "")
BOARD_ID = os.environ.get("JIRA_PROJECT", "")
DEFAULT_OUTPUT_DIR = "tickets"

# Authentication: typically via an API token
# Create tokens at: https://support.atlassian.com/atlassian-account/docs/
USERNAME = os.environ.get("JIRA_USERNAME", "<your-email>")
API_TOKEN = os.environ.get("JIRA_API_TOKEN", "<your-api-token>")


def sanitize_filename(s):
    """Convert string to a valid filename by removing or replacing invalid characters."""
    # Replace invalid characters with underscores
    invalid_chars = '<>:"/\\|?*'
    for c in invalid_chars:
        s = s.replace(c, "_")
    # Remove any leading/trailing spaces or dots
    s = s.strip(". ")
    return s


def cleanup_directory(directory):
    """Remove all contents of the specified directory."""
    if os.path.exists(directory):
        for item in os.listdir(directory):
            item_path = os.path.join(directory, item)
            if os.path.isdir(item_path):
                import shutil

                shutil.rmtree(item_path)
            else:
                os.remove(item_path)


def get_issue_relationships(issue, jira):
    """Get parent and children relationships for an issue."""
    relationships = {"parent": None, "children": []}

    # Debug: print available fields
    print(f"\nDebug: Checking fields for {issue.key}")
    for field_name in dir(issue.fields):
        if not field_name.startswith("_"):
            try:
                value = getattr(issue.fields, field_name)
                if value is not None:
                    print(f"  {field_name}: {value}")
            except AttributeError:
                # Skip fields that can't be accessed
                continue

    # Get parent (could be epic link or initiative)
    if hasattr(issue.fields, "parent") and issue.fields.parent is not None:
        parent = issue.fields.parent
        relationships["parent"] = {"key": parent.key, "type": parent.fields.issuetype.name}
    elif (
        hasattr(issue.fields, "customfield_10014") and issue.fields.customfield_10014
    ):  # Epic link field
        epic_key = issue.fields.customfield_10014
        epic = jira.issue(epic_key)
        relationships["parent"] = {"key": epic_key, "type": epic.fields.issuetype.name}

    return relationships


def get_type_prefix(issue_type):
    """Get a short prefix for the issue type."""
    type_lower = issue_type.lower()
    if "initiative" in type_lower:
        return "INI"
    elif "epic" in type_lower:
        return "EPIC"
    elif "story" in type_lower:
        return "STORY"
    else:
        return "TASK"


def create_ticket_directory(issue, jira, parent_dir=None):
    """Create directory for a ticket and write its contents."""
    # Get type prefix
    type_prefix = get_type_prefix(issue.fields.issuetype.name)

    # Create directory name with type prefix, key and truncated summary
    summary = sanitize_filename(issue.fields.summary)
    if len(summary) > 50:  # Truncate long summaries
        summary = summary[:47] + "..."

    dir_name = f"{type_prefix}-{issue.key}-{summary}"

    # Use parent directory if provided, otherwise use OUTPUT_DIR
    base_dir = parent_dir if parent_dir else DEFAULT_OUTPUT_DIR
    issue_dir = os.path.join(base_dir, dir_name)
    os.makedirs(issue_dir, exist_ok=True)

    # Get relationships
    relationships = get_issue_relationships(issue, jira)

    # Build metadata
    metadata = {
        "key": issue.key,
        "id": issue.id,
        "url": f"{JIRA_URL}/browse/{issue.key}",
        "type": str(issue.fields.issuetype.name),
        "status": str(issue.fields.status.name),
        "summary": issue.fields.summary,
        "reporter": str(issue.fields.reporter),
        "assignee": str(issue.fields.assignee) if issue.fields.assignee else None,
        "updated": str(issue.fields.updated),
    }

    # Add parent info if exists
    if relationships["parent"]:
        metadata["parent"] = relationships["parent"]

    # Write metadata
    metadata_path = os.path.join(issue_dir, "metadata.json")
    with open(metadata_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)
        f.write("\n")

    # Write the main ticket markdown file
    ticket_file = os.path.join(issue_dir, "ticket.md")
    with open(ticket_file, "w", encoding="utf-8") as f:
        # Write ticket header with metadata
        f.write(f"# {issue.key}: {issue.fields.summary}\n\n")

        # Write metadata section
        f.write("# Metadata\n\n")
        f.write(f"- Type: {issue.fields.issuetype.name}\n")
        f.write(f"- Status: {issue.fields.status.name}\n")
        f.write(f"- Reporter: {issue.fields.reporter}\n")
        f.write(f"- Assignee: {issue.fields.assignee if issue.fields.assignee else 'Unassigned'}\n")
        f.write(f"- Updated: {issue.fields.updated}\n")
        f.write(f"- URL: {JIRA_URL}/browse/{issue.key}\n")

        # Add parent info if exists
        if relationships["parent"]:
            parent = relationships["parent"]
            f.write(f"- Parent: [{parent['key']}]({JIRA_URL}/browse/{parent['key']})")
            if "summary" in parent:
                f.write(f" - {parent['summary']}")
            f.write("\n")
        f.write("\n")

        # Write description
        f.write("# Description\n\n")
        description = (
            issue.fields.description if issue.fields.description else "_No description provided_"
        )
        f.write(f"{description}\n\n")

        # Write comments if any
        comments = jira.comments(issue)
        if comments:
            f.write("# Comments\n\n")
            for c in comments:
                f.write(f"## {c.author.displayName} - {c.updated}\n\n")
                f.write(f"{c.body}\n\n")
        f.write("\n")

    return issue_dir


def check_directory(directory):
    """Check if directory exists and is empty, create if missing."""
    if os.path.exists(directory):
        if os.path.isdir(directory):
            if os.listdir(directory):
                raise ValueError(
                    f"Output directory '{directory}' exists and is not empty.\n"
                    "Please specify a different directory or remove the existing content."
                )
        else:
            raise ValueError(
                f"'{directory}' exists but is not a directory.\n" "Please specify a different path."
            )
    else:
        os.makedirs(directory)


def parse_args():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(
        description="Sync JIRA tickets to local filesystem with hierarchy."
    )
    parser.add_argument(
        "-o",
        "--output",
        default=DEFAULT_OUTPUT_DIR,
        help=f"Output directory (default: {DEFAULT_OUTPUT_DIR})",
    )
    parser.add_argument(
        "ticket",
        nargs="?",
        help="Specific ticket to fetch (e.g., SECOPS-123). If not provided, fetches all tickets.",
    )
    parser.add_argument(
        "-r",
        "--recursive",
        action="store_true",
        help="When fetching a specific ticket, also fetch its children recursively",
    )
    return parser.parse_args()


def main():
    """Run the main JIRA ticket sync process.

    Returns:
        int: Exit code (0 for success, 1 for error)
    """
    # Parse command line arguments
    args = parse_args()
    output_dir = args.output

    # Check and prepare output directory
    try:
        check_directory(output_dir)
    except ValueError as e:
        print(f"Error: {e}")
        return 1

    # Check for credentials
    print("Debug: Reading environment variables...")
    print(f"JIRA_USERNAME: {os.environ.get('JIRA_USERNAME', 'not set')}")
    print(f"JIRA_API_TOKEN: {'[hidden]' if os.environ.get('JIRA_API_TOKEN') else 'not set'}")

    default_username = os.environ.get("JIRA_USERNAME", "<your-email>")
    default_token = os.environ.get("JIRA_API_TOKEN", "<your-api-token>")
    if USERNAME == default_username or API_TOKEN == default_token:
        print("Error: Please set JIRA_USERNAME and JIRA_API_TOKEN environment variables")
        print(
            "Visit: https://support.atlassian.com/atlassian-account/docs/"
            "manage-api-tokens-for-your-atlassian-account/"
        )
        return 1

    # Connect to Jira
    jira = JIRA(server=JIRA_URL, basic_auth=(USERNAME, API_TOKEN))

    # Build JQL query based on arguments
    if args.ticket:
        if args.recursive:
            # Get the ticket and all its children
            jql = f"""project = {BOARD_ID} AND (
                key = {args.ticket} OR
                parent = {args.ticket} OR
                "Epic Link" = {args.ticket}
            )"""
        else:
            # Get just the specific ticket
            jql = f"project = {BOARD_ID} AND key = {args.ticket}"
    else:
        # Default behavior: get all tickets with hierarchy rules
        jql = f"""project = {BOARD_ID} AND type != Sub-task AND (
            issuetype in (Initiative, Epic) OR
            parent is not empty OR
            "Epic Link" is not empty OR
            (issuetype not in (Initiative, Epic) AND statusCategory != Done AND status != Cancelled)
        )"""

    # Collect all issues (paging through results)
    start_at = 0
    max_results = 50
    all_issues = []

    while True:
        issues_batch = jira.search_issues(jql, startAt=start_at, maxResults=max_results)
        if not issues_batch:
            break
        all_issues.extend(issues_batch)
        start_at += max_results
        # Temporary limit: stop after 20 tickets
        if len(all_issues) >= 50:
            all_issues = all_issues[:50]  # Ensure exactly 20 if we got more
            break
        if len(issues_batch) < max_results:
            break

    # Create output directory and unassigned directory
    os.makedirs(output_dir, exist_ok=True)
    unassigned_dir = os.path.join(output_dir, "0-UNASSIGNED")
    os.makedirs(unassigned_dir, exist_ok=True)

    # Sort issues by type to ensure proper hierarchy
    initiatives = []
    epics = []
    others = []

    for issue in all_issues:
        issue_type = issue.fields.issuetype.name.lower()
        if "initiative" in issue_type:
            initiatives.append(issue)
        elif "epic" in issue_type:
            epics.append(issue)
        else:
            others.append(issue)

    # Process in hierarchical order
    issue_dirs = {}  # Keep track of created directories

    # First: Create initiatives
    for issue in initiatives:
        issue_dirs[issue.key] = create_ticket_directory(issue, jira)

    # Second: Create epics under their initiatives
    for issue in epics:
        relationships = get_issue_relationships(issue, jira)
        if relationships["parent"] and relationships["parent"]["key"] in issue_dirs:
            issue_dirs[issue.key] = create_ticket_directory(
                issue, jira, issue_dirs[relationships["parent"]["key"]]
            )
        else:
            issue_dirs[issue.key] = create_ticket_directory(issue, jira)

    # Finally: Create stories/tasks under their epics or in unassigned
    for issue in others:
        relationships = get_issue_relationships(issue, jira)
        if relationships["parent"] and relationships["parent"]["key"] in issue_dirs:
            issue_dirs[issue.key] = create_ticket_directory(
                issue, jira, issue_dirs[relationships["parent"]["key"]]
            )
        else:
            issue_dirs[issue.key] = create_ticket_directory(issue, jira, unassigned_dir)

    print(f"Synced {len(all_issues)} open issues into '{output_dir}/'")
    print(f"- Initiatives: {len(initiatives)}")
    print(f"- Epics: {len(epics)}")
    print(f"- Stories/Tasks: {len(others)}")
    print("Tickets are organized in a hierarchy based on their relationships")

    return 0


if __name__ == "__main__":
    exit(main())
