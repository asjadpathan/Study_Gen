package com.asjad.studygen.dto.roadmap;

public record ModuleResponse(
        Long id,
        String title,
        String description,
        Integer sequenceOrder,
        Boolean isLocked,
        Boolean isCompleted,
        Double masteryScore
) {}