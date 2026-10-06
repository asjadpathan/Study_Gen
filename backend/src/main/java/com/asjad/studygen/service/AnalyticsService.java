package com.asjad.studygen.service;

import com.asjad.studygen.entity.User;
import com.asjad.studygen.entity.UserActivityLog;
import com.asjad.studygen.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final UserActivityLogRepository activityLogRepository;
    private final UserBadgeRepository badgeRepository;
    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;
    private final RoadmapRepository roadmapRepository;
    private final UserAssessmentAttemptRepository assessmentAttemptRepository;

    public record HeatmapDayEntry(
            LocalDate date,
            int count,
            int totalMinutes,
            List<String> activities
    ) {}

    public record StreakHeatmapResponse(
            int currentStreak,
            int longestStreak,
            int totalActiveDays,
            List<HeatmapDayEntry> heatmap
    ) {}

    public record BadgeResponse(
            Long id,
            String badgeName,
            String badgeDescription,
            LocalDateTime earnedAt
    ) {}

    public record ProfileSummaryResponse(
            Long userId,
            String email,
            String fullName,
            int currentStreak,
            int totalStudyMinutes,
            long totalRoadmaps,
            long completedModulesCount,
            int totalAssessmentsTaken,
            List<BadgeResponse> badges,
            double skillVelocityModulesPerWeek,
            LocalDateTime memberSince
    ) {}

    @Transactional(readOnly = true)
    public StreakHeatmapResponse getStreakHeatmap(Long userId, Integer daysBack) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        int days = (daysBack != null && daysBack > 0) ? daysBack : 180;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        List<UserActivityLog> logs = activityLogRepository.findByUserIdAndLoggedDateBetweenOrderByLoggedDateAsc(
                userId, startDate, endDate);

        // Group by date
        Map<LocalDate, List<UserActivityLog>> logsByDate = logs.stream()
                .collect(Collectors.groupingBy(UserActivityLog::getLoggedDate));

        List<HeatmapDayEntry> heatmap = new ArrayList<>();
        LocalDate cur = startDate;
        while (!cur.isAfter(endDate)) {
            List<UserActivityLog> dayLogs = logsByDate.getOrDefault(cur, Collections.emptyList());
            int count = dayLogs.size();
            int totalMinutes = dayLogs.stream().mapToInt(UserActivityLog::getMinutesLogged).sum();
            List<String> activities = dayLogs.stream()
                    .map(UserActivityLog::getActivityType)
                    .distinct()
                    .toList();

            heatmap.add(new HeatmapDayEntry(cur, count, totalMinutes, activities));
            cur = cur.plusDays(1);
        }

        // Calculate longest streak from all distinct active dates
        List<LocalDate> allDates = activityLogRepository.findDistinctActiveDatesByUserId(userId);
        int longestStreak = calculateLongestStreak(allDates);

        int currentStreak = user.getCurrentStreak() != null ? user.getCurrentStreak() : 0;

        return new StreakHeatmapResponse(
                currentStreak,
                longestStreak,
                allDates.size(),
                heatmap
        );
    }

    @Transactional(readOnly = true)
    public ProfileSummaryResponse getProfileSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        long totalRoadmaps = roadmapRepository.findByUserId(userId).size();
        long completedModulesCount = moduleRepository.countByRoadmapUserIdAndCompletedTrue(userId);
        int totalAssessments = assessmentAttemptRepository.findByUserIdOrderByAttemptedAtDesc(userId).size();

        List<BadgeResponse> badges = badgeRepository.findByUserIdOrderByEarnedAtDesc(userId).stream()
                .map(b -> new BadgeResponse(b.getId(), b.getBadgeName(), b.getBadgeDescription(), b.getEarnedAt()))
                .toList();

        // Skill velocity: modules completed / weeks active (at least 1 week)
        double weeksActive = Math.max(1.0, Math.ceil(
                java.time.Duration.between(user.getCreatedAt(), LocalDateTime.now()).toDays() / 7.0
        ));
        double velocity = Math.round((completedModulesCount / weeksActive) * 10.0) / 10.0;

        return new ProfileSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getCurrentStreak() != null ? user.getCurrentStreak() : 0,
                user.getTotalStudyMinutes() != null ? user.getTotalStudyMinutes() : 0,
                totalRoadmaps,
                completedModulesCount,
                totalAssessments,
                badges,
                velocity,
                user.getCreatedAt()
        );
    }

    private int calculateLongestStreak(List<LocalDate> sortedDatesDesc) {
        if (sortedDatesDesc.isEmpty()) return 0;
        // Dates are ordered desc
        Set<LocalDate> dateSet = new HashSet<>(sortedDatesDesc);
        int maxStreak = 0;

        for (LocalDate date : sortedDatesDesc) {
            // Check if this date is the start of a consecutive sequence
            if (!dateSet.contains(date.plusDays(1))) {
                int currentStreak = 0;
                LocalDate check = date;
                while (dateSet.contains(check)) {
                    currentStreak++;
                    check = check.minusDays(1);
                }
                if (currentStreak > maxStreak) {
                    maxStreak = currentStreak;
                }
            }
        }
        return maxStreak;
    }
}
