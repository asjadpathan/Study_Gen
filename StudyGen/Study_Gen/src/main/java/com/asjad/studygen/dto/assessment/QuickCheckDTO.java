package com.asjad.studygen.dto.assessment;

import java.util.List;

public record QuickCheckDTO(
        String questionText,
        List<String> options,
        int correctAnswerIndex,
        String explanation
) {}
