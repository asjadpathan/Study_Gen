package com.asjad.studygen.dto.roadmap;

public record LearnMoreRequest(
        String partTitle,
        String partLevel,
        String contentBody,
        Integer partIndex,
        String roadmapTopic
) {}
