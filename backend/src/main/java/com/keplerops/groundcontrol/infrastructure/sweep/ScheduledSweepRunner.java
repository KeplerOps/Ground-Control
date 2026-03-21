package com.keplerops.groundcontrol.infrastructure.sweep;

import com.keplerops.groundcontrol.domain.requirements.service.AnalysisSweepService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "groundcontrol.sweep.enabled", havingValue = "true")
public class ScheduledSweepRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledSweepRunner.class);

    private final AnalysisSweepService analysisSweepService;

    public ScheduledSweepRunner(AnalysisSweepService analysisSweepService) {
        this.analysisSweepService = analysisSweepService;
    }

    // Runs synchronously on the scheduling thread; a long sweep blocks the next scheduled tick.
    @Scheduled(cron = "${groundcontrol.sweep.cron:0 0 6 * * *}")
    public void runScheduledSweep() {
        log.info("scheduled_sweep_triggered");
        var reports = analysisSweepService.sweepAll();
        long withProblems = reports.stream().filter(r -> r.hasProblems()).count();
        log.info("scheduled_sweep_finished: projects={} with_problems={}", reports.size(), withProblems);
    }
}
