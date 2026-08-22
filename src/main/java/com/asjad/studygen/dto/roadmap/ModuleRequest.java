package com.asjad.studygen.dto.roadmap;

public record ModuleRequest(
        String title,
        String description,
        Integer sequenceOrder
) {}