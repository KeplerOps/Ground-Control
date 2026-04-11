package com.keplerops.groundcontrol.domain.controlpacks.repository;

import com.keplerops.groundcontrol.domain.controlpacks.model.ControlPack;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ControlPackRepository extends JpaRepository<ControlPack, UUID> {

    Optional<ControlPack> findByProjectIdAndPackId(UUID projectId, String packId);

    List<ControlPack> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    boolean existsByProjectIdAndPackId(UUID projectId, String packId);
}
