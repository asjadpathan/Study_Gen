package com.asjad.studygen.controller;

import com.asjad.studygen.entity.Module;
import com.asjad.studygen.entity.Roadmap;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.entity.UserActivityLog;
import com.asjad.studygen.entity.UserAssessmentAttempt;
import com.asjad.studygen.entity.UserBadge;
import com.asjad.studygen.repository.*;
import com.asjad.studygen.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final RoadmapRepository roadmapRepository;
    private final ModuleRepository moduleRepository;
    private final UserAssessmentAttemptRepository attemptRepository;
    private final UserActivityLogRepository activityLogRepository;
    private final UserBadgeRepository badgeRepository;
    private final AnalyticsService analyticsService;

    public record DashboardRoadmapItem(
            Long id,
            String title,
            String targetRole,
            String status,
            int completedModules,
            int totalModules,
            int progressPercentage,
            String nextModuleTitle
    ) {}

    public record UserDashboardResponse(
            Long userId,
            String fullName,
            String email,
            Integer currentStreak,
            Integer weeklyStudyMinutes,
            Long completedModules,
            Long passedTests,
            List<DashboardRoadmapItem> roadmaps
    ) {}

    public record UpdateUserRequest(
            String fullName
    ) {}

    public record SkillRadarScore(
            String axis,
            int score
    ) {}

    public record BadgeItemResponse(
            Long id,
            String badgeName,
            String badgeDescription,
            String earnedAt
    ) {}

    public record UserProfileResponse(
            Long userId,
            String fullName,
            String email,
            String memberSince,
            int currentStreak,
            int totalStudyMinutes,
            double totalStudyHours,
            long totalRoadmaps,
            long completedModulesCount,
            long passedTestsCount,
            double skillVelocityModulesPerWeek,
            int totalBadgesEarned,
            List<BadgeItemResponse> badges,
            List<SkillRadarScore> skillsRadar
    ) {}

    @GetMapping("/dashboard")
    public ResponseEntity<UserDashboardResponse> getDashboardData(@AuthenticationPrincipal User user) {
        Long userId = user.getId();

        // 1. Fetch roadmaps for user
        List<Roadmap> userRoadmaps = roadmapRepository.findByUserId(userId);

        List<DashboardRoadmapItem> roadmapItems = userRoadmaps.stream().map(r -> {
            List<Module> modules = r.getModules() != null ? r.getModules() : List.of();
            int total = modules.size();
            int completed = (int) modules.stream().filter(Module::isCompleted).count();
            int progress = total > 0 ? (completed * 100) / total : 0;

            String nextModule = modules.stream()
                    .filter(m -> !m.isCompleted())
                    .min(Comparator.comparingInt(Module::getSequenceOrder))
                    .map(Module::getTitle)
                    .orElse("All Modules Completed");

            String status = r.isActive() ? "ACTIVE" : "DRAFT";

            return new DashboardRoadmapItem(
                    r.getId(),
                    r.getTitle(),
                    r.getTargetRole(),
                    status,
                    completed,
                    total,
                    progress,
                    nextModule
            );
        }).toList();

        // 2. Weekly study minutes (from Monday of current week to today)
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate today = LocalDate.now();
        List<UserActivityLog> weeklyLogs = activityLogRepository.findByUserIdAndLoggedDateBetweenOrderByLoggedDateAsc(userId, monday, today);
        int weeklyMinutes = weeklyLogs.stream().mapToInt(UserActivityLog::getMinutesLogged).sum();

        // 3. Totals
        long completedModulesCount = moduleRepository.countByRoadmapUserIdAndCompletedTrue(userId);
        long passedTestsCount = attemptRepository.findByUserIdOrderByAttemptedAtDesc(userId).stream()
                .filter(UserAssessmentAttempt::isPassed)
                .count();

        return ResponseEntity.ok(new UserDashboardResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getCurrentStreak() != null ? user.getCurrentStreak() : 0,
                weeklyMinutes > 0 ? weeklyMinutes : (user.getTotalStudyMinutes() != null ? user.getTotalStudyMinutes() : 0),
                completedModulesCount,
                passedTestsCount,
                roadmapItems
        ));
    }

    @GetMapping({"", "/profile"})
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal User user) {
        User managedUser = userRepository.findById(user.getId()).orElse(user);
        Long userId = managedUser.getId();

        long totalRoadmaps = roadmapRepository.findByUserId(userId).size();
        long completedModulesCount = moduleRepository.countByRoadmapUserIdAndCompletedTrue(userId);
        long passedTestsCount = attemptRepository.findByUserIdOrderByAttemptedAtDesc(userId).stream()
                .filter(UserAssessmentAttempt::isPassed)
                .count();

        int totalMinutes = managedUser.getTotalStudyMinutes() != null ? managedUser.getTotalStudyMinutes() : 0;
        double totalHours = Math.round((totalMinutes / 60.0) * 10.0) / 10.0;

        // Velocity: modules completed / weeks active
        double weeksActive = Math.max(1.0, Math.ceil(
                Duration.between(managedUser.getCreatedAt(), LocalDateTime.now()).toDays() / 7.0
        ));
        double velocity = Math.round((completedModulesCount / weeksActive) * 10.0) / 10.0;

        // Earned badges
        List<UserBadge> earnedBadges = badgeRepository.findByUserIdOrderByEarnedAtDesc(userId);
        DateTimeFormatter badgeDateFmt = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        List<BadgeItemResponse> badgeResponses = earnedBadges.stream()
                .map(b -> new BadgeItemResponse(
                        b.getId(),
                        b.getBadgeName(),
                        b.getBadgeDescription(),
                        b.getEarnedAt() != null ? b.getEarnedAt().format(badgeDateFmt) : "Recently"
                ))
                .toList();

        // Member since formatted
        String memberSince = managedUser.getCreatedAt() != null
                ? managedUser.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                : "2026";

        // Calculate 6-Axis Skill Radar Scores from completed modules & test performance
        List<Module> completedModules = moduleRepository.findByRoadmapUserIdAndCompletedTrue(userId);
        Map<String, Integer> domainCounts = new HashMap<>();
        for (Module m : completedModules) {
            String combined = (m.getTitle() + " " + (m.getDescription() != null ? m.getDescription() : "")).toLowerCase();
            if (combined.matches(".*(react|vue|ui|css|html|frontend|javascript|typescript).*")) {
                domainCounts.merge("Frontend", 1, Integer::sum);
            }
            if (combined.matches(".*(java|spring|python|backend|api|rest|node|golang).*")) {
                domainCounts.merge("Backend", 1, Integer::sum);
            }
            if (combined.matches(".*(sql|postgres|mysql|database|mongo|redis).*")) {
                domainCounts.merge("Database", 1, Integer::sum);
            }
            if (combined.matches(".*(docker|kubernetes|aws|cloud|devops|linux|ci/cd).*")) {
                domainCounts.merge("Cloud & DevOps", 1, Integer::sum);
            }
            if (combined.matches(".*(system|architecture|design|microservice|scale).*")) {
                domainCounts.merge("Architecture", 1, Integer::sum);
            }
            if (combined.matches(".*(algorithm|data structure|graph|tree|sorting).*")) {
                domainCounts.merge("Algorithms", 1, Integer::sum);
            }
        }

        // Radar scores: baseline 20, plus 25 per relevant completed module, plus test bonus
        int testBonus = (int) Math.min(20, passedTestsCount * 5);
        List<SkillRadarScore> skillsRadar = List.of(
                new SkillRadarScore("Frontend", Math.min(100, 20 + domainCounts.getOrDefault("Frontend", 0) * 30 + testBonus)),
                new SkillRadarScore("Backend", Math.min(100, 20 + domainCounts.getOrDefault("Backend", 0) * 30 + testBonus)),
                new SkillRadarScore("Database", Math.min(100, 20 + domainCounts.getOrDefault("Database", 0) * 30 + testBonus)),
                new SkillRadarScore("Cloud & DevOps", Math.min(100, 20 + domainCounts.getOrDefault("Cloud & DevOps", 0) * 30 + testBonus)),
                new SkillRadarScore("Architecture", Math.min(100, 20 + domainCounts.getOrDefault("Architecture", 0) * 30 + testBonus)),
                new SkillRadarScore("Algorithms", Math.min(100, 20 + domainCounts.getOrDefault("Algorithms", 0) * 30 + testBonus))
        );

        return ResponseEntity.ok(new UserProfileResponse(
                userId,
                managedUser.getFullName(),
                managedUser.getEmail(),
                memberSince,
                managedUser.getCurrentStreak() != null ? managedUser.getCurrentStreak() : 0,
                totalMinutes,
                totalHours,
                totalRoadmaps,
                completedModulesCount,
                passedTestsCount,
                velocity,
                badgeResponses.size(),
                badgeResponses,
                skillsRadar
        ));
    }

    @PatchMapping({"", "/"})
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody UpdateUserRequest request) {

        if (request.fullName() == null || request.fullName().trim().length() < 2) {
            throw new IllegalArgumentException("Full name must be at least 2 characters long.");
        }

        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        managedUser.setFullName(request.fullName().trim());
        userRepository.save(managedUser);

        return getProfile(managedUser);
    }
}
