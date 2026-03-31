---
name: merge
description: Merge current branch's PR to dev and clean up
disable-model-invocation: true
---

# Merge Current Branch

## Step 1: Identify PR

1. Determine the current branch: `git branch --show-current`
2. Find the PR: `gh pr list --head <branch> --json number,url,title,state`
3. If no open PR exists, report and stop.

## Step 2: Verify Checks

1. Confirm all checks are green: `gh pr checks <number>`
2. If any checks are failing, report which ones and stop.

## Step 3: Merge

1. `gh pr merge <number> --merge --delete-branch`

## Step 4: Local Cleanup

1. `git checkout dev`
2. `git pull origin dev`
3. Delete the local feature branch if it still exists: `git branch -d <branch>` (use -D if needed)

## Step 5: Report

- PR merged successfully
- Now on dev branch, up to date
- Local feature branch cleaned up
