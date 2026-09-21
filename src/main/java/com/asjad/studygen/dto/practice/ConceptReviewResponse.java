package com.asjad.studygen.dto.practice;

public record ConceptReviewResponse(
        Long conceptId,
        int repetitionNumber,
        int intervalDays,
        double easinessFactor,
        String nextReviewDate
) {}
