package com.asjad.studygen.service;

import com.asjad.studygen.dto.document.StudyGuideResponse;
import com.asjad.studygen.entity.Concept;
import com.asjad.studygen.entity.Module;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.repository.ConceptRepository;
import com.asjad.studygen.repository.ModuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyGuideService {

    private final ModuleRepository moduleRepository;
    private final ConceptRepository conceptRepository;
    private final ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;

    @PostConstruct
    void init() {
        this.chatClient = chatClientBuilder.build();
    }

    @Transactional(readOnly = true)
    public StudyGuideResponse generateStudyGuide(User user, Long moduleId) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleId));

        List<Concept> concepts = conceptRepository.findByModuleId(moduleId);

        StringBuilder conceptsInfo = new StringBuilder();
        for (Concept c : concepts) {
            conceptsInfo.append("### ").append(c.getTitle()).append("\n")
                    .append(c.getContentBody()).append("\n\n");
            if (c.getSimplifiedRemediationBody() != null) {
                conceptsInfo.append("> Intuitive Analogy: ").append(c.getSimplifiedRemediationBody()).append("\n\n");
            }
        }

        String prompt = """
                Generate a comprehensive, beautifully structured Markdown study guide for the learning module: "%s".
                Description: %s

                Module Content:
                %s

                Include:
                1. Executive Summary & Core Objectives
                2. In-depth Concept Breakdowns with clear headings and bullet points
                3. Code/Practical Architecture Examples (if applicable)
                4. Common Pitfalls & Mistakes to Avoid
                5. Quick Review Checklist
                """.formatted(module.getTitle(), module.getDescription(), conceptsInfo.toString());

        String markdownContent = null;
        try {
            markdownContent = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("AI study guide generation failed for module {}: {}. Generating structured guide from concepts.",
                    module.getTitle(), e.getMessage());
        }

        if (markdownContent == null || markdownContent.isBlank()) {
            markdownContent = "# " + module.getTitle() + " — Comprehensive Study Guide\n\n" +
                    "## Executive Summary & Core Objectives\n" +
                    module.getDescription() + "\n\n" +
                    "## Detailed Concept & Architectural Breakdown\n" +
                    conceptsInfo +
                    "\n## Common Pitfalls & Anti-Patterns\n" +
                    "- Neglecting boundary validation and defensive input sanitization.\n" +
                    "- Unchecked exceptions and resource leaks across asynchronous contexts.\n" +
                    "- Premature micro-optimization before establishing deterministic behavior.\n\n" +
                    "## Quick Review Checklist\n" +
                    "- [x] Understood core domain invariants and state models.\n" +
                    "- [x] Reviewed practical implementation patterns and code snippets.\n" +
                    "- [x] Aware of edge cases, latency constraints, and fault recovery.";
        }

        List<String> keyTakeaways = new ArrayList<>();
        keyTakeaways.add("Master the foundational principles of " + module.getTitle());
        for (Concept c : concepts) {
            keyTakeaways.add("Understand " + c.getTitle() + " and its applications.");
        }

        return new StudyGuideResponse(
                module.getId(),
                module.getTitle(),
                markdownContent,
                keyTakeaways
        );
    }
}
