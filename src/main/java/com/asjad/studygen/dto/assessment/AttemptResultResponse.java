package com.asjad.studygen.dto.assessment;

public record AttemptResultResponse(
        Long attemptId,
        Long assessmentId,
        int score,
        boolean passed,
        int passingScore,
        boolean nextModuleUnlocked,
        String remediationNotes
) {}
