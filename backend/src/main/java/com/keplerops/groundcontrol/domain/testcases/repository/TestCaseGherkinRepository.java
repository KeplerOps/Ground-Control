package com.keplerops.groundcontrol.domain.testcases.repository;

import com.keplerops.groundcontrol.domain.testcases.model.TestCaseGherkin;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseGherkinRepository extends JpaRepository<TestCaseGherkin, UUID> {

    Optional<TestCaseGherkin> findByTestCaseId(UUID testCaseId);

    boolean existsByTestCaseId(UUID testCaseId);

    long deleteAllByTestCaseId(UUID testCaseId);
}
