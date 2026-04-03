package com.keplerops.groundcontrol.domain.riskscenarios.repository;

import com.keplerops.groundcontrol.domain.riskscenarios.model.MethodologyProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MethodologyProfileRepository extends JpaRepository<MethodologyProfile, UUID> {

    boolean existsByProjectIdAndProfileKeyAndVersion(UUID projectId, String profileKey, String version);

    Optional<MethodologyProfile> findByIdAndProjectId(UUID id, UUID projectId);

    Optional<MethodologyProfile> findByProjectIdAndProfileKeyAndVersion(
            UUID projectId, String profileKey, String version);

    List<MethodologyProfile> findByProjectIdOrderByNameAscVersionDesc(UUID projectId);
}
