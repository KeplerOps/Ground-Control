package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.packregistry.model.TrustPolicy;
import com.keplerops.groundcontrol.domain.packregistry.model.TrustPolicyRule;
import com.keplerops.groundcontrol.domain.packregistry.repository.TrustPolicyRepository;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustPolicyField;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustPolicyRuleOperator;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TrustPolicyService {

    private static final Logger log = LoggerFactory.getLogger(TrustPolicyService.class);

    private final TrustPolicyRepository trustPolicyRepository;
    private final ProjectService projectService;

    public TrustPolicyService(TrustPolicyRepository trustPolicyRepository, ProjectService projectService) {
        this.trustPolicyRepository = trustPolicyRepository;
        this.projectService = projectService;
    }

    public TrustPolicy create(CreateTrustPolicyCommand command) {
        var project = projectService.getById(command.projectId());

        if (trustPolicyRepository.existsByProjectIdAndName(command.projectId(), command.name())) {
            throw new ConflictException(
                    String.format("Trust policy '%s' already exists in this project", command.name()));
        }

        validateRules(command.rules());

        var policy = new TrustPolicy(project, command.name(), command.defaultOutcome());
        policy.setDescription(command.description());
        policy.setRules(command.rules());
        policy.setPriority(command.priority());
        policy.setEnabled(command.enabled());

        var saved = trustPolicyRepository.save(policy);
        log.info("trust_policy_created: name={}, priority={}", saved.getName(), saved.getPriority());
        return saved;
    }

    public TrustPolicy update(UUID id, UpdateTrustPolicyCommand command) {
        var policy = get(id);

        if (command.name() != null && !command.name().equals(policy.getName())) {
            if (trustPolicyRepository.existsByProjectIdAndName(
                    policy.getProject().getId(), command.name())) {
                throw new ConflictException(
                        String.format("Trust policy '%s' already exists in this project", command.name()));
            }
            policy.setName(command.name());
        }
        if (command.description() != null) policy.setDescription(command.description());
        if (command.defaultOutcome() != null) policy.setDefaultOutcome(command.defaultOutcome());
        if (command.rules() != null) {
            validateRules(command.rules());
            policy.setRules(command.rules());
        }
        if (command.priority() != null) policy.setPriority(command.priority());
        if (command.enabled() != null) policy.setEnabled(command.enabled());

        var saved = trustPolicyRepository.save(policy);
        log.info("trust_policy_updated: id={}, name={}", id, policy.getName());
        return saved;
    }

    @Transactional(readOnly = true)
    public TrustPolicy get(UUID id) {
        return trustPolicyRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Trust policy not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<TrustPolicy> list(UUID projectId) {
        return trustPolicyRepository.findByProjectIdOrderByPriorityAsc(projectId);
    }

    public void delete(UUID id) {
        if (!trustPolicyRepository.existsById(id)) {
            throw new NotFoundException("Trust policy not found: " + id);
        }
        trustPolicyRepository.deleteById(id);
        log.info("trust_policy_deleted: id={}", id);
    }

    private void validateRules(List<TrustPolicyRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return;
        }

        Map<String, String> violations = new LinkedHashMap<>();
        for (int index = 0; index < rules.size(); index++) {
            var rule = rules.get(index);
            if (rule.operator() == TrustPolicyRuleOperator.MATCHES_PATTERN) {
                violations.put("rules[" + index + "].operator", "MATCHES_PATTERN is disabled for security reasons");
            }
            if (rule.field() == TrustPolicyField.SIGNATURE_VERIFIED) {
                violations.put(
                        "rules[" + index + "].field",
                        "signatureVerified is informational only; use signerTrusted for trust policy");
            }
        }

        if (!violations.isEmpty()) {
            throw new DomainValidationException(
                    "Trust policy contains unsupported rule fields or operators", "validation_error", violations);
        }
    }
}
