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
}
