package com.asjad.studygen.dto.assessment;

import java.util.List;

public record QuestionReviewDTO(
        Long questionId,
        String questionText,
        List<String> options,
        Integer userAnswerIndex,
        Integer correctAnswerIndex,
        boolean isCorrect,
        String explanation
) {}
