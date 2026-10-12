package com.asjad.studygen.dto.roadmap;

import java.util.List;

public record ModuleLearnMoreResponse(
        String title,
        String summary,
        String deepDiveMarkdown,
        String codeSnippet,
        String codeLanguage,
        List<String> realWorldUseCases,
        List<String> bestPractices,
        List<InterviewQnADTO> interviewQuestions
) {}
