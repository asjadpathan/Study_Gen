package com.asjad.studygen.repository;

import com.asjad.studygen.entity.Concept;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConceptRepository extends JpaRepository<Concept, Long> {
    List<Concept> findByModuleId(Long moduleId);
}
