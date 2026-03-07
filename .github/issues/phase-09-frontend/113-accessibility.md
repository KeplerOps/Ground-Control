---
title: "Accessibility audit and WCAG 2.1 AA compliance"
labels: [frontend, ui, accessibility, quality]
phase: 9
priority: P1
---

## Description

Conduct an accessibility audit and ensure all frontend components meet WCAG 2.1 AA standards.

## References

- PRD: Section 7 (Non-Functional Requirements — WCAG 2.1 AA)

## Acceptance Criteria

- [ ] Automated accessibility testing: `eslint-plugin-jsx-a11y` + axe-core in Playwright tests
- [ ] Keyboard navigation for all interactive elements
- [ ] Screen reader support: ARIA labels, live regions, landmark roles
- [ ] Color contrast: all text meets 4.5:1 ratio (AA)
- [ ] Focus management: visible focus indicators, logical tab order
- [ ] Form accessibility: labels, error messages linked to inputs, required field indicators
- [ ] Data table accessibility: header associations, sortable columns announced
- [ ] Heat map: non-color alternatives (patterns, text overlays)
- [ ] CI check: axe-core scan on key pages
- [ ] Accessibility statement page
