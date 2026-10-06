package com.asjad.studygen.service;

import com.asjad.studygen.dto.assessment.AttemptResultResponse;
import com.asjad.studygen.dto.assessment.SubmitAttemptRequest;
import com.asjad.studygen.entity.Assessment;
import com.asjad.studygen.entity.AssessmentQuestion;
import com.asjad.studygen.entity.Module;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.entity.UserAssessmentAttempt;
import com.asjad.studygen.repository.AssessmentRepository;
import com.asjad.studygen.repository.ModuleRepository;
import com.asjad.studygen.repository.UserAssessmentAttemptRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MasteryProgressionService {

    private final AssessmentRepository assessmentRepository;
    private final UserAssessmentAttemptRepository attemptRepository;
    private final ModuleRepository moduleRepository;
    private final UserActivityService userActivityService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public AttemptResultResponse submitAttempt(User user, Long assessmentId, SubmitAttemptRequest request) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + assessmentId));

        List<AssessmentQuestion> questions = assessment.getQuestions();
        if (questions.isEmpty()) {
            throw new IllegalStateException("Assessment has no questions configured");
        }

        // Check if cooldown is active from a previous failed attempt
        var previousAttempts = attemptRepository.findByAssessmentIdAndUserIdOrderByAttemptedAtDesc(assessmentId, user.getId());
        if (!previousAttempts.isEmpty()) {
            UserAssessmentAttempt lastAttempt = previousAttempts.get(0);
            if (!lastAttempt.isPassed() && lastAttempt.getAttemptedAt() != null) {
                long elapsed = java.time.Duration.between(lastAttempt.getAttemptedAt(), java.time.LocalDateTime.now()).toSeconds();
                if (elapsed < 300) {
                    throw new IllegalStateException("Cooldown active: Please wait " + (300 - elapsed) + "s before retrying.");
                }
            }
        }

        List<Integer> userAnswers = request.selectedOptions();
        int totalQuestions = questions.size();
        int correctCount = 0;

        List<com.asjad.studygen.dto.assessment.QuestionReviewDTO> reviewList = new java.util.ArrayList<>();
        List<String> weakConcepts = new java.util.ArrayList<>();

        for (int i = 0; i < totalQuestions; i++) {
            AssessmentQuestion q = questions.get(i);
            Integer userChoice = (i < userAnswers.size()) ? userAnswers.get(i) : null;
            boolean isCorrect = userChoice != null && userChoice == q.getCorrectAnswerIndex();
            if (isCorrect) {
                correctCount++;
            } else {
                String concept = q.getQuestionText().length() > 40
                        ? q.getQuestionText().substring(0, 40) + "..."
                        : q.getQuestionText();
                weakConcepts.add(concept);
            }

            List<String> options;
            try {
                options = objectMapper.readValue(q.getOptionsJson(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
            } catch (Exception e) {
                options = new java.util.ArrayList<>();
            }

            reviewList.add(new com.asjad.studygen.dto.assessment.QuestionReviewDTO(
                    q.getId(),
                    q.getQuestionText(),
                    options,
                    userChoice,
                    q.getCorrectAnswerIndex(),
                    isCorrect,
                    q.getExplanation()
            ));
        }

        int score = (int) Math.round(((double) correctCount / totalQuestions) * 100);
        // Explicit boundary: passingScore is 80 (79% -> false, 80% -> true)
        int passingScore = assessment.getPassingScore() > 0 ? assessment.getPassingScore() : 80;
        boolean passed = score >= passingScore;
        boolean nextModuleUnlocked = false;
        Long nextModuleId = null;
        String nextModuleTitle = null;

        Module module = assessment.getModule();
        if (module != null && passed) {
            // Mark current module as completed and record score
            module.setCompleted(true);
            module.setMasteryScore(score);
            moduleRepository.save(module);

            // Unlock next module in sequence if available
            int nextSequence = module.getSequenceOrder() + 1;
            var nextModuleOpt = moduleRepository.findByRoadmapIdAndSequenceOrder(
                    module.getRoadmap().getId(), nextSequence
            );
            if (nextModuleOpt.isPresent()) {
                Module nextModule = nextModuleOpt.get();
                nextModule.setLocked(false);
                moduleRepository.save(nextModule);
                nextModuleUnlocked = true;
                nextModuleId = nextModule.getId();
                nextModuleTitle = nextModule.getTitle();
            }
        }

        String answersJson;
        try {
            answersJson = objectMapper.writeValueAsString(userAnswers);
        } catch (Exception e) {
            answersJson = "[]";
        }

        String remediationNotes = null;
        if (!passed) {
            remediationNotes = "Score (" + score + "%) below passing threshold (" +
                    passingScore + "%). Remediation session recommended.";
        }

        UserAssessmentAttempt attempt = new UserAssessmentAttempt(
                assessment, user, score, passed, answersJson
        );
        attempt.setRemediationNotes(remediationNotes);
        UserAssessmentAttempt savedAttempt = attemptRepository.save(attempt);

        if (passed) {
            userActivityService.logActivity(user, "TEST_PASSED", 15);
            if (module != null) {
                userActivityService.logActivity(user, "MODULE_COMPLETED", 30);
            }
        } else {
            userActivityService.logActivity(user, "ASSESSMENT_ATTEMPT", 10);
        }

        int maxAttempts = 3;
        int failedCount = (int) previousAttempts.stream().filter(a -> !a.isPassed()).count() + (passed ? 0 : 1);
        int attemptsLeft = Math.max(0, maxAttempts - (failedCount % maxAttempts));
        long cooldownSeconds = passed ? 0L : 300L;

        return new AttemptResultResponse(
                savedAttempt.getId(),
                assessment.getId(),
                score,
                passed,
                passingScore,
                nextModuleUnlocked,
                remediationNotes,
                attemptsLeft,
                cooldownSeconds,
                nextModuleId,
                nextModuleTitle,
                weakConcepts,
                reviewList
        );
    }
}
