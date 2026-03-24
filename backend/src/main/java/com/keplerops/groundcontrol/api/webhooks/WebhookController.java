package com.keplerops.groundcontrol.api.webhooks;

import com.keplerops.groundcontrol.domain.executions.service.ExecutionService;
import com.keplerops.groundcontrol.domain.triggers.service.TriggerService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Incoming webhook endpoint for triggering workflows.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final TriggerService triggerService;
    private final ExecutionService executionService;

    public WebhookController(TriggerService triggerService, ExecutionService executionService) {
        this.triggerService = triggerService;
        this.executionService = executionService;
    }

    @PostMapping("/{token}")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @PathVariable String token, @RequestBody(required = false) String body) {
        var matchedTrigger = triggerService.findWebhookByToken(token);
        if (matchedTrigger.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No webhook trigger found"));
        }

        var trigger = matchedTrigger.get();
        triggerService.recordFired(trigger.getId());

        var execution = executionService.createAndExecute(
                trigger.getWorkflow().getId(), "WEBHOOK", token, body != null ? body : "{}");

        return ResponseEntity.accepted()
                .body(Map.of("executionId", execution.getId().toString(), "status", execution.getStatus().name()));
    }
}
