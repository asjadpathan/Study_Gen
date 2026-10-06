package com.asjad.studygen.repository;

import com.asjad.studygen.entity.UserAssessmentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserAssessmentAttemptRepository extends JpaRepository<UserAssessmentAttempt, Long> {
    List<UserAssessmentAttempt> findByUserIdOrderByAttemptedAtDesc(Long userId);
    List<UserAssessmentAttempt> findByAssessmentIdAndUserIdOrderByAttemptedAtDesc(Long assessmentId, Long userId);
}
