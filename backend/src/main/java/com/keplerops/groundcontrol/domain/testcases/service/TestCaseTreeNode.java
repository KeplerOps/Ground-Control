package com.keplerops.groundcontrol.domain.testcases.service;

import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.model.TestCaseFolder;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import java.util.List;
import java.util.UUID;

/**
 * TC-005 / ADR-043 — Tree node for hierarchical test repository browsing.
 *
 * <p>A node is either a folder or a test case (discriminated by {@link #kind()}).
 * Folder nodes carry nested {@code children}; test-case nodes are leaves and
 * carry the test-case payload via {@link #testCase()}.
 */
public record TestCaseTreeNode(
        Kind kind,
        UUID id,
        UUID parentFolderId,
        String title,
        String description,
        int sortOrder,
        TestCaseLeaf testCase,
        List<TestCaseTreeNode> children) {

    public enum Kind {
        FOLDER,
        TEST_CASE
    }

    public static TestCaseTreeNode folder(TestCaseFolder folder, List<TestCaseTreeNode> children) {
        return new TestCaseTreeNode(
                Kind.FOLDER,
                folder.getId(),
                folder.getParent() != null ? folder.getParent().getId() : null,
                folder.getTitle(),
                folder.getDescription(),
                folder.getSortOrder(),
                null,
                List.copyOf(children));
    }

    public static TestCaseTreeNode testCase(TestCase testCase) {
        return new TestCaseTreeNode(
                Kind.TEST_CASE,
                testCase.getId(),
                testCase.getParentFolder() != null ? testCase.getParentFolder().getId() : null,
                testCase.getTitle(),
                testCase.getDescription(),
                testCase.getSortOrder(),
                TestCaseLeaf.from(testCase),
                List.of());
    }

    public record TestCaseLeaf(
            String uid, TestCaseStatus status, TestCaseType type, TestCasePriority priority, TestCaseFormat format) {
        public static TestCaseLeaf from(TestCase t) {
            return new TestCaseLeaf(t.getUid(), t.getStatus(), t.getType(), t.getPriority(), t.getFormat());
        }
    }
}
