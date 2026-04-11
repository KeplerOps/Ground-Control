package com.keplerops.groundcontrol.domain.controlpacks.repository;

import com.keplerops.groundcontrol.domain.controlpacks.model.ControlPackEntry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ControlPackEntryRepository extends JpaRepository<ControlPackEntry, UUID> {

    List<ControlPackEntry> findByControlPackId(UUID controlPackId);

    Optional<ControlPackEntry> findByControlPackIdAndEntryUid(UUID controlPackId, String entryUid);

    Optional<ControlPackEntry> findByControlPackIdAndControlId(UUID controlPackId, UUID controlId);

    List<ControlPackEntry> findByControlId(UUID controlId);

    boolean existsByControlPackIdAndEntryUid(UUID controlPackId, String entryUid);
}
