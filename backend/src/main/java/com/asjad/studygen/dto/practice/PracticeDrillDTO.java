package com.asjad.studygen.dto.practice;

import com.asjad.studygen.dto.assessment.QuestionResponse;
import java.util.List;

public record PracticeDrillDTO(
        Long moduleId,
        String moduleTitle,
        List<FlashcardDTO> flashcards,
        List<QuestionResponse> practiceQuestions
) {}
