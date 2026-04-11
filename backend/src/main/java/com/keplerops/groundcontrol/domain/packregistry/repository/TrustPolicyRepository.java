package com.keplerops.groundcontrol.domain.packregistry.repository;

import com.keplerops.groundcontrol.domain.packregistry.model.TrustPolicy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrustPolicyRepository extends JpaRepository<TrustPolicy, UUID> {

    List<TrustPolicy> findByProjectIdAndEnabledOrderByPriorityAsc(UUID projectId, boolean enabled);

    List<TrustPolicy> findByProjectIdOrderByPriorityAsc(UUID projectId);

    Optional<TrustPolicy> findByProjectIdAndName(UUID projectId, String name);

    boolean existsByProjectIdAndName(UUID projectId, String name);
}
