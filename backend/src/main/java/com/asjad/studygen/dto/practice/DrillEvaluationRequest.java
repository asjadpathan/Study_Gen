package com.asjad.studygen.dto.practice;

import java.util.List;

public record DrillEvaluationRequest(
        String userAnswer,
        String prompt,
        List<String> criteria,
        String referenceAnswer
) {}
