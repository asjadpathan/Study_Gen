package com.asjad.studygen.dto.visual;

import java.util.List;

public record RoadmapGraphDTO(
        Long roadmapId,
        String title,
        List<GraphNodeDTO> nodes,
        List<GraphLinkDTO> links
) {}
