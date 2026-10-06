package com.asjad.studygen.dto.visual;

import java.util.List;

public record MindMapNodeDTO(
        String id,
        String label,
        String category,
        List<MindMapNodeDTO> children
) {}
