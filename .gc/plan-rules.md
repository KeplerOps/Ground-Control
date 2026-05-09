# Ground-Control plan rules

Mandatory constraints the `/implement` skill applies during plan phase.

- Plans that add database migrations MUST update the hardcoded version
  lists in `MigrationSmokeTest.java` and
  `RequirementsE2EIntegrationTest.java`.
- Plans that add `@Audited` JPA entities MUST add `@NotAudited` on any
  `@ManyToOne` references to non-audited entities (e.g., Project), and
  MUST include a Flyway migration for the `_audit` table.
- Plans that add API endpoints MUST include `@WebMvcTest` controller
  unit tests (not just integration tests). The sonar CI job does not run
  Testcontainers, so only unit tests contribute to SonarCloud coverage.
