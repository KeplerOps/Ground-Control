package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import org.junit.jupiter.api.Test;

class TestCaseFormatTest {

    @Test
    void enumExposesOnlyStepBasedAndGherkin() {
        assertThat(TestCaseFormat.values()).containsExactly(TestCaseFormat.STEP_BASED, TestCaseFormat.GHERKIN);
    }
}
