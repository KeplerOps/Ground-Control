package com.keplerops.groundcontrol.domain.testcases.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.model.TestCaseFolder;
import com.keplerops.groundcontrol.domain.testcases.repository.TestCaseFolderRepository;
import com.keplerops.groundcontrol.domain.testcases.repository.TestCaseRepository;
import com.keplerops.groundcontrol.domain.testcases.repository.TestRunCaseResultRepository;
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
    private static final String FOLDER_NOT_FOUND_PREFIX = "Test case folder not found: ";

    private final TestCaseRepository testCaseRepository;
    private final TestCaseFolderRepository folderRepository;
    private final TestRunCaseResultRepository testRunCaseResultRepository;
    private final ProjectService projectService;
    private final TestCaseStepService testCaseStepService;
    private final TestCaseGherkinService testCaseGherkinService;

    public TestCaseService(
            TestCaseRepository testCaseRepository,
            TestCaseFolderRepository folderRepository,
            TestRunCaseResultRepository testRunCaseResultRepository,
            ProjectService projectService,
            TestCaseStepService testCaseStepService,
            TestCaseGherkinService testCaseGherkinService) {
        this.testCaseRepository = testCaseRepository;
        this.folderRepository = folderRepository;
        this.testRunCaseResultRepository = testRunCaseResultRepository;
        this.projectService = projectService;
        this.testCaseStepService = testCaseStepService;
        this.testCaseGherkinService = testCaseGherkinService;
    }

    public TestCase create(CreateTestCaseCommand command) {
        var project = projectService.getById(command.projectId());
        if (testCaseRepository.existsByProjectIdAndUid(project.getId(), command.uid())) {
            throw new ConflictException("Test case with UID " + command.uid() + " already exists in this project");
        }
        TestCaseFolder parentFolder = null;
        if (command.parentFolderId() != null) {
            parentFolder = folderRepository
                    .findByIdAndProjectId(command.parentFolderId(), project.getId())
                    .orElseThrow(() -> new NotFoundException(FOLDER_NOT_FOUND_PREFIX + command.parentFolderId()));
        }
        var format = command.format() != null ? command.format() : TestCaseFormat.STEP_BASED;
        var testCase =
                new TestCase(project, command.uid(), command.title(), command.type(), command.priority(), format);
        testCase.setDescription(command.description());
        testCase.setPreconditions(command.preconditions());
        testCase.setPostconditions(command.postconditions());
        testCase.setEstimatedDurationSeconds(command.estimatedDurationSeconds());
        testCase.setParentFolder(parentFolder);
        // Insertion semantics (codex cycle-2 finding): shift target
        // siblings before assigning sort_order so an explicit position is
        // honoured. placingId is null because the entity is not yet
        // persisted.
        UUID parentIdForPlacement = parentFolder != null ? parentFolder.getId() : null;
        testCase.setSortOrder(
                placeTestCaseInContainer(project.getId(), parentIdForPlacement, null, command.sortOrder()));
        testCase = testCaseRepository.save(testCase);
        log.info(
                "test_case_created: uid={} project={} parent_folder={} type={} format={} priority={}",
                testCase.getUid(),
                project.getIdentifier(),
                parentFolder != null ? parentFolder.getId() : null,
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
        // TC-008 / ADR-049: TestRunCaseResult rows carry a NOT NULL FK to
        // this test case (snapshotted execution evidence on a TestRun). Reject
        // deletion with a domain-aware conflict — execution history is
        // append-only and a parent delete that would orphan it is a user
        // decision (archive the runs first, or keep the test case).
        if (testRunCaseResultRepository.existsByTestCaseId(id)) {
            throw new ConflictException("Test case " + testCase.getUid()
                    + " has associated test-run results; archive or delete those runs first");
        }
        // Cascade authored children through Hibernate so Envers captures the
        // deletes. Mirrors ADR-041 §Cascade on parent deletion.
        testCaseStepService.deleteAllByTestCase(id);
        testCaseGherkinService.cascadeDeleteByTestCase(id);
        testCaseRepository.delete(testCase);
        log.info("test_case_deleted: uid={} id={}", testCase.getUid(), id);
    }

    /**
     * TC-005 / ADR-043 — Move a test case to another folder (or to the
     * project root). Project scope is enforced via {@code findByIdAndProjectId}
     * on both the test case and the target folder, so cross-project moves
     * are rejected.
     */
    public TestCase move(UUID projectId, UUID id, MoveTestCaseCommand command) {
        var testCase = findOrThrow(projectId, id);
        TestCaseFolder targetFolder = null;
        if (command.parentFolderId() != null) {
            targetFolder = folderRepository
                    .findByIdAndProjectId(command.parentFolderId(), projectId)
                    .orElseThrow(() -> new NotFoundException(FOLDER_NOT_FOUND_PREFIX + command.parentFolderId()));
        }
        UUID currentParentId =
                testCase.getParentFolder() != null ? testCase.getParentFolder().getId() : null;
        UUID newParentId = targetFolder != null ? targetFolder.getId() : null;
        boolean sameContainer = java.util.Objects.equals(currentParentId, newParentId);
        testCase.setParentFolder(targetFolder);
        // Same-container moves with omitted sortOrder preserve placement
        // (parity with TestCaseFolderService.move; codex cycle-1 finding).
        // Explicit sortOrder uses insertion semantics — target siblings
        // shift to make room (codex cycle-2 finding).
        if (!sameContainer || command.sortOrder() != null) {
            testCase.setSortOrder(
                    placeTestCaseInContainer(projectId, newParentId, testCase.getId(), command.sortOrder()));
        }
        testCase = testCaseRepository.save(testCase);
        log.info(
                "test_case_moved: uid={} id={} parent_folder={} sort_order={}",
                testCase.getUid(),
                testCase.getId(),
                targetFolder != null ? targetFolder.getId() : null,
                testCase.getSortOrder());
        return testCase;
    }

    /**
     * TC-005 / ADR-043 — Copy a test case to a (possibly different) container.
     * The copy is a new aggregate root with a caller-supplied UID, status
     * reset to DRAFT, and freshly cloned authored children. The source test
     * case is unchanged.
     */
    public TestCase copy(UUID projectId, UUID sourceId, CopyTestCaseCommand command) {
        var source = findOrThrow(projectId, sourceId);
        if (command.newUid() == null || command.newUid().isBlank()) {
            throw new ConflictException("Copy requires a new UID");
        }
        if (testCaseRepository.existsByProjectIdAndUid(projectId, command.newUid())) {
            throw new ConflictException("Test case with UID " + command.newUid() + " already exists in this project");
        }
        // Copy placement follows the same semantics as move (codex
        // cycle-1 finding): explicit null parentFolderId means the
        // project root; a UUID means that folder. The previous "preserve
        // source's folder when null" semantic conflated JSON omission
        // and explicit null, leaving the project root unreachable from a
        // test originally in a folder. Callers that want to keep the
        // copy in the source's folder must pass the source's
        // parentFolderId explicitly.
        TestCaseFolder targetFolder = null;
        if (command.parentFolderId() != null) {
            targetFolder = folderRepository
                    .findByIdAndProjectId(command.parentFolderId(), projectId)
                    .orElseThrow(() -> new NotFoundException(FOLDER_NOT_FOUND_PREFIX + command.parentFolderId()));
        }
        var copy = new TestCase(
                source.getProject(),
                command.newUid(),
                source.getTitle(),
                source.getType(),
                source.getPriority(),
                source.getFormat());
        copy.setDescription(source.getDescription());
        copy.setPreconditions(source.getPreconditions());
        copy.setPostconditions(source.getPostconditions());
        copy.setEstimatedDurationSeconds(source.getEstimatedDurationSeconds());
        copy.setParentFolder(targetFolder);
        // Persist first so the placement helper sees the copy in the
        // sibling query result and can exclude it correctly.
        copy = testCaseRepository.save(copy);
        UUID copyParentId = targetFolder != null ? targetFolder.getId() : null;
        copy.setSortOrder(placeTestCaseInContainer(projectId, copyParentId, copy.getId(), command.sortOrder()));
        copy = testCaseRepository.save(copy);
        // Status is implicitly DRAFT via the entity default — a copy is
        // unreviewed even when the source was APPROVED.
        testCaseStepService.copyStepsToTestCase(sourceId, copy);
        testCaseGherkinService.copyGherkinToTestCase(sourceId, copy);
        log.info(
                "test_case_copied: source_uid={} source_id={} new_uid={} new_id={} parent_folder={}",
                source.getUid(),
                source.getId(),
                copy.getUid(),
                copy.getId(),
                targetFolder != null ? targetFolder.getId() : null);
        return copy;
    }

    /**
     * TC-005 / ADR-043 — Bulk reorder of test cases within one container.
     * Delegates to {@link TestCaseFolderService#reorderTestCases} so the
     * applyOrdering algorithm has one home.
     */
    public void reorder(UUID projectId, ReorderTestCasesCommand command) {
        if (command.parentFolderId() != null
                && folderRepository
                        .findByIdAndProjectId(command.parentFolderId(), projectId)
                        .isEmpty()) {
            // Containment guard before touching siblings: a missing or
            // cross-project target must surface as 404 rather than being
            // silently no-op'd by an empty current-sibling set.
            throw new NotFoundException(FOLDER_NOT_FOUND_PREFIX + command.parentFolderId());
        }
        var siblings = command.parentFolderId() == null
                ? testCaseRepository.findRootByProjectIdOrderBySortOrder(projectId)
                : testCaseRepository.findByProjectIdAndParentFolderIdOrderBySortOrder(
                        projectId, command.parentFolderId());
        SiblingOrderingHelper.applyOrdering(
                "test_case", command.orderedTestCaseIds(), siblings, TestCase::getId, (testCase, newOrder) -> {
                    testCase.setSortOrder(newOrder);
                    testCaseRepository.save(testCase);
                });
        log.info(
                "test_case_container_reordered: project={} parent_folder={} count={}",
                projectId,
                command.parentFolderId(),
                command.orderedTestCaseIds().size());
    }

    /**
     * Insertion semantics for an explicit sort order (codex cycle-2
     * finding): renumbers the target container so the placing test
     * case lands at the requested position. Null {@code requested}
     * appends. The {@code placingId} is excluded from the sibling list
     * so same-container moves don't double-count the moved row; pass
     * {@code null} for create/copy where the entity is not yet in the
     * sibling query result.
     */
    private int placeTestCaseInContainer(UUID projectId, UUID parentFolderId, UUID placingId, Integer requested) {
        if (requested != null && requested < 0) {
            throw new ConflictException("Sort order must be non-negative");
        }
        List<TestCase> others = (parentFolderId == null
                        ? testCaseRepository.findRootByProjectIdOrderBySortOrder(projectId)
                        : testCaseRepository.findByProjectIdAndParentFolderIdOrderBySortOrder(
                                projectId, parentFolderId))
                .stream()
                        .filter(t -> placingId == null || !placingId.equals(t.getId()))
                        .toList();
        int pos = requested == null ? others.size() : Math.min(requested, others.size());
        for (int i = 0; i < others.size(); i++) {
            int newOrder = i < pos ? i : i + 1;
            TestCase sibling = others.get(i);
            if (sibling.getSortOrder() != newOrder) {
                sibling.setSortOrder(newOrder);
                testCaseRepository.save(sibling);
            }
        }
        return pos;
    }

    private TestCase findOrThrow(UUID projectId, UUID id) {
        return testCaseRepository
                .findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new NotFoundException("Test case not found: " + id));
    }
}
