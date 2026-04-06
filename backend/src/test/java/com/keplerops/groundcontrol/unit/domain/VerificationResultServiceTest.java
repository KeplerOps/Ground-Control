package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.verification.model.VerificationResult;
import com.keplerops.groundcontrol.domain.verification.repository.VerificationResultRepository;
import com.keplerops.groundcontrol.domain.verification.service.CreateVerificationResultCommand;
import com.keplerops.groundcontrol.domain.verification.service.UpdateVerificationResultCommand;
import com.keplerops.groundcontrol.domain.verification.service.VerificationResultService;
import com.keplerops.groundcontrol.domain.verification.state.AssuranceLevel;
import com.keplerops.groundcontrol.domain.verification.state.VerificationStatus;
import java.time.Instant;
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

@ExtendWith(MockitoExtension.class)
class VerificationResultServiceTest {

    @Mock
    private VerificationResultRepository verificationResultRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private TraceabilityLinkRepository traceabilityLinkRepository;

    @InjectMocks
    private VerificationResultService verificationResultService;

    private Project project;
    private UUID projectId;

    private static final Instant NOW = Instant.parse("2026-04-05T12:00:00Z");

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
    }

    private VerificationResult makeResult() {
        var vr = new VerificationResult(project, "openjml-esc", VerificationStatus.PROVEN, AssuranceLevel.L1, NOW);
        vr.setProperty("requires x > 0");
        setField(vr, "id", UUID.randomUUID());
        setField(vr, "createdAt", NOW);
        setField(vr, "updatedAt", NOW);
        return vr;
    }

    private Requirement makeRequirement() {
        var req = new Requirement(project, "REQ-001", "Test Req", "statement");
        setField(req, "id", UUID.randomUUID());
        return req;
    }

    private TraceabilityLink makeLink(Requirement requirement) {
        var link = new TraceabilityLink(requirement, ArtifactType.CODE_FILE, "src/Main.java", LinkType.IMPLEMENTS);
        setField(link, "id", UUID.randomUUID());
        return link;
    }

    @Nested
    class Create {

        @Test
        void createsVerificationResult() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(verificationResultRepository.save(any(VerificationResult.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateVerificationResultCommand(
                    projectId,
                    null,
                    null,
                    "openjml-esc",
                    "requires x > 0",
                    VerificationStatus.PROVEN,
                    AssuranceLevel.L1,
                    null,
                    NOW,
                    null);
            var result = verificationResultService.create(command);

            assertThat(result.getProver()).isEqualTo("openjml-esc");
            assertThat(result.getResult()).isEqualTo(VerificationStatus.PROVEN);
            assertThat(result.getAssuranceLevel()).isEqualTo(AssuranceLevel.L1);
            assertThat(result.getProperty()).isEqualTo("requires x > 0");
        }

        @Test
        void createsWithRequirementLink() {
            var requirement = makeRequirement();
            when(projectService.getById(projectId)).thenReturn(project);
            when(requirementRepository.findByIdAndProjectId(requirement.getId(), projectId))
                    .thenReturn(Optional.of(requirement));
            when(verificationResultRepository.save(any(VerificationResult.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateVerificationResultCommand(
                    projectId,
                    null,
                    requirement.getId(),
                    "tlaplus-tlc",
                    null,
                    VerificationStatus.PROVEN,
                    AssuranceLevel.L2,
                    null,
                    NOW,
                    null);
            var result = verificationResultService.create(command);

            assertThat(result.getRequirement()).isEqualTo(requirement);
        }

        @Test
        void createsWithTargetLink() {
            var requirement = makeRequirement();
            var link = makeLink(requirement);
            when(projectService.getById(projectId)).thenReturn(project);
            when(traceabilityLinkRepository.findById(link.getId())).thenReturn(Optional.of(link));
            when(verificationResultRepository.save(any(VerificationResult.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateVerificationResultCommand(
                    projectId,
                    link.getId(),
                    null,
                    "openjml-esc",
                    null,
                    VerificationStatus.PROVEN,
                    AssuranceLevel.L1,
                    null,
                    NOW,
                    null);
            var result = verificationResultService.create(command);

            assertThat(result.getTarget()).isEqualTo(link);
        }

        @Test
        void throwsWhenRequirementNotFound() {
            var reqId = UUID.randomUUID();
            when(projectService.getById(projectId)).thenReturn(project);
            when(requirementRepository.findByIdAndProjectId(reqId, projectId)).thenReturn(Optional.empty());

            var command = new CreateVerificationResultCommand(
                    projectId,
                    null,
                    reqId,
                    "openjml-esc",
                    null,
                    VerificationStatus.PROVEN,
                    AssuranceLevel.L1,
                    null,
                    NOW,
                    null);

            assertThatThrownBy(() -> verificationResultService.create(command)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void throwsWhenTargetNotFound() {
            var targetId = UUID.randomUUID();
            when(projectService.getById(projectId)).thenReturn(project);
            when(traceabilityLinkRepository.findById(targetId)).thenReturn(Optional.empty());

            var command = new CreateVerificationResultCommand(
                    projectId,
                    targetId,
                    null,
                    "openjml-esc",
                    null,
                    VerificationStatus.PROVEN,
                    AssuranceLevel.L1,
                    null,
                    NOW,
                    null);

            assertThatThrownBy(() -> verificationResultService.create(command)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void throwsWhenTargetBelongsToDifferentProject() {
            var otherProject = new Project("other", "Other");
            var otherProjectId = UUID.randomUUID();
            setField(otherProject, "id", otherProjectId);
            var requirement = new Requirement(otherProject, "REQ-001", "Test", "stmt");
            setField(requirement, "id", UUID.randomUUID());
            var link = new TraceabilityLink(requirement, ArtifactType.CODE_FILE, "src/Main.java", LinkType.IMPLEMENTS);
            setField(link, "id", UUID.randomUUID());

            when(projectService.getById(projectId)).thenReturn(project);
            when(traceabilityLinkRepository.findById(link.getId())).thenReturn(Optional.of(link));

            var command = new CreateVerificationResultCommand(
                    projectId,
                    link.getId(),
                    null,
                    "openjml-esc",
                    null,
                    VerificationStatus.PROVEN,
                    AssuranceLevel.L1,
                    null,
                    NOW,
                    null);

            assertThatThrownBy(() -> verificationResultService.create(command))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void updatesVerificationResult() {
            var vr = makeResult();
            when(verificationResultRepository.findByIdAndProjectId(vr.getId(), projectId))
                    .thenReturn(Optional.of(vr));
            when(verificationResultRepository.save(any(VerificationResult.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            var command = new UpdateVerificationResultCommand(
                    null, null, "tlaplus-tlc", null, VerificationStatus.REFUTED, null, null, null, null);
            var result = verificationResultService.update(projectId, vr.getId(), command);

            assertThat(result.getProver()).isEqualTo("tlaplus-tlc");
            assertThat(result.getResult()).isEqualTo(VerificationStatus.REFUTED);
            assertThat(result.getAssuranceLevel()).isEqualTo(AssuranceLevel.L1);
        }
    }

    @Nested
    class GetById {

        @Test
        void returnsResult() {
            var vr = makeResult();
            when(verificationResultRepository.findByIdAndProjectId(vr.getId(), projectId))
                    .thenReturn(Optional.of(vr));

            var result = verificationResultService.getById(projectId, vr.getId());

            assertThat(result.getProver()).isEqualTo("openjml-esc");
        }

        @Test
        void throwsWhenNotFound() {
            var id = UUID.randomUUID();
            when(verificationResultRepository.findByIdAndProjectId(id, projectId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> verificationResultService.getById(projectId, id))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class ListByProject {

        @Test
        void listsResults() {
            when(verificationResultRepository.findByProjectIdOrderByVerifiedAtDesc(projectId))
                    .thenReturn(List.of(makeResult()));

            var results = verificationResultService.listByProject(projectId, null, null, null);

            assertThat(results).hasSize(1);
        }

        @Test
        void filtersByRequirement() {
            var reqId = UUID.randomUUID();
            when(verificationResultRepository.findByProjectIdAndRequirementIdOrderByVerifiedAtDesc(projectId, reqId))
                    .thenReturn(List.of(makeResult()));

            var results = verificationResultService.listByProject(projectId, reqId, null, null);

            assertThat(results).hasSize(1);
        }

        @Test
        void filtersByProver() {
            when(verificationResultRepository.findByProjectIdAndProverOrderByVerifiedAtDesc(projectId, "openjml-esc"))
                    .thenReturn(List.of(makeResult()));

            var results = verificationResultService.listByProject(projectId, null, "openjml-esc", null);

            assertThat(results).hasSize(1);
        }

        @Test
        void filtersByResult() {
            when(verificationResultRepository.findByProjectIdAndResultOrderByVerifiedAtDesc(
                            projectId, VerificationStatus.PROVEN))
                    .thenReturn(List.of(makeResult()));

            var results = verificationResultService.listByProject(projectId, null, null, VerificationStatus.PROVEN);

            assertThat(results).hasSize(1);
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesResult() {
            var vr = makeResult();
            when(verificationResultRepository.findByIdAndProjectId(vr.getId(), projectId))
                    .thenReturn(Optional.of(vr));

            verificationResultService.delete(projectId, vr.getId());

            verify(verificationResultRepository).delete(vr);
        }
    }
}
