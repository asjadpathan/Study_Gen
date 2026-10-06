package com.asjad.studygen.dto.chat;

import java.util.List;

public record ChatMessageItemDTO(
        Long id,
        String role,
        String content,
        List<ChatCitationDTO> citations,
        String createdAt
) {}
