package com.keplerops.groundcontrol.domain.testcases.repository;

import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * TC-007 / ADR-047 — Typed {@link Specification} predicates for the
 * QUERY_BASED test-suite resolve path.
 *
 * <p>Each method binds exactly one enum / UUID / bounded-text column into
 * a parameterized predicate. There is no raw SQL/JPQL/Cypher/JSON
 * predicate entry point and no caller-supplied field name; the query
 * vocabulary is whatever this class exposes. Adding a future criterion
 * is a new method here plus a column on the suite, not a config knob.
 */
public final class TestCaseSpecifications {

    private TestCaseSpecifications() {
        // utility
    }

    public static Specification<TestCase> hasProject(UUID projectId) {
        return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<TestCase> hasStatus(TestCaseStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<TestCase> hasType(TestCaseType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<TestCase> hasPriority(TestCasePriority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<TestCase> hasFormat(TestCaseFormat format) {
        return (root, query, cb) -> cb.equal(root.get("format"), format);
    }

    public static Specification<TestCase> inFolder(UUID folderId) {
        return (root, query, cb) -> cb.equal(root.get("parentFolder").get("id"), folderId);
    }

    /**
     * TC-007 / ADR-047 — Folder criteria resolve to the named folder AND
     * all descendants. The caller passes the expanded id set computed from
     * {@link TestCaseFolderRepository#findByProjectIdOrderBySortOrder}; this
     * predicate just turns it into an IN clause. An empty collection
     * predicate is impossible (the suite has a folder id set), so the
     * caller is responsible for guarding the call.
     */
    public static Specification<TestCase> inFolderTree(Collection<UUID> folderIds) {
        return (root, query, cb) -> root.get("parentFolder").get("id").in(folderIds);
    }

    public static Specification<TestCase> searchTitleOrDescription(String search) {
        return (root, query, cb) -> {
            var pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern), cb.like(cb.lower(root.get("description")), pattern));
        };
    }
}
