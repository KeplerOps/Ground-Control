---
title: "Scaffold frontend project (React + TypeScript + Vite)"
labels: [foundation, frontend, devex]
phase: 0
priority: P1
---

## Description

Create the React + TypeScript frontend project using Vite as the build tool. Establish the initial project configuration, dependency set, and folder structure for a modern SPA that will serve as Ground Control's web UI.

## References

- Architecture: Section 7 (React + TypeScript, Shadcn/ui + Tailwind CSS)
- PRD: Section 10 (v0.1 — Basic web UI)
- User Stories: All UI-related stories

## Acceptance Criteria

- [ ] `frontend/` directory with Vite + React + TypeScript scaffold
- [ ] `package.json` with core dependencies:
  - `react`, `react-dom`, `react-router-dom`
  - `@tanstack/react-query` (data fetching/caching)
  - `tailwindcss`, `postcss`, `autoprefixer`
  - `zod` (runtime validation)
  - `axios` or `ky` (HTTP client)
  - `lucide-react` (icons)
- [ ] Dev dependencies: `typescript`, `@types/react`, `@types/react-dom`, `eslint`, `prettier`, `vitest`, `@testing-library/react`, `playwright`
- [ ] `tsconfig.json` with `strict: true`, path aliases (`@/` → `src/`)
- [ ] `tailwind.config.ts` configured
- [ ] ESLint + Prettier configured per coding standards (#003)
- [ ] Basic `src/` structure:
  ```
  src/
  ├── components/     # Shared UI components
  ├── features/       # Feature-based modules
  ├── hooks/          # Custom React hooks
  ├── lib/            # Utilities, API client
  ├── pages/          # Route-level components
  ├── types/          # TypeScript type definitions
  └── App.tsx
  ```
- [ ] Dev server proxies `/api` to backend (Vite proxy config)
- [ ] `npm run dev`, `npm run build`, `npm run lint`, `npm run test` all work

## Technical Notes

- Shadcn/ui setup deferred to Phase 9 (#118) — just Tailwind for now
- Keep frontend lightweight at this stage; focus on build tooling and structure
