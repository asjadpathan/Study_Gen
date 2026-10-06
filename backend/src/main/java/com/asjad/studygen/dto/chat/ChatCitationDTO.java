package com.asjad.studygen.dto.chat;

public record ChatCitationDTO(
        Long id,
        String title,
        String source,
        String snippet,
        String link
) {}
