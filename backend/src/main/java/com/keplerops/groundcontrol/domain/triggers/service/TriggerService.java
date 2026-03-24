package com.keplerops.groundcontrol.domain.triggers.service;

import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.triggers.model.Trigger;
import com.keplerops.groundcontrol.domain.triggers.repository.TriggerRepository;
import com.keplerops.groundcontrol.domain.workflows.repository.WorkflowRepository;
import com.keplerops.groundcontrol.domain.workflows.state.TriggerType;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TriggerService {

    private final TriggerRepository triggerRepository;
    private final WorkflowRepository workflowRepository;

    public TriggerService(TriggerRepository triggerRepository, WorkflowRepository workflowRepository) {
        this.triggerRepository = triggerRepository;
        this.workflowRepository = workflowRepository;
    }

    public Trigger create(UUID workflowId, String name, TriggerType triggerType, String config) {
        var workflow =
                workflowRepository
                        .findById(workflowId)
                        .orElseThrow(() -> new NotFoundException("Workflow not found: " + workflowId));
        var trigger = new Trigger(workflow, name, triggerType);
        if (config != null) trigger.setConfig(config);
        return triggerRepository.save(trigger);
    }

    @Transactional(readOnly = true)
    public Trigger getById(UUID id) {
        return triggerRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Trigger not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Trigger> listByWorkflow(UUID workflowId) {
        return triggerRepository.findByWorkflowId(workflowId);
    }

    @Transactional(readOnly = true)
    public List<Trigger> listEnabled() {
        return triggerRepository.findByEnabledTrue();
    }

    @Transactional(readOnly = true)
    public List<Trigger> listCronTriggers() {
        return triggerRepository.findByTriggerTypeAndEnabledTrue(TriggerType.CRON);
    }

    public Trigger update(UUID id, String name, String config) {
        var trigger = getById(id);
        if (name != null) trigger.setName(name);
        if (config != null) trigger.setConfig(config);
        return triggerRepository.save(trigger);
    }

    public Trigger toggle(UUID id) {
        var trigger = getById(id);
        trigger.setEnabled(!trigger.isEnabled());
        return triggerRepository.save(trigger);
    }

    public void delete(UUID id) {
        var trigger = getById(id);
        triggerRepository.delete(trigger);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Trigger> findWebhookByToken(String token) {
        var webhookTriggers = triggerRepository.findByTriggerTypeAndEnabledTrue(TriggerType.WEBHOOK);
        return webhookTriggers.stream().filter(t -> t.getConfig().contains(token)).findFirst();
    }

    public Trigger recordFired(UUID id) {
        var trigger = getById(id);
        trigger.recordFired();
        return triggerRepository.save(trigger);
    }
}
