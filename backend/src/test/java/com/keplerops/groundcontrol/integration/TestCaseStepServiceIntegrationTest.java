package com.keplerops.groundcontrol.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.model.TestCaseStep;
import com.keplerops.groundcontrol.domain.testcases.service.CreateTestCaseCommand;
import com.keplerops.groundcontrol.domain.testcases.service.CreateTestCaseStepCommand;
import com.keplerops.groundcontrol.domain.testcases.service.TestCaseService;
import com.keplerops.groundcontrol.domain.testcases.service.TestCaseStepService;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exercises the Envers commit/flush path for TestCaseStep. The controller
 * integration test is @Transactional with implicit rollback, which means the
 * MockMvc work never flushes through Envers — a missing audit column or a
 * misordered cascade would not surface there. This test uses the established
 * `TestTransaction.flagForCommit()` pattern (see RequirementServiceIntegrationTest)
 * so every step boundary commits real rows, including audit rows. ADR-041 makes
 * the service-level cascade with preserved per-step audit revisions
 * load-bearing; this test pins it.
 */
@Transactional
class TestCaseStepServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private TestCaseStepService stepService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EntityManager entityManager;

    private Project project;

    @BeforeEach
    void setUp() {
        project = projectRepository.findByIdentifier("ground-control").orElseThrow();
    }

    @Test
    void enversCapturesStepCreateAndCascadeDeleteRevisions() {
        // 1. Create the parent test case and one step in committed transactions
        //    so Envers flushes audit revisions to test_case_step_audit.
        var testCase = testCaseService.create(new CreateTestCaseCommand(
                project.getId(),
                "TC-ENVERS-001",
                "Envers cascade roundtrip",
                TestCaseType.MANUAL,
                TestCasePriority.MEDIUM,
                null,
                null,
                null,
                null));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        var step = stepService.create(new CreateTestCaseStepCommand(
                project.getId(), testCase.getId(), 1, "Open the login page", "Page renders the email field", null));
        UUID stepId = step.getId();
        UUID testCaseId = testCase.getId();
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // 2. Verify the audit table received the step's create revision. This
        //    is the load-bearing assertion that would have failed if V074 was
        //    missing the BaseEntity timestamp columns — Envers would have
        //    refused the audit insert at flush time. Use the same
        //    forRevisionsOfEntity(includeRevType=true) shape as the DEL check
        //    below so both ends are explicit RevisionType assertions; a
        //    weaker isNotEmpty() check could pass on a stale phantom row.
        TestTransaction.start();
        var afterCreateReader = AuditReaderFactory.get(entityManager);
        @SuppressWarnings("unchecked")
        List<Object[]> afterCreateRevisions = (List<Object[]>) afterCreateReader
                .createQuery()
                .forRevisionsOfEntity(TestCaseStep.class, false, true)
                .add(AuditEntity.id().eq(stepId))
                .getResultList();
        var afterCreateRevTypes =
                afterCreateRevisions.stream().map(row -> (RevisionType) row[2]).toList();
        assertThat(afterCreateRevTypes)
                .as("expected an ADD audit row immediately after step creation")
                .contains(RevisionType.ADD);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // 3. Delete the parent test case. The service-level cascade in
        //    TestCaseService.delete() routes through Hibernate (NOT a DB
        //    ON DELETE CASCADE), so Envers writes a DELETE revision for each
        //    step before the parent row goes away.
        TestTransaction.start();
        testCaseService.delete(project.getId(), testCaseId);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // 4. Verify the audit DELETE revision exists for the step. This is the
        //    audit-preservation guarantee ADR-041 §Cascade on parent deletion
        //    pins. Use forRevisionsOfEntity with `includeRevType=true` so each
        //    row carries the explicit RevisionType — a stronger assertion than
        //    "find returns null at the latest revision", which could pass for a
        //    soft-delete snapshot too.
        TestTransaction.start();
        var reader = AuditReaderFactory.get(entityManager);
        @SuppressWarnings("unchecked")
        List<Object[]> stepRevisions = (List<Object[]>) reader.createQuery()
                .forRevisionsOfEntity(TestCaseStep.class, false, true)
                .add(AuditEntity.id().eq(stepId))
                .getResultList();
        assertThat(stepRevisions)
                .as("expected ADD plus DEL audit rows for the cascaded step delete")
                .hasSizeGreaterThanOrEqualTo(2);
        var stepRevTypes =
                stepRevisions.stream().map(row -> (RevisionType) row[2]).toList();
        assertThat(stepRevTypes)
                .as("step audit history must contain a DEL revtype after cascade delete")
                .contains(RevisionType.DEL);

        // 5. Same guarantee for the parent test case — the audit chain on the
        //    parent must include the DELETE revision so retention sweeps can
        //    find the parent and child revisions together.
        @SuppressWarnings("unchecked")
        List<Object[]> parentRevisions = (List<Object[]>) reader.createQuery()
                .forRevisionsOfEntity(TestCase.class, false, true)
                .add(AuditEntity.id().eq(testCaseId))
                .getResultList();
        assertThat(parentRevisions).hasSizeGreaterThanOrEqualTo(2);
        var parentRevTypes =
                parentRevisions.stream().map(row -> (RevisionType) row[2]).toList();
        assertThat(parentRevTypes)
                .as("test case audit history must contain a DEL revtype after delete")
                .contains(RevisionType.DEL);
    }
}
