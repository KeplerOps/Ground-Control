package com.keplerops.groundcontrol.unit.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.shared.security.SecurityProperties;
import com.keplerops.groundcontrol.shared.security.SecurityProperties.ApiCredential;
import com.keplerops.groundcontrol.shared.security.SecurityProperties.Role;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SecurityPropertiesTest {

    @Nested
    class Defaults {

        @Test
        void newInstance_isEnabledWithEmptyLists_andOpenapiNotPublic() {
            var props = new SecurityProperties();

            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getCredentials()).isEmpty();
            assertThat(props.getIpAllowlist()).isEmpty();
            assertThat(props.isOpenapiPublic()).isFalse();
        }

        @Test
        void setNullLists_normalizesToEmpty() {
            var props = new SecurityProperties();

            props.setCredentials(null);
            props.setIpAllowlist(null);

            assertThat(props.getCredentials()).isEmpty();
            assertThat(props.getIpAllowlist()).isEmpty();
        }
    }

    @Nested
    class CredentialValidation {

        @Test
        void blankPrincipalName_rejected() {
            var cred = new ApiCredential();
            cred.setPrincipalName(" ");
            cred.setToken("t1");
            cred.setRole(Role.USER);

            assertThatThrownBy(cred::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("principalName");
        }

        @Test
        void blankToken_rejected() {
            var cred = new ApiCredential();
            cred.setPrincipalName("alice");
            cred.setToken("");
            cred.setRole(Role.ADMIN);

            assertThatThrownBy(cred::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("token");
        }

        @Test
        void nullRole_rejected() {
            var cred = new ApiCredential();
            cred.setPrincipalName("alice");
            cred.setToken("t1");

            assertThatThrownBy(cred::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("role");
        }

        @Test
        void wellFormed_accepted() {
            var cred = new ApiCredential();
            cred.setPrincipalName("alice");
            cred.setToken("strong-token-1234");
            cred.setRole(Role.ADMIN);

            cred.validate();

            assertThat(cred.getPrincipalName()).isEqualTo("alice");
            assertThat(cred.getToken()).isEqualTo("strong-token-1234");
            assertThat(cred.getRole()).isEqualTo(Role.ADMIN);
        }
    }

    @Nested
    class IpAllowlistValidation {

        @Test
        void invalidCidr_rejectedDuringValidate() {
            var props = new SecurityProperties();
            props.setIpAllowlist(List.of("not-a-cidr"));

            assertThatThrownBy(props::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ipAllowlist");
        }

        @Test
        void wellFormedCidrs_accepted() {
            var props = new SecurityProperties();
            props.setIpAllowlist(List.of("10.0.0.0/8", "127.0.0.1/32", "::1/128"));

            props.validate();

            assertThat(props.getIpAllowlist()).hasSize(3);
        }

        @Test
        void duplicateCredentialTokens_rejected() {
            var c1 = new ApiCredential();
            c1.setPrincipalName("a");
            c1.setToken("same-token");
            c1.setRole(Role.USER);
            var c2 = new ApiCredential();
            c2.setPrincipalName("b");
            c2.setToken("same-token");
            c2.setRole(Role.ADMIN);

            var props = new SecurityProperties();
            props.setCredentials(List.of(c1, c2));

            assertThatThrownBy(props::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("duplicate");
        }
    }
}
