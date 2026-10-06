package com.asjad.studygen.dto.assessment;

import java.util.List;

public record AssessmentResponse(
        Long id,
        Long moduleId,
        String title,
        String type,
        int passingScore,
        List<QuestionResponse> questions,
        boolean alreadyPassed,
        Integer bestScore,
        int attemptsLeft,
        long cooldownSeconds,
        String moduleTitle
) {
    public AssessmentResponse(
            Long id,
            Long moduleId,
            String title,
            String type,
            int passingScore,
            List<QuestionResponse> questions
    ) {
        this(id, moduleId, title, type, passingScore, questions, false, null, 3, 0L, title);
    }
}
