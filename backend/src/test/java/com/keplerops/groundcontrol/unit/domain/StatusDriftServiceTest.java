package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.TestUtil;
import com.keplerops.groundcontrol.domain.adrs.model.ArchitectureDecisionRecord;
import com.keplerops.groundcontrol.domain.adrs.repository.AdrRepository;
import com.keplerops.groundcontrol.domain.adrs.state.AdrStatus;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.requirements.service.StatusDriftResult;
import com.keplerops.groundcontrol.domain.requirements.service.StatusDriftService;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.ConfidenceLevel;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import com.keplerops.groundcontrol.domain.requirements.state.StatusDriftSignal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatusDriftServiceTest {

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private TraceabilityLinkRepository traceabilityLinkRepository;

    @Mock
    private AdrRepository adrRepository;

    private StatusDriftService service;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Project TEST_PROJECT = createTestProject();
    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 5, 10);

    private static Project createTestProject() {
        var project = new Project("test-project", "Test Project");
        TestUtil.setField(project, "id", PROJECT_ID);
        return project;
    }

    @BeforeEach
    void setUp() {
        service = new StatusDriftService(requirementRepository, traceabilityLinkRepository, adrRepository);
        lenient().when(traceabilityLinkRepository.findByRequirementIdIn(any())).thenReturn(List.of());
        lenient()
                .when(adrRepository.findByProjectIdOrderByDecisionDateDesc(PROJECT_ID))
                .thenReturn(List.of());
    }

    private void stubAdrs(ArchitectureDecisionRecord... adrs) {
        when(adrRepository.findByProjectIdOrderByDecisionDateDesc(PROJECT_ID)).thenReturn(List.of(adrs));
    }

    // ------------------------------------------------------------------------
    // Test data helpers
    // ------------------------------------------------------------------------

    private static Requirement draft(String uid) {
        return requirement(uid, Status.DRAFT);
    }

    private static Requirement requirement(String uid, Status status) {
        var req = new Requirement(TEST_PROJECT, uid, "Title for " + uid, "Statement for " + uid);
        TestUtil.setField(req, "id", UUID.randomUUID());
        TestUtil.setField(req, "status", status);
        return req;
    }

    private static TraceabilityLink link(Requirement req, ArtifactType type, String identifier, LinkType linkType) {
        var l = new TraceabilityLink(req, type, identifier, linkType);
        l.setArtifactTitle("artifact " + identifier);
        l.setArtifactUrl("https://example/" + identifier);
        return l;
    }

    private static ArchitectureDecisionRecord adr(String uid, AdrStatus status) {
        var adrRecord = new ArchitectureDecisionRecord(
                TEST_PROJECT, uid, "ADR " + uid, FIXED_DATE, "context", "decision", "consequences", "tester");
        if (status == AdrStatus.ACCEPTED) {
            adrRecord.transitionStatus(AdrStatus.ACCEPTED);
        }
        return adrRecord;
    }

    private void stubDrafts(Requirement... reqs) {
        when(requirementRepository.findByProjectIdAndStatusAndArchivedAtIsNull(PROJECT_ID, Status.DRAFT))
                .thenReturn(List.of(reqs));
    }

    private void stubLinks(TraceabilityLink... links) {
        when(traceabilityLinkRepository.findByRequirementIdIn(any())).thenReturn(List.of(links));
    }

    // ------------------------------------------------------------------------
    // Acceptance criteria
    // ------------------------------------------------------------------------

    @Test
    void draftWithImplementsLinkToClosedIssue_flaggedHigh() {
        // The GC-O007 / #794 shape: an IMPLEMENTS edge to a GitHub issue is allowed on a DRAFT
        // requirement and is strong evidence the work has landed.
        var req = draft("GC-O007");
        stubDrafts(req);
        stubLinks(link(req, ArtifactType.GITHUB_ISSUE, "794", LinkType.IMPLEMENTS));

        var result = service.analyze(PROJECT_ID, ConfidenceLevel.MEDIUM);

        assertThat(result.draftRequirementsScanned()).isEqualTo(1);
        assertThat(result.findings()).hasSize(1);
        var finding = result.findings().getFirst();
        assertThat(finding.uid()).isEqualTo("GC-O007");
        assertThat(finding.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        assertThat(finding.strongestSignal()).isEqualTo(StatusDriftSignal.IMPLEMENTS_LINK_ON_DRAFT);
        assertThat(finding.evidence())
                .extracting(StatusDriftResult.Evidence::signal)
                .containsExactly(StatusDriftSignal.IMPLEMENTS_LINK_ON_DRAFT);
    }

    @Test
    void draftWithAcceptedAdrDocumentsLink_flaggedMedium() {
        var req = draft("GC-H001");
        stubDrafts(req);
        stubLinks(link(req, ArtifactType.ADR, "ADR-024", LinkType.DOCUMENTS));
        stubAdrs(adr("ADR-024", AdrStatus.ACCEPTED));

        var result = service.analyze(PROJECT_ID, ConfidenceLevel.MEDIUM);

        assertThat(result.findings()).hasSize(1);
        var finding = result.findings().getFirst();
        assertThat(finding.uid()).isEqualTo("GC-H001");
        assertThat(finding.confidence()).isEqualTo(ConfidenceLevel.MEDIUM);
        assertThat(finding.strongestSignal()).isEqualTo(StatusDriftSignal.ACCEPTED_ADR_DOCUMENTS_LINK);
    }

    @Test
    void draftWithNoEvidence_omitted() {
        var withEvidence = draft("GC-T001");
        var noEvidence = draft("GC-X999");
        stubDrafts(withEvidence, noEvidence);
        stubLinks(link(withEvidence, ArtifactType.GITHUB_ISSUE, "256", LinkType.IMPLEMENTS));

        var result = service.analyze(PROJECT_ID, ConfidenceLevel.MEDIUM);

        assertThat(result.draftRequirementsScanned()).isEqualTo(2);
        assertThat(result.findings()).extracting(StatusDriftResult.Finding::uid).containsExactly("GC-T001");
    }

    @Test
    void onlyDraftRequirementsAreScanned() {
        // The service queries only DRAFT requirements; ACTIVE/DEPRECATED/ARCHIVED are never examined,
        // so they can never be flagged regardless of any IMPLEMENTS / accepted-ADR / linked-issue evidence.
        when(requirementRepository.findByProjectIdAndStatusAndArchivedAtIsNull(PROJECT_ID, Status.DRAFT))
                .thenReturn(List.of());

        var result = service.analyze(PROJECT_ID, ConfidenceLevel.MEDIUM);

        assertThat(result.draftRequirementsScanned()).isZero();
        assertThat(result.findings()).isEmpty();
        verify(requirementRepository).findByProjectIdAndStatusAndArchivedAtIsNull(PROJECT_ID, Status.DRAFT);
    }

    // ------------------------------------------------------------------------
    // Threshold + signal coverage
    // ------------------------------------------------------------------------

    @Test
    void minimumConfidenceHigh_dropsMediumAndLowFindings() {
        var high = draft("GC-A001"); // IMPLEMENTS link -> HIGH
        var medium = draft("GC-A002"); // linked GitHub issue -> MEDIUM
        var low = draft("GC-A003"); // DOCUMENTS link to a code file -> LOW
        stubDrafts(high, medium, low);
        when(traceabilityLinkRepository.findByRequirementIdIn(any()))
                .thenReturn(List.of(
                        link(high, ArtifactType.CODE_FILE, "src/Foo.java", LinkType.IMPLEMENTS),
                        link(medium, ArtifactType.GITHUB_ISSUE, "10", LinkType.DOCUMENTS),
                        link(low, ArtifactType.CODE_FILE, "src/Bar.java", LinkType.DOCUMENTS)));

        assertThat(service.analyze(PROJECT_ID, ConfidenceLevel.HIGH).findings())
                .extracting(StatusDriftResult.Finding::uid)
                .containsExactly("GC-A001");
        assertThat(service.analyze(PROJECT_ID, ConfidenceLevel.MEDIUM).findings())
                .extracting(StatusDriftResult.Finding::uid)
                .containsExactly("GC-A001", "GC-A002");
        assertThat(service.analyze(PROJECT_ID, ConfidenceLevel.LOW).findings())
                .extracting(StatusDriftResult.Finding::uid)
                .containsExactlyInAnyOrder("GC-A001", "GC-A002", "GC-A003");
    }

    @ParameterizedTest(name = "{0}/{1} -> {2} ({3})")
    @CsvSource({
        // artifactType, linkType, expectedSignal, expectedConfidence
        // Any IMPLEMENTS link on a DRAFT requirement is the strongest signal, regardless of artifact type.
        "GITHUB_ISSUE,  IMPLEMENTS,  IMPLEMENTS_LINK_ON_DRAFT,     HIGH",
        "CODE_FILE,     IMPLEMENTS,  IMPLEMENTS_LINK_ON_DRAFT,     HIGH",
        "TEST,          IMPLEMENTS,  IMPLEMENTS_LINK_ON_DRAFT,     HIGH",
        // Non-IMPLEMENTS links to issues / PRs are medium evidence.
        "GITHUB_ISSUE,  DOCUMENTS,   LINKED_GITHUB_ISSUE,         MEDIUM",
        "PULL_REQUEST,  DOCUMENTS,   LINKED_PULL_REQUEST,         MEDIUM",
        "PULL_REQUEST,  CONSTRAINS,  LINKED_PULL_REQUEST,         MEDIUM",
        // Non-IMPLEMENTS links to code / test / spec / proof artifacts are low evidence.
        "CODE_FILE,     TESTS,       LINKED_CODE_ARTIFACT,        LOW",
        "TEST,          TESTS,       LINKED_CODE_ARTIFACT,        LOW",
        "SPEC,          DOCUMENTS,   LINKED_CODE_ARTIFACT,        LOW",
        "PROOF,         VERIFIES,    LINKED_CODE_ARTIFACT,        LOW",
        // Non-IMPLEMENTS links to documentation / config / policy artifacts are low evidence.
        "DOCUMENTATION, DOCUMENTS,   LINKED_DOC_ARTIFACT,         LOW",
        "CONFIG,        DOCUMENTS,   LINKED_DOC_ARTIFACT,         LOW",
        "POLICY,        CONSTRAINS,  LINKED_DOC_ARTIFACT,         LOW",
    })
    void linkClassification(
            ArtifactType artifactType,
            LinkType linkType,
            StatusDriftSignal expectedSignal,
            ConfidenceLevel expectedConfidence) {
        var req = draft("GC-X100");
        stubDrafts(req);
        stubLinks(link(req, artifactType, "art-1", linkType));

        // Use the LOW threshold so even LOW-confidence signals surface.
        assertThat(service.analyze(PROJECT_ID, ConfidenceLevel.LOW).findings())
                .singleElement()
                .satisfies(f -> {
                    assertThat(f.confidence()).isEqualTo(expectedConfidence);
                    assertThat(f.strongestSignal()).isEqualTo(expectedSignal);
                    assertThat(f.evidence())
                            .extracting(StatusDriftResult.Evidence::signal)
                            .containsExactly(expectedSignal);
                });
    }

    @ParameterizedTest(name = "non-IMPLEMENTS {0} link -> not evidence")
    @CsvSource({"RISK_SCENARIO, CONSTRAINS", "CONTROL, CONSTRAINS"})
    void nonImplementsLinkToRiskOrControl_isNotEvidence(ArtifactType artifactType, LinkType linkType) {
        var req = draft("GC-X101");
        stubDrafts(req);
        stubLinks(link(req, artifactType, "x-1", linkType));

        assertThat(service.analyze(PROJECT_ID, ConfidenceLevel.LOW).findings()).isEmpty();
    }

    @Test
    void proposedAdrDocumentsLink_isNotEvidence() {
        var req = draft("GC-Q010");
        stubDrafts(req);
        stubLinks(link(req, ArtifactType.ADR, "ADR-099", LinkType.DOCUMENTS));
        stubAdrs(adr("ADR-099", AdrStatus.PROPOSED));

        assertThat(service.analyze(PROJECT_ID, ConfidenceLevel.LOW).findings()).isEmpty();
    }

    @Test
    void unknownAdrLink_isNotEvidence() {
        var req = draft("GC-Q011");
        stubDrafts(req);
        stubLinks(link(req, ArtifactType.ADR, "ADR-404", LinkType.DOCUMENTS));
        // ADR-404 is not among the project's ADRs (default stub returns none) -> no evidence.

        assertThat(service.analyze(PROJECT_ID, ConfidenceLevel.LOW).findings()).isEmpty();
    }

    @Test
    void riskScenarioAndControlLinks_areNotEvidence() {
        var req = draft("GC-T011");
        stubDrafts(req);
        stubLinks(
                link(req, ArtifactType.RISK_SCENARIO, "RS-001", LinkType.CONSTRAINS),
                link(req, ArtifactType.CONTROL, "CTRL-001", LinkType.CONSTRAINS));

        assertThat(service.analyze(PROJECT_ID, ConfidenceLevel.LOW).findings()).isEmpty();
    }

    @Test
    void multipleEvidence_takesStrongestConfidenceAndSignal() {
        var req = draft("GC-T010");
        stubDrafts(req);
        stubLinks(
                link(req, ArtifactType.GITHUB_ISSUE, "826", LinkType.IMPLEMENTS),
                link(req, ArtifactType.ADR, "ADR-024", LinkType.DOCUMENTS));
        stubAdrs(adr("ADR-024", AdrStatus.ACCEPTED));

        var finding =
                service.analyze(PROJECT_ID, ConfidenceLevel.MEDIUM).findings().getFirst();
        assertThat(finding.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        assertThat(finding.strongestSignal()).isEqualTo(StatusDriftSignal.IMPLEMENTS_LINK_ON_DRAFT);
        assertThat(finding.evidence()).hasSize(2);
    }

    @Test
    void findingsSortedByConfidenceDescThenUid() {
        var a = draft("GC-A001"); // ADR -> MEDIUM
        var b = draft("GC-B002"); // IMPLEMENTS -> HIGH
        var c = draft("GC-C003"); // IMPLEMENTS -> HIGH
        stubDrafts(a, b, c);
        when(traceabilityLinkRepository.findByRequirementIdIn(any()))
                .thenReturn(List.of(
                        link(a, ArtifactType.ADR, "ADR-001", LinkType.DOCUMENTS),
                        link(b, ArtifactType.GITHUB_ISSUE, "2", LinkType.IMPLEMENTS),
                        link(c, ArtifactType.GITHUB_ISSUE, "3", LinkType.IMPLEMENTS)));
        stubAdrs(adr("ADR-001", AdrStatus.ACCEPTED));

        assertThat(service.analyze(PROJECT_ID, ConfidenceLevel.MEDIUM).findings())
                .extracting(StatusDriftResult.Finding::uid)
                .containsExactly("GC-B002", "GC-C003", "GC-A001");
    }

    @Test
    void evidenceCarriesLinkArtifactDetails() {
        var req = draft("GC-T010");
        stubDrafts(req);
        var l = link(req, ArtifactType.GITHUB_ISSUE, "826", LinkType.IMPLEMENTS);
        l.setArtifactTitle("GC-T010: Risk Assessment Result Entity");
        l.setArtifactUrl("https://github.com/KeplerOps/Ground-Control/issues/826");
        stubLinks(l);

        var evidence = service.analyze(PROJECT_ID, ConfidenceLevel.MEDIUM)
                .findings()
                .getFirst()
                .evidence()
                .getFirst();
        assertThat(evidence.artifactType()).isEqualTo("GITHUB_ISSUE");
        assertThat(evidence.artifactIdentifier()).isEqualTo("826");
        assertThat(evidence.artifactTitle()).isEqualTo("GC-T010: Risk Assessment Result Entity");
        assertThat(evidence.artifactUrl()).isEqualTo("https://github.com/KeplerOps/Ground-Control/issues/826");
        assertThat(evidence.confidence()).isEqualTo(ConfidenceLevel.HIGH);
        assertThat(evidence.detail()).isNotBlank();
    }

    @Test
    void noDraftRequirements_returnsEmptyResult() {
        when(requirementRepository.findByProjectIdAndStatusAndArchivedAtIsNull(PROJECT_ID, Status.DRAFT))
                .thenReturn(List.of());

        var result = service.analyze(PROJECT_ID, ConfidenceLevel.LOW);

        assertThat(result.draftRequirementsScanned()).isZero();
        assertThat(result.minimumConfidence()).isEqualTo(ConfidenceLevel.LOW);
        assertThat(result.findings()).isEmpty();
    }
}
