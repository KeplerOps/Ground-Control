package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.plugins.service.Plugin;
import com.keplerops.groundcontrol.domain.plugins.service.PluginDescriptor;
import com.keplerops.groundcontrol.domain.plugins.state.PluginType;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Contract tests for {@link Plugin}. Uses minimal stubs to verify the interface is expressive enough
 * for all plugin categories listed in ADR-022: pack handlers, registry backends, validators, and
 * policy hooks.
 */
class PluginContractTest {

    static class StubPlugin implements Plugin {

        private final PluginDescriptor descriptor;
        private final boolean available;

        StubPlugin(PluginDescriptor descriptor, boolean available) {
            this.descriptor = descriptor;
            this.available = available;
        }

        @Override
        public PluginDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }
    }

    @Nested
    class PackHandler {

        @Test
        void expressesRequirementsPackHandler() {
            var descriptor = new PluginDescriptor(
                    "requirements-pack-handler",
                    "1.0.0",
                    "Handles installation and upgrade of requirements packs",
                    PluginType.PACK_HANDLER,
                    Set.of("requirements-pack", "install", "upgrade"),
                    Map.of("formats", "sdoc,reqif"));
            var plugin = new StubPlugin(descriptor, true);

            assertThat(plugin.descriptor().name()).isEqualTo("requirements-pack-handler");
            assertThat(plugin.descriptor().type()).isEqualTo(PluginType.PACK_HANDLER);
            assertThat(plugin.descriptor().version()).isEqualTo("1.0.0");
            assertThat(plugin.descriptor().capabilities()).contains("requirements-pack", "install");
            assertThat(plugin.isAvailable()).isTrue();
        }

        @Test
        void expressesControlPackHandler() {
            var descriptor = new PluginDescriptor(
                    "control-pack-handler",
                    "1.0.0",
                    "Handles installation and upgrade of control packs",
                    PluginType.PACK_HANDLER,
                    Set.of("control-pack", "install", "upgrade"),
                    Map.of("frameworks", "nist-csf,iso-27001"));
            var plugin = new StubPlugin(descriptor, true);

            assertThat(plugin.descriptor().name()).isEqualTo("control-pack-handler");
            assertThat(plugin.descriptor().type()).isEqualTo(PluginType.PACK_HANDLER);
            assertThat(plugin.descriptor().capabilities()).contains("control-pack");
        }
    }

    @Nested
    class RegistryBackend {

        @Test
        void expressesRemoteRegistryBackend() {
            var descriptor = new PluginDescriptor(
                    "oci-registry",
                    "0.1.0",
                    "OCI-based pack registry backend",
                    PluginType.REGISTRY_BACKEND,
                    Set.of("oci", "pull", "push"),
                    Map.of("registry_url", "ghcr.io"));
            var plugin = new StubPlugin(descriptor, true);

            assertThat(plugin.descriptor().name()).isEqualTo("oci-registry");
            assertThat(plugin.descriptor().type()).isEqualTo(PluginType.REGISTRY_BACKEND);
            assertThat(plugin.descriptor().capabilities()).contains("oci", "pull", "push");
            assertThat(plugin.descriptor().metadata()).containsKey("registry_url");
        }
    }

    @Nested
    class Validator {

        @Test
        void expressesPackValidator() {
            var descriptor = new PluginDescriptor(
                    "pack-integrity-validator",
                    "1.0.0",
                    "Validates pack checksums and signatures",
                    PluginType.VALIDATOR,
                    Set.of("checksum", "signature", "compatibility"),
                    Map.of());
            var plugin = new StubPlugin(descriptor, true);

            assertThat(plugin.descriptor().name()).isEqualTo("pack-integrity-validator");
            assertThat(plugin.descriptor().type()).isEqualTo(PluginType.VALIDATOR);
            assertThat(plugin.descriptor().capabilities()).contains("checksum", "signature");
        }
    }

    @Nested
    class PolicyHook {

        @Test
        void expressesInstallTimePolicyHook() {
            var descriptor = new PluginDescriptor(
                    "trust-policy-hook",
                    "1.0.0",
                    "Enforces trust policy during pack installation",
                    PluginType.POLICY_HOOK,
                    Set.of("trust-check", "publisher-allowlist"),
                    Map.of("enforcement", "strict"));
            var plugin = new StubPlugin(descriptor, true);

            assertThat(plugin.descriptor().name()).isEqualTo("trust-policy-hook");
            assertThat(plugin.descriptor().type()).isEqualTo(PluginType.POLICY_HOOK);
            assertThat(plugin.descriptor().capabilities()).contains("trust-check");
            assertThat(plugin.descriptor().metadata()).containsEntry("enforcement", "strict");
        }
    }

    @Nested
    class DefaultLifecycle {

        @Test
        void defaultMethodsAreNoOps() {
            var descriptor =
                    new PluginDescriptor("noop-plugin", "1.0.0", "Test plugin", PluginType.CUSTOM, Set.of(), Map.of());
            var plugin = new StubPlugin(descriptor, true);

            // Default lifecycle methods should not throw
            plugin.initialize();
            plugin.start();
            plugin.stop();

            assertThat(plugin.isAvailable()).isTrue();
        }

        @Test
        void unavailablePluginReportsCorrectly() {
            var descriptor = new PluginDescriptor(
                    "disabled-plugin", "1.0.0", "Disabled plugin", PluginType.CUSTOM, Set.of(), Map.of());
            var plugin = new StubPlugin(descriptor, false);

            assertThat(plugin.isAvailable()).isFalse();
            assertThat(plugin.descriptor().name()).isEqualTo("disabled-plugin");
        }
    }
}
