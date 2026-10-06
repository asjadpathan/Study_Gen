package com.asjad.studygen.dto.document;

public record ChatMessageResponse(
        Long id,
        Long moduleId,
        String sender,
        String messageText,
        String createdAt
) {}
