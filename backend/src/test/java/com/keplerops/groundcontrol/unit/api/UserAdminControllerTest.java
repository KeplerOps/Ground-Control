package com.keplerops.groundcontrol.unit.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.admin.users.UserAdminController;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.shared.security.SecurityProperties.Role;
import com.keplerops.groundcontrol.shared.security.UserSummary;
import com.keplerops.groundcontrol.shared.security.service.UserAdminService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slice tests for the {@link UserAdminController}. {@code @AutoConfigureMockMvc(addFilters=false)}
 * skips the Spring Security chain so role-based access is verified separately (by the path
 * matrix in {@code ApiSecurityConfig} + the integration test); this slice's contract is just the
 * request/response wire format. The controller is mandatory unit-test surface per
 * {@code .gc/plan-rules.md} (ADR-037 admin user surface) so SonarCloud coverage stays honest
 * (the {@code sonar} CI job does not run Testcontainers, so integration tests do not
 * contribute coverage there).
 */
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(UserAdminController.class)
class UserAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAdminService service;

    @Nested
    class ListUsers {

        @Test
        void returnsUserResponseList() throws Exception {
            when(service.list())
                    .thenReturn(List.of(
                            new UserSummary("alice", Role.ADMIN, true), new UserSummary("bob", Role.USER, false)));

            mockMvc.perform(get("/api/v1/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users", org.hamcrest.Matchers.hasSize(2)))
                    .andExpect(jsonPath("$.users[0].username", is("alice")))
                    .andExpect(jsonPath("$.users[0].role", is("ADMIN")))
                    .andExpect(jsonPath("$.users[0].enabled", is(true)))
                    .andExpect(jsonPath("$.users[1].username", is("bob")))
                    .andExpect(jsonPath("$.users[1].role", is("USER")))
                    .andExpect(jsonPath("$.users[1].enabled", is(false)));
        }
    }

    @Nested
    class CreateUser {

        @Test
        void createsUserAndReturns201() throws Exception {
            when(service.createUser("alice", "correct-horse-battery-staple", Role.ADMIN))
                    .thenReturn(new UserSummary("alice", Role.ADMIN, true));

            mockMvc.perform(
                            post("/api/v1/admin/users")
                                    .contentType("application/json")
                                    .content(
                                            "{\"username\":\"alice\",\"password\":\"correct-horse-battery-staple\",\"role\":\"ADMIN\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username", is("alice")))
                    .andExpect(jsonPath("$.role", is("ADMIN")))
                    .andExpect(jsonPath("$.enabled", is(true)));
        }

        @Test
        void rejectsShortPasswordWith422() throws Exception {
            mockMvc.perform(post("/api/v1/admin/users")
                            .contentType("application/json")
                            .content("{\"username\":\"alice\",\"password\":\"tooshort\",\"role\":\"USER\"}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.code", is("validation_error")));
            verify(service, never()).createUser(anyString(), anyString(), any());
        }

        @Test
        void rejectsBadUsernameWith422() throws Exception {
            mockMvc.perform(
                            post("/api/v1/admin/users")
                                    .contentType("application/json")
                                    .content(
                                            "{\"username\":\"Bad Name\",\"password\":\"long-enough-password\",\"role\":\"USER\"}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.code", is("validation_error")));
        }

        @Test
        void rejectsMissingRoleWith422() throws Exception {
            mockMvc.perform(post("/api/v1/admin/users")
                            .contentType("application/json")
                            .content("{\"username\":\"alice\",\"password\":\"long-enough-password\"}"))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        void duplicateUsernameReturns409Conflict() throws Exception {
            when(service.createUser(eq("alice"), anyString(), any()))
                    .thenThrow(new ConflictException("dup", "user_exists", Map.of("username", "alice")));

            mockMvc.perform(
                            post("/api/v1/admin/users")
                                    .contentType("application/json")
                                    .content(
                                            "{\"username\":\"alice\",\"password\":\"correct-horse-battery-staple\",\"role\":\"USER\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code", is("user_exists")));
        }
    }

    @Nested
    class UpdateRole {

        @Test
        void roleChangeReturns200() throws Exception {
            when(service.updateRole("alice", Role.ADMIN)).thenReturn(new UserSummary("alice", Role.ADMIN, true));

            mockMvc.perform(patch("/api/v1/admin/users/alice/role")
                            .contentType("application/json")
                            .content("{\"role\":\"ADMIN\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role", is("ADMIN")));
        }

        @Test
        void lastAdminDemotionReturns409Conflict() throws Exception {
            when(service.updateRole("alice", Role.USER))
                    .thenThrow(new ConflictException("last", "last_admin", Map.of("username", "alice")));

            mockMvc.perform(patch("/api/v1/admin/users/alice/role")
                            .contentType("application/json")
                            .content("{\"role\":\"USER\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code", is("last_admin")));
        }

        @Test
        void unknownUserReturns404() throws Exception {
            when(service.updateRole(eq("ghost"), any())).thenThrow(new NotFoundException("ghost not found"));

            mockMvc.perform(patch("/api/v1/admin/users/ghost/role")
                            .contentType("application/json")
                            .content("{\"role\":\"USER\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code", is("not_found")));
        }

        @Test
        void rejectsMissingRoleField() throws Exception {
            mockMvc.perform(patch("/api/v1/admin/users/alice/role")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    class UpdateEnabled {

        @Test
        void enableReturns200() throws Exception {
            when(service.updateEnabled("alice", true)).thenReturn(new UserSummary("alice", Role.ADMIN, true));

            mockMvc.perform(patch("/api/v1/admin/users/alice/enabled")
                            .contentType("application/json")
                            .content("{\"enabled\":true}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled", is(true)));
        }

        @Test
        void rejectsMissingEnabledField() throws Exception {
            mockMvc.perform(patch("/api/v1/admin/users/alice/enabled")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    class DeleteUser {

        @Test
        void deleteReturns204() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/users/alice")).andExpect(status().isNoContent());

            verify(service).deleteUser("alice");
        }

        @Test
        void lastAdminDeleteReturns409Conflict() throws Exception {
            org.mockito.Mockito.doThrow(new ConflictException(
                            "Cannot remove or demote the last enabled admin",
                            "last_admin",
                            Map.of("username", "alice")))
                    .when(service)
                    .deleteUser("alice");

            mockMvc.perform(delete("/api/v1/admin/users/alice"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code", is("last_admin")))
                    .andExpect(jsonPath("$.error.message", containsString("admin")));
        }
    }
}
