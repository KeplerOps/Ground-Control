package com.keplerops.groundcontrol.domain.testcases.repository;

import com.keplerops.groundcontrol.domain.testcases.model.TestSuiteSourceRequirement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestSuiteSourceRequirementRepository extends JpaRepository<TestSuiteSourceRequirement, UUID> {

    @Query("SELECT s FROM TestSuiteSourceRequirement s JOIN FETCH s.requirement r "
            + "WHERE s.testSuite.id = :suiteId ORDER BY r.uid, s.id")
    List<TestSuiteSourceRequirement> findByTestSuiteIdOrderByRequirementUid(@Param("suiteId") UUID suiteId);

    boolean existsByTestSuiteIdAndRequirementId(UUID testSuiteId, UUID requirementId);

    Optional<TestSuiteSourceRequirement> findByTestSuiteIdAndRequirementId(UUID testSuiteId, UUID requirementId);

    /**
     * Bare-rows variant used by the service's delete cascade — see the
     * matching note on {@link TestSuiteMemberRepository#findByTestSuiteId}.
     */
    List<TestSuiteSourceRequirement> findByTestSuiteId(UUID testSuiteId);
}
