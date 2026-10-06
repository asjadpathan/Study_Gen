package com.asjad.studygen.repository;

import com.asjad.studygen.entity.UserSavedResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSavedResourceRepository extends JpaRepository<UserSavedResource, Long> {
    List<UserSavedResource> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<UserSavedResource> findByUserIdAndResourceId(Long userId, Long resourceId);
    boolean existsByUserIdAndResourceId(Long userId, Long resourceId);
    void deleteByUserIdAndResourceId(Long userId, Long resourceId);
}
