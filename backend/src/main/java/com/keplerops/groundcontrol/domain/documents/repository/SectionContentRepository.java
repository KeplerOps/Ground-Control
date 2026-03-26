package com.keplerops.groundcontrol.domain.documents.repository;

import com.keplerops.groundcontrol.domain.documents.model.SectionContent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionContentRepository extends JpaRepository<SectionContent, UUID> {

    List<SectionContent> findBySectionIdOrderBySortOrder(UUID sectionId);

    List<SectionContent> findBySectionIdInOrderBySortOrder(List<UUID> sectionIds);
}
