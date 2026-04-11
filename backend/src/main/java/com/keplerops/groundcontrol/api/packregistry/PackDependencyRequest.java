package com.keplerops.groundcontrol.api.packregistry;

import com.keplerops.groundcontrol.domain.packregistry.model.PackDependency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PackDependencyRequest(
        @NotBlank @Size(max = 200) String packId, @Size(max = 100) String versionConstraint) {

    public PackDependency toDomain() {
        return new PackDependency(packId, versionConstraint);
    }

    public static List<PackDependency> toDomainList(List<PackDependencyRequest> requests) {
        return requests != null
                ? requests.stream().map(PackDependencyRequest::toDomain).toList()
                : null;
    }
}
