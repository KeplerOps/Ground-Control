package com.keplerops.groundcontrol.domain.testcases.repository;

import com.keplerops.groundcontrol.domain.testcases.model.TestRunTesterAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestRunTesterAssignmentRepository extends JpaRepository<TestRunTesterAssignment, UUID> {

    @Query("SELECT a FROM TestRunTesterAssignment a WHERE a.testRun.id = :runId ORDER BY a.testerName, a.id")
    List<TestRunTesterAssignment> findByTestRunIdOrderByTesterName(@Param("runId") UUID runId);

    boolean existsByTestRunIdAndTesterName(UUID testRunId, String testerName);

    Optional<TestRunTesterAssignment> findByTestRunIdAndTesterName(UUID testRunId, String testerName);

    List<TestRunTesterAssignment> findByTestRunId(UUID testRunId);
}
