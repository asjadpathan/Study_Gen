package com.asjad.studygen.dto.assessment;

import java.util.List;

public record RemediationResponse(
        Long attemptId,
        int originalScore,
        String diagnosis,
        String simplifiedExplanation,
        List<QuestionResponse> retestQuestions
) {}
