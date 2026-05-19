package com.keplerops.groundcontrol.domain.testcases.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.model.TestCaseGherkin;
import com.keplerops.groundcontrol.domain.testcases.repository.TestCaseGherkinRepository;
import com.keplerops.groundcontrol.domain.testcases.repository.TestCaseRepository;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gherkin authored-content service for TC-004 / ADR-042. One Gherkin document
 * per parent {@link TestCase}; the {@code GHERKIN} format gate is enforced on
 * every write, and project scoping is checked against the parent test case
 * before any read or write.
 */
@Service
@Transactional
public class TestCaseGherkinService {

    private static final Logger log = LoggerFactory.getLogger(TestCaseGherkinService.class);

    private final TestCaseGherkinRepository gherkinRepository;
    private final TestCaseRepository testCaseRepository;
    private final GherkinValidator validator;

    public TestCaseGherkinService(
            TestCaseGherkinRepository gherkinRepository,
            TestCaseRepository testCaseRepository,
            GherkinValidator validator) {
        this.gherkinRepository = gherkinRepository;
        this.testCaseRepository = testCaseRepository;
        this.validator = validator;
    }

    public TestCaseGherkin create(CreateTestCaseGherkinCommand command) {
        var testCase = requireGherkinTestCase(command.projectId(), command.testCaseId());
        if (gherkinRepository.existsByTestCaseId(testCase.getId())) {
            throw new ConflictException("Gherkin source already exists for test case " + testCase.getUid());
        }
        validator.validate(command.source());
        var gherkin = gherkinRepository.save(new TestCaseGherkin(testCase, command.source()));
        log.info(
                "test_case_gherkin_created: test_case={} id={} length={}",
                testCase.getUid(),
                gherkin.getId(),
                command.source().length());
        return gherkin;
    }

    public TestCaseGherkin update(UUID projectId, UUID testCaseId, UpdateTestCaseGherkinCommand command) {
        var testCase = requireGherkinTestCase(projectId, testCaseId);
        var gherkin = findOrThrow(testCase);
        validator.validate(command.source());
        gherkin.setSource(command.source());
        gherkin = gherkinRepository.save(gherkin);
        log.info(
                "test_case_gherkin_updated: test_case={} id={} length={}",
                testCase.getUid(),
                gherkin.getId(),
                command.source().length());
        return gherkin;
    }

    @Transactional(readOnly = true)
    public TestCaseGherkin getByTestCase(UUID projectId, UUID testCaseId) {
        var testCase = requireTestCaseInProject(projectId, testCaseId);
        return findOrThrow(testCase);
    }

    public void deleteByTestCase(UUID projectId, UUID testCaseId) {
        var testCase = requireTestCaseInProject(projectId, testCaseId);
        var gherkin = findOrThrow(testCase);
        gherkinRepository.delete(gherkin);
        log.info("test_case_gherkin_deleted: test_case={} id={}", testCase.getUid(), gherkin.getId());
    }

    /**
     * Cascade-delete the Gherkin row when the parent test case is being removed.
     * Returns the number of rows deleted (0 if the parent never had a Gherkin
     * doc). Mirrors {@link TestCaseStepService#deleteAllByTestCase} so Envers
     * captures the delete via the Hibernate flush — a DB-level CASCADE would
     * silently lose the audit revision.
     */
    public long cascadeDeleteByTestCase(UUID testCaseId) {
        long deleted = gherkinRepository.deleteAllByTestCaseId(testCaseId);
        if (deleted > 0) {
            log.info("test_case_gherkin_cascade_deleted: test_case={} count={}", testCaseId, deleted);
        }
        return deleted;
    }

    /**
     * TC-005 / ADR-043 — Clone the source's Gherkin source onto {@code target}.
     * Returns 1 when a Gherkin row was cloned, 0 when the source had no
     * Gherkin content or the target is not in GHERKIN format. Goes through
     * Hibernate so Envers captures the insert on the target.
     */
    public int copyGherkinToTestCase(UUID sourceTestCaseId, TestCase target) {
        if (target.getFormat() != TestCaseFormat.GHERKIN) {
            return 0;
        }
        var sourceGherkin = gherkinRepository.findByTestCaseId(sourceTestCaseId).orElse(null);
        if (sourceGherkin == null) {
            return 0;
        }
        var clone = gherkinRepository.save(new TestCaseGherkin(target, sourceGherkin.getSource()));
        log.info(
                "test_case_gherkin_copied: source={} target={} id={}", sourceTestCaseId, target.getId(), clone.getId());
        return 1;
    }

    private TestCaseGherkin findOrThrow(TestCase testCase) {
        return gherkinRepository
                .findByTestCaseId(testCase.getId())
                .orElseThrow(
                        () -> new NotFoundException("Gherkin source not found for test case " + testCase.getUid()));
    }

    private TestCase requireGherkinTestCase(UUID projectId, UUID testCaseId) {
        var testCase = requireTestCaseInProject(projectId, testCaseId);
        if (testCase.getFormat() != TestCaseFormat.GHERKIN) {
            throw new DomainValidationException(
                    "Test case format must be GHERKIN to attach Gherkin content",
                    "invalid_test_case_format",
                    Map.of(
                            "field", "format",
                            "expected", TestCaseFormat.GHERKIN.name(),
                            "actual", testCase.getFormat().name()));
        }
        return testCase;
    }

    private TestCase requireTestCaseInProject(UUID projectId, UUID testCaseId) {
        return testCaseRepository
                .findByIdAndProjectId(testCaseId, projectId)
                .orElseThrow(() -> new NotFoundException("Test case not found: " + testCaseId));
    }
}
