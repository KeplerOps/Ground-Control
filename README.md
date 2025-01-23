[![Quality Checks](https://github.com/KeplerOps/Ground-Control/actions/workflows/quality.yml/badge.svg)](https://github.com/KeplerOps/Ground-Control/actions/workflows/quality.yml)
[![Security Checks](https://github.com/KeplerOps/Ground-Control/actions/workflows/security.yml/badge.svg)](https://github.com/KeplerOps/Ground-Control/actions/workflows/security.yml)

# Ground Control

Download JIRA tickets to your filesystem. Keeps the hierarchy (initiatives -> epics -> stories) intact.

## Setup

1. Create a JIRA API token

2. Set environment variables:

```bash
JIRA_URL=https://your-org.atlassian.net
JIRA_PROJECT=YOUR-PROJECT
JIRA_USERNAME=your.email@example.com
JIRA_API_TOKEN=your-token
```

## Install

```bash
pip install poetry
poetry install
```

## Usage

```bash
# Download all tickets
poetry run ground-control

# Download a specific ticket
poetry run ground-control PROJ-123

# Download a ticket and its children
poetry run ground-control PROJ-123 -r

# Use a different output directory
poetry run ground-control -o /path/to/dir
```
