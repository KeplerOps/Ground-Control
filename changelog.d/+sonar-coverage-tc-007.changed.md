### Changed

- Backfill TestSuite test coverage to clear SonarCloud 80% new-coverage gate on PR #924 (TestSuiteMemberTest + TestSuiteSourceRequirementTest entity invariants; TestCaseSpecificationsTest exercises each Specification lambda body; new TestSuiteServiceTest cases cover getById/getByUid/listByProject/delete/listMembers/listSourceRequirements/addMember-null/addSourceRequirement-null+duplicate/removeSourceRequirement+missing/mode-mismatch/reorder-resort/criteria-folder-not-found/all-criteria-composed paths). No behavior change.
