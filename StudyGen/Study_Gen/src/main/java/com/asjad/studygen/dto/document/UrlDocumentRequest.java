package com.asjad.studygen.dto.document;

import jakarta.validation.constraints.NotBlank;

public record UrlDocumentRequest(
        @NotBlank(message = "URL is required")
        String url,
        Long roadmapId
) {}
