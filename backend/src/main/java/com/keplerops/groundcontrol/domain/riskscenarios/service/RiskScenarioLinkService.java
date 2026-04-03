package com.keplerops.groundcontrol.domain.riskscenarios.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
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

    public RiskScenarioLinkService(
            RiskScenarioLinkRepository linkRepository, RiskScenarioRepository riskScenarioRepository) {
        this.linkRepository = linkRepository;
        this.riskScenarioRepository = riskScenarioRepository;
    }

    public RiskScenarioLink create(
            UUID riskScenarioId,
            RiskScenarioLinkTargetType targetType,
            String targetIdentifier,
            RiskScenarioLinkType linkType,
            String targetUrl,
            String targetTitle) {
        var scenario = riskScenarioRepository
                .findById(riskScenarioId)
                .orElseThrow(() -> new NotFoundException("Risk scenario not found: " + riskScenarioId));

        if (linkRepository.existsByRiskScenarioIdAndTargetTypeAndTargetIdentifierAndLinkType(
                riskScenarioId, targetType, targetIdentifier, linkType)) {
            throw new ConflictException("Link already exists: " + targetType + ":" + targetIdentifier + " (" + linkType
                    + ") on scenario " + scenario.getUid());
        }

        var link = new RiskScenarioLink(scenario, targetType, targetIdentifier, linkType);
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
                targetIdentifier,
                linkType,
                saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<RiskScenarioLink> listByScenario(UUID riskScenarioId, RiskScenarioLinkTargetType targetType) {
        verifyScenarioExists(riskScenarioId);
        if (targetType != null) {
            return linkRepository.findByRiskScenarioIdAndTargetType(riskScenarioId, targetType);
        }
        return linkRepository.findByRiskScenarioId(riskScenarioId);
    }

    public void delete(UUID riskScenarioId, UUID linkId) {
        var link = linkRepository
                .findById(linkId)
                .orElseThrow(() -> new NotFoundException("Risk scenario link not found: " + linkId));
        if (!link.getRiskScenario().getId().equals(riskScenarioId)) {
            throw new NotFoundException("Link " + linkId + " does not belong to risk scenario " + riskScenarioId);
        }
        linkRepository.delete(link);
        log.info("risk_scenario_link_deleted: id={} scenario={}", linkId, riskScenarioId);
    }

    private void verifyScenarioExists(UUID riskScenarioId) {
        if (!riskScenarioRepository.existsById(riskScenarioId)) {
            throw new NotFoundException("Risk scenario not found: " + riskScenarioId);
        }
    }
}
