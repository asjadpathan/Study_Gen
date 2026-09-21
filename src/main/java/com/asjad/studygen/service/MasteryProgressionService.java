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

        List<Integer> userAnswers = request.selectedOptions();
        int totalQuestions = questions.size();
        int correctCount = 0;

        for (int i = 0; i < totalQuestions; i++) {
            if (i < userAnswers.size()) {
                int userAnswer = userAnswers.get(i);
                if (userAnswer == questions.get(i).getCorrectAnswerIndex()) {
                    correctCount++;
                }
            }
        }

        int score = (int) Math.round(((double) correctCount / totalQuestions) * 100);
        boolean passed = score >= assessment.getPassingScore();
        boolean nextModuleUnlocked = false;

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
                    assessment.getPassingScore() + "%). Remediation session recommended.";
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

        return new AttemptResultResponse(
                savedAttempt.getId(),
                assessment.getId(),
                score,
                passed,
                assessment.getPassingScore(),
                nextModuleUnlocked,
                remediationNotes
        );
    }
}
