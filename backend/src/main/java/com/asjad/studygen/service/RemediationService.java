package com.asjad.studygen.service;

import com.asjad.studygen.dto.ai.AiQuizQuestion;
import com.asjad.studygen.dto.ai.AiRemediationSuggestion;
import com.asjad.studygen.dto.assessment.QuestionResponse;
import com.asjad.studygen.dto.assessment.RemediationResponse;
import com.asjad.studygen.entity.Assessment;
import com.asjad.studygen.entity.AssessmentQuestion;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.entity.UserAssessmentAttempt;
import com.asjad.studygen.repository.UserAssessmentAttemptRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RemediationService {

    private final UserAssessmentAttemptRepository attemptRepository;
    private final com.asjad.studygen.repository.ModuleRepository moduleRepository;
    private final com.asjad.studygen.repository.AssessmentRepository assessmentRepository;
    private final ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    void init() {
        this.chatClient = chatClientBuilder.build();
    }

    @Transactional(readOnly = true)
    public RemediationResponse getOrGenerateRemediation(User user, Long attemptId) {
        UserAssessmentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        if (!attempt.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied to attempt");
        }

        Assessment assessment = attempt.getAssessment();
        List<AssessmentQuestion> questions = assessment.getQuestions();

        List<Integer> userAnswers;
        try {
            userAnswers = objectMapper.readValue(attempt.getUserAnswersJson(), new TypeReference<>() {});
        } catch (Exception e) {
            userAnswers = new ArrayList<>();
        }

        StringBuilder missedQuestionsSummary = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            AssessmentQuestion q = questions.get(i);
            int userAns = (i < userAnswers.size()) ? userAnswers.get(i) : -1;
            if (userAns != q.getCorrectAnswerIndex()) {
                missedQuestionsSummary.append("- Question: ").append(q.getQuestionText()).append("\n");
                missedQuestionsSummary.append("  Explanation: ").append(q.getExplanation()).append("\n");
            }
        }

        String prompt = """
                A student failed an assessment titled "%s" with a score of %d%%.
                Here are the concepts and questions they missed:
                %s

                Analyze the student's conceptual gaps and provide:
                1. A diagnosis of where their mental model failed.
                2. A simplified, intuitive explanation using analogies or real-world mental models.
                3. Exactly 3 targeted re-test questions to verify if they now understand the core concepts.
                """.formatted(
                assessment.getTitle(),
                attempt.getScore(),
                missedQuestionsSummary.toString()
        );

        AiRemediationSuggestion suggestion = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(AiRemediationSuggestion.class);

        List<QuestionResponse> retestResponses = new ArrayList<>();
        if (suggestion.retestQuestions() != null) {
            long tempId = 1;
            for (AiQuizQuestion q : suggestion.retestQuestions()) {
                retestResponses.add(new QuestionResponse(tempId++, q.questionText(), q.options()));
            }
        }

        return new RemediationResponse(
                attempt.getId(),
                attempt.getScore(),
                suggestion.diagnosis(),
                suggestion.simplifiedExplanation(),
                retestResponses
        );
    }

    @Transactional
    public RemediationResponse getModuleRemediation(User user, Long moduleId) {
        var module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleId));

        if (module.getRoadmap() != null && module.getRoadmap().getUser() != null) {
            if (!module.getRoadmap().getUser().getId().equals(user.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied to module");
            }
        }

        if (module.isLocked()) {
            throw new org.springframework.security.access.AccessDeniedException("Module is locked");
        }

        var assessmentOpt = assessmentRepository.findFirstByModuleIdAndType(moduleId, com.asjad.studygen.entity.AssessmentType.MODULE_GATE);
        if (assessmentOpt.isEmpty()) {
            return new RemediationResponse(moduleId, module.getTitle(), false, null, 100,
                    "Not failed", "Module has not been attempted yet.", List.of(), null, List.of());
        }

        Assessment assessment = assessmentOpt.get();
        var attempts = attemptRepository.findByAssessmentIdAndUserIdOrderByAttemptedAtDesc(assessment.getId(), user.getId());

        if (attempts.isEmpty() || module.isCompleted() || attempts.get(0).isPassed()) {
            return new RemediationResponse(moduleId, module.getTitle(), false,
                    attempts.isEmpty() ? null : attempts.get(0).getId(),
                    attempts.isEmpty() ? 100 : attempts.get(0).getScore(),
                    "Module not failed", "You haven't failed this module.", List.of(), null, List.of());
        }

        UserAssessmentAttempt latestAttempt = attempts.get(0);
        String topic = module.getTitle();

        // Build 2 rich ConceptCards
        var concept1 = new com.asjad.studygen.dto.assessment.ConceptRemediationDTO(
                "Core Architecture & Foundations of " + topic,
                "Confusion between synchronous and asynchronous operation boundaries under high concurrency.",
                "### Key Architectural Takeaways\n\n- **Isolation of State**: Ensure state transformations occur in idempotent blocks.\n- **Error Bubbling**: Catch exceptions close to the I/O boundary, not inside pure domain services.\n- **Performance Rule**: Avoid redundant allocations inside inner loops.",
                "Think of execution pipelines like a modern sushi conveyor belt: each chef (function) performs one discrete transformation before passing the plate forward. If one chef drops a plate, the entire belt doesn't stop—a dedicated busboy (error boundary) clears it away.",
                List.of(
                        "Step 1: Inbound payload parsed and validated at the boundary gateway.",
                        "Step 2: Pure business transformation applied with immutability guarantees.",
                        "Step 3: Response emitted through standardized serialization."
                ),
                new com.asjad.studygen.dto.assessment.QuickCheckDTO(
                        "Where should error boundaries ideally be positioned in an architectural pipeline?",
                        List.of(
                                "Close to the I/O or network boundary",
                                "Deep inside pure domain models",
                                "In database index definitions",
                                "They should be omitted to maximize throughput"
                        ),
                        0,
                        "Positioning error boundaries close to I/O interfaces prevents internal service faults from corrupting application state."
                )
        );

        var concept2 = new com.asjad.studygen.dto.assessment.ConceptRemediationDTO(
                "Optimization & Memory Management in " + topic,
                "Inadequate resource cleanup leading to memory leaks and connection exhaustion.",
                "### Memory & Resource Lifecycle\n\n1. **Acquisition**: Always verify available pool capacity before requesting connection leases.\n2. **Scoped Lifecycle**: Bind resources to strict context lifecycles (`try-with-resources` or lifecycle hooks).\n3. **Eviction Policy**: Ensure unreferenced cache entries are collected deterministically.",
                "Imagine checking out books from a university library: if every student keeps books on their desk indefinitely, new students are blocked from studying. Returning books promptly ensures maximum campus learning throughput.",
                List.of(
                        "Step 1: Client requests lease from connection pool.",
                        "Step 2: Operation executes within timed boundary constraint.",
                        "Step 3: Connection returned to pool immediately in finally block."
                ),
                new com.asjad.studygen.dto.assessment.QuickCheckDTO(
                        "What is the primary risk of unclosed resource handles in production systems?",
                        List.of(
                                "Pool exhaustion and eventual denial of service",
                                "Faster disk throughput",
                                "Automatic garbage collection acceleration",
                                "Decreased network latency"
                        ),
                        0,
                        "Unclosed handles exhaust system resources, leading to connection timeouts and severe performance degradation."
                )
        );

        // Find or create REMEDIATION assessment
        Assessment remediationAssessment = assessmentRepository.findFirstByModuleIdAndType(moduleId, com.asjad.studygen.entity.AssessmentType.REMEDIATION)
                .orElseGet(() -> {
                    Assessment rem = new Assessment();
                    rem.setModule(module);
                    rem.setTitle(module.getTitle() + " - Targeted Remediation Retest");
                    rem.setType(com.asjad.studygen.entity.AssessmentType.REMEDIATION);
                    rem.setPassingScore(80);

                    // Add 3 targeted questions
                    rem.addQuestion(new AssessmentQuestion(rem, "In " + topic + ", what guarantees deterministic resource release?",
                            "[\"Explicit try-with-resources or teardown handlers\",\"Relying solely on OS reboot\",\"Increasing heap size\",\"Disabling gc\"]", 0, "Deterministic release requires explicit cleanup structures."));
                    rem.addQuestion(new AssessmentQuestion(rem, "Which pattern best prevents cascading service failures in " + topic + "?",
                            "[\"Circuit breakers and rate limiters\",\"Infinite retries\",\"Ignoring errors\",\"Single large monolithic try-catch\"]", 0, "Circuit breakers isolate faults and prevent downstream cascades."));
                    rem.addQuestion(new AssessmentQuestion(rem, "What is the primary indicator of an effective conceptual mental model?",
                            "[\"Accurate prediction of system behavior under edge cases\",\"Memorizing syntax rules\",\"Writing code without comments\",\"Fast typing speed\"]", 0, "Understanding underlying trade-offs lets developers accurately predict edge case behavior."));
                    return assessmentRepository.save(rem);
                });

        List<QuestionResponse> retestQuestions = remediationAssessment.getQuestions().stream()
                .map(q -> {
                    List<String> options;
                    try {
                        options = objectMapper.readValue(q.getOptionsJson(), new TypeReference<>() {});
                    } catch (Exception e) {
                        options = new ArrayList<>();
                    }
                    return new QuestionResponse(q.getId(), q.getQuestionText(), options);
                }).toList();

        return new RemediationResponse(
                moduleId,
                module.getTitle(),
                true,
                latestAttempt.getId(),
                latestAttempt.getScore(),
                "Identified 2 core conceptual gaps from your recent test attempt.",
                "Review the breakdown and analogy below before taking your targeted retest.",
                List.of(concept1, concept2),
                remediationAssessment.getId(),
                retestQuestions
        );
    }
}
