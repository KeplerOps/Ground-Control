package com.keplerops.groundcontrol.domain.triggers.repository;

import com.keplerops.groundcontrol.domain.triggers.model.Trigger;
import com.keplerops.groundcontrol.domain.workflows.state.TriggerType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TriggerRepository extends JpaRepository<Trigger, UUID> {
    List<Trigger> findByWorkflowId(UUID workflowId);
    List<Trigger> findByTriggerTypeAndEnabledTrue(TriggerType triggerType);
    List<Trigger> findByEnabledTrue();
}
