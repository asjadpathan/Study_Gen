package com.asjad.studygen.dto.practice;

public record ConceptReviewResponse(
        Long conceptId,
        Long cardId,
        int repetitionNumber,
        int intervalDays,
        double easinessFactor,
        String nextReviewDate
) {
    public ConceptReviewResponse(Long conceptId, int repetitionNumber, int intervalDays, double easinessFactor, String nextReviewDate) {
        this(conceptId, conceptId, repetitionNumber, intervalDays, easinessFactor, nextReviewDate);
    }
}
