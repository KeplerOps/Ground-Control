package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.service.TestCaseTreeNode;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import java.util.List;
import java.util.UUID;

public record TestCaseTreeNodeResponse(
        String kind,
        UUID id,
        UUID parentFolderId,
        String title,
        String description,
        int sortOrder,
        TestCaseLeaf testCase,
        List<TestCaseTreeNodeResponse> children) {

    public static TestCaseTreeNodeResponse from(TestCaseTreeNode node) {
        return new TestCaseTreeNodeResponse(
                node.kind().name(),
                node.id(),
                node.parentFolderId(),
                node.title(),
                node.description(),
                node.sortOrder(),
                node.testCase() == null
                        ? null
                        : new TestCaseLeaf(
                                node.testCase().uid(),
                                node.testCase().status(),
                                node.testCase().type(),
                                node.testCase().priority(),
                                node.testCase().format()),
                node.children().stream().map(TestCaseTreeNodeResponse::from).toList());
    }

    public record TestCaseLeaf(
            String uid, TestCaseStatus status, TestCaseType type, TestCasePriority priority, TestCaseFormat format) {}
}
