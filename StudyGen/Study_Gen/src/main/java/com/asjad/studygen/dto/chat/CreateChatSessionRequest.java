package com.asjad.studygen.dto.chat;

public record CreateChatSessionRequest(
        String title,
        String contextTag,
        Long moduleId,
        Long roadmapId
) {}
