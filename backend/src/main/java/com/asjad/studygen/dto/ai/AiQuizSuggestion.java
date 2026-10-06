package com.asjad.studygen.dto.ai;

import java.util.List;

public record AiQuizSuggestion(
        String title,
        List<AiQuizQuestion> questions
) {}
