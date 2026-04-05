package com.keplerops.groundcontrol.domain.requirements.repository;

import com.keplerops.groundcontrol.domain.requirements.model.GitHubPullRequestSync;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GitHubPullRequestSyncRepository extends JpaRepository<GitHubPullRequestSync, UUID> {

    Optional<GitHubPullRequestSync> findByPrNumber(Integer prNumber);
}
