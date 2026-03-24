package com.keplerops.groundcontrol.api.credentials;

import jakarta.validation.constraints.NotBlank;

public record CredentialRequest(
        @NotBlank String name,
        @NotBlank String credentialType,
        String data) {}
