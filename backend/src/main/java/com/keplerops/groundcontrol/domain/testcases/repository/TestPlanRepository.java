package com.keplerops.groundcontrol.domain.testcases.repository;

import com.keplerops.groundcontrol.domain.testcases.model.TestPlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestPlanRepository extends JpaRepository<TestPlan, UUID> {

    Optional<TestPlan> findByIdAndProjectId(UUID id, UUID projectId);

    Optional<TestPlan> findByProjectIdAndUid(UUID projectId, String uid);

    boolean existsByProjectIdAndUid(UUID projectId, String uid);

    @Query("SELECT p FROM TestPlan p WHERE p.project.id = :projectId ORDER BY p.createdAt DESC, p.id")
    List<TestPlan> findByProjectIdOrderByCreatedAtDesc(@Param("projectId") UUID projectId);
}
