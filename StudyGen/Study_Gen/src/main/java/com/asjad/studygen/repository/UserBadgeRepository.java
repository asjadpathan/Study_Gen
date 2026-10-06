package com.asjad.studygen.repository;

import com.asjad.studygen.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    List<UserBadge> findByUserIdOrderByEarnedAtDesc(Long userId);

    boolean existsByUserIdAndBadgeName(Long userId, String badgeName);
}
