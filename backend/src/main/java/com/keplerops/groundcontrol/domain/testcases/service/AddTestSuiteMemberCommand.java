package com.keplerops.groundcontrol.domain.testcases.service;

import java.util.UUID;

/**
 * Add a test case to a STATIC {@link
 * com.keplerops.groundcontrol.domain.testcases.model.TestSuite}.
 *
 * <p>{@code position} is optional — null means "append" (the service
 * computes max(position) + 1). A duplicate test_case_id is rejected with
 * a {@code ConflictException}.
 */
public record AddTestSuiteMemberCommand(UUID testCaseId, Integer position) {}
