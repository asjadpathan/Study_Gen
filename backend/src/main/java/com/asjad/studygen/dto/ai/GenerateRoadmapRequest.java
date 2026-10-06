package com.asjad.studygen.dto.ai;

public record GenerateRoadmapRequest(
        String targetTopic,
        String targetRole,
        String selfAssessedBaseline,
        Integer hoursPerWeek,
        String learningStyle,
        String param6,
        String param7,
        String param8,
        Integer diagnosticScore,
        String diagnosticNotes
) {
    public GenerateRoadmapRequest(
            String targetTopic,
            String targetRole,
            String selfAssessedBaseline,
            Integer hoursPerWeek,
            String learningStyle
    ) {
        this(targetTopic, targetRole, selfAssessedBaseline, hoursPerWeek, learningStyle, null, null, null, null, null);
    }
}