package com.asjad.studygen.repository;

import com.asjad.studygen.entity.UserJobMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserJobMatchRepository extends JpaRepository<UserJobMatch, Long> {
    List<UserJobMatch> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<UserJobMatch> findByIdAndUserId(Long id, Long userId);
}
