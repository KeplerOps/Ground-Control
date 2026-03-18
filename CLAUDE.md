Always use the package manager to install dependencies.
Always follow the coding standards.
Keep docs and ADRs up to date.
Always do the right thing, not the easy thing.

## Build

This is a Java 21 / Spring Boot 3.4 / Gradle project.

- Rapid dev loop: `make rapid` (format + compile, ~3-5s)
- Test: `make test` (unit tests, no static analysis)
- Format: `cd backend && ./gradlew spotlessApply`
- Lint: `cd backend && ./gradlew spotlessCheck`
- Full check: `make check` (CI-equivalent: build + tests + all static analysis)
- Full verify: `make verify` (check + integration tests + OpenJML ESC)
- Run: `cd backend && ./gradlew bootRun`

Use `make rapid` for the inner dev loop. Use `make check` before pushing.

## Frontend

React 19 / Vite 6 / TypeScript 5 / Tailwind 4 app in `frontend/`.

- Install: `cd frontend && npm install`
- Dev server: `cd frontend && npm run dev` (proxies `/api` to `:8000`)
- Build: `cd frontend && npm run build`
- Lint: `cd frontend && npm run lint`
- Format: `cd frontend && npm run format`

Or use `make frontend-install`, `make frontend-dev`, `make frontend-build`, etc.

## Package

`com.keplerops.groundcontrol`

Architecture: `api/ -> domain/ <- infrastructure/` (enforced by ArchUnit).
Domain layer has no Spring web imports.

## Development Philosophy (Pre-Alpha)

Ship features, not ceremony. L0 is the default assurance level. Add JML contracts only where invalid input causes silent data corruption (state transitions, security boundaries). One test per significant behavior. See docs/CODING_STANDARDS.md and ADR-012 for the full framework.

When implementing a feature, query Ground Control for related requirements. After completing work, create IMPLEMENTS and TESTS traceability links for any requirements you satisfied.
