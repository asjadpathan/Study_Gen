package com.asjad.studygen.dto.document;

import java.util.List;

public record StudyGuideResponse(
        Long moduleId,
        String moduleTitle,
        String markdownContent,
        List<String> keyTakeaways
) {}
