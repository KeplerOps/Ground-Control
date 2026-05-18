package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.model.TestCaseFolder;
import com.keplerops.groundcontrol.domain.testcases.model.TestSuite;
import com.keplerops.groundcontrol.domain.testcases.model.TestSuiteMember;
import com.keplerops.groundcontrol.domain.testcases.model.TestSuiteSourceRequirement;
import com.keplerops.groundcontrol.domain.testcases.repository.TestCaseFolderRepository;
import com.keplerops.groundcontrol.domain.testcases.repository.TestCaseRepository;
import com.keplerops.groundcontrol.domain.testcases.repository.TestSuiteMemberRepository;
import com.keplerops.groundcontrol.domain.testcases.repository.TestSuiteRepository;
import com.keplerops.groundcontrol.domain.testcases.repository.TestSuiteSourceRequirementRepository;
import com.keplerops.groundcontrol.domain.testcases.service.AddTestSuiteMemberCommand;
import com.keplerops.groundcontrol.domain.testcases.service.CreateTestSuiteCommand;
import com.keplerops.groundcontrol.domain.testcases.service.TestSuiteCriteriaCommand;
import com.keplerops.groundcontrol.domain.testcases.service.TestSuiteService;
import com.keplerops.groundcontrol.domain.testcases.service.UpdateTestSuiteCommand;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import com.keplerops.groundcontrol.domain.testcases.state.TestSuitePopulationMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class TestSuiteServiceTest {

    @Mock
    private TestSuiteRepository testSuiteRepository;

    @Mock
    private TestSuiteMemberRepository memberRepository;

    @Mock
    private TestSuiteSourceRequirementRepository sourceRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    @Mock
    private TestCaseFolderRepository folderRepository;

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private TestSuiteService testSuiteService;

    private Project project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
    }

    private TestSuite suite(String uid, TestSuitePopulationMode mode) {
        var s = new TestSuite(project, uid, "name " + uid, mode);
        setField(s, "id", UUID.randomUUID());
        return s;
    }

    private TestCase testCase(String uid) {
        var tc = new TestCase(project, uid, "title " + uid, TestCaseType.MANUAL, TestCasePriority.MEDIUM);
        setField(tc, "id", UUID.randomUUID());
        return tc;
    }

    private Requirement requirement(String uid) {
        var req = new Requirement(project, uid, "title " + uid, "statement " + uid);
        setField(req, "id", UUID.randomUUID());
        return req;
    }

    @Nested
    class CreateStatic {

        @Test
        void createsStaticSuiteWithoutCriteria() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(testSuiteRepository.existsByProjectIdAndUid(projectId, "TS-S-001"))
                    .thenReturn(false);
            when(testSuiteRepository.save(any(TestSuite.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = testSuiteService.create(new CreateTestSuiteCommand(
                    projectId,
                    "TS-S-001",
                    "Static suite",
                    "scope",
                    TestSuitePopulationMode.STATIC,
                    TestSuiteCriteriaCommand.empty()));

            assertThat(result.getUid()).isEqualTo("TS-S-001");
            assertThat(result.getPopulationMode()).isEqualTo(TestSuitePopulationMode.STATIC);
            assertThat(result.hasAnyCriteria()).isFalse();
        }

        @Test
        void rejectsCriteriaOnStaticCreate() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(testSuiteRepository.existsByProjectIdAndUid(projectId, "TS-S-002"))
                    .thenReturn(false);
            var cmd = new CreateTestSuiteCommand(
                    projectId,
                    "TS-S-002",
                    "n",
                    null,
                    TestSuitePopulationMode.STATIC,
                    new TestSuiteCriteriaCommand(TestCaseStatus.APPROVED, null, null, null, null, null));

            assertThatThrownBy(() -> testSuiteService.create(cmd))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("QUERY_BASED");
            verify(testSuiteRepository, never()).save(any());
        }

        @Test
        void rejectsDuplicateUid() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(testSuiteRepository.existsByProjectIdAndUid(projectId, "TS-S-003"))
                    .thenReturn(true);
            var cmd = new CreateTestSuiteCommand(
                    projectId, "TS-S-003", "n", null, TestSuitePopulationMode.STATIC, TestSuiteCriteriaCommand.empty());

            assertThatThrownBy(() -> testSuiteService.create(cmd))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("TS-S-003");
            verify(testSuiteRepository, never()).save(any());
        }
    }

    @Nested
    class CreateQueryBased {

        @Test
        void createsQueryBasedSuiteWithCriteria() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(testSuiteRepository.existsByProjectIdAndUid(projectId, "TS-Q-001"))
                    .thenReturn(false);
            when(testSuiteRepository.save(any(TestSuite.class))).thenAnswer(inv -> inv.getArgument(0));

            var criteria = new TestSuiteCriteriaCommand(
                    TestCaseStatus.APPROVED, TestCaseType.AUTOMATED, null, null, null, null);
            var result = testSuiteService.create(new CreateTestSuiteCommand(
                    projectId, "TS-Q-001", "Query suite", null, TestSuitePopulationMode.QUERY_BASED, criteria));

            assertThat(result.getCriteriaStatus()).isEqualTo(TestCaseStatus.APPROVED);
            assertThat(result.getCriteriaType()).isEqualTo(TestCaseType.AUTOMATED);
        }

        @Test
        void rejectsQueryBasedCreateWithoutAnyCriterion() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(testSuiteRepository.existsByProjectIdAndUid(projectId, "TS-Q-002"))
                    .thenReturn(false);
            var cmd = new CreateTestSuiteCommand(
                    projectId,
                    "TS-Q-002",
                    "n",
                    null,
                    TestSuitePopulationMode.QUERY_BASED,
                    TestSuiteCriteriaCommand.empty());

            assertThatThrownBy(() -> testSuiteService.create(cmd))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("QUERY_BASED");
            verify(testSuiteRepository, never()).save(any());
        }

        @Test
        void rejectsCriteriaFolderFromAnotherProject() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(testSuiteRepository.existsByProjectIdAndUid(projectId, "TS-Q-003"))
                    .thenReturn(false);

            var foreignProject = new Project("other", "Other");
            setField(foreignProject, "id", UUID.randomUUID());
            var folder = new TestCaseFolder(foreignProject, null, "Folder", null, 0);
            UUID folderId = UUID.randomUUID();
            setField(folder, "id", folderId);
            when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));

            var criteria = new TestSuiteCriteriaCommand(null, null, null, null, folderId, null);
            var cmd = new CreateTestSuiteCommand(
                    projectId, "TS-Q-003", "n", null, TestSuitePopulationMode.QUERY_BASED, criteria);
            assertThatThrownBy(() -> testSuiteService.create(cmd))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(folderId.toString());
        }
    }

    @Nested
    class CreateRequirementsBased {

        @Test
        void createsRequirementsBasedSuite() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(testSuiteRepository.existsByProjectIdAndUid(projectId, "TS-R-001"))
                    .thenReturn(false);
            when(testSuiteRepository.save(any(TestSuite.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = testSuiteService.create(new CreateTestSuiteCommand(
                    projectId,
                    "TS-R-001",
                    "Req suite",
                    null,
                    TestSuitePopulationMode.REQUIREMENTS_BASED,
                    TestSuiteCriteriaCommand.empty()));

            assertThat(result.getPopulationMode()).isEqualTo(TestSuitePopulationMode.REQUIREMENTS_BASED);
            // Mirror the depth of the STATIC and QUERY_BASED create tests
            // (test-quality cycle 1 F5): pin uid, name, that no criteria
            // leaked into a non-QUERY_BASED suite, and that save() ran.
            assertThat(result.getUid()).isEqualTo("TS-R-001");
            assertThat(result.getName()).isEqualTo("Req suite");
            assertThat(result.hasAnyCriteria()).isFalse();
            verify(testSuiteRepository).save(any(TestSuite.class));
        }
    }

    @Nested
    class Updates {

        @Test
        void partialUpdatePreservesUntouchedFields() {
            var existing = suite("TS-S-001", TestSuitePopulationMode.STATIC);
            existing.setDescription("original");
            when(testSuiteRepository.findByIdAndProjectId(existing.getId(), projectId))
                    .thenReturn(Optional.of(existing));
            when(testSuiteRepository.save(any(TestSuite.class))).thenAnswer(inv -> inv.getArgument(0));

            var updated = testSuiteService.update(
                    projectId,
                    existing.getId(),
                    new UpdateTestSuiteCommand(
                            "Renamed", null, null, null, null, null, null, null, false, false, false, false, false,
                            false, false));

            assertThat(updated.getName()).isEqualTo("Renamed");
            assertThat(updated.getDescription()).isEqualTo("original");
        }

        @Test
        void clearFlagWipesField() {
            var existing = suite("TS-S-001", TestSuitePopulationMode.STATIC);
            existing.setDescription("original");
            when(testSuiteRepository.findByIdAndProjectId(existing.getId(), projectId))
                    .thenReturn(Optional.of(existing));
            when(testSuiteRepository.save(any(TestSuite.class))).thenAnswer(inv -> inv.getArgument(0));

            var updated = testSuiteService.update(
                    projectId,
                    existing.getId(),
                    new UpdateTestSuiteCommand(
                            null, null, null, null, null, null, null, null, true, false, false, false, false, false,
                            false));

            assertThat(updated.getDescription()).isNull();
        }

        @Test
        void rejectsCriteriaPatchOnStaticSuite() {
            var existing = suite("TS-S-001", TestSuitePopulationMode.STATIC);
            when(testSuiteRepository.findByIdAndProjectId(existing.getId(), projectId))
                    .thenReturn(Optional.of(existing));
            UUID existingId = existing.getId();
            var cmd = new UpdateTestSuiteCommand(
                    null,
                    null,
                    TestCaseStatus.APPROVED,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false);

            assertThatThrownBy(() -> testSuiteService.update(projectId, existingId, cmd))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("QUERY_BASED");
        }

        @Test
        void rejectsClearAllCriteriaOnQuerySuite() {
            var existing = suite("TS-Q-001", TestSuitePopulationMode.QUERY_BASED);
            existing.setCriteriaStatus(TestCaseStatus.APPROVED);
            when(testSuiteRepository.findByIdAndProjectId(existing.getId(), projectId))
                    .thenReturn(Optional.of(existing));
            UUID existingId = existing.getId();
            // Clearing the only criterion leaves the suite with no rule — rejected.
            var cmd = new UpdateTestSuiteCommand(
                    null, null, null, null, null, null, null, null, false, true, false, false, false, false, false);

            assertThatThrownBy(() -> testSuiteService.update(projectId, existingId, cmd))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("QUERY_BASED");
        }
    }

    @Nested
    class StaticMembers {

        @Test
        void rejectsAddMemberOnNonStaticSuite() {
            var s = suite("TS-Q-001", TestSuitePopulationMode.QUERY_BASED);
            when(testSuiteRepository.findByIdAndProjectIdForUpdate(s.getId(), projectId))
                    .thenReturn(Optional.of(s));
            UUID suiteId = s.getId();
            var cmd = new AddTestSuiteMemberCommand(UUID.randomUUID(), null);

            assertThatThrownBy(() -> testSuiteService.addMember(projectId, suiteId, cmd))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("STATIC");
        }

        @Test
        void appendsMemberWhenPositionOmitted() {
            var s = suite("TS-S-001", TestSuitePopulationMode.STATIC);
            var tc = testCase("TC-001");
            // Three existing members at positions 0..2; the new member must
            // land at position 3 (append-on-end) per the F4 fix.
            var existingA = new TestSuiteMember(s, testCase("TC-EXISTING-A"), 0);
            var existingB = new TestSuiteMember(s, testCase("TC-EXISTING-B"), 1);
            var existingC = new TestSuiteMember(s, testCase("TC-EXISTING-C"), 2);
            when(testSuiteRepository.findByIdAndProjectIdForUpdate(s.getId(), projectId))
                    .thenReturn(Optional.of(s));
            when(testCaseRepository.findByIdAndProjectId(tc.getId(), projectId)).thenReturn(Optional.of(tc));
            when(memberRepository.existsByTestSuiteIdAndTestCaseId(s.getId(), tc.getId()))
                    .thenReturn(false);
            when(memberRepository.findByTestSuiteIdOrderByPosition(s.getId()))
                    .thenReturn(List.of(existingA, existingB, existingC));
            when(memberRepository.save(any(TestSuiteMember.class))).thenAnswer(inv -> inv.getArgument(0));

            var member =
                    testSuiteService.addMember(projectId, s.getId(), new AddTestSuiteMemberCommand(tc.getId(), null));

            assertThat(member.getPosition()).isEqualTo(3);
            assertThat(member.getTestCase()).isSameAs(tc);
        }

        @Test
        void shiftsExistingMembersWhenInsertingAtOccupiedPosition() {
            var s = suite("TS-S-001", TestSuitePopulationMode.STATIC);
            var tc = testCase("TC-NEW");
            var existingA = new TestSuiteMember(s, testCase("TC-A"), 0);
            var existingB = new TestSuiteMember(s, testCase("TC-B"), 1);
            var existingC = new TestSuiteMember(s, testCase("TC-C"), 2);
            when(testSuiteRepository.findByIdAndProjectIdForUpdate(s.getId(), projectId))
                    .thenReturn(Optional.of(s));
            when(testCaseRepository.findByIdAndProjectId(tc.getId(), projectId)).thenReturn(Optional.of(tc));
            when(memberRepository.existsByTestSuiteIdAndTestCaseId(s.getId(), tc.getId()))
                    .thenReturn(false);
            when(memberRepository.findByTestSuiteIdOrderByPosition(s.getId()))
                    .thenReturn(List.of(existingA, existingB, existingC));
            when(memberRepository.save(any(TestSuiteMember.class))).thenAnswer(inv -> inv.getArgument(0));

            var member = testSuiteService.addMember(projectId, s.getId(), new AddTestSuiteMemberCommand(tc.getId(), 1));

            // New member lands at requested slot; existing B / C shift up by 1.
            assertThat(member.getPosition()).isEqualTo(1);
            assertThat(existingA.getPosition()).isZero();
            assertThat(existingB.getPosition()).isEqualTo(2);
            assertThat(existingC.getPosition()).isEqualTo(3);
        }

        @Test
        void compactsPositionsAfterMidListRemove() {
            var s = suite("TS-S-001", TestSuitePopulationMode.STATIC);
            var tcA = testCase("TC-A");
            var tcB = testCase("TC-B");
            var tcC = testCase("TC-C");
            var mA = new TestSuiteMember(s, tcA, 0);
            setField(mA, "id", UUID.randomUUID());
            var mB = new TestSuiteMember(s, tcB, 1);
            setField(mB, "id", UUID.randomUUID());
            var mC = new TestSuiteMember(s, tcC, 2);
            setField(mC, "id", UUID.randomUUID());
            when(testSuiteRepository.findByIdAndProjectIdForUpdate(s.getId(), projectId))
                    .thenReturn(Optional.of(s));
            when(memberRepository.findByTestSuiteIdAndTestCaseId(s.getId(), tcB.getId()))
                    .thenReturn(Optional.of(mB));
            when(memberRepository.findByTestSuiteIdOrderByPosition(s.getId())).thenReturn(List.of(mA, mB, mC));

            testSuiteService.removeMember(projectId, s.getId(), tcB.getId());

            // mC compacts from position 2 → 1; mA stays at 0.
            assertThat(mA.getPosition()).isZero();
            assertThat(mC.getPosition()).isEqualTo(1);
        }

        @Test
        void rejectsDuplicateMember() {
            var s = suite("TS-S-001", TestSuitePopulationMode.STATIC);
            var tc = testCase("TC-001");
            when(testSuiteRepository.findByIdAndProjectIdForUpdate(s.getId(), projectId))
                    .thenReturn(Optional.of(s));
            when(testCaseRepository.findByIdAndProjectId(tc.getId(), projectId)).thenReturn(Optional.of(tc));
            when(memberRepository.existsByTestSuiteIdAndTestCaseId(s.getId(), tc.getId()))
                    .thenReturn(true);
            UUID suiteId = s.getId();
            var cmd = new AddTestSuiteMemberCommand(tc.getId(), null);

            assertThatThrownBy(() -> testSuiteService.addMember(projectId, suiteId, cmd))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("TC-001");
        }

        @Test
        void rejectsTestCaseFromAnotherProject() {
            var s = suite("TS-S-001", TestSuitePopulationMode.STATIC);
            var foreignTcId = UUID.randomUUID();
            when(testSuiteRepository.findByIdAndProjectIdForUpdate(s.getId(), projectId))
                    .thenReturn(Optional.of(s));
            when(testCaseRepository.findByIdAndProjectId(foreignTcId, projectId))
                    .thenReturn(Optional.empty());
            UUID suiteId = s.getId();
            var cmd = new AddTestSuiteMemberCommand(foreignTcId, null);

            assertThatThrownBy(() -> testSuiteService.addMember(projectId, suiteId, cmd))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(foreignTcId.toString());
        }

        @Test
        void removeMemberRejectsMissing() {
            var s = suite("TS-S-001", TestSuitePopulationMode.STATIC);
            var tcId = UUID.randomUUID();
            when(testSuiteRepository.findByIdAndProjectIdForUpdate(s.getId(), projectId))
                    .thenReturn(Optional.of(s));
            when(memberRepository.findByTestSuiteIdAndTestCaseId(s.getId(), tcId))
                    .thenReturn(Optional.empty());
            UUID suiteId = s.getId();

            assertThatThrownBy(() -> testSuiteService.removeMember(projectId, suiteId, tcId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void reorderRejectsMismatchedIdSet() {
            var s = suite("TS-S-001", TestSuitePopulationMode.STATIC);
            var tcA = testCase("TC-A");
            var memberA = new TestSuiteMember(s, tcA, 0);
            when(testSuiteRepository.findByIdAndProjectIdForUpdate(s.getId(), projectId))
                    .thenReturn(Optional.of(s));
            when(memberRepository.findByTestSuiteIdOrderByPosition(s.getId())).thenReturn(List.of(memberA));
            UUID suiteId = s.getId();
            // Reorder delegates to SiblingOrderingHelper, which surfaces a
            // set-mismatch as ConflictException (same contract as
            // test-case / folder reorder).
            List<UUID> mismatch = List.of(UUID.randomUUID(), UUID.randomUUID());

            assertThatThrownBy(() -> testSuiteService.reorderMembers(projectId, suiteId, mismatch))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("siblings");
        }
    }

    @Nested
    class RequirementsBasedSources {

        @Test
        void rejectsAddSourceOnNonRequirementsBasedSuite() {
            var s = suite("TS-S-001", TestSuitePopulationMode.STATIC);
            when(testSuiteRepository.findByIdAndProjectId(s.getId(), projectId)).thenReturn(Optional.of(s));
            UUID suiteId = s.getId();
            UUID reqId = UUID.randomUUID();

            assertThatThrownBy(() -> testSuiteService.addSourceRequirement(projectId, suiteId, reqId))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("REQUIREMENTS_BASED");
        }

        @Test
        void addSourceCreatesSourceRow() {
            var s = suite("TS-R-001", TestSuitePopulationMode.REQUIREMENTS_BASED);
            var req = requirement("REQ-001");
            when(testSuiteRepository.findByIdAndProjectId(s.getId(), projectId)).thenReturn(Optional.of(s));
            when(requirementRepository.findByIdAndProjectId(req.getId(), projectId))
                    .thenReturn(Optional.of(req));
            when(sourceRepository.existsByTestSuiteIdAndRequirementId(s.getId(), req.getId()))
                    .thenReturn(false);
            when(sourceRepository.save(any(TestSuiteSourceRequirement.class))).thenAnswer(inv -> inv.getArgument(0));

            var source = testSuiteService.addSourceRequirement(projectId, s.getId(), req.getId());

            assertThat(source.getRequirement()).isSameAs(req);
        }

        @Test
        void addSourceRejectsRequirementFromAnotherProject() {
            var s = suite("TS-R-001", TestSuitePopulationMode.REQUIREMENTS_BASED);
            var foreignReqId = UUID.randomUUID();
            when(testSuiteRepository.findByIdAndProjectId(s.getId(), projectId)).thenReturn(Optional.of(s));
            when(requirementRepository.findByIdAndProjectId(foreignReqId, projectId))
                    .thenReturn(Optional.empty());
            UUID suiteId = s.getId();

            assertThatThrownBy(() -> testSuiteService.addSourceRequirement(projectId, suiteId, foreignReqId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class StaticResolve {

        @Test
        void returnsMembersInPositionOrder() {
            var s = suite("TS-S-001", TestSuitePopulationMode.STATIC);
            var tcA = testCase("TC-A");
            var tcB = testCase("TC-B");
            var mA = new TestSuiteMember(s, tcA, 0);
            var mB = new TestSuiteMember(s, tcB, 1);
            when(testSuiteRepository.findByIdAndProjectId(s.getId(), projectId)).thenReturn(Optional.of(s));
            // F3 fix: resolve uses the pageable variant so the cap reaches the DB.
            when(memberRepository.findByTestSuiteIdOrderByPosition(eq(s.getId()), any(Pageable.class)))
                    .thenReturn(List.of(mA, mB));

            var resolved = testSuiteService.resolveTestCases(projectId, s.getId());

            assertThat(resolved).containsExactly(tcA, tcB);
        }
    }

    @Nested
    class RequirementsBasedResolve {

        @Test
        void resolvesAcrossSourceRequirementsViaTraceability() {
            var s = suite("TS-R-001", TestSuitePopulationMode.REQUIREMENTS_BASED);
            var req1 = requirement("REQ-001");
            var req2 = requirement("REQ-002");
            var src1 = new TestSuiteSourceRequirement(s, req1);
            var src2 = new TestSuiteSourceRequirement(s, req2);
            var tcA = testCase("TC-A");
            var tcB = testCase("TC-B");

            when(testSuiteRepository.findByIdAndProjectId(s.getId(), projectId)).thenReturn(Optional.of(s));
            when(sourceRepository.findByTestSuiteIdOrderByRequirementUid(s.getId()))
                    .thenReturn(List.of(src1, src2));
            // Codex pre-push cycle 3: REQUIREMENTS_BASED resolve dispatches
            // through a single filter+join+sort+cap repo query whose result
            // is already the live, project-scoped, UID-sorted set.
            when(testCaseRepository.findLinkedTestCasesForRequirements(
                            eq(projectId),
                            eq(List.of(req1.getId(), req2.getId())),
                            eq(LinkType.TESTS),
                            eq(ArtifactType.TEST),
                            any(Pageable.class)))
                    .thenReturn(List.of(tcA, tcB));

            var resolved = testSuiteService.resolveTestCases(projectId, s.getId());

            assertThat(resolved).containsExactlyInAnyOrder(tcA, tcB);
        }

        @Test
        void returnsEmptyWhenNoSources() {
            var s = suite("TS-R-002", TestSuitePopulationMode.REQUIREMENTS_BASED);
            when(testSuiteRepository.findByIdAndProjectId(s.getId(), projectId)).thenReturn(Optional.of(s));
            when(sourceRepository.findByTestSuiteIdOrderByRequirementUid(s.getId()))
                    .thenReturn(List.of());

            assertThat(testSuiteService.resolveTestCases(projectId, s.getId())).isEmpty();
        }
    }

    @Nested
    class QueryBasedResolve {

        @Test
        void composesSpecificationFromCriteriaAndDelegatesToRepository() {
            var s = suite("TS-Q-001", TestSuitePopulationMode.QUERY_BASED);
            s.setCriteriaStatus(TestCaseStatus.APPROVED);
            var tcA = testCase("TC-A");
            when(testSuiteRepository.findByIdAndProjectId(s.getId(), projectId)).thenReturn(Optional.of(s));
            // F3 fix: resolve uses Pageable so the cap reaches the DB. The
            // mock returns a Page so the .getContent() call on the service
            // side reaches our fixture.
            when(testCaseRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(tcA)));

            assertThat(testSuiteService.resolveTestCases(projectId, s.getId())).containsExactly(tcA);
        }

        @Test
        void composesEveryCriterionWhenAllSet() {
            var s = suite("TS-Q-FULL", TestSuitePopulationMode.QUERY_BASED);
            s.setCriteriaStatus(TestCaseStatus.APPROVED);
            s.setCriteriaType(TestCaseType.AUTOMATED);
            s.setCriteriaPriority(TestCasePriority.HIGH);
            s.setCriteriaFormat(com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat.STEP_BASED);
            UUID folderId = UUID.randomUUID();
            s.setCriteriaFolderId(folderId);
            s.setCriteriaTextSearch("payment");

            var folder = new TestCaseFolder(project, null, "Root", null, 0);
            setField(folder, "id", folderId);
            when(testSuiteRepository.findByIdAndProjectId(s.getId(), projectId)).thenReturn(Optional.of(s));
            when(folderRepository.findByProjectIdOrderBySortOrder(projectId)).thenReturn(List.of(folder));
            when(testCaseRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

            // Successfully composes every criterion branch (type / priority /
            // format / folder-tree / text search) without throwing.
            assertThat(testSuiteService.resolveTestCases(projectId, s.getId())).isEmpty();
        }
    }

    @Nested
    class CrudReads {

        @Test
        void getByIdDelegatesToRequireSuiteInProject() {
            var s = suite("TS-G-001", TestSuitePopulationMode.STATIC);
            when(testSuiteRepository.findByIdAndProjectId(s.getId(), projectId)).thenReturn(Optional.of(s));

            assertThat(testSuiteService.getById(projectId, s.getId())).isSameAs(s);
        }

        @Test
        void getByIdThrowsNotFoundWhenMissing() {
            var id = UUID.randomUUID();
            when(testSuiteRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testSuiteService.getById(projectId, id))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(id.toString());
        }

        @Test
        void getByUidReturnsExistingSuite() {
            var s = suite("TS-U-001", TestSuitePopulationMode.STATIC);
            when(testSuiteRepository.findByProjectIdAndUid(projectId, "TS-U-001"))
                    .thenReturn(Optional.of(s));

            assertThat(testSuiteService.getByUid(projectId, "TS-U-001")).isSameAs(s);
        }

        @Test
        void getByUidThrowsNotFoundWhenMissing() {
            when(testSuiteRepository.findByProjectIdAndUid(projectId, "TS-MISSING"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> testSuiteService.getByUid(projectId, "TS-MISSING"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("TS-MISSING");
        }

        @Test
        void listByProjectDelegatesToRepository() {
            var a = suite("TS-L-001", TestSuitePopulationMode.STATIC);
            var b = suite("TS-L-002", TestSuitePopulationMode.QUERY_BASED);
            when(testSuiteRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                    .thenReturn(List.of(a, b));

            assertThat(testSuiteService.listByProject(projectId)).containsExactly(a, b);
        }

        @Test
        void deleteRemovesMembersSourcesAndSuite() {
            var s = suite("TS-D-001", TestSuitePopulationMode.STATIC);
            var member = new TestSuiteMember(s, testCase("TC-D-1"), 0);
            when(testSuiteRepository.findByIdAndProjectId(s.getId(), projectId)).thenReturn(Optional.of(s));
            when(memberRepository.findByTestSuiteId(s.getId())).thenReturn(List.of(member));
            when(sourceRepository.findByTestSuiteId(s.getId())).thenReturn(List.of());

            testSuiteService.delete(projectId, s.getId());

            verify(memberRepository).deleteAll(List.of(member));
            verify(sourceRepository).deleteAll(List.of());
            verify(testSuiteRepository).delete(s);
        }

        @Test
        void listMembersDelegatesToRepository() {
            var s = suite("TS-LM-001", TestSuitePopulationMode.STATIC);
            var member = new TestSuiteMember(s, testCase("TC-X"), 0);
            when(testSuiteRepository.findByIdAndProjectId(s.getId(), projectId)).thenReturn(Optional.of(s));
            when(memberRepository.findByTestSuiteIdOrderByPosition(s.getId())).thenReturn(List.of(member));

            assertThat(testSuiteService.listMembers(projectId, s.getId())).containsExactly(member);
        }

        @Test
        void listSourceRequirementsDelegatesToRepository() {
            var s = suite("TS-LSR-001", TestSuitePopulationMode.REQUIREMENTS_BASED);
            var src = new TestSuiteSourceRequirement(s, requirement("REQ-X"));
            when(testSuiteRepository.findByIdAndProjectId(s.getId(), projectId)).thenReturn(Optional.of(s));
            when(sourceRepository.findByTestSuiteIdOrderByRequirementUid(s.getId()))
                    .thenReturn(List.of(src));

            assertThat(testSuiteService.listSourceRequirements(projectId, s.getId()))
                    .containsExactly(src);
        }
    }

    @Nested
    class MutationGuards {

        @Test
        void addMemberRejectsNullTestCaseId() {
            var s = suite("TS-S-001", TestSuitePopulationMode.STATIC);
            when(testSuiteRepository.findByIdAndProjectIdForUpdate(s.getId(), projectId))
                    .thenReturn(Optional.of(s));
            UUID suiteId = s.getId();
            var cmd = new AddTestSuiteMemberCommand(null, null);

            assertThatThrownBy(() -> testSuiteService.addMember(projectId, suiteId, cmd))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("test_case_id");
        }

        @Test
        void requireSuiteForMutationThrowsNotFoundWhenAbsent() {
            var missingId = UUID.randomUUID();
            when(testSuiteRepository.findByIdAndProjectIdForUpdate(missingId, projectId))
                    .thenReturn(Optional.empty());
            var cmd = new AddTestSuiteMemberCommand(UUID.randomUUID(), null);

            assertThatThrownBy(() -> testSuiteService.addMember(projectId, missingId, cmd))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(missingId.toString());
        }

        @Test
        void addSourceRequirementRejectsNullRequirementId() {
            var s = suite("TS-R-001", TestSuitePopulationMode.REQUIREMENTS_BASED);
            when(testSuiteRepository.findByIdAndProjectId(s.getId(), projectId)).thenReturn(Optional.of(s));
            UUID suiteId = s.getId();

            assertThatThrownBy(() -> testSuiteService.addSourceRequirement(projectId, suiteId, null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("requirement_id");
        }

        @Test
        void addSourceRequirementRejectsDuplicate() {
            var s = suite("TS-R-001", TestSuitePopulationMode.REQUIREMENTS_BASED);
            var req = requirement("REQ-DUP");
            when(testSuiteRepository.findByIdAndProjectId(s.getId(), projectId)).thenReturn(Optional.of(s));
            when(requirementRepository.findByIdAndProjectId(req.getId(), projectId))
                    .thenReturn(Optional.of(req));
            when(sourceRepository.existsByTestSuiteIdAndRequirementId(s.getId(), req.getId()))
                    .thenReturn(true);
            UUID suiteId = s.getId();
            UUID reqId = req.getId();

            assertThatThrownBy(() -> testSuiteService.addSourceRequirement(projectId, suiteId, reqId))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("REQ-DUP");
        }

        @Test
        void removeSourceRequirementSucceedsWhenPresent() {
            var s = suite("TS-R-001", TestSuitePopulationMode.REQUIREMENTS_BASED);
            var req = requirement("REQ-RM");
            var src = new TestSuiteSourceRequirement(s, req);
            when(testSuiteRepository.findByIdAndProjectId(s.getId(), projectId)).thenReturn(Optional.of(s));
            when(sourceRepository.findByTestSuiteIdAndRequirementId(s.getId(), req.getId()))
                    .thenReturn(Optional.of(src));

            testSuiteService.removeSourceRequirement(projectId, s.getId(), req.getId());

            verify(sourceRepository).delete(src);
        }

        @Test
        void removeSourceRequirementRejectsMissing() {
            var s = suite("TS-R-001", TestSuitePopulationMode.REQUIREMENTS_BASED);
            var reqId = UUID.randomUUID();
            when(testSuiteRepository.findByIdAndProjectId(s.getId(), projectId)).thenReturn(Optional.of(s));
            when(sourceRepository.findByTestSuiteIdAndRequirementId(s.getId(), reqId))
                    .thenReturn(Optional.empty());
            UUID suiteId = s.getId();

            assertThatThrownBy(() -> testSuiteService.removeSourceRequirement(projectId, suiteId, reqId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void requireSuiteInModeThrowsWhenModeMismatch() {
            // listMembers requires STATIC; calling it on REQUIREMENTS_BASED
            // exercises the requireSuiteInMode mode-mismatch path.
            var s = suite("TS-R-001", TestSuitePopulationMode.REQUIREMENTS_BASED);
            when(testSuiteRepository.findByIdAndProjectId(s.getId(), projectId)).thenReturn(Optional.of(s));
            UUID suiteId = s.getId();

            assertThatThrownBy(() -> testSuiteService.listMembers(projectId, suiteId))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("STATIC");
        }
    }

    @Nested
    class ReorderResorts {

        @Test
        void reorderReturnsMembersSortedByNewPosition() {
            var s = suite("TS-S-RO", TestSuitePopulationMode.STATIC);
            var tcA = testCase("TC-A");
            var tcB = testCase("TC-B");
            var mA = new TestSuiteMember(s, tcA, 0);
            var mB = new TestSuiteMember(s, tcB, 1);
            when(testSuiteRepository.findByIdAndProjectIdForUpdate(s.getId(), projectId))
                    .thenReturn(Optional.of(s));
            // SiblingOrderingHelper inspects current; resolve / re-sort runs
            // against the in-memory list.
            when(memberRepository.findByTestSuiteIdOrderByPosition(s.getId())).thenReturn(List.of(mA, mB));

            var reordered = testSuiteService.reorderMembers(projectId, s.getId(), List.of(tcB.getId(), tcA.getId()));

            // Returned in NEW position order: mB at 0, mA at 1.
            assertThat(reordered).containsExactly(mB, mA);
            assertThat(mB.getPosition()).isZero();
            assertThat(mA.getPosition()).isEqualTo(1);
        }
    }

    @Nested
    class CriteriaFolderValidation {

        @Test
        void rejectsCriteriaFolderThatDoesNotExist() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(testSuiteRepository.existsByProjectIdAndUid(projectId, "TS-Q-NF"))
                    .thenReturn(false);
            UUID missingFolderId = UUID.randomUUID();
            when(folderRepository.findById(missingFolderId)).thenReturn(Optional.empty());
            var cmd = new CreateTestSuiteCommand(
                    projectId,
                    "TS-Q-NF",
                    "n",
                    null,
                    TestSuitePopulationMode.QUERY_BASED,
                    new TestSuiteCriteriaCommand(null, null, null, null, missingFolderId, null));

            assertThatThrownBy(() -> testSuiteService.create(cmd))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(missingFolderId.toString());
        }
    }
}
