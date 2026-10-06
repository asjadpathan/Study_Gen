package com.asjad.studygen.dto.chat;

import java.util.List;

public record ChatSessionDetailResponse(
        Long id,
        String title,
        String contextTag,
        String lastActivity,
        Long moduleId,
        String moduleTitle,
        List<ChatMessageItemDTO> messages
) {}
