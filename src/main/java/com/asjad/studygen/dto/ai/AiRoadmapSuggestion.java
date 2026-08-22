package com.asjad.studygen.dto.ai;

import java.util.List;

public record AiRoadmapSuggestion(
        String title,
        List<AiModuleSuggestion> modules
) {}