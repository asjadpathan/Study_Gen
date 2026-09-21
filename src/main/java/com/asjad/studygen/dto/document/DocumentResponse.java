package com.asjad.studygen.dto.document;

public record DocumentResponse(
        Long id,
        String filename,
        String fileType,
        long fileSize,
        int totalChunks,
        String uploadedAt
) {}
