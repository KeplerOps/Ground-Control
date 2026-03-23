---
name: pr-review
description: Assess and review a GitHub PR, ensuring it is required, meets the project's coding standards and requirements, and addressing all code and security review issues.
argument-hint: <requirement-uid>
disable-model-invocation: true
---

DO NOT commit or push any changes to the PR.

# PR Review Requirement: $ARGUMENTS

## 1. Read the PR and ensure you are on the correct branch. Merge dev into the PR branch.

## 2. Ensure there is still a need for the PR and its proposed changes based on the state of the `dev` branch.

- If the PR is no longer needed, tell the user and stop.

## 3. Review the PR and ensure it meets the project's coding standards and requirements. Fix any issues.

## 4. Use the /review tool. Fix ALL issues. DO NOT defer issues until later. If any issues require more than minor changes, tell the user and stop.

## 5. Use the /security-review tool. Fix ALL issues. DO NOT defer issues until later. If any issues require more than minor changes, tell the user and stop.

## 6. Ensure there is a changelog entry for the PR.

## 7. Summarize the changes and report the status. Provide a succinct one line commit message recommendation.
