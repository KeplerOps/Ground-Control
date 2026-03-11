package com.keplerops.groundcontrol.domain.requirements.repository;

import com.keplerops.groundcontrol.domain.requirements.model.RequirementImport;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementImportRepository extends JpaRepository<RequirementImport, UUID> {}
