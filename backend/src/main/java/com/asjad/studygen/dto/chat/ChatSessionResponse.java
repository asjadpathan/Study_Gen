package com.asjad.studygen.dto.chat;

public record ChatSessionResponse(
        Long id,
        String title,
        String contextTag,
        String lastActivity,
        Long moduleId,
        String moduleTitle,
        Long roadmapId,
        String roadmapTitle
) {}
