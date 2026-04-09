package com.keplerops.groundcontrol.unit.api;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.packregistry.TrustPolicyController;
import com.keplerops.groundcontrol.domain.packregistry.model.TrustPolicy;
import com.keplerops.groundcontrol.domain.packregistry.model.TrustPolicyRule;
import com.keplerops.groundcontrol.domain.packregistry.service.CreateTrustPolicyCommand;
import com.keplerops.groundcontrol.domain.packregistry.service.TrustPolicyService;
import com.keplerops.groundcontrol.domain.packregistry.service.UpdateTrustPolicyCommand;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustPolicyField;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustPolicyRuleOperator;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TrustPolicyController.class)
class TrustPolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrustPolicyService trustPolicyService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID POLICY_ID = UUID.fromString("00000000-0000-0000-0000-000000000070");

    private Project makeProject() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        return project;
    }

    private TrustPolicy makePolicy() {
        var project = makeProject();
        var policy = new TrustPolicy(project, "allow-nist", TrustOutcome.REJECTED);
        setField(policy, "id", POLICY_ID);
        setField(policy, "createdAt", Instant.now());
        setField(policy, "updatedAt", Instant.now());
        policy.setDescription("Allow NIST packs");
        policy.setPriority(1);
        policy.setRules(List.of(new TrustPolicyRule(
                TrustPolicyField.PUBLISHER, TrustPolicyRuleOperator.EQUALS, "NIST", TrustOutcome.TRUSTED)));
        return policy;
    }

    @Test
    void createReturnsCreated() throws Exception {
        when(projectService.resolveProjectId(null)).thenReturn(PROJECT_ID);
        when(trustPolicyService.create(any(CreateTrustPolicyCommand.class))).thenReturn(makePolicy());

        mockMvc.perform(
                        post("/api/v1/trust-policies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"name":"allow-nist","defaultOutcome":"REJECTED","priority":1,"enabled":true}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("allow-nist")))
                .andExpect(jsonPath("$.defaultOutcome", is("REJECTED")));
    }

    @Test
    void listReturnsPolicies() throws Exception {
        when(projectService.resolveProjectId(null)).thenReturn(PROJECT_ID);
        when(trustPolicyService.list(PROJECT_ID)).thenReturn(List.of(makePolicy()));

        mockMvc.perform(get("/api/v1/trust-policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("allow-nist")));
    }

    @Test
    void getReturnsPolicy() throws Exception {
        when(trustPolicyService.get(POLICY_ID)).thenReturn(makePolicy());

        mockMvc.perform(get("/api/v1/trust-policies/" + POLICY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("allow-nist")));
    }

    @Test
    void updateReturnsUpdatedPolicy() throws Exception {
        var updated = makePolicy();
        updated.setDescription("Updated description");
        when(trustPolicyService.update(eq(POLICY_ID), any(UpdateTrustPolicyCommand.class)))
                .thenReturn(updated);

        mockMvc.perform(
                        put("/api/v1/trust-policies/" + POLICY_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"description":"Updated description","defaultOutcome":"REJECTED","priority":1,"enabled":true}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", is("Updated description")));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/trust-policies/" + POLICY_ID)).andExpect(status().isNoContent());

        verify(trustPolicyService).delete(POLICY_ID);
    }
}
