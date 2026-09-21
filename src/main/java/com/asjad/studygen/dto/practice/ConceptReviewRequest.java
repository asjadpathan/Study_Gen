package com.asjad.studygen.dto.practice;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConceptReviewRequest(
        @NotNull(message = "Concept ID is required")
        Long conceptId,

        @Min(value = 0, message = "Quality rating must be between 0 and 5")
        @Max(value = 5, message = "Quality rating must be between 0 and 5")
        int qualityRating
) {}
