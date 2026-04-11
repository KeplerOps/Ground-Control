package com.keplerops.groundcontrol.unit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.api.packregistry.PackRegistryAccessGuard;
import com.keplerops.groundcontrol.domain.exception.AuthenticationException;
import com.keplerops.groundcontrol.domain.packregistry.service.PackRegistrySecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class PackRegistryAccessGuardTest {

    @Test
    void returnsConfiguredAdminPrincipalForBearerToken() {
        var properties = new PackRegistrySecurityProperties();
        var credential = new PackRegistrySecurityProperties.AdminCredential();
        credential.setPrincipalName("pack-admin");
        credential.setToken("test-admin-token");
        properties.setAdminCredentials(java.util.List.of(credential));
        var guard = new PackRegistryAccessGuard(properties);
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer test-admin-token");

        var actor = guard.requireAdminActor(request);

        assertThat(actor).isEqualTo("pack-admin");
    }

    @Test
    void rejectsRequestsWhenAdminCredentialsAreNotConfigured() {
        var guard = new PackRegistryAccessGuard(new PackRegistrySecurityProperties());
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer test-admin-token");

        assertThatThrownBy(() -> guard.requireAdminActor(request)).isInstanceOf(AuthenticationException.class);
    }

    @Test
    void rejectsInvalidToken() {
        var properties = new PackRegistrySecurityProperties();
        var credential = new PackRegistrySecurityProperties.AdminCredential();
        credential.setPrincipalName("pack-admin");
        credential.setToken("test-admin-token");
        properties.setAdminCredentials(java.util.List.of(credential));
        var guard = new PackRegistryAccessGuard(properties);
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer wrong-token");

        assertThatThrownBy(() -> guard.requireAdminActor(request)).isInstanceOf(AuthenticationException.class);
    }
}
