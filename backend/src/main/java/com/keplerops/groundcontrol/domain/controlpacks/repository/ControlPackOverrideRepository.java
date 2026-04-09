package com.keplerops.groundcontrol.domain.controlpacks.repository;

import com.keplerops.groundcontrol.domain.controlpacks.model.ControlPackOverride;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ControlPackOverrideRepository extends JpaRepository<ControlPackOverride, UUID> {

    List<ControlPackOverride> findByControlPackEntryId(UUID entryId);

    Optional<ControlPackOverride> findByControlPackEntryIdAndFieldName(UUID entryId, String fieldName);
}
