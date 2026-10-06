package com.asjad.studygen.dto.ai;

import java.util.List;

public record AiModuleSuggestion(
        String title,
        String description,
        int sequenceOrder,
        List<AiConceptSuggestion> concepts
) {
    public AiModuleSuggestion(String title, String description, int sequenceOrder) {
        this(title, description, sequenceOrder, List.of());
    }
}