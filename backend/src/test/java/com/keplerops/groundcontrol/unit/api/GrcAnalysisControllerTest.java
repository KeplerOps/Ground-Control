package com.keplerops.groundcontrol.unit.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.grcanalysis.GrcAnalysisController;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.grcanalysis.service.EvidenceFreshnessResult;
import com.keplerops.groundcontrol.domain.grcanalysis.service.GrcAnalysisService;
import com.keplerops.groundcontrol.domain.grcanalysis.service.ObservationProjectionMode;
import com.keplerops.groundcontrol.domain.grcanalysis.service.ObservationProjectionResult;
import com.keplerops.groundcontrol.domain.grcanalysis.service.VendorRiskAggregationResult;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(GrcAnalysisController.class)
class GrcAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GrcAnalysisService grcAnalysisService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
    }

    @Nested
    class EvidenceFreshness {

        @Test
        void happyPath_returns200WithStructuredFields() throws Exception {
            var inputs = new EvidenceFreshnessResult.Inputs(
                    "ground-control", Instant.parse("2026-05-18T00:00:00Z"), 90, false, null, null);
            var counts = new EvidenceFreshnessResult.EvidenceFreshnessCounts(2, 1, 0, 0, 3);
            var result = new EvidenceFreshnessResult(
                    "evidence_freshness",
                    "ground-control",
                    Instant.parse("2026-05-18T00:00:00Z"),
                    "evidence-freshness-projection-v1",
                    inputs,
                    List.of(),
                    List.of(),
                    List.of(),
                    counts,
                    List.of("note"));
            when(grcAnalysisService.evidenceFreshness(eq(PROJECT_ID), any(), anyInt(), anyBoolean(), any(), any()))
                    .thenReturn(result);

            mockMvc.perform(get("/api/v1/analysis/grc/evidence-freshness").param("project", "ground-control"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.analysisKind", is("evidence_freshness")))
                    .andExpect(jsonPath("$.project", is("ground-control")))
                    .andExpect(jsonPath("$.derivationMethod", is("evidence-freshness-projection-v1")))
                    .andExpect(jsonPath("$.counts.fresh", is(2)))
                    .andExpect(jsonPath("$.counts.stale", is(1)))
                    .andExpect(jsonPath("$.limitations", hasSize(1)));
        }

        @Test
        void projectNotFound_returns404() throws Exception {
            when(projectService.resolveProjectId(any())).thenThrow(new NotFoundException("Project not found"));

            mockMvc.perform(get("/api/v1/analysis/grc/evidence-freshness").param("project", "missing"))
                    .andExpect(status().isNotFound());
        }

        /** Finding #8: freshnessWindowDays=0 must return 400. */
        @Test
        void zeroFreshnessWindow_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/analysis/grc/evidence-freshness")
                            .param("project", "ground-control")
                            .param("freshnessWindowDays", "0"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void negativeFreshnessWindow_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/analysis/grc/evidence-freshness")
                            .param("project", "ground-control")
                            .param("freshnessWindowDays", "-30"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class ObservationProjection {

        @Test
        void happyPath_returns200WithModeSwitch() throws Exception {
            var inputs = new ObservationProjectionResult.Inputs(
                    "ground-control",
                    Instant.parse("2026-05-18T00:00:00Z"),
                    ObservationProjectionMode.ASSET_EXPOSURE,
                    null,
                    null);
            var result = new ObservationProjectionResult(
                    "observation_exposure",
                    "ground-control",
                    Instant.parse("2026-05-18T00:00:00Z"),
                    "observation-current-state-projection-v1",
                    inputs,
                    List.of(),
                    List.of(),
                    List.of());
            when(grcAnalysisService.observationProjection(
                            eq(PROJECT_ID), any(), eq(ObservationProjectionMode.ASSET_EXPOSURE), any(), any()))
                    .thenReturn(result);

            mockMvc.perform(get("/api/v1/analysis/grc/observation-projection")
                            .param("project", "ground-control")
                            .param("mode", "ASSET_EXPOSURE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.analysisKind", is("observation_exposure")))
                    .andExpect(jsonPath("$.inputs.mode", is("ASSET_EXPOSURE")));
        }

        @Test
        void missingMode_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/analysis/grc/observation-projection").param("project", "ground-control"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void invalidMode_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/analysis/grc/observation-projection")
                            .param("project", "ground-control")
                            .param("mode", "BOGUS"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void projectNotFound_returns404() throws Exception {
            when(projectService.resolveProjectId(any())).thenThrow(new NotFoundException("Project not found"));

            mockMvc.perform(get("/api/v1/analysis/grc/observation-projection")
                            .param("project", "missing")
                            .param("mode", "ASSET_EXPOSURE"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class VendorRisk {

        @Test
        void happyPath_returns200WithThirdPartyLabel() throws Exception {
            var inputs = new VendorRiskAggregationResult.Inputs(
                    "ground-control", Instant.parse("2026-05-18T00:00:00Z"), 90, null);
            var result = new VendorRiskAggregationResult(
                    "vendor_risk_aggregation",
                    "ground-control",
                    Instant.parse("2026-05-18T00:00:00Z"),
                    "vendor-third-party-rollup-v1",
                    inputs,
                    "THIRD_PARTY",
                    List.of(),
                    List.of(
                            "not a first-class vendor aggregate; modeled as OperationalAsset.THIRD_PARTY per GC-L009 carve-out"));
            when(grcAnalysisService.vendorRisk(eq(PROJECT_ID), any(), anyInt(), any()))
                    .thenReturn(result);

            mockMvc.perform(get("/api/v1/analysis/grc/vendor-risk").param("project", "ground-control"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.analysisKind", is("vendor_risk_aggregation")))
                    .andExpect(jsonPath("$.assetType", is("THIRD_PARTY")))
                    .andExpect(jsonPath("$.derivationMethod", is("vendor-third-party-rollup-v1")))
                    .andExpect(jsonPath("$.limitations", hasSize(1)));
        }

        @Test
        void projectNotFound_returns404() throws Exception {
            when(projectService.resolveProjectId(any())).thenThrow(new NotFoundException("Project not found"));

            mockMvc.perform(get("/api/v1/analysis/grc/vendor-risk").param("project", "missing"))
                    .andExpect(status().isNotFound());
        }

        /** Finding #8: freshnessWindowDays=0 must return 400. */
        @Test
        void zeroFreshnessWindow_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/analysis/grc/vendor-risk")
                            .param("project", "ground-control")
                            .param("freshnessWindowDays", "0"))
                    .andExpect(status().isBadRequest());
        }
    }
}
