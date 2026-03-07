---
title: "Implement comment and notification entities"
labels: [data-model, backend, collaboration]
phase: 1
priority: P1
---

## Description

Create the comment (threaded, polymorphic) and notification entities for platform collaboration features.

## References

- Data Model: Section 2.19 (Notification & Comment)
- PRD: Section 4.7 (Workflow & Collaboration)
- User Stories: US-3.4 (Review and Approve Workpapers — review notes)

## Acceptance Criteria

- [ ] SQLAlchemy models: `Comment`, `Notification`
- [ ] Comment: polymorphic (entity_type + entity_id), threaded (parent_id), tenant-scoped
- [ ] Notification: user-targeted, typed, entity-linked, read status
- [ ] Notification types enum: assignment, deadline, review_request, evidence_request, mention, system
- [ ] Alembic migration with indexes on (entity_type, entity_id) and (user_id, is_read)
- [ ] Repositories for both entities
- [ ] Pydantic schemas
- [ ] Comment supports @-mention parsing (extract mentioned user IDs from body)
- [ ] Unit tests

## Technical Notes

- Comments and notifications are high-volume — indexes are critical for query performance
- @-mention format: `@[user_id]` in comment body, parsed to create notifications
- Notifications should be insertable in bulk (e.g., notify all reviewers)
