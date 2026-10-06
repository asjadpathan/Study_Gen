package com.asjad.studygen.dto.document;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageRequest(
        Long moduleId,

        @NotBlank(message = "Message cannot be blank")
        String message
) {}
