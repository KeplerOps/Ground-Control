package com.keplerops.groundcontrol.api.triggers;

import com.keplerops.groundcontrol.domain.triggers.service.TriggerService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/triggers")
public class TriggerController {

    private final TriggerService triggerService;

    public TriggerController(TriggerService triggerService) {
        this.triggerService = triggerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TriggerResponse create(@Valid @RequestBody TriggerRequest request) {
        return TriggerResponse.from(
                triggerService.create(request.workflowId(), request.name(), request.triggerType(), request.config()));
    }

    @GetMapping("/{id}")
    public TriggerResponse getById(@PathVariable UUID id) {
        return TriggerResponse.from(triggerService.getById(id));
    }

    @GetMapping
    public List<TriggerResponse> listByWorkflow(@RequestParam UUID workflowId) {
        return triggerService.listByWorkflow(workflowId).stream().map(TriggerResponse::from).toList();
    }

    @PutMapping("/{id}")
    public TriggerResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTriggerRequest request) {
        return TriggerResponse.from(triggerService.update(id, request.name(), request.config()));
    }

    @PostMapping("/{id}/toggle")
    public TriggerResponse toggle(@PathVariable UUID id) {
        return TriggerResponse.from(triggerService.toggle(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        triggerService.delete(id);
    }
}
