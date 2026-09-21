package com.asjad.studygen.dto.assessment;

import java.util.List;

public record AssessmentResponse(
        Long id,
        Long moduleId,
        String title,
        String type,
        int passingScore,
        List<QuestionResponse> questions
) {}
