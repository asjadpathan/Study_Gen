package com.asjad.studygen.dto.practice;

import java.util.List;

public record ModuleDrillsResponse(
        Long moduleId,
        String moduleTitle,
        boolean isTechnical,
        List<DrillItemDTO> drills
) {}
