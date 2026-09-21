package com.asjad.studygen.service;

import com.asjad.studygen.entity.Module;
import com.asjad.studygen.entity.PomodoroSession;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.repository.ModuleRepository;
import com.asjad.studygen.repository.PomodoroSessionRepository;
import com.asjad.studygen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PomodoroService {

    private final PomodoroSessionRepository pomodoroSessionRepository;
    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;
    private final UserActivityService userActivityService;

    public record PomodoroLogRequest(Integer durationMinutes, Long moduleId) {}

    public record PomodoroSessionResponse(
            Long id,
            Long userId,
            Long moduleId,
            String moduleTitle,
            Integer durationMinutes,
            LocalDateTime completedAt,
            Integer userTotalStudyMinutes,
            Integer currentStreak
    ) {}

    @Transactional
    public PomodoroSessionResponse logSession(Long userId, PomodoroLogRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        Module module = null;
        if (request.moduleId() != null) {
            module = moduleRepository.findById(request.moduleId()).orElse(null);
        }

        int duration = (request.durationMinutes() != null && request.durationMinutes() > 0)
                ? request.durationMinutes() : 25;

        PomodoroSession session = PomodoroSession.builder()
                .user(user)
                .module(module)
                .durationMinutes(duration)
                .build();

        PomodoroSession saved = pomodoroSessionRepository.save(session);

        // Update total study minutes
        int currentMinutes = user.getTotalStudyMinutes() != null ? user.getTotalStudyMinutes() : 0;
        user.setTotalStudyMinutes(currentMinutes + duration);
        userRepository.save(user);

        // Log user activity and recalculate streak & badges
        userActivityService.logActivity(user, "POMODORO_SESSION", duration);

        return new PomodoroSessionResponse(
                saved.getId(),
                user.getId(),
                module != null ? module.getId() : null,
                module != null ? module.getTitle() : null,
                saved.getDurationMinutes(),
                saved.getCompletedAt(),
                user.getTotalStudyMinutes(),
                user.getCurrentStreak()
        );
    }

    public List<PomodoroSessionResponse> getUserSessions(Long userId) {
        return pomodoroSessionRepository.findByUserIdOrderByCompletedAtDesc(userId).stream()
                .map(s -> new PomodoroSessionResponse(
                        s.getId(),
                        s.getUser().getId(),
                        s.getModule() != null ? s.getModule().getId() : null,
                        s.getModule() != null ? s.getModule().getTitle() : null,
                        s.getDurationMinutes(),
                        s.getCompletedAt(),
                        s.getUser().getTotalStudyMinutes(),
                        s.getUser().getCurrentStreak()
                ))
                .toList();
    }
}
