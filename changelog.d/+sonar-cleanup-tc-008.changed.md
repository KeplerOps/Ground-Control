### Changed

- Address SonarCloud findings on the TC-008 PR (cycle 1): replaced multi-line policy comments with Javadoc on `TestPlanService.delete` / `TestSuiteService.delete`; consolidated three near-identical 422 controller tests behind a `@ParameterizedTest`; extracted multi-throw lambdas in test setups to single-call assertion targets; switched `isEqualTo(0)` to `isZero()`; dropped redundant `eq(...)` matchers when fixed values suffice.
