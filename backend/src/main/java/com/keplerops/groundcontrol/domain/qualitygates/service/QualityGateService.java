package com.keplerops.groundcontrol.domain.qualitygates.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.qualitygates.model.QualityGate;
import com.keplerops.groundcontrol.domain.qualitygates.repository.QualityGateRepository;
import com.keplerops.groundcontrol.domain.qualitygates.state.MetricType;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.service.AnalysisService;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QualityGateService {

    private static final Logger log = LoggerFactory.getLogger(QualityGateService.class);

    private final QualityGateRepository qualityGateRepository;
    private final ProjectService projectService;
    private final RequirementRepository requirementRepository;
    private final AnalysisService analysisService;

    public QualityGateService(
            QualityGateRepository qualityGateRepository,
            ProjectService projectService,
            RequirementRepository requirementRepository,
            AnalysisService analysisService) {
        this.qualityGateRepository = qualityGateRepository;
        this.projectService = projectService;
        this.requirementRepository = requirementRepository;
        this.analysisService = analysisService;
    }

    public QualityGate create(CreateQualityGateCommand command) {
        var project = projectService.getById(command.projectId());

        if (qualityGateRepository.existsByProjectIdAndName(project.getId(), command.name())) {
            throw new ConflictException("Quality gate with name '" + command.name() + "' already exists in project "
                    + project.getIdentifier());
        }

        validateMetricParam(command.metricType(), command.metricParam());

        var gate = new QualityGate(
                project,
                command.name(),
                command.description(),
                command.metricType(),
                command.metricParam(),
                command.scopeStatus(),
                command.operator(),
                command.threshold());

        var saved = qualityGateRepository.save(gate);
        log.info(
                "quality_gate_created: project={} name={} metric={} id={}",
                project.getIdentifier(),
                saved.getName(),
                saved.getMetricType(),
                saved.getId());
        return saved;
    }

    public QualityGate update(UUID id, UpdateQualityGateCommand command) {
        var gate = qualityGateRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Quality gate not found: " + id));

        if (command.name() != null) {
            if (!command.name().equals(gate.getName())
                    && qualityGateRepository.existsByProjectIdAndName(
                            gate.getProject().getId(), command.name())) {
                throw new ConflictException("Quality gate with name '" + command.name() + "' already exists in project "
                        + gate.getProject().getIdentifier());
            }
            gate.setName(command.name());
        }
        if (command.description() != null) {
            gate.setDescription(command.description().orElse(null));
        }
        if (command.metricType() != null) {
            gate.setMetricType(command.metricType());
        }
        if (command.metricParam() != null) {
            gate.setMetricParam(command.metricParam().orElse(null));
        }
        if (command.scopeStatus() != null) {
            gate.setScopeStatus(command.scopeStatus().orElse(null));
        }
        if (command.operator() != null) {
            gate.setOperator(command.operator());
        }
        if (command.threshold() != null) {
            gate.setThreshold(command.threshold());
        }
        if (command.enabled() != null) {
            gate.setEnabled(command.enabled());
        }

        validateMetricParam(gate.getMetricType(), gate.getMetricParam());

        var saved = qualityGateRepository.save(gate);
        log.info("quality_gate_updated: id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional(readOnly = true)
    public QualityGate getById(UUID id) {
        return qualityGateRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Quality gate not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<QualityGate> listByProject(UUID projectId) {
        return qualityGateRepository.findByProjectIdOrderByNameAsc(projectId);
    }

    public void delete(UUID id) {
        var gate = qualityGateRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Quality gate not found: " + id));
        qualityGateRepository.delete(gate);
        log.info("quality_gate_deleted: id={} name={}", gate.getId(), gate.getName());
    }

    @Transactional(readOnly = true)
    public QualityGateEvaluationResult evaluate(String projectIdentifierParam) {
        var projectId = projectService.resolveProjectId(projectIdentifierParam);
        var project = projectService.getById(projectId);
        var projectIdentifier = project.getIdentifier();
        var gates = qualityGateRepository.findByProjectIdAndEnabledTrueOrderByNameAsc(projectId);

        List<QualityGateResult> results = new ArrayList<>();
        for (QualityGate gate : gates) {
            double actualValue = computeMetric(projectId, gate);
            boolean passed = gate.getOperator().evaluate(actualValue, gate.getThreshold());
            results.add(new QualityGateResult(
                    gate.getId(),
                    gate.getName(),
                    gate.getMetricType().name(),
                    gate.getMetricParam(),
                    gate.getScopeStatus() != null ? gate.getScopeStatus().name() : null,
                    gate.getOperator().name(),
                    gate.getThreshold(),
                    actualValue,
                    passed));
        }

        int passedCount =
                (int) results.stream().filter(QualityGateResult::passed).count();
        int failedCount = results.size() - passedCount;
        boolean allPassed = failedCount == 0;

        log.info(
                "quality_gates_evaluated: project={} total={} passed={} failed={}",
                projectIdentifier,
                results.size(),
                passedCount,
                failedCount);

        return new QualityGateEvaluationResult(
                projectIdentifier, Instant.now(), allPassed, results.size(), passedCount, failedCount, results);
    }

    private double computeMetric(UUID projectId, QualityGate gate) {
        return switch (gate.getMetricType()) {
            case COVERAGE -> computeCoveragePercentage(projectId, gate.getMetricParam(), gate.getScopeStatus());
            case ORPHAN_COUNT -> computeOrphanCount(projectId, gate.getScopeStatus());
            case COMPLETENESS -> computeCompletenessIssueCount(projectId);
        };
    }

    private double computeCoveragePercentage(UUID projectId, String metricParam, Status scopeStatus) {
        LinkType linkType = LinkType.valueOf(metricParam);
        long total = requirementRepository.countByScope(projectId, scopeStatus);
        if (total == 0) {
            return 100.0;
        }
        long covered = requirementRepository.countCoveredByLinkType(projectId, linkType, scopeStatus);
        return Math.round(covered * 1000.0 / total) / 10.0;
    }

    private double computeOrphanCount(UUID projectId, Status scopeStatus) {
        var orphans = analysisService.findOrphans(projectId);
        if (scopeStatus != null) {
            orphans = orphans.stream().filter(r -> r.getStatus() == scopeStatus).toList();
        }
        return orphans.size();
    }

    private double computeCompletenessIssueCount(UUID projectId) {
        var completeness = analysisService.analyzeCompleteness(projectId);
        return completeness.issues().size();
    }

    private void validateMetricParam(MetricType metricType, String metricParam) {
        if (metricType == MetricType.COVERAGE) {
            if (metricParam == null || metricParam.isBlank()) {
                throw new DomainValidationException(
                        "metric_param is required for COVERAGE metric type",
                        "validation_error",
                        Map.of("field", "metricParam", "metricType", metricType.name()));
            }
            try {
                LinkType.valueOf(metricParam);
            } catch (IllegalArgumentException e) {
                throw new DomainValidationException(
                        "Invalid metric_param for COVERAGE: " + metricParam + ". Must be a valid LinkType.",
                        "validation_error",
                        Map.of("field", "metricParam", "value", metricParam));
            }
        }
    }
}
