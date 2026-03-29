package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.requirements.service.CsvUtils;
import org.junit.jupiter.api.Test;

class CsvUtilsTest {

    @Test
    void escapeCsv_null_returnsEmpty() {
        assertThat(CsvUtils.escapeCsv(null)).isEqualTo("");
    }

    @Test
    void escapeCsv_plainValue_returnsUnchanged() {
        assertThat(CsvUtils.escapeCsv("hello")).isEqualTo("hello");
    }

    @Test
    void escapeCsv_commaInValue_isQuoted() {
        assertThat(CsvUtils.escapeCsv("a,b")).isEqualTo("\"a,b\"");
    }

    @Test
    void escapeCsv_quoteInValue_isDoubled() {
        assertThat(CsvUtils.escapeCsv("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
    }

    @Test
    void escapeCsv_formulaInjectionEquals_isPrefixed() {
        assertThat(CsvUtils.escapeCsv("=CMD()")).startsWith("'=");
    }

    @Test
    void escapeCsv_formulaInjectionPlus_isPrefixed() {
        assertThat(CsvUtils.escapeCsv("+1")).startsWith("'+");
    }

    @Test
    void escapeCsv_formulaInjectionAt_isPrefixed() {
        assertThat(CsvUtils.escapeCsv("@SUM")).startsWith("'@");
    }

    @Test
    void escapeCsv_newlineInValue_isQuoted() {
        assertThat(CsvUtils.escapeCsv("line1\nline2")).isEqualTo("\"line1\nline2\"");
    }
}
