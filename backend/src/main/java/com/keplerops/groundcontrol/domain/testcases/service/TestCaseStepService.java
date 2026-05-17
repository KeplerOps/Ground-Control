package com.keplerops.groundcontrol.domain.testcases.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.model.TestCaseStep;
import com.keplerops.groundcontrol.domain.testcases.repository.TestCaseRepository;
import com.keplerops.groundcontrol.domain.testcases.repository.TestCaseStepRepository;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TestCaseStepService {

    private static final Logger log = LoggerFactory.getLogger(TestCaseStepService.class);

    private final TestCaseStepRepository stepRepository;
    private final TestCaseRepository testCaseRepository;

    public TestCaseStepService(TestCaseStepRepository stepRepository, TestCaseRepository testCaseRepository) {
        this.stepRepository = stepRepository;
        this.testCaseRepository = testCaseRepository;
    }

    public TestCaseStep create(CreateTestCaseStepCommand command) {
        var testCase = testCaseRepository
                .findByIdAndProjectId(command.testCaseId(), command.projectId())
                .orElseThrow(() -> new NotFoundException("Test case not found: " + command.testCaseId()));
        if (testCase.getFormat() != TestCaseFormat.STEP_BASED) {
            throw new ConflictException("Test case " + testCase.getUid() + " has format " + testCase.getFormat()
                    + "; steps can only be added to STEP_BASED test cases");
        }
        if (stepRepository.existsByTestCaseIdAndStepNumber(testCase.getId(), command.stepNumber())) {
            throw new ConflictException(
                    "Step number " + command.stepNumber() + " already exists in test case " + testCase.getUid());
        }
        var step = new TestCaseStep(testCase, command.stepNumber(), command.action(), command.expectedResult());
        step.setActualResult(command.actualResult());
        step = stepRepository.save(step);
        log.info(
                "test_case_step_created: test_case={} step_number={} id={}",
                testCase.getUid(),
                step.getStepNumber(),
                step.getId());
        return step;
    }

    public TestCaseStep update(UUID projectId, UUID testCaseId, UUID stepId, UpdateTestCaseStepCommand command) {
        var step = findStepOrThrow(projectId, testCaseId, stepId);
        if (command.stepNumber() != null && command.stepNumber() != step.getStepNumber()) {
            if (stepRepository.existsByTestCaseIdAndStepNumber(testCaseId, command.stepNumber())) {
                throw new ConflictException(
                        "Step number " + command.stepNumber() + " already exists in this test case");
            }
            step.setStepNumber(command.stepNumber());
        }
        if (command.action() != null) {
            step.setAction(command.action());
        }
        if (command.expectedResult() != null) {
            step.setExpectedResult(command.expectedResult());
        }
        if (command.clearActualResult()) {
            step.setActualResult(null);
        } else if (command.actualResult() != null) {
            step.setActualResult(command.actualResult());
        }
        step = stepRepository.save(step);
        log.info(
                "test_case_step_updated: id={} test_case={} step_number={}",
                step.getId(),
                testCaseId,
                step.getStepNumber());
        return step;
    }

    @Transactional(readOnly = true)
    public TestCaseStep getById(UUID projectId, UUID testCaseId, UUID stepId) {
        return findStepOrThrow(projectId, testCaseId, stepId);
    }

    @Transactional(readOnly = true)
    public List<TestCaseStep> listByTestCase(UUID projectId, UUID testCaseId) {
        requireTestCaseInProject(projectId, testCaseId);
        return stepRepository.findByTestCaseIdOrderByStepNumberAsc(testCaseId);
    }

    public void delete(UUID projectId, UUID testCaseId, UUID stepId) {
        var step = findStepOrThrow(projectId, testCaseId, stepId);
        stepRepository.delete(step);
        log.info("test_case_step_deleted: id={} test_case={}", stepId, testCaseId);
    }

    public long deleteAllByTestCase(UUID testCaseId) {
        long deleted = stepRepository.deleteAllByTestCaseId(testCaseId);
        if (deleted > 0) {
            log.info("test_case_steps_deleted: test_case={} count={}", testCaseId, deleted);
        }
        return deleted;
    }

    /**
     * TC-005 / ADR-043 — Clone every step from {@code sourceTestCaseId} onto
     * {@code target}. Used by {@link TestCaseService#copy} so a STEP_BASED
     * copy carries its authored children. The clones go through Hibernate so
     * Envers captures them as inserts on the target. Returns the number of
     * cloned rows.
     */
    public int copyStepsToTestCase(UUID sourceTestCaseId, TestCase target) {
        if (target.getFormat() != TestCaseFormat.STEP_BASED) {
            return 0;
        }
        var sourceSteps = stepRepository.findByTestCaseIdOrderByStepNumberAsc(sourceTestCaseId);
        int count = 0;
        for (TestCaseStep source : sourceSteps) {
            var clone =
                    new TestCaseStep(target, source.getStepNumber(), source.getAction(), source.getExpectedResult());
            // Actual result is run-time evidence (ADR-041); copying a definition
            // should leave the new test case's actual result blank.
            clone.setActualResult(null);
            stepRepository.save(clone);
            count++;
        }
        if (count > 0) {
            log.info("test_case_steps_copied: source={} target={} count={}", sourceTestCaseId, target.getId(), count);
        }
        return count;
    }

    private TestCaseStep findStepOrThrow(UUID projectId, UUID testCaseId, UUID stepId) {
        requireTestCaseInProject(projectId, testCaseId);
        return stepRepository
                .findByIdAndTestCaseId(stepId, testCaseId)
                .orElseThrow(() -> new NotFoundException("Test case step not found: " + stepId));
    }

    private void requireTestCaseInProject(UUID projectId, UUID testCaseId) {
        if (!testCaseRepository.existsByIdAndProjectId(testCaseId, projectId)) {
            throw new NotFoundException("Test case not found: " + testCaseId);
        }
    }
}
