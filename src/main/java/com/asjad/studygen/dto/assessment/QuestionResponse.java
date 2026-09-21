package com.asjad.studygen.dto.assessment;

import java.util.List;

public record QuestionResponse(
        Long id,
        String questionText,
        List<String> options
) {}
