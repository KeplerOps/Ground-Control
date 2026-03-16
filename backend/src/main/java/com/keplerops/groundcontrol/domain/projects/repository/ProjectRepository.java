package com.keplerops.groundcontrol.domain.projects.repository;

import com.keplerops.groundcontrol.domain.projects.model.Project;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByIdentifier(String identifier);

    boolean existsByIdentifier(String identifier);

    long count();
}
