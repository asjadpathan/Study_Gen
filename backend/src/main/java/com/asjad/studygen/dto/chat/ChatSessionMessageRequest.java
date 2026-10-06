package com.asjad.studygen.dto.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatSessionMessageRequest(
        @NotBlank(message = "Message is required")
        String message,
        Long moduleId,
        Long roadmapId
) {}
