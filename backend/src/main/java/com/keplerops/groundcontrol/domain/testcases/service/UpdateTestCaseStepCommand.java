package com.keplerops.groundcontrol.domain.testcases.service;

public record UpdateTestCaseStepCommand(
        Integer stepNumber, String action, String expectedResult, String actualResult, boolean clearActualResult) {}
