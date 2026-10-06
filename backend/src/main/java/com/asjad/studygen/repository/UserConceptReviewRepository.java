package com.asjad.studygen.repository;

import com.asjad.studygen.entity.UserConceptReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserConceptReviewRepository extends JpaRepository<UserConceptReview, Long> {
    Optional<UserConceptReview> findByUserIdAndConceptId(Long userId, Long conceptId);
    List<UserConceptReview> findByUserIdAndNextReviewDateLessThanEqual(Long userId, LocalDate date);
    List<UserConceptReview> findByUserId(Long userId);
}
