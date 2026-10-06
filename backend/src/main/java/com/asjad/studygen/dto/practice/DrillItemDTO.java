package com.asjad.studygen.dto.practice;

import java.util.List;

public record DrillItemDTO(
        String id,
        String type, // "SCENARIO", "CODE", "CONCEPTUAL"
        String title,
        String difficulty, // "BEGINNER", "INTERMEDIATE", "ADVANCED"
        String prompt,
        List<String> criteria,
        String referenceAnswer,
        String language,
        String starterCode,
        String expectedBehavior,
        List<DrillTestCaseDTO> testCases,
        String hint,
        String solution,
        List<String> options,
        Integer correctIndex,
        String explanation
) {}
