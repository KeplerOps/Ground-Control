---
title: "Implement notification system (in-app + email)"
labels: [backend, collaboration, notifications]
phase: 5
priority: P1
---

## Description

Build the notification system that delivers in-app notifications and email alerts based on domain events and workflow actions.

## References

- PRD: Section 4.7 (Notifications — in-app, email, Slack/Teams)
- Data Model: Section 2.19 (Notification entity)

## Acceptance Criteria

- [ ] In-app notifications:
  - Created from domain events (assignment, deadline, review request, etc.)
  - Stored in notifications table
  - API: list, mark-read, mark-all-read, count unread
  - Real-time delivery via SSE (Server-Sent Events) or WebSocket (optional)
- [ ] Email notifications:
  - SMTP integration (configurable via settings)
  - HTML email templates (Jinja2)
  - Template types: assignment, evidence request, overdue, review, finding
  - Configurable per-user preferences (opt-in/out per event type)
  - Queued as background jobs
- [ ] Notification preferences: user can configure which events trigger email vs. in-app only
- [ ] Bulk notification creation (e.g., notify all reviewers)
- [ ] Unit tests with mock SMTP
