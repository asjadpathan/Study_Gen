package com.asjad.studygen.dto.roadmap;

import java.util.List;

public record CreateRoadmapRequest(
        String title,
        String targetRole,
        List<ModuleRequest> modules
) {}