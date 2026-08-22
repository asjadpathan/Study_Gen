package com.asjad.studygen.dto.ai;

public record GenerateRoadmapRequest(
        String targetTopic,
        String targetRole,
        String selfAssessedBaseline,
        Integer hoursPerWeek,
        String learningStyle
) {}