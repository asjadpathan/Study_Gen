package com.asjad.studygen.dto.assessment;

import jakarta.validation.constraints.NotBlank;

public record DiagnosticQuizRequest(
        @NotBlank(message = "Topic is required")
        String topic,

        String claimedPrerequisites
) {}
