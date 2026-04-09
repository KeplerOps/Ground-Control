package com.keplerops.groundcontrol.domain.packregistry.repository;

import com.keplerops.groundcontrol.domain.packregistry.model.PackInstallRecord;
import com.keplerops.groundcontrol.domain.packregistry.state.InstallOutcome;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PackInstallRecordRepository extends JpaRepository<PackInstallRecord, UUID> {

    List<PackInstallRecord> findByProjectIdOrderByPerformedAtDesc(UUID projectId);

    List<PackInstallRecord> findByProjectIdAndPackIdOrderByPerformedAtDesc(UUID projectId, String packId);

    List<PackInstallRecord> findByProjectIdAndInstallOutcomeOrderByPerformedAtDesc(
            UUID projectId, InstallOutcome outcome);
}
