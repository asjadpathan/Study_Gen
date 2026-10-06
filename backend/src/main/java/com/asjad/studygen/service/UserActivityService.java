package com.asjad.studygen.service;

import com.asjad.studygen.entity.User;
import com.asjad.studygen.entity.UserActivityLog;
import com.asjad.studygen.entity.UserBadge;
import com.asjad.studygen.repository.UserActivityLogRepository;
import com.asjad.studygen.repository.UserBadgeRepository;
import com.asjad.studygen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserActivityService {

    private final UserActivityLogRepository activityLogRepository;
    private final UserBadgeRepository badgeRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserActivityLog logActivity(User user, String activityType, int minutesLogged) {
        User managedUser = userRepository.findById(user.getId()).orElse(user);
        LocalDate today = LocalDate.now();

        UserActivityLog activity = UserActivityLog.builder()
                .user(managedUser)
                .activityType(activityType)
                .minutesLogged(minutesLogged)
                .loggedDate(today)
                .build();

        UserActivityLog saved = activityLogRepository.save(activity);

        // Update streak
        updateUserStreak(managedUser);

        // Check badge milestones
        checkAndAwardBadges(managedUser);

        return saved;
    }

    @Transactional
    public int updateUserStreak(User user) {
        User managedUser = userRepository.findById(user.getId()).orElse(user);
        List<LocalDate> activeDates = activityLogRepository.findDistinctActiveDatesByUserId(managedUser.getId());
        Set<LocalDate> dateSet = new HashSet<>(activeDates);

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        int streak = 0;
        LocalDate checkDate;

        if (dateSet.contains(today)) {
            checkDate = today;
        } else if (dateSet.contains(yesterday)) {
            checkDate = yesterday;
        } else {
            managedUser.setCurrentStreak(0);
            userRepository.save(managedUser);
            return 0;
        }

        while (dateSet.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }

        managedUser.setCurrentStreak(streak);
        userRepository.save(managedUser);
        return streak;
    }

    @Transactional
    public void checkAndAwardBadges(User user) {
        // First Study Session / Activity
        awardBadgeIfNew(user, "First Step", "Logged your first active study engagement.");

        // Streak badges
        int streak = user.getCurrentStreak() != null ? user.getCurrentStreak() : 0;
        if (streak >= 3) {
            awardBadgeIfNew(user, "Streak Starter", "Maintained a 3-day continuous study streak.");
        }
        if (streak >= 7) {
            awardBadgeIfNew(user, "Dedication Champion", "Maintained a 7-day continuous study streak.");
        }
        if (streak >= 30) {
            awardBadgeIfNew(user, "Unstoppable", "Maintained an epic 30-day continuous study streak.");
        }

        // Minutes badges
        int minutes = user.getTotalStudyMinutes() != null ? user.getTotalStudyMinutes() : 0;
        if (minutes >= 100) {
            awardBadgeIfNew(user, "Century Club", "Logged over 100 minutes of focused study.");
        }
        if (minutes >= 500) {
            awardBadgeIfNew(user, "Focus Master", "Logged over 500 minutes of focused study.");
        }
    }

    @Transactional
    public void awardBadgeIfNew(User user, String badgeName, String badgeDescription) {
        if (!badgeRepository.existsByUserIdAndBadgeName(user.getId(), badgeName)) {
            UserBadge badge = UserBadge.builder()
                    .user(user)
                    .badgeName(badgeName)
                    .badgeDescription(badgeDescription)
                    .build();
            badgeRepository.save(badge);
            log.info("Badge '{}' awarded to user id {}", badgeName, user.getId());
        }
    }
}
