package com.asjad.studygen.dto.ai;

import java.util.List;

public record AiQuizQuestion(
        String questionText,
        List<String> options,
        int correctAnswerIndex,
        String explanation
) {}
