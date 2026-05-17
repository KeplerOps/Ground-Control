package com.keplerops.groundcontrol.domain.testcases.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.model.TestCaseFolder;
import com.keplerops.groundcontrol.domain.testcases.repository.TestCaseFolderRepository;
import com.keplerops.groundcontrol.domain.testcases.repository.TestCaseRepository;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TC-005 / ADR-043 — Hierarchical organisation for test cases.
 *
 * <p>Folders are project-scoped, self-referencing, container-locally ordered
 * via {@code sort_order}, and uniquely titled per container. Move/reorder
 * operations stay inside one project; cycle protection rejects moving a
 * folder under itself or any of its descendants.
 */
@Service
@Transactional
public class TestCaseFolderService {

    private static final Logger log = LoggerFactory.getLogger(TestCaseFolderService.class);

    /**
     * Defensive ceiling on hierarchy depth during cycle checks. The product
     * statement says "unlimited nesting"; in practice anything past a few
     * thousand is malformed data and walking the whole chain would lock the
     * thread. The cap is well above any plausible real tree depth.
     */
    private static final int MAX_DEPTH = 10_000;

    private final TestCaseFolderRepository folderRepository;
    private final TestCaseRepository testCaseRepository;
    private final ProjectService projectService;

    public TestCaseFolderService(
            TestCaseFolderRepository folderRepository,
            TestCaseRepository testCaseRepository,
            ProjectService projectService) {
        this.folderRepository = folderRepository;
        this.testCaseRepository = testCaseRepository;
        this.projectService = projectService;
    }

    public TestCaseFolder create(CreateTestCaseFolderCommand command) {
        var project = projectService.getById(command.projectId());
        TestCaseFolder parent = null;
        if (command.parentFolderId() != null) {
            parent = requireFolderInProject(project.getId(), command.parentFolderId());
        }
        UUID parentId = parent != null ? parent.getId() : null;
        requireUniqueTitleInContainer(project.getId(), parentId, command.title());
        // Insertion semantics: shift target siblings before assigning the
        // new folder's sort_order (codex cycle-2 finding).
        int sortOrder = placeFolderInContainer(project.getId(), parentId, null, command.sortOrder());
        var folder = new TestCaseFolder(project, parent, command.title(), command.description(), sortOrder);
        folder = folderRepository.save(folder);
        log.info(
                "test_case_folder_created: project={} parent={} id={} title={}",
                project.getIdentifier(),
                parentId,
                folder.getId(),
                folder.getTitle());
        return folder;
    }

    public TestCaseFolder update(UUID projectId, UUID id, UpdateTestCaseFolderCommand command) {
        var folder = requireFolderInProject(projectId, id);
        if (command.title() != null && !command.title().equals(folder.getTitle())) {
            requireUniqueTitleInContainer(
                    projectId, folder.getParent() != null ? folder.getParent().getId() : null, command.title());
            folder.setTitle(command.title());
        }
        if (command.clearDescription()) {
            folder.setDescription(null);
        } else if (command.description() != null) {
            folder.setDescription(command.description());
        }
        folder = folderRepository.save(folder);
        log.info("test_case_folder_updated: id={} title={}", folder.getId(), folder.getTitle());
        return folder;
    }

    public TestCaseFolder move(UUID projectId, UUID folderId, MoveTestCaseFolderCommand command) {
        var folder = requireFolderInProject(projectId, folderId);
        TestCaseFolder newParent = null;
        if (command.parentFolderId() != null) {
            newParent = requireFolderInProject(projectId, command.parentFolderId());
        }
        if (newParent != null && wouldCreateCycle(folder, newParent)) {
            throw new ConflictException(
                    "Cannot move folder " + folder.getId() + " under itself or one of its descendants");
        }
        UUID currentParentId = folder.getParent() != null ? folder.getParent().getId() : null;
        UUID newParentId = newParent != null ? newParent.getId() : null;
        boolean sameContainer = Objects.equals(currentParentId, newParentId);
        if (!sameContainer) {
            requireTitleAvailableInContainer(projectId, newParentId, folder.getTitle(), folder.getId());
        }
        folder.setParent(newParent);
        // Same-container moves with an omitted sortOrder preserve the
        // folder's current placement so idempotent move calls do not
        // rebalance siblings or produce audit revisions. Explicit
        // sortOrder uses insertion semantics — siblings shift to make
        // room (codex cycle-2 finding).
        if (sameContainer && command.sortOrder() == null) {
            // No-op on sort_order; keep current value.
        } else {
            folder.setSortOrder(placeFolderInContainer(projectId, newParentId, folder.getId(), command.sortOrder()));
        }
        folder = folderRepository.save(folder);
        log.info(
                "test_case_folder_moved: id={} new_parent={} sort_order={}",
                folder.getId(),
                newParentId,
                folder.getSortOrder());
        return folder;
    }

    public void reorder(UUID projectId, ReorderTestCaseFoldersCommand command) {
        if (command.parentFolderId() != null
                && folderRepository
                        .findByIdAndProjectId(command.parentFolderId(), projectId)
                        .isEmpty()) {
            // Containment guard: a missing or cross-project parent must
            // surface as 404 rather than be silently no-op'd by an empty
            // sibling set (codex cycle-1 finding, parity with
            // TestCaseService.reorder).
            throw new NotFoundException("Test case folder not found: " + command.parentFolderId());
        }
        var siblings = command.parentFolderId() == null
                ? folderRepository.findRootByProjectIdOrderBySortOrder(projectId)
                : folderRepository.findByProjectIdAndParentIdOrderBySortOrder(projectId, command.parentFolderId());
        applyFolderOrdering(command.orderedFolderIds(), siblings);
    }

    public void delete(UUID projectId, UUID id) {
        var folder = requireFolderInProject(projectId, id);
        if (folderRepository.countByParentId(id) > 0) {
            throw new ConflictException(
                    "Folder " + folder.getTitle() + " still contains subfolders; move or delete them first");
        }
        if (testCaseRepository.countByParentFolderId(id) > 0) {
            throw new ConflictException(
                    "Folder " + folder.getTitle() + " still contains test cases; move or delete them first");
        }
        folderRepository.delete(folder);
        log.info("test_case_folder_deleted: id={} title={}", folder.getId(), folder.getTitle());
    }

    @Transactional(readOnly = true)
    public TestCaseFolder getById(UUID projectId, UUID id) {
        return requireFolderInProject(projectId, id);
    }

    @Transactional(readOnly = true)
    public List<TestCaseFolder> listByProject(UUID projectId) {
        return folderRepository.findByProjectIdOrderBySortOrder(projectId);
    }

    @Transactional(readOnly = true)
    public List<TestCaseTreeNode> getTree(UUID projectId) {
        // Single-batch fetch per entity type so the tree assembly is O(n)
        // without recursive repository calls. SectionService.getTree shape;
        // adapted for two entity kinds.
        var folders = folderRepository.findByProjectIdOrderBySortOrder(projectId);
        var testCases = testCaseRepository.findAllByProjectIdOrderBySortOrder(projectId);

        Map<UUID, List<TestCaseFolder>> childFoldersByParent = new LinkedHashMap<>();
        List<TestCaseFolder> rootFolders = new ArrayList<>();
        for (TestCaseFolder folder : folders) {
            if (folder.getParent() == null) {
                rootFolders.add(folder);
            } else {
                childFoldersByParent
                        .computeIfAbsent(folder.getParent().getId(), k -> new ArrayList<>())
                        .add(folder);
            }
        }

        Map<UUID, List<TestCase>> testCasesByParent = new LinkedHashMap<>();
        List<TestCase> rootTestCases = new ArrayList<>();
        for (TestCase testCase : testCases) {
            if (testCase.getParentFolder() == null) {
                rootTestCases.add(testCase);
            } else {
                testCasesByParent
                        .computeIfAbsent(testCase.getParentFolder().getId(), k -> new ArrayList<>())
                        .add(testCase);
            }
        }

        // Tie-breakers (sortOrder, createdAt, id) keep the order
        // deterministic when two siblings share a sort_order — codex
        // cycle-1 class finding. Applied to roots and to every container.
        Comparator<TestCaseFolder> folderOrder = Comparator.comparingInt(TestCaseFolder::getSortOrder)
                .thenComparing(TestCaseFolder::getCreatedAt)
                .thenComparing(TestCaseFolder::getId);
        Comparator<TestCase> testCaseOrder = Comparator.comparingInt(TestCase::getSortOrder)
                .thenComparing(TestCase::getCreatedAt)
                .thenComparing(TestCase::getId);
        rootFolders.sort(folderOrder);
        rootTestCases.sort(testCaseOrder);
        childFoldersByParent.values().forEach(list -> list.sort(folderOrder));
        testCasesByParent.values().forEach(list -> list.sort(testCaseOrder));

        Map<UUID, TestCaseTreeNode> folderNodesById =
                buildFolderNodesIteratively(rootFolders, childFoldersByParent, testCasesByParent);

        List<TestCaseTreeNode> roots = new ArrayList<>();
        for (TestCaseFolder folder : rootFolders) {
            roots.add(folderNodesById.get(folder.getId()));
        }
        for (TestCase testCase : rootTestCases) {
            roots.add(TestCaseTreeNode.testCase(testCase));
        }
        return roots;
    }

    /**
     * Iterative O(n) tree assembly (codex cycle-1 + cycle-2 findings).
     * Walks the already-grouped children map from each root with an
     * explicit stack to produce a post-order visit list, then builds
     * TestCaseTreeNode instances in that order so each parent finds its
     * children's nodes already constructed. Each folder is touched
     * exactly twice (push + pop) and no per-node parent walk is
     * performed, so a 5000-deep linear tree stays linear rather than
     * quadratic.
     *
     * <p>Recursive descent is intentionally avoided so a valid deep tree
     * does not blow the JVM stack (TC-005 promises unlimited nesting).
     * The {@code visited} set defends against malformed graphs that
     * slipped past cycle protection.
     */
    private Map<UUID, TestCaseTreeNode> buildFolderNodesIteratively(
            List<TestCaseFolder> rootFolders,
            Map<UUID, List<TestCaseFolder>> childFoldersByParent,
            Map<UUID, List<TestCase>> testCasesByParent) {
        List<TestCaseFolder> postOrder = new ArrayList<>();
        HashSet<UUID> visited = new HashSet<>();
        Deque<StackFrame> stack = new ArrayDeque<>();
        for (TestCaseFolder root : rootFolders) {
            stack.push(new StackFrame(root));
            while (!stack.isEmpty()) {
                StackFrame frame = stack.peek();
                if (frame.expanded) {
                    stack.pop();
                    postOrder.add(frame.folder);
                    continue;
                }
                if (!visited.add(frame.folder.getId())) {
                    // Already visited (shouldn't happen with a valid
                    // tree, but defend against malformed graphs).
                    stack.pop();
                    continue;
                }
                frame.expanded = true;
                List<TestCaseFolder> children = childFoldersByParent.getOrDefault(frame.folder.getId(), List.of());
                // Push in reverse so left-to-right child order pops first.
                for (int i = children.size() - 1; i >= 0; i--) {
                    stack.push(new StackFrame(children.get(i)));
                }
            }
        }

        Map<UUID, TestCaseTreeNode> folderNodesById = new HashMap<>();
        for (TestCaseFolder folder : postOrder) {
            List<TestCaseFolder> childFolders = childFoldersByParent.getOrDefault(folder.getId(), List.of());
            List<TestCase> childTestCases = testCasesByParent.getOrDefault(folder.getId(), List.of());
            List<TestCaseTreeNode> children = new ArrayList<>(childFolders.size() + childTestCases.size());
            for (TestCaseFolder child : childFolders) {
                children.add(folderNodesById.get(child.getId()));
            }
            for (TestCase testCase : childTestCases) {
                children.add(TestCaseTreeNode.testCase(testCase));
            }
            folderNodesById.put(folder.getId(), TestCaseTreeNode.folder(folder, children));
        }
        return folderNodesById;
    }

    /** Stack frame used by the iterative post-order traversal in {@link #buildFolderNodesIteratively}. */
    private static final class StackFrame {
        final TestCaseFolder folder;
        boolean expanded;

        StackFrame(TestCaseFolder folder) {
            this.folder = folder;
        }
    }

    private TestCaseFolder requireFolderInProject(UUID projectId, UUID folderId) {
        return folderRepository
                .findByIdAndProjectId(folderId, projectId)
                .orElseThrow(() -> new NotFoundException("Test case folder not found: " + folderId));
    }

    private void requireUniqueTitleInContainer(UUID projectId, UUID parentId, String title) {
        boolean exists = parentId == null
                ? folderRepository.existsRootByProjectIdAndTitle(projectId, title)
                : folderRepository.existsByProjectIdAndParentIdAndTitle(projectId, parentId, title);
        if (exists) {
            throw new ConflictException("A folder titled '" + title + "' already exists at this level");
        }
    }

    private void requireTitleAvailableInContainer(UUID projectId, UUID parentId, String title, UUID excludedId) {
        if (!titleAvailable(projectId, parentId, title, excludedId)) {
            throw new ConflictException("A folder titled '" + title + "' already exists at the target location");
        }
    }

    private boolean titleAvailable(UUID projectId, UUID parentId, String title, UUID excludedId) {
        var siblings = parentId == null
                ? folderRepository.findRootByProjectIdOrderBySortOrder(projectId)
                : folderRepository.findByProjectIdAndParentIdOrderBySortOrder(projectId, parentId);
        return siblings.stream()
                .noneMatch(f -> !f.getId().equals(excludedId) && f.getTitle().equals(title));
    }

    /**
     * Insertion semantics for an explicit sort order (codex cycle-2
     * finding): the caller hands us a target container and a desired
     * position; the helper renumbers the target container atomically so
     * the placing folder lands at the requested position and existing
     * siblings shift up by 1. Null {@code requested} appends. Source
     * container (on a cross-container move) is left with a gap; the
     * (sortOrder, createdAt, id) tie-breaker keeps it deterministic and
     * the reorder API can normalize.
     */
    private int placeFolderInContainer(UUID projectId, UUID parentId, UUID placingId, Integer requested) {
        if (requested != null && requested < 0) {
            throw new DomainValidationException(
                    "Sort order must be non-negative", "invalid_test_case_folder", Map.of());
        }
        List<TestCaseFolder> others = (parentId == null
                        ? folderRepository.findRootByProjectIdOrderBySortOrder(projectId)
                        : folderRepository.findByProjectIdAndParentIdOrderBySortOrder(projectId, parentId))
                .stream()
                        .filter(f -> placingId == null || !placingId.equals(f.getId()))
                        .toList();
        int pos = requested == null ? others.size() : Math.min(requested, others.size());
        for (int i = 0; i < others.size(); i++) {
            int newOrder = i < pos ? i : i + 1;
            TestCaseFolder sibling = others.get(i);
            if (sibling.getSortOrder() != newOrder) {
                sibling.setSortOrder(newOrder);
                folderRepository.save(sibling);
            }
        }
        return pos;
    }

    private boolean wouldCreateCycle(TestCaseFolder folder, TestCaseFolder proposedParent) {
        if (folder.getId() == null || proposedParent == null) {
            return false;
        }
        TestCaseFolder cursor = proposedParent;
        var visited = new HashSet<UUID>();
        for (int depth = 0; depth < MAX_DEPTH && cursor != null; depth++) {
            if (cursor.getId().equals(folder.getId())) {
                return true;
            }
            if (!visited.add(cursor.getId())) {
                // Already-malformed data; bail out conservatively.
                return true;
            }
            cursor = cursor.getParent();
        }
        // If we exited because of the depth cap, treat as cycle so the user
        // fixes the tree rather than silently allowing more breadth.
        return cursor != null;
    }

    /**
     * Reorder helper used by {@link #reorder}. Containment for the parent
     * folder is checked above; this helper just verifies the requested
     * list matches the current sibling set exactly and renumbers
     * sort_order = 0..N-1 via {@link SiblingOrderingHelper}.
     */
    private void applyFolderOrdering(List<UUID> orderedIds, List<TestCaseFolder> siblings) {
        SiblingOrderingHelper.applyOrdering(
                "test_case_folder", orderedIds, siblings, TestCaseFolder::getId, (folder, newOrder) -> {
                    folder.setSortOrder(newOrder);
                    folderRepository.save(folder);
                });
        log.info("test_case_folder_container_reordered: count={}", orderedIds.size());
    }
}
