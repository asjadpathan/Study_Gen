package com.asjad.studygen.repository;

import com.asjad.studygen.entity.UploadedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UploadedDocumentRepository extends JpaRepository<UploadedDocument, Long> {
    List<UploadedDocument> findByUserIdOrderByUploadedAtDesc(Long userId);
}
