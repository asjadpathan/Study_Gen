package com.asjad.studygen.service;

import com.asjad.studygen.dto.assessment.QuestionResponse;
import com.asjad.studygen.dto.practice.ConceptReviewRequest;
import com.asjad.studygen.dto.practice.ConceptReviewResponse;
import com.asjad.studygen.dto.practice.FlashcardDTO;
import com.asjad.studygen.dto.practice.PracticeDrillDTO;
import com.asjad.studygen.entity.*;
import com.asjad.studygen.entity.Module;
import com.asjad.studygen.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PracticeService {

    private final ModuleRepository moduleRepository;
    private final ConceptRepository conceptRepository;
    private final AssessmentRepository assessmentRepository;
    private final UserConceptReviewRepository reviewRepository;
    private final UserActivityService userActivityService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public PracticeDrillDTO getDrillsForModule(User user, Long moduleId) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleId));

        List<Concept> concepts = conceptRepository.findByModuleId(moduleId);
        List<FlashcardDTO> flashcards = new ArrayList<>();

        if (concepts.isEmpty()) {
            flashcards.add(new FlashcardDTO(
                    1L,
                    module.getTitle(),
                    module.getDescription(),
                    "Key foundational takeaway"
            ));
        } else {
            for (Concept c : concepts) {
                flashcards.add(new FlashcardDTO(
                        c.getId(),
                        c.getTitle(),
                        c.getContentBody(),
                        c.getSimplifiedRemediationBody()
                ));
            }
        }

        List<QuestionResponse> questions = new ArrayList<>();
        var assessmentOpt = assessmentRepository.findFirstByModuleIdAndType(moduleId, AssessmentType.MODULE_GATE);
        if (assessmentOpt.isPresent()) {
            for (AssessmentQuestion q : assessmentOpt.get().getQuestions()) {
                List<String> opts;
                try {
                    opts = objectMapper.readValue(q.getOptionsJson(), new TypeReference<>() {});
                } catch (Exception e) {
                    opts = new ArrayList<>();
                }
                questions.add(new QuestionResponse(q.getId(), q.getQuestionText(), opts));
            }
        }

        return new PracticeDrillDTO(module.getId(), module.getTitle(), flashcards, questions);
    }

    /**
     * SuperMemo-2 (SM-2) Spaced Repetition Algorithm Implementation.
     */
    @Transactional
    public ConceptReviewResponse recordReview(User user, ConceptReviewRequest request) {
        Concept concept = conceptRepository.findById(request.conceptId())
                .orElseThrow(() -> new IllegalArgumentException("Concept not found: " + request.conceptId()));

        UserConceptReview review = reviewRepository.findByUserIdAndConceptId(user.getId(), concept.getId())
                .orElseGet(() -> new UserConceptReview(concept, user));

        int q = request.qualityRating();
        int n = review.getRepetitionNumber();
        int interval = review.getIntervalDays();
        double ef = review.getEasinessFactor();

        if (q < 3) {
            // Failure: reset repetitions and interval
            n = 0;
            interval = 1;
        } else {
            // Successful recall
            if (n == 0) {
                interval = 1;
            } else if (n == 1) {
                interval = 6;
            } else {
                interval = (int) Math.round(interval * ef);
            }
            n++;
        }

        // Update Easiness Factor (EF)
        ef = ef + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
        if (ef < 1.3) {
            ef = 1.3;
        }

        review.setRepetitionNumber(n);
        review.setIntervalDays(interval);
        review.setEasinessFactor(Math.round(ef * 100.0) / 100.0);
        LocalDate nextDate = LocalDate.now().plusDays(interval);
        review.setNextReviewDate(nextDate);
        review.setLastReviewedAt(LocalDateTime.now());

        UserConceptReview saved = reviewRepository.save(review);
        userActivityService.logActivity(user, "PRACTICE_REVIEW", 5);

        return new ConceptReviewResponse(
                concept.getId(),
                saved.getRepetitionNumber(),
                saved.getIntervalDays(),
                saved.getEasinessFactor(),
                saved.getNextReviewDate().toString()
        );
    }
}
