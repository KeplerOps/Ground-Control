package com.keplerops.groundcontrol.domain.packregistry.repository;

import com.keplerops.groundcontrol.domain.packregistry.model.PackRegistryEntry;
import com.keplerops.groundcontrol.domain.packregistry.state.CatalogStatus;
import com.keplerops.groundcontrol.domain.packregistry.state.PackType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PackRegistryEntryRepository extends JpaRepository<PackRegistryEntry, UUID> {

    Optional<PackRegistryEntry> findByProjectIdAndPackIdAndVersion(UUID projectId, String packId, String version);

    List<PackRegistryEntry> findByProjectIdAndPackIdOrderByRegisteredAtDesc(UUID projectId, String packId);

    List<PackRegistryEntry> findByProjectIdOrderByRegisteredAtDesc(UUID projectId);

    List<PackRegistryEntry> findByProjectIdAndPackTypeOrderByRegisteredAtDesc(UUID projectId, PackType packType);

    List<PackRegistryEntry> findByProjectIdAndCatalogStatusOrderByRegisteredAtDesc(
            UUID projectId, CatalogStatus status);

    List<PackRegistryEntry> findByProjectIdAndPackIdAndCatalogStatusOrderByRegisteredAtDesc(
            UUID projectId, String packId, CatalogStatus status);

    boolean existsByProjectIdAndPackIdAndVersion(UUID projectId, String packId, String version);
}
