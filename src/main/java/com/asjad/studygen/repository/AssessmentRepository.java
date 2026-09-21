package com.asjad.studygen.repository;

import com.asjad.studygen.entity.Assessment;
import com.asjad.studygen.entity.AssessmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findByModuleId(Long moduleId);
    Optional<Assessment> findFirstByModuleIdAndType(Long moduleId, AssessmentType type);
    List<Assessment> findByType(AssessmentType type);
}
