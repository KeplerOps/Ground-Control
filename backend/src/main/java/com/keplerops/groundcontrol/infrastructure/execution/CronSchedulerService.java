package com.keplerops.groundcontrol.infrastructure.execution;

import com.keplerops.groundcontrol.domain.executions.service.ExecutionService;
import com.keplerops.groundcontrol.domain.triggers.model.Trigger;
import com.keplerops.groundcontrol.domain.triggers.service.TriggerService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls cron triggers and fires executions when due.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "groundcontrol.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class CronSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(CronSchedulerService.class);

    private final TriggerService triggerService;
    private final ExecutionService executionService;

    public CronSchedulerService(TriggerService triggerService, ExecutionService executionService) {
        this.triggerService = triggerService;
        this.executionService = executionService;
    }

    @Scheduled(fixedDelayString = "${groundcontrol.scheduler.poll-interval-ms:60000}")
    public void pollCronTriggers() {
        List<Trigger> cronTriggers = triggerService.listCronTriggers();
        for (Trigger trigger : cronTriggers) {
            try {
                log.debug("Evaluating cron trigger: {} for workflow {}", trigger.getName(),
                        trigger.getWorkflow().getName());

                triggerService.recordFired(trigger.getId());

                executionService.createAndExecute(
                        trigger.getWorkflow().getId(), "CRON", trigger.getId().toString(), "{}");

                log.info("Fired cron trigger {} for workflow {}",
                        trigger.getName(), trigger.getWorkflow().getName());
            } catch (Exception e) {
                log.error("Failed to evaluate cron trigger {}: {}", trigger.getName(), e.getMessage());
            }
        }
    }
}
