package com.keplerops.groundcontrol.domain.testcases.repository;

import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {

    boolean existsByProjectIdAndUid(UUID projectId, String uid);

    boolean existsByIdAndProjectId(UUID id, UUID projectId);

    Optional<TestCase> findByIdAndProjectId(UUID id, UUID projectId);

    Optional<TestCase> findByProjectIdAndUid(UUID projectId, String uid);

    List<TestCase> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
