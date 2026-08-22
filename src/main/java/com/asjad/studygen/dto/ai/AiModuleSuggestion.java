package com.asjad.studygen.dto.ai;

import java.util.List;

public record AiModuleSuggestion(
        String title,
        String description,
        int sequenceOrder
) {}