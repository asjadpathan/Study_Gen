package com.asjad.studygen.service;

import com.asjad.studygen.dto.ai.AiQuizQuestion;
import com.asjad.studygen.dto.ai.AiQuizSuggestion;
import com.asjad.studygen.dto.assessment.*;
import com.asjad.studygen.entity.*;
import com.asjad.studygen.entity.Module;
import com.asjad.studygen.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final ModuleRepository moduleRepository;
    private final UserAssessmentAttemptRepository attemptRepository;
    private final ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    void init() {
        this.chatClient = chatClientBuilder.build();
    }

    @Transactional
    public AssessmentResponse generateDiagnosticQuiz(User user, DiagnosticQuizRequest request) {
        String prompt = """
                Generate a 5-question baseline diagnostic quiz to assess a student's starting knowledge.
                Target topic: %s
                Claimed prerequisites: %s

                For each question, provide 4 multiple-choice options, specify the 0-based index of the correct answer,
                and provide a short explanation.
                IMPORTANT: Return ONLY raw valid JSON. Do NOT wrap the response in ```json``` markdown tags.
                """.formatted(request.topic(), request.claimedPrerequisites());

        AiQuizSuggestion suggestion;
        try {
            suggestion = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(AiQuizSuggestion.class);
        } catch (Exception e) {
            log.warn("AI diagnostic quiz generation failed: {}. Using fallback.", e.getMessage(), e);
            suggestion = fallbackQuiz(request.topic());
        }

        Assessment assessment = new Assessment();
        assessment.setTitle("Diagnostic: " + request.topic());
        assessment.setType(AssessmentType.DIAGNOSTIC);
        assessment.setPassingScore(80);

        return persistQuizAndMapToResponse(assessment, suggestion);
    }

    @Transactional
    public AssessmentResponse generateModuleAssessment(User user, Long moduleId) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleId));

        // Check if assessment already exists for this module
        var existing = assessmentRepository.findFirstByModuleIdAndType(moduleId, AssessmentType.MODULE_GATE);
        if (existing.isPresent()) {
            return mapToResponse(existing.get());
        }

        String prompt = """
                Generate a 5-question mastery assessment for the following learning module.
                Module title: %s
                Module description: %s

                Create challenging multiple-choice questions assessing understanding of key concepts.
                Provide 4 options per question, the 0-based correct answer index, and an explanation.
                IMPORTANT: Return ONLY raw valid JSON. Do NOT wrap the response in ```json``` markdown tags.
                """.formatted(module.getTitle(), module.getDescription());

        AiQuizSuggestion suggestion;
        try {
            suggestion = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(AiQuizSuggestion.class);
        } catch (Exception e) {
            log.warn("AI module assessment generation failed for module {}: {}. Using fallback.", module.getTitle(), e.getMessage(), e);
            suggestion = fallbackQuiz(module.getTitle());
        }

        Assessment assessment = new Assessment();
        assessment.setModule(module);
        assessment.setTitle(module.getTitle() + " - Mastery Assessment");
        assessment.setType(AssessmentType.MODULE_GATE);
        assessment.setPassingScore(80);

        return persistQuizAndMapToResponse(assessment, suggestion);
    }

    @Transactional(readOnly = true)
    public AssessmentResponse getAssessmentById(User user, Long assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + assessmentId));
        return mapToResponse(assessment);
    }

    @Transactional
    public AssessmentResponse getOrCreateModuleTest(User user, Long moduleId) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleId));

        Assessment assessment = assessmentRepository.findFirstByModuleIdAndType(moduleId, AssessmentType.MODULE_GATE)
                .orElseGet(() -> {
                    Assessment newAssessment = new Assessment();
                    newAssessment.setModule(module);
                    newAssessment.setTitle(module.getTitle() + " - Mastery Assessment");
                    newAssessment.setType(AssessmentType.MODULE_GATE);
                    newAssessment.setPassingScore(80);
                    return persistQuiz(newAssessment, fallbackQuiz(module.getTitle()));
                });

        var attempts = attemptRepository.findByAssessmentIdAndUserIdOrderByAttemptedAtDesc(assessment.getId(), user.getId());

        boolean alreadyPassed = attempts.stream().anyMatch(UserAssessmentAttempt::isPassed) || module.isCompleted();
        Integer bestScore = attempts.stream().map(UserAssessmentAttempt::getScore).max(Integer::compareTo)
                .orElse(module.isCompleted() ? (int) module.getMasteryScore() : null);

        int maxAttempts = 3;
        int failedAttempts = (int) attempts.stream().filter(a -> !a.isPassed()).count();
        int attemptsLeft = Math.max(0, maxAttempts - (failedAttempts % maxAttempts));

        long cooldownSeconds = 0;
        if (!attempts.isEmpty() && !alreadyPassed) {
            UserAssessmentAttempt lastAttempt = attempts.get(0);
            if (!lastAttempt.isPassed() && lastAttempt.getAttemptedAt() != null) {
                long elapsed = java.time.Duration.between(lastAttempt.getAttemptedAt(), java.time.LocalDateTime.now()).toSeconds();
                if (elapsed < 300) {
                    cooldownSeconds = 300 - elapsed;
                }
            }
        }

        List<QuestionResponse> questions = assessment.getQuestions().stream()
                .map(q -> {
                    List<String> options;
                    try {
                        options = objectMapper.readValue(q.getOptionsJson(), new TypeReference<>() {});
                    } catch (Exception e) {
                        options = new ArrayList<>();
                    }
                    return new QuestionResponse(q.getId(), q.getQuestionText(), options);
                }).collect(Collectors.toList());

        return new AssessmentResponse(
                assessment.getId(),
                moduleId,
                assessment.getTitle(),
                assessment.getType().name(),
                assessment.getPassingScore(),
                questions,
                alreadyPassed,
                bestScore,
                attemptsLeft,
                cooldownSeconds,
                module.getTitle()
        );
    }

    private Assessment persistQuiz(Assessment assessment, AiQuizSuggestion suggestion) {
        if (suggestion.questions() != null) {
            for (AiQuizQuestion q : suggestion.questions()) {
                try {
                    String optionsJson = objectMapper.writeValueAsString(q.options());
                    AssessmentQuestion question = new AssessmentQuestion(
                            assessment,
                            q.questionText(),
                            optionsJson,
                            q.correctAnswerIndex(),
                            q.explanation()
                    );
                    assessment.addQuestion(question);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to serialize question options", e);
                }
            }
        }
        return assessmentRepository.save(assessment);
    }

    private AssessmentResponse persistQuizAndMapToResponse(Assessment assessment, AiQuizSuggestion suggestion) {
        Assessment saved = persistQuiz(assessment, suggestion);
        return mapToResponse(saved);
    }

    public AssessmentResponse mapToResponse(Assessment assessment) {
        List<QuestionResponse> questions = assessment.getQuestions().stream()
                .map(q -> {
                    List<String> options;
                    try {
                        options = objectMapper.readValue(q.getOptionsJson(), new TypeReference<>() {});
                    } catch (Exception e) {
                        options = new ArrayList<>();
                    }
                    return new QuestionResponse(q.getId(), q.getQuestionText(), options);
                }).collect(Collectors.toList());

        Long moduleId = assessment.getModule() != null ? assessment.getModule().getId() : null;

        return new AssessmentResponse(
                assessment.getId(),
                moduleId,
                assessment.getTitle(),
                assessment.getType().name(),
                assessment.getPassingScore(),
                questions
        );
    }

    private AiQuizSuggestion fallbackQuiz(String topic) {
        List<AiQuizQuestion> questions = List.of(
                new AiQuizQuestion(
                        "What is the primary purpose of " + topic + "?",
                        List.of("To solve complex problems efficiently", "To manage data storage", "To improve user interfaces", "To handle network requests"),
                        0, "Understanding the core purpose is foundational to mastering " + topic + "."),
                new AiQuizQuestion(
                        "Which of the following is a key concept in " + topic + "?",
                        List.of("Abstraction", "Randomization", "Obfuscation", "Deprecation"),
                        0, "Abstraction is a fundamental principle in most technical domains."),
                new AiQuizQuestion(
                        "What is the best approach to learning " + topic + "?",
                        List.of("Practice with real projects", "Only read documentation", "Memorize syntax", "Skip fundamentals"),
                        0, "Hands-on practice with real projects accelerates learning."),
                new AiQuizQuestion(
                        "When working with " + topic + ", which practice should be avoided?",
                        List.of("Ignoring error handling", "Writing clean code", "Testing regularly", "Reading documentation"),
                        0, "Ignoring error handling leads to fragile, unreliable systems."),
                new AiQuizQuestion(
                        "What distinguishes an advanced practitioner of " + topic + " from a beginner?",
                        List.of("Deep understanding of trade-offs and design decisions", "Faster typing speed", "Using more libraries", "Writing longer code"),
                        0, "Advanced practitioners understand the 'why' behind design decisions.")
        );
        return new AiQuizSuggestion("Diagnostic: " + topic, questions);
    }
}
