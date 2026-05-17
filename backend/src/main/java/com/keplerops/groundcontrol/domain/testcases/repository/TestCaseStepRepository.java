package com.keplerops.groundcontrol.domain.testcases.repository;

import com.keplerops.groundcontrol.domain.testcases.model.TestCaseStep;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseStepRepository extends JpaRepository<TestCaseStep, UUID> {

    List<TestCaseStep> findByTestCaseIdOrderByStepNumberAsc(UUID testCaseId);

    Optional<TestCaseStep> findByIdAndTestCaseId(UUID id, UUID testCaseId);

    boolean existsByTestCaseIdAndStepNumber(UUID testCaseId, int stepNumber);

    long deleteAllByTestCaseId(UUID testCaseId);
}
