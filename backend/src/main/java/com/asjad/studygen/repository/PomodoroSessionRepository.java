package com.asjad.studygen.repository;

import com.asjad.studygen.entity.PomodoroSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PomodoroSessionRepository extends JpaRepository<PomodoroSession, Long> {

    List<PomodoroSession> findByUserIdOrderByCompletedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(p.durationMinutes), 0) FROM PomodoroSession p WHERE p.user.id = :userId")
    Integer getTotalMinutesByUserId(@Param("userId") Long userId);
}
