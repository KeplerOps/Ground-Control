---
title: "Implement Slack and Microsoft Teams notification integration"
labels: [backend, integrations, notifications]
phase: 5
priority: P2
---

## Description

Build Slack and Teams webhook integrations for sending notifications to team channels.

## References

- PRD: Section 4.7 (Notifications — Slack/Teams webhooks)
- PRD: Section 8.2 (Communication — Slack, Microsoft Teams)

## Acceptance Criteria

- [ ] Slack incoming webhook integration (configurable per tenant)
- [ ] Teams incoming webhook integration
- [ ] Message formatting: rich cards with entity details and action links
- [ ] Configurable: which events trigger channel notifications
- [ ] Channel routing: different events → different channels
- [ ] Admin configuration endpoint
- [ ] Unit tests with mock webhook endpoints
