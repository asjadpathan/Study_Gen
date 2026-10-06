package com.asjad.studygen.dto.practice;

public record FlashcardDTO(
        Long conceptId,
        String front,
        String back,
        String analogy
) {}
