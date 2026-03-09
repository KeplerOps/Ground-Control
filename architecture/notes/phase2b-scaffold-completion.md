# Phase 2B: Scaffold Completion — TODO Checklist

## Step 1: Envers Migrations
- [x] Create `V003__create_revinfo.sql`: `revinfo` table with `revision_id SERIAL PK`, `revision_timestamp BIGINT NOT NULL`
- [x] Create `V004__create_requirement_audit.sql`: all `requirement` columns + `revision_id INT NOT NULL REFERENCES revinfo(revision_id)`, `revision_type SMALLINT NOT NULL`, composite PK `(id, revision_id)`
- [x] Create `V005__create_requirement_relation_audit.sql`: all `requirement_relation` columns (without FKs to requirement — audit tables don't enforce referential integrity) + `revision_id`, `revision_type`, composite PK `(id, revision_id)`
- [x] Verify column names match `application.yml` Envers config: `revision_id`, `revision_type`, `_audit` suffix
- [x] Integration tests confirm Hibernate validation passes with Envers audit tables (revinfo uses `rev`/`revtstmp` + `revinfo_seq` sequence, audit tables use `rev`/`revtype`)

## Step 2: Move Exception Hierarchy
- [x] Create `backend/src/main/java/com/keplerops/groundcontrol/domain/exception/` package
- [x] Move all 6 exception files, update `package` declaration in each
- [x] Update imports in `Requirement.java` (2 imports: `DomainValidationException`, `GroundControlException` is indirect)
- [x] Update imports in `RequirementRelation.java` (`DomainValidationException`)
- [x] Update imports in `GlobalExceptionHandler.java` (all 6 exception imports)
- [x] Update imports in `RequirementTest.java` (`DomainValidationException`)
- [x] Update imports in `TransitionPropertyTest.java` (`DomainValidationException`)
- [x] Update `ArchitectureTest.java` — change `GroundControlException.class` reference to new package
- [x] Delete empty `domain/requirements/exception/` directory
- [x] `./gradlew check` — all 22 tests pass

## Step 3: OpenJML Integration
- [x] Create `backend/gradle/openjml.gradle.kts` with three tasks:
  - [x] `downloadOpenJml` — downloads OpenJML 21-0.21 release tarball from GitHub, extracts to `backend/.openjml/`, skips if already present
  - [x] `openjmlEsc` — `Exec` task: runs `.openjml/openjml --esc -classpath <compile classpath> <domain source files>`, depends on `downloadOpenJml` and `compileJava`
  - [x] `openjmlRac` — `Exec` task: runs `.openjml/openjml --rac -classpath <compile classpath> -d build/classes/rac <domain source files>`, depends on `downloadOpenJml`
- [x] Add `.openjml/` to `backend/.gitignore`
- [x] Add `apply(from = "gradle/openjml.gradle.kts")` to `build.gradle.kts`
- [x] Run `./gradlew downloadOpenJml` — verify download works
- [x] Run `./gradlew openjmlEsc` on `domain/requirements/state/Status.java` first (no framework annotations, cleanest test)
- [x] If Status.java passes, expand to `domain/requirements/model/Requirement.java` and `RequirementRelation.java`
- [x] Entities fail due to JPA no-arg constructor (NullField false positives). Exception classes fail due to OpenJML's CharSequence.jml spec bug. ESC scoped to `state/` only. Documented in CODING_STANDARDS.md and ADR-012.
- [x] No JML contract issues found by ESC on state/ — all 4 enums pass
- [x] Update `architecture/adrs/012-formal-methods-process.md` — replaced placeholder text with actual commands, scope, and known limitations
- [ ] Add OpenJML ESC step to `.github/workflows/ci.yml` — deferred to Step 12

## Step 4: Static Analysis
- [x] Add SpotBugs Gradle plugin `com.github.spotbugs` to `build.gradle.kts` plugins block
- [x] Create `backend/config/spotbugs/exclusions.xml` — exclude JPA metamodel classes, test code, generated code, CT_CONSTRUCTOR_THROW on model/exception
- [x] Configure SpotBugs: effort `max`, report format `html`, use exclusions file
- [x] Add Error Prone plugin `net.ltgt.errorprone` to `build.gradle.kts` plugins block
- [x] Add `errorprone("com.google.errorprone:error_prone_core:2.36.0")` to `dependencies` as annotation processor
- [x] Configure Error Prone: `import net.ltgt.gradle.errorprone.errorprone`, disable MissingSummary
- [x] Add `checkstyle` plugin to `build.gradle.kts` plugins block
- [x] Create `backend/config/checkstyle/checkstyle.xml` — focused on naming, coding patterns, imports; allow `log` constant for SLF4J
- [x] Configure Checkstyle: version 10.21.1, config file path, exclude test sources
- [x] Wire all three into `check` task (SpotBugs and Checkstyle do this automatically, Error Prone runs at compile time)
- [x] `./gradlew check` — all findings fixed, build passes

## Step 5: JaCoCo Coverage Thresholds
- [x] Add `jacocoTestCoverageVerification` task to `build.gradle.kts`
- [x] Configure rule: BUNDLE 0.30 minimum (will increase as service/controller tests are added)
- [x] Add `check.dependsOn(jacocoTestCoverageVerification)`
- [x] `./gradlew check` — passes with current coverage

## Step 6: Testcontainers Base + Test Task Separation
- [x] Add test task separation to `build.gradle.kts`:
  - [x] `tasks.test` — `excludeTags("integration")`
  - [x] `tasks.register<Test>("integrationTest")` — `includeTags("integration")`, `shouldRunAfter(tasks.test)`
- [x] Create `BaseIntegrationTest.java`:
  - [x] `@SpringBootTest`, `@Testcontainers`, `@ActiveProfiles("test")`, `@Tag("integration")`
  - [x] Static `PostgreSQLContainer` with `postgres:16` image
  - [x] `@DynamicPropertySource` for datasource properties
- [x] Create `MigrationSmokeTest.java` extending `BaseIntegrationTest`:
  - [x] Test: Spring context boots successfully
  - [x] Test: query `flyway_schema_history`, verify V001-V005 all ran
  - [x] Test: verify `requirement`, `requirement_audit`, `revinfo` tables exist
- [x] `./gradlew test` — runs only unit tests (fast, no DB)
- [x] `./gradlew integrationTest` — 3 tests pass against Testcontainers Postgres
- [x] Note: Docker 29+ requires `docker-java.properties` with `api.version=1.44`; Testcontainers 1.21.1 upgraded

## Step 7: RequirementService
- [x] Create `CreateRequirementCommand.java` record
- [x] Create `UpdateRequirementCommand.java` record
- [x] Create `RequirementService.java` — `@Service`, `@Transactional`
- [x] JML contracts on L1 methods (transitionStatus, archive, createRelation); retained as docs on L0 CRUD methods
- [x] `RequirementServiceTest.java` — 20 test methods (Mockito), happy-path + violation for L1; one-per-behavior for L0
- [x] Implementation satisfies all contracts and tests
- [x] `./gradlew test` — all 42 tests pass
- [x] `./gradlew spotlessApply` — formatted

## Step 8: Integration Tests — Persistence + Envers
- [x] Create `RequirementServiceIntegrationTest.java` extending `BaseIntegrationTest`
- [x] Inject `RequirementService` (real, not mocked)
- [x] Test: `create()` persists, `getById()` retrieves same entity
- [x] Test: `getByUid()` retrieves by human-readable UID
- [x] Test: `transitionStatus()` from DRAFT → ACTIVE persists
- [x] Test: `archive()` sets archivedAt timestamp, persists
- [x] Test: duplicate UID on `create()` throws ConflictException
- [x] Test: self-loop `createRelation()` throws DomainValidationException
- [ ] Test: duplicate `(source, target, relationType)` throws exception — deferred (needs committed data)
- [x] Test: Envers audit — `create()` then `transitionStatus()`, AuditReader returns 2 revisions (uses TestTransaction.flagForCommit)
- [x] `./gradlew integrationTest` — 10 tests pass
- [x] Singleton container pattern for stable shared Postgres across test classes

## Step 9: REST Controller + DTOs
- [x] All 5 DTOs created as records with Jakarta validation
- [x] `RequirementController` with 9 endpoints
- [x] `MethodArgumentNotValidException` handler added to `GlobalExceptionHandler`
- [x] `./gradlew check` passes

## Step 10: Controller Integration Tests
- [x] 13 MockMvc tests: all endpoints happy-path + 404/409/422 error envelopes
- [x] `./gradlew integrationTest` — all 23 integration tests pass

## Step 11: ArchUnit Rules Update
- [x] Controllers must not access repositories directly
- [x] Controllers must not import domain entities (Response DTOs have `from()` mapping methods)
- [x] Services must reside in `..service..` packages
- [x] `./gradlew test --tests '*ArchitectureTest*'` passes

## Step 12: CI Workflow Update
- [x] `integration` job: Testcontainers, no external DB, artifact upload
- [x] `verify` job: OpenJML ESC after test
- [x] Removed standalone `architecture` job
- [x] `sonarcloud.yml`: combined `test integrationTest jacocoTestReport`

## Step 13: Design Notes + CHANGELOG
- [ ] Rewrite `architecture/notes/phase1-requirements-design.md` for Java
- [x] `CHANGELOG.md` 0.20.0 entry with all Phase 2B items
- [x] CHANGELOG follows Keep a Changelog format with semver
