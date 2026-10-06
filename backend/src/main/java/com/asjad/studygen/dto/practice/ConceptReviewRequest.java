package com.asjad.studygen.dto.practice;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ConceptReviewRequest(
        @JsonAlias({"cardId", "id"})
        Long conceptId,

        @JsonAlias("quality")
        @Min(value = 0, message = "Quality rating must be between 0 and 5")
        @Max(value = 5, message = "Quality rating must be between 0 and 5")
        int qualityRating
) {
    public Long getResolvedConceptId() {
        return conceptId;
    }

    public int getResolvedQuality() {
        return qualityRating;
    }
}
