package com.keplerops.groundcontrol.api.riskscenarios;

import com.keplerops.groundcontrol.domain.riskscenarios.service.RiskScenarioLinkService;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkTargetType;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/risk-scenarios/{riskScenarioId}/links")
public class RiskScenarioLinkController {

    private final RiskScenarioLinkService linkService;

    public RiskScenarioLinkController(RiskScenarioLinkService linkService) {
        this.linkService = linkService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RiskScenarioLinkResponse create(
            @PathVariable UUID riskScenarioId, @Valid @RequestBody RiskScenarioLinkRequest request) {
        return RiskScenarioLinkResponse.from(linkService.create(
                riskScenarioId,
                request.targetType(),
                request.targetIdentifier(),
                request.linkType(),
                request.targetUrl(),
                request.targetTitle()));
    }

    @GetMapping
    public List<RiskScenarioLinkResponse> list(
            @PathVariable UUID riskScenarioId, @RequestParam(required = false) RiskScenarioLinkTargetType targetType) {
        return linkService.listByScenario(riskScenarioId, targetType).stream()
                .map(RiskScenarioLinkResponse::from)
                .toList();
    }

    @DeleteMapping("/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID riskScenarioId, @PathVariable UUID linkId) {
        linkService.delete(riskScenarioId, linkId);
    }
}
