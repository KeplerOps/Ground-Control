---
title: "Set up frontend UI component library and application shell"
labels: [frontend, ui]
phase: 9
priority: P0
---

## Description

Set up Shadcn/ui component library, Tailwind CSS design tokens, and build the application shell (layout, navigation, routing, auth context).

## References

- Architecture: Section 7 (Shadcn/ui + Tailwind CSS, React + TypeScript)
- Issue #005 (Frontend Scaffold)

## Acceptance Criteria

- [ ] Shadcn/ui initialized with components: Button, Input, Select, Dialog, Table, Card, Tabs, Badge, Toast, DropdownMenu, Form, Avatar
- [ ] Tailwind design tokens: colors (risk heat map scale), typography, spacing
- [ ] Application shell:
  - Sidebar navigation (collapsible)
  - Top bar with user menu, tenant selector, notifications
  - Breadcrumbs
  - Main content area with route outlet
- [ ] React Router routes:
  - `/login` — auth pages
  - `/dashboard` — main dashboard
  - `/risks`, `/controls`, `/assessments`, `/evidence`, `/findings`
  - `/admin/*` — admin pages
- [ ] Auth context: JWT storage, auto-refresh, protected routes
- [ ] API client: axios/ky instance with auth headers, error interceptors, tenant header
- [ ] React Query setup for data fetching/caching
- [ ] Dark mode toggle (Tailwind dark mode)
