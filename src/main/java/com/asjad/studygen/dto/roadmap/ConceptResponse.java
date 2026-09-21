package com.asjad.studygen.dto.roadmap;

public record ConceptResponse(
        Long id,
        Long moduleId,
        String title,
        String contentBody,
        String keyTakeaway,
        boolean isCompleted
) {}
