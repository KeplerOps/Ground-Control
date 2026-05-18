package com.keplerops.groundcontrol.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.repository.TestCaseRepository;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class TestSuiteControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private TraceabilityLinkRepository traceabilityLinkRepository;

    private Project project;

    @BeforeEach
    void seed() {
        project = projectRepository
                .findByIdentifier("ground-control")
                .orElseGet(() -> projectRepository.save(new Project("ground-control", "Ground Control")));
    }

    private TestCase saveTestCase(String uid, String title, TestCaseStatus status) {
        var tc = new TestCase(project, uid, title, TestCaseType.MANUAL, TestCasePriority.MEDIUM);
        if (status != null) {
            // The lifecycle test transitions DRAFT -> APPROVED, etc.; we want a
            // direct seed here so the test reads as a one-off setup rather than
            // simulating a workflow.
            switch (status) {
                case APPROVED -> tc.transitionStatus(TestCaseStatus.APPROVED);
                case DEPRECATED -> {
                    tc.transitionStatus(TestCaseStatus.APPROVED);
                    tc.transitionStatus(TestCaseStatus.DEPRECATED);
                }
                case ARCHIVED -> tc.transitionStatus(TestCaseStatus.ARCHIVED);
                case DRAFT -> {
                    // already DRAFT by default
                }
            }
        }
        return testCaseRepository.save(tc);
    }

    private Requirement saveRequirement(String uid, String title) {
        return requirementRepository.save(new Requirement(project, uid, title, "statement " + uid));
    }

    private void linkRequirementToTestCase(Requirement req, String testCaseUid) {
        traceabilityLinkRepository.save(new TraceabilityLink(req, ArtifactType.TEST, testCaseUid, LinkType.TESTS));
    }

    private Map<String, Object> staticSuiteBody(String uid) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("uid", uid);
        body.put("name", "Static suite " + uid);
        body.put("populationMode", "STATIC");
        return body;
    }

    private Map<String, Object> requirementsBasedSuiteBody(String uid) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("uid", uid);
        body.put("name", "Req suite " + uid);
        body.put("populationMode", "REQUIREMENTS_BASED");
        return body;
    }

    private Map<String, Object> queryBasedSuiteBody(String uid, Map<String, Object> criteria) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("uid", uid);
        body.put("name", "Query suite " + uid);
        body.put("populationMode", "QUERY_BASED");
        body.putAll(criteria);
        return body;
    }

    private String createSuite(Map<String, Object> body) throws Exception {
        var result = mockMvc.perform(post("/api/v1/test-suites")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText();
    }

    @Test
    void staticSuite_addMembers_resolveReturnsInPositionOrder() throws Exception {
        var tc1 = saveTestCase("TC-S-1", "first", null);
        var tc2 = saveTestCase("TC-S-2", "second", null);
        String suiteId = createSuite(staticSuiteBody("TS-S-INT-001"));

        // Add two members in reverse order; resolve should return them in
        // position order (append => 0, 1).
        mockMvc.perform(post("/api/v1/test-suites/{id}/members", suiteId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testCaseId\":\"" + tc1.getId() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position", is(0)));
        mockMvc.perform(post("/api/v1/test-suites/{id}/members", suiteId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testCaseId\":\"" + tc2.getId() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position", is(1)));

        // Duplicate add rejected.
        mockMvc.perform(post("/api/v1/test-suites/{id}/members", suiteId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testCaseId\":\"" + tc1.getId() + "\"}"))
                .andExpect(status().isConflict());

        // Resolve returns members in position order.
        mockMvc.perform(get("/api/v1/test-suites/{id}/test-cases", suiteId).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].uid", is("TC-S-1")))
                .andExpect(jsonPath("$[1].uid", is("TC-S-2")));

        // Reorder to (tc2, tc1).
        mockMvc.perform(put("/api/v1/test-suites/{id}/members/reorder", suiteId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("orderedTestCaseIds", List.of(tc2.getId(), tc1.getId())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].testCaseUid", is("TC-S-2")))
                .andExpect(jsonPath("$[1].testCaseUid", is("TC-S-1")));

        // Remove tc2, resolve drops to one.
        mockMvc.perform(delete("/api/v1/test-suites/{id}/members/{tc}", suiteId, tc2.getId())
                        .param("project", "ground-control"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/test-suites/{id}/test-cases", suiteId).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uid", is("TC-S-1")));
    }

    @Test
    void requirementsBasedSuite_resolvesViaTraceability() throws Exception {
        var req1 = saveRequirement("REQ-T1", "first");
        var req2 = saveRequirement("REQ-T2", "second");
        saveTestCase("TC-R-1", "first", null);
        saveTestCase("TC-R-2", "second", null);
        saveTestCase("TC-R-3", "third", null);
        // req1 -> TC-R-1; req2 -> TC-R-2 + TC-R-3 (one requirement, two
        // tests). Resolve should deduplicate if the same UID is linked
        // through multiple requirements.
        linkRequirementToTestCase(req1, "TC-R-1");
        linkRequirementToTestCase(req2, "TC-R-2");
        linkRequirementToTestCase(req2, "TC-R-3");
        // Wrong link type — must be ignored by resolve.
        traceabilityLinkRepository.save(
                new TraceabilityLink(req1, ArtifactType.TEST, "TC-R-IGNORED", LinkType.DOCUMENTS));

        String suiteId = createSuite(requirementsBasedSuiteBody("TS-R-INT-001"));

        mockMvc.perform(post("/api/v1/test-suites/{id}/source-requirements", suiteId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirementId\":\"" + req1.getId() + "\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/test-suites/{id}/source-requirements", suiteId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirementId\":\"" + req2.getId() + "\"}"))
                .andExpect(status().isCreated());

        // Duplicate source rejected.
        mockMvc.perform(post("/api/v1/test-suites/{id}/source-requirements", suiteId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirementId\":\"" + req1.getId() + "\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/test-suites/{id}/test-cases", suiteId).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].uid", hasItem("TC-R-1")))
                .andExpect(jsonPath("$[*].uid", hasItem("TC-R-2")))
                .andExpect(jsonPath("$[*].uid", hasItem("TC-R-3")));

        // Remove req2 → only TC-R-1 left.
        mockMvc.perform(delete("/api/v1/test-suites/{id}/source-requirements/{req}", suiteId, req2.getId())
                        .param("project", "ground-control"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/test-suites/{id}/test-cases", suiteId).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uid", is("TC-R-1")));
    }

    @Test
    void queryBasedSuite_resolvesDynamicallyAsTestCasesChange() throws Exception {
        // Three cases — one APPROVED, two DRAFT — only APPROVED should match.
        saveTestCase("TC-Q-1", "alpha", TestCaseStatus.APPROVED);
        saveTestCase("TC-Q-2", "beta", null);
        var tc3 = saveTestCase("TC-Q-3", "gamma", null);

        Map<String, Object> criteria = new LinkedHashMap<>();
        criteria.put("criteriaStatus", "APPROVED");
        String suiteId = createSuite(queryBasedSuiteBody("TS-Q-INT-001", criteria));

        mockMvc.perform(get("/api/v1/test-suites/{id}/test-cases", suiteId).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uid", is("TC-Q-1")));

        // Approve TC-Q-3 — the resolve result must dynamically include it.
        tc3.transitionStatus(TestCaseStatus.APPROVED);
        testCaseRepository.save(tc3);
        mockMvc.perform(get("/api/v1/test-suites/{id}/test-cases", suiteId).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].uid", hasItem("TC-Q-1")))
                .andExpect(jsonPath("$[*].uid", hasItem("TC-Q-3")));
    }

    @Test
    void queryBasedSuite_rejectedWhenNoCriteriaPresent() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("uid", "TS-Q-EMPTY");
        body.put("name", "n");
        body.put("populationMode", "QUERY_BASED");
        mockMvc.perform(post("/api/v1/test-suites")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void staticSuite_rejectsAddSourceRequirement() throws Exception {
        var req = saveRequirement("REQ-X", "x");
        String suiteId = createSuite(staticSuiteBody("TS-S-INT-MIX"));

        mockMvc.perform(post("/api/v1/test-suites/{id}/source-requirements", suiteId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirementId\":\"" + req.getId() + "\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void requirementsBasedSuite_rejectsAddMember() throws Exception {
        var tc = saveTestCase("TC-X", "x", null);
        String suiteId = createSuite(requirementsBasedSuiteBody("TS-R-INT-MIX"));

        mockMvc.perform(post("/api/v1/test-suites/{id}/members", suiteId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testCaseId\":\"" + tc.getId() + "\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void staticSuite_rejectsCriteriaOnUpdate() throws Exception {
        String suiteId = createSuite(staticSuiteBody("TS-S-INT-CRIT"));

        mockMvc.perform(put("/api/v1/test-suites/{id}", suiteId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"criteriaStatus\":\"APPROVED\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void duplicateSuiteUidRejected() throws Exception {
        createSuite(staticSuiteBody("TS-DUP"));
        mockMvc.perform(post("/api/v1/test-suites")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staticSuiteBody("TS-DUP"))))
                .andExpect(status().isConflict());
    }

    @Test
    void getById_returns404_forUnknownSuite() throws Exception {
        mockMvc.perform(get("/api/v1/test-suites/{id}", UUID.randomUUID()).param("project", "ground-control"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCascadesMembers() throws Exception {
        var tc = saveTestCase("TC-DEL", "x", null);
        String suiteId = createSuite(staticSuiteBody("TS-DEL"));
        mockMvc.perform(post("/api/v1/test-suites/{id}/members", suiteId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testCaseId\":\"" + tc.getId() + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/test-suites/{id}", suiteId).param("project", "ground-control"))
                .andExpect(status().isNoContent());
        // Subsequent GET of the suite is 404; the test case still exists
        // (member cascade is suite -> member, not member -> testCase).
        mockMvc.perform(get("/api/v1/test-suites/{id}", suiteId).param("project", "ground-control"))
                .andExpect(status().isNotFound());
        // Test case still resolvable. AssertJ instead of Java assert so the
        // check actually runs without -ea (codex pre-push cycle 3).
        assertThat(testCaseRepository.findByIdAndProjectId(tc.getId(), project.getId()))
                .as("delete suite should not cascade to test_case rows")
                .isPresent();
    }
}
