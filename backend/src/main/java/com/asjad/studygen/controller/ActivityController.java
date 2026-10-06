package com.asjad.studygen.controller;

import com.asjad.studygen.entity.User;
import com.asjad.studygen.entity.UserActivityLog;
import com.asjad.studygen.repository.UserActivityLogRepository;
import com.asjad.studygen.repository.UserRepository;
import com.asjad.studygen.service.AnalyticsService;
import com.asjad.studygen.service.PomodoroService;
import com.asjad.studygen.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
@Slf4j
public class ActivityController {

    private final UserRepository userRepository;
    private final UserActivityService userActivityService;
    private final UserActivityLogRepository activityLogRepository;
    private final AnalyticsService analyticsService;
    private final PomodoroService pomodoroService;

    public record PomodoroActivityRequest(
            Integer minutes,
            Long moduleId
    ) {}

    public record PomodoroActivityResponse(
            boolean success,
            int minutes,
            int streak,
            int totalStudyMinutes
    ) {}

    public record WeekDayActivity(
            String day,
            String date,
            int minutes
    ) {}

    public record ActivityHeatmapResponse(
            int todayMinutes,
            double totalHours,
            String bestDay,
            int bestDayMinutes,
            int currentStreak,
            int longestStreak,
            int totalActiveDays,
            List<WeekDayActivity> weekDays,
            List<AnalyticsService.HeatmapDayEntry> heatmap
    ) {}

    @PostMapping("/pomodoro")
    public ResponseEntity<PomodoroActivityResponse> logPomodoroSession(
            @AuthenticationPrincipal User user,
            @RequestBody(required = false) PomodoroActivityRequest request) {

        int minutes = (request != null && request.minutes() != null && request.minutes() > 0)
                ? request.minutes()
                : 25;

        // 1. Log activity and refresh streak / badges
        userActivityService.logActivity(user, "POMODORO_FOCUS", minutes);

        // 2. Increment total study minutes
        User managedUser = userRepository.findById(user.getId()).orElse(user);
        int currentTotal = managedUser.getTotalStudyMinutes() != null ? managedUser.getTotalStudyMinutes() : 0;
        managedUser.setTotalStudyMinutes(currentTotal + minutes);
        userRepository.save(managedUser);

        // 3. Log to PomodoroService history
        try {
            Long modId = request != null ? request.moduleId() : null;
            pomodoroService.logSession(user.getId(), new PomodoroService.PomodoroLogRequest(minutes, modId));
        } catch (Exception e) {
            log.warn("Optional Pomodoro session record could not be saved: {}", e.getMessage());
        }

        int updatedStreak = managedUser.getCurrentStreak() != null ? managedUser.getCurrentStreak() : 1;

        return ResponseEntity.ok(new PomodoroActivityResponse(
                true,
                minutes,
                updatedStreak,
                managedUser.getTotalStudyMinutes()
        ));
    }

    @GetMapping("/heatmap")
    public ResponseEntity<ActivityHeatmapResponse> getActivityHeatmap(
            @AuthenticationPrincipal User user) {

        User managedUser = userRepository.findById(user.getId()).orElse(user);

        // 1. Get streak heatmap (last 60 days)
        AnalyticsService.StreakHeatmapResponse streakData = analyticsService.getStreakHeatmap(user.getId(), 60);

        LocalDate today = LocalDate.now();

        // 2. Today's minutes
        int todayMinutes = 0;
        if (streakData.heatmap() != null) {
            for (AnalyticsService.HeatmapDayEntry entry : streakData.heatmap()) {
                if (entry.date().equals(today)) {
                    todayMinutes = entry.totalMinutes();
                    break;
                }
            }
        }

        // 3. Total study hours
        int totalMinutes = managedUser.getTotalStudyMinutes() != null ? managedUser.getTotalStudyMinutes() : 0;
        double totalHours = Math.round((totalMinutes / 60.0) * 10.0) / 10.0;

        // 4. WeekDays array for SVG Bar Chart (last 7 days)
        List<WeekDayActivity> weekDays = new ArrayList<>();
        Map<LocalDate, Integer> dayMinuteMap = new HashMap<>();
        if (streakData.heatmap() != null) {
            for (AnalyticsService.HeatmapDayEntry entry : streakData.heatmap()) {
                dayMinuteMap.put(entry.date(), entry.totalMinutes());
            }
        }

        int bestMinutes = 0;
        String bestDayName = "None";

        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            int mins = dayMinuteMap.getOrDefault(d, 0);
            String dayShort = d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            weekDays.add(new WeekDayActivity(dayShort, d.toString(), mins));

            if (mins > bestMinutes) {
                bestMinutes = mins;
                bestDayName = d.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            }
        }

        if (bestMinutes == 0) {
            bestDayName = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        }

        int currentStreak = managedUser.getCurrentStreak() != null ? managedUser.getCurrentStreak() : 0;

        return ResponseEntity.ok(new ActivityHeatmapResponse(
                todayMinutes,
                totalHours,
                bestDayName,
                bestMinutes,
                currentStreak,
                streakData.longestStreak(),
                streakData.totalActiveDays(),
                weekDays,
                streakData.heatmap()
        ));
    }
}
