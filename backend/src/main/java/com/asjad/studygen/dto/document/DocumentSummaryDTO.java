package com.asjad.studygen.dto.document;

import java.util.List;

public record DocumentSummaryDTO(
        String overview,
        List<String> keyPoints,
        String audience
) {}
