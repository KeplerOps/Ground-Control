package com.keplerops.groundcontrol.api.admin.users;

import com.keplerops.groundcontrol.shared.security.UserCredentialPolicy;
import com.keplerops.groundcontrol.shared.security.UserSummary;
import com.keplerops.groundcontrol.shared.security.service.UserAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only REST surface for ADR-037 user lifecycle operations: list, create, role change,
 * enable/disable, delete. Authorization to {@code ROLE_ADMIN} is enforced by the path matrix in
 * {@link com.keplerops.groundcontrol.shared.security.ApiSecurityConfig} (entry already present
 * via the {@code /api/v1/admin/**} rule); the controller carries no in-band auth check so the
 * path matrix stays the single source of truth (ADR-026 §3 / ADR-037 §1).
 *
 * <p>All mutating operations delegate to {@link UserAdminService}, which is where the last-admin
 * guard, username/password validation, and role-mutation transactional semantics live. Keeping
 * the controller thin keeps the {@code WebMvcTest} slice fast and focused on wire-format
 * coverage (per {@code .gc/plan-rules.md}).
 */
@Validated
@RestController
@RequestMapping("/api/v1/admin/users")
public class UserAdminController {

    // Path-binding pattern reuses the shared policy regex so a future widening (e.g., longer
    // usernames or an extra-character set) only has to be applied at UserCredentialPolicy +
    // V059 CHECK + docs, not also here.
    private static final String USERNAME_REGEX = UserCredentialPolicy.USERNAME_REGEX;

    private final UserAdminService service;

    public UserAdminController(UserAdminService service) {
        this.service = service;
    }

    @GetMapping
    public UsersListResponse list() {
        return new UsersListResponse(
                service.list().stream().map(UserAdminController::toResponse).toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@RequestBody @Valid CreateUserRequest request) {
        return toResponse(service.createUser(request.username(), request.password(), request.role()));
    }

    @PatchMapping("/{username}/role")
    public UserResponse updateRole(
            @PathVariable @NotBlank @Pattern(regexp = USERNAME_REGEX) String username,
            @RequestBody @Valid UpdateUserRoleRequest request) {
        return toResponse(service.updateRole(username, request.role()));
    }

    @PatchMapping("/{username}/enabled")
    public UserResponse updateEnabled(
            @PathVariable @NotBlank @Pattern(regexp = USERNAME_REGEX) String username,
            @RequestBody @Valid UpdateUserEnabledRequest request) {
        return toResponse(service.updateEnabled(username, request.enabled()));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> delete(@PathVariable @NotBlank @Pattern(regexp = USERNAME_REGEX) String username) {
        service.deleteUser(username);
        return ResponseEntity.noContent().build();
    }

    /**
     * Map the service-layer view to the API wire shape. Keeping the mapping at the controller
     * boundary lets the service surface stay framework-neutral (callable from a CLI runner,
     * scheduled task, etc.) without re-exposing the JSON DTO.
     */
    private static UserResponse toResponse(UserSummary summary) {
        return new UserResponse(summary.username(), summary.role(), summary.enabled());
    }

    /** List wrapper so the JSON response shape stays consistent with the rest of the admin API. */
    public record UsersListResponse(List<UserResponse> users) {}
}
