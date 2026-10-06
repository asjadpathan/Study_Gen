package com.asjad.studygen.dto.visual;

public record GraphNodeDTO(
        Long id,
        String label,
        String status,
        int sequenceOrder,
        double masteryScore,
        int estimatedMinutes
) {}
