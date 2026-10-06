package com.asjad.studygen.dto.ai;

import java.util.List;

public record AiRemediationSuggestion(
        String diagnosis,
        String simplifiedExplanation,
        List<AiQuizQuestion> retestQuestions
) {}
