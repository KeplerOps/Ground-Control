#!/usr/bin/env python3

import os
import json
from jira import JIRA

JIRA_URL = os.environ.get("JIRA_URL", "")
BOARD_ID = os.environ.get("JIRA_PROJECT", "")
OUTPUT_DIR = "tickets"

# Authentication: typically via an API token
# To create an API token: https://support.atlassian.com/atlassian-account/docs/manage-api-tokens-for-your-atlassian-account/
# Then store the token and username in environment variables or a safe config
USERNAME = os.environ.get("JIRA_USERNAME", "<your-email>")
API_TOKEN = os.environ.get("JIRA_API_TOKEN", "<your-api-token>")

def sanitize_filename(s):
    """Convert string to a valid filename by removing or replacing invalid characters."""
    # Replace invalid characters with underscores
    invalid_chars = '<>:"/\\|?*'
    for c in invalid_chars:
        s = s.replace(c, '_')
    # Remove any leading/trailing spaces or dots
    s = s.strip('. ')
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

def get_issue_relationships(issue):
    """Get parent and children relationships for an issue."""
    relationships = {
        "parent": None,
        "children": []
    }
    
    # Debug: print available fields
    print(f"\nDebug: Checking fields for {issue.key}")
    for field_name in dir(issue.fields):
        if not field_name.startswith('_'):
            try:
                value = getattr(issue.fields, field_name)
                if value is not None:
                    print(f"  {field_name}: {value}")
            except Exception:
                pass
    
    # Get parent (could be epic link or initiative)
    if hasattr(issue.fields, 'parent'):
        parent = issue.fields.parent
        relationships["parent"] = {
            "key": parent.key,
            "type": parent.fields.issuetype.name
        }
    elif hasattr(issue.fields, 'customfield_10014') and issue.fields.customfield_10014:  # Epic link field
        epic_key = issue.fields.customfield_10014
        relationships["parent"] = {
            "key": epic_key,
            "type": "Epic"
        }

    return relationships

def get_type_prefix(issue_type):
    """Get a short prefix for the issue type."""
    type_lower = issue_type.lower()
    if 'initiative' in type_lower:
        return 'INI'
    elif 'epic' in type_lower:
        return 'EPIC'
    elif 'story' in type_lower:
        return 'STORY'
    else:
        return 'TASK'

def create_ticket_directory(issue, jira, parent_dir=None):
    """Create directory for a ticket and write its contents."""
    # Get type prefix
    type_prefix = get_type_prefix(issue.fields.issuetype.name)
    
    # Create directory name with type prefix, key and truncated summary
    summary = issue.fields.summary
    if len(summary) > 50:  # Truncate long summaries
        summary = summary[:47] + "..."
    
    dir_name = f"{type_prefix}-{issue.key}-{summary}"
    # Replace invalid characters
    invalid_chars = '<>:"/\\|?*'
    for c in invalid_chars:
        dir_name = dir_name.replace(c, '_')
    dir_name = dir_name.strip('. ')
    
    # Use parent directory if provided, otherwise use OUTPUT_DIR
    base_dir = parent_dir if parent_dir else OUTPUT_DIR
    issue_dir = os.path.join(base_dir, dir_name)
    os.makedirs(issue_dir, exist_ok=True)

    # Get relationships
    relationships = get_issue_relationships(issue)

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
        description = issue.fields.description if issue.fields.description else "_No description provided_"
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

def main():
    # Check for credentials
    print(f"Debug: Reading environment variables...")
    print(f"JIRA_USERNAME: {os.environ.get('JIRA_USERNAME', 'not set')}")
    print(f"JIRA_API_TOKEN: {'[hidden]' if os.environ.get('JIRA_API_TOKEN') else 'not set'}")
    
    if USERNAME == "<your-email>" or API_TOKEN == "<your-api-token>":
        print("Error: Please set JIRA_USERNAME and JIRA_API_TOKEN environment variables")
        print("Visit: https://support.atlassian.com/atlassian-account/docs/manage-api-tokens-for-your-atlassian-account/")
        return

    # Connect to Jira
    jira = JIRA(
        server=JIRA_URL,
        basic_auth=(USERNAME, API_TOKEN)
    )

    # Build JQL query:
    # 1. All Initiatives and Epics
    # 2. Stories/Tasks that have parents
    # 3. Open Stories/Tasks without parents
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
        issues_batch = jira.search_issues(
            jql, startAt=start_at, maxResults=max_results
        )
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
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    unassigned_dir = os.path.join(OUTPUT_DIR, "0-UNASSIGNED")
    os.makedirs(unassigned_dir, exist_ok=True)
    
    # Sort issues by type to ensure proper hierarchy
    initiatives = []
    epics = []
    others = []
    
    for issue in all_issues:
        issue_type = issue.fields.issuetype.name.lower()
        if 'initiative' in issue_type:
            initiatives.append(issue)
        elif 'epic' in issue_type:
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
        relationships = get_issue_relationships(issue)
        if relationships["parent"] and relationships["parent"]["key"] in issue_dirs:
            issue_dirs[issue.key] = create_ticket_directory(issue, jira, issue_dirs[relationships["parent"]["key"]])
        else:
            issue_dirs[issue.key] = create_ticket_directory(issue, jira)

    # Finally: Create stories/tasks under their epics or in unassigned
    for issue in others:
        relationships = get_issue_relationships(issue)
        if relationships["parent"] and relationships["parent"]["key"] in issue_dirs:
            issue_dirs[issue.key] = create_ticket_directory(issue, jira, issue_dirs[relationships["parent"]["key"]])
        else:
            issue_dirs[issue.key] = create_ticket_directory(issue, jira, unassigned_dir)

    print(f"Synced {len(all_issues)} open issues into '{OUTPUT_DIR}/'")
    print(f"- Initiatives: {len(initiatives)}")
    print(f"- Epics: {len(epics)}")
    print(f"- Stories/Tasks: {len(others)}")
    print("Tickets are organized in a hierarchy based on their relationships")

if __name__ == "__main__":
    main()
