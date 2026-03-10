package com.keplerops.groundcontrol.domain.requirements.repository;

import com.keplerops.groundcontrol.domain.requirements.model.GitHubIssueSync;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GitHubIssueSyncRepository extends JpaRepository<GitHubIssueSync, UUID> {

    Optional<GitHubIssueSync> findByIssueNumber(Integer issueNumber);
}
