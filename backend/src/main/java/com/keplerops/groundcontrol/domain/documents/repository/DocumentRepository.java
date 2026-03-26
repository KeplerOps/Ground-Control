package com.keplerops.groundcontrol.domain.documents.repository;

import com.keplerops.groundcontrol.domain.documents.model.Document;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    boolean existsByProjectIdAndTitle(UUID projectId, String title);
}
