package com.keplerops.groundcontrol.domain.baselines.repository;

import com.keplerops.groundcontrol.domain.baselines.model.Baseline;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaselineRepository extends JpaRepository<Baseline, UUID> {

    List<Baseline> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    boolean existsByProjectIdAndName(UUID projectId, String name);
}
