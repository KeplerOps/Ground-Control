package com.keplerops.groundcontrol.infrastructure.sweep;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.requirements.service.AnalysisSweepService;
import com.keplerops.groundcontrol.domain.requirements.service.CompletenessResult;
import com.keplerops.groundcontrol.domain.requirements.service.SweepReport;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduledSweepRunnerTest {

    @Mock
    private AnalysisSweepService analysisSweepService;

    @InjectMocks
    private ScheduledSweepRunner runner;

    @Test
    void runScheduledSweepDelegatesToService() {
        var report = new SweepReport(
                "test-project",
                Instant.now(),
                List.of(),
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                new CompletenessResult(0, Map.of(), List.of()),
                null);
        when(analysisSweepService.sweepAll()).thenReturn(List.of(report));

        runner.runScheduledSweep();

        verify(analysisSweepService).sweepAll();
    }
}
