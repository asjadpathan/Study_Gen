package com.asjad.studygen.repository;

import com.asjad.studygen.entity.UserActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {

    List<UserActivityLog> findByUserIdOrderByLoggedDateDesc(Long userId);

    List<UserActivityLog> findByUserIdAndLoggedDateBetweenOrderByLoggedDateAsc(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT DISTINCT a.loggedDate FROM UserActivityLog a WHERE a.user.id = :userId ORDER BY a.loggedDate DESC")
    List<LocalDate> findDistinctActiveDatesByUserId(@Param("userId") Long userId);
}
