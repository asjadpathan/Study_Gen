package com.asjad.studygen.dto.document;

import java.util.List;

public record DocumentDetailResponse(
        Long id,
        String filename,
        String fileType,
        long fileSize,
        String status,
        String uploadedAt,
        Long roadmapId,
        String roadmapTitle,
        int totalChunks,
        DocumentSummaryDTO summary,
        String notes,
        List<DocumentCitationDTO> usedIn
) {}
