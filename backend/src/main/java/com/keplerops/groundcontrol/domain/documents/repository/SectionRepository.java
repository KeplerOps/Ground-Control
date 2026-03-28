package com.keplerops.groundcontrol.domain.documents.repository;

import com.keplerops.groundcontrol.domain.documents.model.Section;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionRepository extends JpaRepository<Section, UUID> {

    List<Section> findByDocumentIdOrderBySortOrder(UUID documentId);

    List<Section> findByDocumentIdAndParentIdIsNullOrderBySortOrder(UUID documentId);

    List<Section> findByParentIdOrderBySortOrder(UUID parentId);

    boolean existsByDocumentIdAndParentIdAndTitle(UUID documentId, UUID parentId, String title);

    boolean existsByDocumentIdAndParentIdIsNullAndTitle(UUID documentId, String title);

    Optional<Section> findFirstByDocumentIdAndParentIdIsNullAndTitle(UUID documentId, String title);
}
