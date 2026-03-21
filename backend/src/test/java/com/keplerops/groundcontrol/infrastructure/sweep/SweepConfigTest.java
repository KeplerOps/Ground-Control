package com.keplerops.groundcontrol.infrastructure.sweep;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SweepConfigTest {

    @Test
    void createsSweepPropertiesFromValues() {
        var config = new SweepConfig();
        var props = config.sweepProperties(true, "0 0 6 * * *", true, "owner/repo", "label1,label2", false, "");

        assertThat(props.enabled()).isTrue();
        assertThat(props.cron()).isEqualTo("0 0 6 * * *");
        assertThat(props.github().enabled()).isTrue();
        assertThat(props.github().repo()).isEqualTo("owner/repo");
        assertThat(props.github().labels()).containsExactly("label1", "label2");
        assertThat(props.webhook().enabled()).isFalse();
    }

    @Test
    void handlesBlankLabels() {
        var config = new SweepConfig();
        var props = config.sweepProperties(true, "0 0 6 * * *", false, "", "", false, "");

        assertThat(props.github().labels()).isEmpty();
    }
}
