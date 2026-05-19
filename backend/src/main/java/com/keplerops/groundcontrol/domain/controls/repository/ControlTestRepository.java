package com.keplerops.groundcontrol.domain.controls.repository;

import com.keplerops.groundcontrol.domain.controls.model.ControlTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ControlTestRepository extends JpaRepository<ControlTest, UUID> {

    boolean existsByProjectIdAndUid(UUID projectId, String uid);

    Optional<ControlTest> findByIdAndProjectId(UUID id, UUID projectId);

    List<ControlTest> findByProjectIdOrderByTestDateDesc(UUID projectId);

    List<ControlTest> findByProjectIdAndControlIdOrderByTestDateDesc(UUID projectId, UUID controlId);

    /**
     * Project-wide tests filtered to {@code testDate <= :asOfDate}. Used by
     * historical-as-of analyses so future tests do not leak into the result
     * (GC-L007 finding #2).
     */
    List<ControlTest> findByProjectIdAndTestDateLessThanEqualOrderByTestDateDesc(UUID projectId, LocalDate asOfDate);

    /**
     * Single-control tests filtered to {@code testDate <= :asOfDate}. Mirrors
     * the project-wide variant so the same as-of guarantee applies when a
     * controlId filter is supplied (GC-L007 finding #2).
     */
    List<ControlTest> findByProjectIdAndControlIdAndTestDateLessThanEqualOrderByTestDateDesc(
            UUID projectId, UUID controlId, LocalDate asOfDate);

    long countByProjectIdAndControlId(UUID projectId, UUID controlId);

    Optional<ControlTest> findByIdAndProjectIdAndControlId(UUID id, UUID projectId, UUID controlId);
}
