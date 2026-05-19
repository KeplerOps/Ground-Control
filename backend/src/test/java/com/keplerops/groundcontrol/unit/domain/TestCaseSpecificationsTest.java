package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.repository.TestCaseSpecifications;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Exercise each {@link TestCaseSpecifications} factory through a mocked
 * {@link CriteriaBuilder} so the lambda body — not just the static
 * factory — runs. Asserts the right CB primitive is invoked for each
 * criterion shape (equality, IN, OR-of-LIKE).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
class TestCaseSpecificationsTest {

    private final Root<TestCase> root = mock(Root.class);
    private final CriteriaQuery query = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);

    private Path<Object> stubNestedPath(String first, String second) {
        Path<Object> outer = mock(Path.class);
        Path<Object> inner = mock(Path.class);
        when(root.get(first)).thenReturn(outer);
        when(outer.get(second)).thenReturn(inner);
        return inner;
    }

    @Test
    void hasProjectEqualsProjectId() {
        var projectId = UUID.randomUUID();
        Path<Object> idPath = stubNestedPath("project", "id");
        Predicate p = mock(Predicate.class);
        when(cb.equal(idPath, projectId)).thenReturn(p);

        var result = TestCaseSpecifications.hasProject(projectId).toPredicate(root, query, cb);

        assertThat(result).isSameAs(p);
    }

    @Test
    void hasStatusEqualsStatus() {
        Path<Object> statusPath = mock(Path.class);
        when(root.get("status")).thenReturn(statusPath);
        Predicate p = mock(Predicate.class);
        when(cb.equal(statusPath, TestCaseStatus.APPROVED)).thenReturn(p);

        var result = TestCaseSpecifications.hasStatus(TestCaseStatus.APPROVED).toPredicate(root, query, cb);

        assertThat(result).isSameAs(p);
    }

    @Test
    void hasTypeEqualsType() {
        Path<Object> typePath = mock(Path.class);
        when(root.get("type")).thenReturn(typePath);
        Predicate p = mock(Predicate.class);
        when(cb.equal(typePath, TestCaseType.AUTOMATED)).thenReturn(p);

        var result = TestCaseSpecifications.hasType(TestCaseType.AUTOMATED).toPredicate(root, query, cb);

        assertThat(result).isSameAs(p);
    }

    @Test
    void hasPriorityEqualsPriority() {
        Path<Object> priorityPath = mock(Path.class);
        when(root.get("priority")).thenReturn(priorityPath);
        Predicate p = mock(Predicate.class);
        when(cb.equal(priorityPath, TestCasePriority.HIGH)).thenReturn(p);

        var result = TestCaseSpecifications.hasPriority(TestCasePriority.HIGH).toPredicate(root, query, cb);

        assertThat(result).isSameAs(p);
    }

    @Test
    void hasFormatEqualsFormat() {
        Path<Object> formatPath = mock(Path.class);
        when(root.get("format")).thenReturn(formatPath);
        Predicate p = mock(Predicate.class);
        when(cb.equal(formatPath, TestCaseFormat.GHERKIN)).thenReturn(p);

        var result = TestCaseSpecifications.hasFormat(TestCaseFormat.GHERKIN).toPredicate(root, query, cb);

        assertThat(result).isSameAs(p);
    }

    @Test
    void inFolderEqualsParentFolderId() {
        var folderId = UUID.randomUUID();
        Path<Object> idPath = stubNestedPath("parentFolder", "id");
        Predicate p = mock(Predicate.class);
        when(cb.equal(idPath, folderId)).thenReturn(p);

        var result = TestCaseSpecifications.inFolder(folderId).toPredicate(root, query, cb);

        assertThat(result).isSameAs(p);
    }

    @Test
    void inFolderTreeBuildsInPredicate() {
        var folderIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        Path<Object> idPath = stubNestedPath("parentFolder", "id");
        Predicate inPredicate = mock(Predicate.class);
        when(idPath.in(folderIds)).thenReturn(inPredicate);

        var result = TestCaseSpecifications.inFolderTree(folderIds).toPredicate(root, query, cb);

        assertThat(result).isSameAs(inPredicate);
    }

    @Test
    void searchTitleOrDescriptionBuildsOrOfLikePredicates() {
        Path<String> titlePath = mock(Path.class);
        Path<String> descPath = mock(Path.class);
        when(root.get("title")).thenReturn((Path) titlePath);
        when(root.get("description")).thenReturn((Path) descPath);

        var titleLower = mock(jakarta.persistence.criteria.Expression.class);
        var descLower = mock(jakarta.persistence.criteria.Expression.class);
        when(cb.lower(titlePath)).thenReturn(titleLower);
        when(cb.lower(descPath)).thenReturn(descLower);

        Predicate titleLike = mock(Predicate.class);
        Predicate descLike = mock(Predicate.class);
        when(cb.like(titleLower, "%payment%")).thenReturn(titleLike);
        when(cb.like(descLower, "%payment%")).thenReturn(descLike);

        Predicate orPredicate = mock(Predicate.class);
        when(cb.or(titleLike, descLike)).thenReturn(orPredicate);

        var result = TestCaseSpecifications.searchTitleOrDescription("Payment").toPredicate(root, query, cb);

        assertThat(result).isSameAs(orPredicate);
        // Pattern is lowercased input wrapped in % at both ends.
    }
}
