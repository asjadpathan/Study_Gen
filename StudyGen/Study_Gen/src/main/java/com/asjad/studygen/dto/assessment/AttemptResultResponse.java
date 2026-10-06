package com.asjad.studygen.dto.assessment;

import java.util.List;

public record AttemptResultResponse(
        Long attemptId,
        Long assessmentId,
        int score,
        boolean passed,
        int passingScore,
        boolean nextModuleUnlocked,
        String remediationNotes,
        int attemptsLeft,
        long cooldownSeconds,
        Long nextModuleId,
        String nextModuleTitle,
        List<String> weakConcepts,
        List<QuestionReviewDTO> questionsReview
) {
    public AttemptResultResponse(
            Long attemptId,
            Long assessmentId,
            int score,
            boolean passed,
            int passingScore,
            boolean nextModuleUnlocked,
            String remediationNotes
    ) {
        this(attemptId, assessmentId, score, passed, passingScore, nextModuleUnlocked, remediationNotes,
                passed ? 3 : 2, passed ? 0L : 300L, null, null, List.of(), List.of());
    }
}
