package com.asjad.studygen.dto.practice;

public record PracticeCardDTO(
        Long cardId,
        Long conceptId,
        Long moduleId,
        String moduleTitle,
        Long roadmapId,
        String roadmapTitle,
        String front,
        String back,
        String hint,
        int repetitionNumber,
        int intervalDays,
        double easinessFactor,
        String nextReviewDate
) {}
