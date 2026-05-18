package com.keplerops.groundcontrol.api.testcases;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddTestRunTesterRequest(
        // Tester names round-trip through `/api/v1/test-runs/{id}/testers/{testerName}`
        // as a URL path segment on remove; constrain at create so any name we accept
        // can actually be deleted. The allow-list keeps letters, digits, spaces, and
        // a small punctuation set used in real human names — slashes, question marks,
        // hashes, percent signs, and other URL-reserved characters are excluded.
        @NotBlank @Size(max = 120) @Pattern(regexp = "^[A-Za-z0-9 _.\\-'@]+$", message = "tester name contains unsupported characters") String testerName) {}
