package com.keplerops.groundcontrol.domain.verification.repository;

import com.keplerops.groundcontrol.domain.verification.model.VerificationResult;
import com.keplerops.groundcontrol.domain.verification.state.VerificationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationResultRepository extends JpaRepository<VerificationResult, UUID> {

    Optional<VerificationResult> findByIdAndProjectId(UUID id, UUID projectId);

    boolean existsByIdAndProjectId(UUID id, UUID projectId);

    List<VerificationResult> findByProjectIdOrderByVerifiedAtDesc(UUID projectId);

    List<VerificationResult> findByProjectIdAndRequirementIdOrderByVerifiedAtDesc(UUID projectId, UUID requirementId);

    List<VerificationResult> findByProjectIdAndProverOrderByVerifiedAtDesc(UUID projectId, String prover);

    List<VerificationResult> findByProjectIdAndResultOrderByVerifiedAtDesc(UUID projectId, VerificationStatus result);
}
