package com.asjad.studygen.dto.practice;

public record RoadmapModuleBreakdown(
        Long roadmapId,
        String roadmapTitle,
        Long moduleId,
        String moduleTitle,
        int dueCount
) {}
