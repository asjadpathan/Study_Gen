package com.asjad.studygen.dto.practice;

import java.util.List;

public record DrillEvaluationResponse(
        String verdict, // "CORRECT", "PARTIALLY_CORRECT", "NEEDS_IMPROVEMENT"
        String feedback,
        List<String> missingPoints
) {}
