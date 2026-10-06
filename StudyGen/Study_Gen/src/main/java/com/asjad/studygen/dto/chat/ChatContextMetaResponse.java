package com.asjad.studygen.dto.chat;

public record ChatContextMetaResponse(
        int roadmapCount,
        int moduleCount,
        int documentCount,
        String activeAssessmentModule
) {}
