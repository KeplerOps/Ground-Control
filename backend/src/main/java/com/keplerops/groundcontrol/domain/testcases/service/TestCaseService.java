package com.keplerops.groundcontrol.domain.testcases.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.repository.TestCaseRepository;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TestCaseService {

    private static final Logger log = LoggerFactory.getLogger(TestCaseService.class);

    private final TestCaseRepository testCaseRepository;
    private final ProjectService projectService;
    private final TestCaseStepService testCaseStepService;
    private final TestCaseGherkinService testCaseGherkinService;

    public TestCaseService(
            TestCaseRepository testCaseRepository,
            ProjectService projectService,
            TestCaseStepService testCaseStepService,
            TestCaseGherkinService testCaseGherkinService) {
        this.testCaseRepository = testCaseRepository;
        this.projectService = projectService;
        this.testCaseStepService = testCaseStepService;
        this.testCaseGherkinService = testCaseGherkinService;
    }

    public TestCase create(CreateTestCaseCommand command) {
        var project = projectService.getById(command.projectId());
        if (testCaseRepository.existsByProjectIdAndUid(project.getId(), command.uid())) {
            throw new ConflictException("Test case with UID " + command.uid() + " already exists in this project");
        }
        var format = command.format() != null ? command.format() : TestCaseFormat.STEP_BASED;
        var testCase =
                new TestCase(project, command.uid(), command.title(), command.type(), command.priority(), format);
        testCase.setDescription(command.description());
        testCase.setPreconditions(command.preconditions());
        testCase.setPostconditions(command.postconditions());
        testCase.setEstimatedDurationSeconds(command.estimatedDurationSeconds());
        testCase = testCaseRepository.save(testCase);
        log.info(
                "test_case_created: uid={} project={} type={} format={} priority={}",
                testCase.getUid(),
                project.getIdentifier(),
                testCase.getType(),
                testCase.getFormat(),
                testCase.getPriority());
        return testCase;
    }

    public TestCase update(UUID projectId, UUID id, UpdateTestCaseCommand command) {
        var testCase = findOrThrow(projectId, id);
        if (command.title() != null) {
            testCase.setTitle(command.title());
        }
        if (command.type() != null) {
            testCase.setType(command.type());
        }
        if (command.priority() != null) {
            testCase.setPriority(command.priority());
        }
        if (command.clearDescription()) {
            testCase.setDescription(null);
        } else if (command.description() != null) {
            testCase.setDescription(command.description());
        }
        if (command.clearPreconditions()) {
            testCase.setPreconditions(null);
        } else if (command.preconditions() != null) {
            testCase.setPreconditions(command.preconditions());
        }
        if (command.clearPostconditions()) {
            testCase.setPostconditions(null);
        } else if (command.postconditions() != null) {
            testCase.setPostconditions(command.postconditions());
        }
        if (command.clearEstimatedDuration()) {
            testCase.setEstimatedDurationSeconds(null);
        } else if (command.estimatedDurationSeconds() != null) {
            testCase.setEstimatedDurationSeconds(command.estimatedDurationSeconds());
        }
        testCase = testCaseRepository.save(testCase);
        log.info("test_case_updated: uid={} id={}", testCase.getUid(), testCase.getId());
        return testCase;
    }

    @Transactional(readOnly = true)
    public TestCase getById(UUID projectId, UUID id) {
        return findOrThrow(projectId, id);
    }

    @Transactional(readOnly = true)
    public TestCase getByUid(String uid, UUID projectId) {
        return testCaseRepository
                .findByProjectIdAndUid(projectId, uid)
                .orElseThrow(() -> new NotFoundException("Test case not found: " + uid));
    }

    @Transactional(readOnly = true)
    public List<TestCase> listByProject(UUID projectId) {
        return testCaseRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public TestCase transitionStatus(UUID projectId, UUID id, TestCaseStatus newStatus) {
        var testCase = findOrThrow(projectId, id);
        testCase.transitionStatus(newStatus);
        testCase = testCaseRepository.save(testCase);
        log.info("test_case_status_changed: uid={} status={}", testCase.getUid(), newStatus);
        return testCase;
    }

    public void delete(UUID projectId, UUID id) {
        var testCase = findOrThrow(projectId, id);
        // Cascade authored children through Hibernate so Envers captures the
        // deletes. Mirrors ADR-041 §Cascade on parent deletion.
        testCaseStepService.deleteAllByTestCase(id);
        testCaseGherkinService.cascadeDeleteByTestCase(id);
        testCaseRepository.delete(testCase);
        log.info("test_case_deleted: uid={} id={}", testCase.getUid(), id);
    }

    private TestCase findOrThrow(UUID projectId, UUID id) {
        return testCaseRepository
                .findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new NotFoundException("Test case not found: " + id));
    }
}
