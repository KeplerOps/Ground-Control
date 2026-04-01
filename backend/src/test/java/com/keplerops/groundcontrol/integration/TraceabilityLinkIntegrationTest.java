package com.keplerops.groundcontrol.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import com.keplerops.groundcontrol.domain.requirements.model.GitHubIssueSync;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementImport;
import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import com.keplerops.groundcontrol.domain.requirements.repository.GitHubIssueSyncRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementImportRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.ImportSourceType;
import com.keplerops.groundcontrol.domain.requirements.state.IssueState;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.SyncStatus;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class TraceabilityLinkIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private TraceabilityLinkRepository traceabilityLinkRepository;

    @Autowired
    private GitHubIssueSyncRepository gitHubIssueSyncRepository;

    @Autowired
    private RequirementImportRepository requirementImportRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ProjectRepository projectRepository;

    private Project testProject;

    @BeforeEach
    void setUp() {
        testProject = projectRepository.findByIdentifier("ground-control").orElseThrow();
    }

    @Test
    void persistAndRetrieveWithRequirementFK() {
        var req = new Requirement(testProject, "REQ-LINK-001", "Linked requirement", "Statement");
        requirementRepository.save(req);

        var link = new TraceabilityLink(req, ArtifactType.CODE_FILE, "file:src/Main.java", LinkType.IMPLEMENTS);
        link.setArtifactUrl("https://github.com/org/repo/blob/main/src/Main.java");
        link.setArtifactTitle("Main.java");
        link.setSyncStatus(SyncStatus.SYNCED);
        traceabilityLinkRepository.save(link);

        entityManager.flush();
        entityManager.clear();

        var found = traceabilityLinkRepository.findByRequirementId(req.getId());
        assertThat(found).hasSize(1);

        var retrieved = found.get(0);
        assertThat(retrieved.getArtifactType()).isEqualTo(ArtifactType.CODE_FILE);
        assertThat(retrieved.getArtifactIdentifier()).isEqualTo("file:src/Main.java");
        assertThat(retrieved.getLinkType()).isEqualTo(LinkType.IMPLEMENTS);
        assertThat(retrieved.getSyncStatus()).isEqualTo(SyncStatus.SYNCED);
        assertThat(retrieved.getArtifactUrl()).isEqualTo("https://github.com/org/repo/blob/main/src/Main.java");
        assertThat(retrieved.getArtifactTitle()).isEqualTo("Main.java");
        assertThat(retrieved.getCreatedAt()).isNotNull();
        assertThat(retrieved.getUpdatedAt()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ArtifactType.class)
    void persistAndRetrieveAllArtifactTypes(ArtifactType type) {
        var req = new Requirement(testProject, "REQ-TYPE-" + type.name(), "Req for " + type, "Statement");
        requirementRepository.save(req);

        var link = new TraceabilityLink(req, type, "id:" + type.name(), LinkType.IMPLEMENTS);
        traceabilityLinkRepository.save(link);

        entityManager.flush();
        entityManager.clear();

        var found = traceabilityLinkRepository.findByRequirementId(req.getId());
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getArtifactType()).isEqualTo(type);
        assertThat(found.get(0).getArtifactIdentifier()).isEqualTo("id:" + type.name());
    }

    @Test
    void jsonbFieldsPersistAndRetrieve() {
        var sync = new GitHubIssueSync(
                175, "Phase 1C entities", IssueState.OPEN, "https://github.com/org/repo/issues/175", Instant.now());
        sync.setIssueLabels(List.of("phase-1", "enhancement", "data-model"));
        sync.setCrossReferences(List.of(174, 176));
        sync.setIssueBody("Implement traceability entities");
        sync.setPhase(1);
        sync.setPriorityLabel("P1");
        gitHubIssueSyncRepository.save(sync);

        entityManager.flush();
        entityManager.clear();

        var found = gitHubIssueSyncRepository.findByIssueNumber(175);
        assertThat(found).isPresent();

        var retrieved = found.get();
        assertThat(retrieved.getIssueTitle()).isEqualTo("Phase 1C entities");
        assertThat(retrieved.getIssueState()).isEqualTo(IssueState.OPEN);
        assertThat(retrieved.getIssueLabels()).containsExactly("phase-1", "enhancement", "data-model");
        assertThat(retrieved.getCrossReferences()).containsExactly(174, 176);
        assertThat(retrieved.getIssueBody()).isEqualTo("Implement traceability entities");
        assertThat(retrieved.getPhase()).isEqualTo(1);
        assertThat(retrieved.getPriorityLabel()).isEqualTo("P1");
        assertThat(retrieved.getCreatedAt()).isNotNull();
    }

    @Test
    void requirementImportJsonbRoundTrip() {
        var imp = new RequirementImport(ImportSourceType.STRICTDOC);
        imp.setSourceFile("project.sdoc");
        imp.setStats(Map.of("total", 80, "imported", 78, "skipped", 2));
        imp.setErrors(List.of(
                Map.of("line", 42, "message", "duplicate UID"), Map.of("line", 99, "message", "missing statement")));
        requirementImportRepository.save(imp);

        entityManager.flush();
        entityManager.clear();

        var found = requirementImportRepository.findById(imp.getId());
        assertThat(found).isPresent();

        var retrieved = found.get();
        assertThat(retrieved.getSourceType()).isEqualTo(ImportSourceType.STRICTDOC);
        assertThat(retrieved.getSourceFile()).isEqualTo("project.sdoc");
        assertThat(retrieved.getImportedAt()).isNotNull();
        assertThat(retrieved.getStats()).containsEntry("total", 80);
        assertThat(retrieved.getErrors()).hasSize(2);
        assertThat(retrieved.getErrors().get(0)).containsEntry("message", "duplicate UID");
    }

    @Test
    void enversAuditTrailRecordsRevisions() {
        // Envers writes audit data on commit, so we must commit to see revisions
        var req = new Requirement(testProject, "REQ-AUDIT-001", "Audited req", "Statement");
        requirementRepository.save(req);
        var link = new TraceabilityLink(req, ArtifactType.CODE_FILE, "file:src/Main.java", LinkType.IMPLEMENTS);
        traceabilityLinkRepository.save(link);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // Update link — creates revision 2
        TestTransaction.start();
        var saved = traceabilityLinkRepository.findById(link.getId()).orElseThrow();
        saved.setSyncStatus(SyncStatus.STALE);
        traceabilityLinkRepository.save(saved);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // Verify 2 revisions exist
        TestTransaction.start();
        var auditReader = AuditReaderFactory.get(entityManager);
        var revisions = auditReader.getRevisions(TraceabilityLink.class, link.getId());
        assertThat(revisions).hasSize(2);
    }
}
