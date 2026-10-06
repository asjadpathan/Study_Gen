package com.asjad.studygen.repository;

import com.asjad.studygen.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ModuleRepository extends JpaRepository<Module, Long> {
    List<Module> findByRoadmapIdOrderBySequenceOrderAsc(Long roadmapId);
    Optional<Module> findByRoadmapIdAndSequenceOrder(Long roadmapId, int sequenceOrder);
    long countByRoadmapUserIdAndCompletedTrue(Long userId);
    List<Module> findByRoadmapUserIdAndCompletedTrue(Long userId);
}
