package com.asjad.studygen.dto.roadmap;

import java.util.List;

public record RoadmapResponse(
        Long id,
        String title,
        String targetRole,
        Boolean isActive,
        List<ModuleResponse> modules
) {}