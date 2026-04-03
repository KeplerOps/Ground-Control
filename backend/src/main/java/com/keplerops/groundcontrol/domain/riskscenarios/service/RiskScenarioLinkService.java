package com.keplerops.groundcontrol.domain.riskscenarios.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.graph.service.GraphTargetResolverService;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenarioLink;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioLinkRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkTargetType;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkType;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RiskScenarioLinkService {

    private static final Logger log = LoggerFactory.getLogger(RiskScenarioLinkService.class);

    private final RiskScenarioLinkRepository linkRepository;
    private final RiskScenarioRepository riskScenarioRepository;
    private final GraphTargetResolverService graphTargetResolverService;

    public RiskScenarioLinkService(
            RiskScenarioLinkRepository linkRepository,
            RiskScenarioRepository riskScenarioRepository,
            GraphTargetResolverService graphTargetResolverService) {
        this.linkRepository = linkRepository;
        this.riskScenarioRepository = riskScenarioRepository;
        this.graphTargetResolverService = graphTargetResolverService;
    }

    public RiskScenarioLink create(
            UUID projectId,
            UUID riskScenarioId,
            RiskScenarioLinkTargetType targetType,
            UUID targetEntityId,
            String targetIdentifier,
            RiskScenarioLinkType linkType,
            String targetUrl,
            String targetTitle) {
        var scenario = riskScenarioRepository
                .findByIdAndProjectId(riskScenarioId, projectId)
                .orElseThrow(() -> new NotFoundException("Risk scenario not found: " + riskScenarioId));
        var target = graphTargetResolverService.validateRiskScenarioTarget(
                projectId, targetType, targetEntityId, targetIdentifier);

        boolean exists = target.internal()
                ? linkRepository.existsByRiskScenarioIdAndTargetTypeAndTargetEntityIdAndLinkType(
                        riskScenarioId, targetType, target.targetEntityId(), linkType)
                : linkRepository.existsByRiskScenarioIdAndTargetTypeAndTargetIdentifierAndLinkType(
                        riskScenarioId, targetType, target.targetIdentifier(), linkType);
        if (exists) {
            throw new ConflictException("Link already exists: " + targetType + ":"
                    + (target.internal() ? target.targetEntityId() : target.targetIdentifier()) + " (" + linkType
                    + ") on scenario " + scenario.getUid());
        }

        var link = new RiskScenarioLink(
                scenario, targetType, target.targetEntityId(), target.targetIdentifier(), linkType);
        if (targetUrl != null) {
            link.setTargetUrl(targetUrl);
        }
        if (targetTitle != null) {
            link.setTargetTitle(targetTitle);
        }

        var saved = linkRepository.save(link);
        log.info(
                "risk_scenario_link_created: scenario={} target_type={} target={} link_type={} id={}",
                scenario.getUid(),
                targetType,
                target.internal() ? target.targetEntityId() : target.targetIdentifier(),
                linkType,
                saved.getId());
        return saved;
    }

    @Deprecated(forRemoval = false)
    public RiskScenarioLink create(
            UUID riskScenarioId,
            RiskScenarioLinkTargetType targetType,
            UUID targetEntityId,
            String targetIdentifier,
            RiskScenarioLinkType linkType,
            String targetUrl,
            String targetTitle) {
        var scenario = riskScenarioRepository
                .findById(riskScenarioId)
                .orElseThrow(() -> new NotFoundException("Risk scenario not found: " + riskScenarioId));
        var target = graphTargetResolverService.validateRiskScenarioTarget(
                scenario.getProject().getId(), targetType, targetEntityId, targetIdentifier);

        boolean exists = target.internal()
                ? linkRepository.existsByRiskScenarioIdAndTargetTypeAndTargetEntityIdAndLinkType(
                        riskScenarioId, targetType, target.targetEntityId(), linkType)
                : linkRepository.existsByRiskScenarioIdAndTargetTypeAndTargetIdentifierAndLinkType(
                        riskScenarioId, targetType, target.targetIdentifier(), linkType);
        if (exists) {
            throw new ConflictException("Link already exists: " + targetType + ":"
                    + (target.internal() ? target.targetEntityId() : target.targetIdentifier()) + " (" + linkType
                    + ") on scenario " + scenario.getUid());
        }

        var link = new RiskScenarioLink(
                scenario, targetType, target.targetEntityId(), target.targetIdentifier(), linkType);
        if (targetUrl != null) {
            link.setTargetUrl(targetUrl);
        }
        if (targetTitle != null) {
            link.setTargetTitle(targetTitle);
        }
        return linkRepository.save(link);
    }

    @Transactional(readOnly = true)
    public List<RiskScenarioLink> listByScenario(
            UUID projectId, UUID riskScenarioId, RiskScenarioLinkTargetType targetType) {
        verifyScenarioExists(projectId, riskScenarioId);
        if (targetType != null) {
            return linkRepository.findByRiskScenarioIdAndTargetType(riskScenarioId, targetType);
        }
        return linkRepository.findByRiskScenarioId(riskScenarioId);
    }

    @Deprecated(forRemoval = false)
    @Transactional(readOnly = true)
    public List<RiskScenarioLink> listByScenario(UUID riskScenarioId, RiskScenarioLinkTargetType targetType) {
        if (!riskScenarioRepository.existsById(riskScenarioId)) {
            throw new NotFoundException("Risk scenario not found: " + riskScenarioId);
        }
        if (targetType != null) {
            return linkRepository.findByRiskScenarioIdAndTargetType(riskScenarioId, targetType);
        }
        return linkRepository.findByRiskScenarioId(riskScenarioId);
    }

    public void delete(UUID projectId, UUID riskScenarioId, UUID linkId) {
        var link = linkRepository
                .findByIdAndRiskScenarioProjectId(linkId, projectId)
                .orElseThrow(() -> new NotFoundException("Risk scenario link not found: " + linkId));
        if (!link.getRiskScenario().getId().equals(riskScenarioId)) {
            throw new NotFoundException("Link " + linkId + " does not belong to risk scenario " + riskScenarioId);
        }
        linkRepository.delete(link);
        log.info("risk_scenario_link_deleted: id={} scenario={}", linkId, riskScenarioId);
    }

    @Deprecated(forRemoval = false)
    public void delete(UUID riskScenarioId, UUID linkId) {
        var link = linkRepository
                .findById(linkId)
                .orElseThrow(() -> new NotFoundException("Risk scenario link not found: " + linkId));
        if (!link.getRiskScenario().getId().equals(riskScenarioId)) {
            throw new NotFoundException("Link " + linkId + " does not belong to risk scenario " + riskScenarioId);
        }
        linkRepository.delete(link);
    }

    private void verifyScenarioExists(UUID projectId, UUID riskScenarioId) {
        if (!riskScenarioRepository.existsByIdAndProjectId(riskScenarioId, projectId)) {
            throw new NotFoundException("Risk scenario not found: " + riskScenarioId);
        }
    }
}
