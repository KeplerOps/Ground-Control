package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.service.CreateObservationCommand;
import com.keplerops.groundcontrol.domain.assets.service.ObservationService;
import com.keplerops.groundcontrol.domain.assets.service.UpdateObservationCommand;
import com.keplerops.groundcontrol.domain.assets.state.ObservationCategory;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assets/{assetId}/observations")
public class ObservationController {

    private final ObservationService observationService;

    public ObservationController(ObservationService observationService) {
        this.observationService = observationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ObservationResponse create(@PathVariable UUID assetId, @Valid @RequestBody ObservationRequest request) {
        var command = new CreateObservationCommand(
                request.category(),
                request.observationKey(),
                request.observationValue(),
                request.source(),
                request.observedAt(),
                request.expiresAt(),
                request.confidence(),
                request.evidenceRef());
        return ObservationResponse.from(observationService.create(assetId, command));
    }

    @GetMapping
    public List<ObservationResponse> list(
            @PathVariable UUID assetId,
            @RequestParam(required = false) ObservationCategory category,
            @RequestParam(required = false) String key) {
        return observationService.listByAsset(assetId, category, key).stream()
                .map(ObservationResponse::from)
                .toList();
    }

    @GetMapping("/{observationId}")
    public ObservationResponse getById(@PathVariable UUID assetId, @PathVariable UUID observationId) {
        return ObservationResponse.from(observationService.getById(assetId, observationId));
    }

    @PutMapping("/{observationId}")
    public ObservationResponse update(
            @PathVariable UUID assetId,
            @PathVariable UUID observationId,
            @Valid @RequestBody UpdateObservationRequest request) {
        var command = new UpdateObservationCommand(
                request.observationValue(), request.expiresAt(), request.confidence(), request.evidenceRef());
        return ObservationResponse.from(observationService.update(assetId, observationId, command));
    }

    @DeleteMapping("/{observationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID assetId, @PathVariable UUID observationId) {
        observationService.delete(assetId, observationId);
    }

    @GetMapping("/latest")
    public List<ObservationResponse> listLatest(@PathVariable UUID assetId) {
        return observationService.listLatest(assetId).stream()
                .map(ObservationResponse::from)
                .toList();
    }
}
