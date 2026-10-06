package com.asjad.studygen.service;

import com.asjad.studygen.dto.assessment.QuestionResponse;
import com.asjad.studygen.dto.practice.*;
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
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PracticeService {

    private final RoadmapRepository roadmapRepository;
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
                    module.getDescription() != null ? module.getDescription() : "Key foundational takeaway",
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
     * Spaced Repetition (SM-2): Retrieves due cards, counts, breakdown, and 7-day schedule.
     */
    @Transactional
    public PracticeDueDetailsResponse getPracticeDueDetails(User user) {
        LocalDate today = LocalDate.now();
        List<Roadmap> roadmaps = roadmapRepository.findByUserId(user.getId());

        List<PracticeCardDTO> dueCards = new ArrayList<>();
        Map<String, RoadmapModuleBreakdown> breakdownMap = new LinkedHashMap<>();
        int[] dailyCounts = new int[7];
        int totalCards = 0;

        for (Roadmap roadmap : roadmaps) {
            List<Module> modules = moduleRepository.findByRoadmapIdOrderBySequenceOrderAsc(roadmap.getId());
            for (Module module : modules) {
                List<Concept> concepts = conceptRepository.findByModuleId(module.getId());
                if (concepts.isEmpty()) {
                    Concept foundational = new Concept(
                            module,
                            module.getTitle() + " - Core Principles",
                            module.getDescription() != null && !module.getDescription().isBlank()
                                    ? module.getDescription()
                                    : "Fundamental principles, definitions, and application of " + module.getTitle(),
                            "Key mechanism and architectural pattern"
                    );
                    foundational = conceptRepository.save(foundational);
                    concepts = List.of(foundational);
                }

                for (Concept concept : concepts) {
                    totalCards++;

                    UserConceptReview review = reviewRepository.findByUserIdAndConceptId(user.getId(), concept.getId())
                            .orElseGet(() -> {
                                UserConceptReview newRev = new UserConceptReview(concept, user);
                                return reviewRepository.save(newRev);
                            });

                    LocalDate reviewDate = review.getNextReviewDate();
                    boolean isDue = reviewDate.isBefore(today.plusDays(1));

                    if (isDue) {
                        PracticeCardDTO card = new PracticeCardDTO(
                                concept.getId(),
                                concept.getId(),
                                module.getId(),
                                module.getTitle(),
                                roadmap.getId(),
                                roadmap.getTitle(),
                                concept.getTitle(),
                                concept.getContentBody(),
                                concept.getSimplifiedRemediationBody() != null && !concept.getSimplifiedRemediationBody().isBlank()
                                        ? concept.getSimplifiedRemediationBody()
                                        : "Focus on the foundational mechanics, core syntax, or essential pattern.",
                                review.getRepetitionNumber(),
                                review.getIntervalDays(),
                                review.getEasinessFactor(),
                                reviewDate.toString()
                        );
                        dueCards.add(card);

                        String key = roadmap.getId() + "_" + module.getId();
                        RoadmapModuleBreakdown curr = breakdownMap.getOrDefault(key,
                                new RoadmapModuleBreakdown(roadmap.getId(), roadmap.getTitle(), module.getId(), module.getTitle(), 0));
                        breakdownMap.put(key, new RoadmapModuleBreakdown(
                                curr.roadmapId(), curr.roadmapTitle(), curr.moduleId(), curr.moduleTitle(), curr.dueCount() + 1
                        ));
                    }

                    // 7-day schedule distribution
                    if (isDue) {
                        dailyCounts[0]++;
                    } else {
                        long daysDiff = ChronoUnit.DAYS.between(today, reviewDate);
                        if (daysDiff >= 0 && daysDiff < 7) {
                            dailyCounts[(int) daysDiff]++;
                        }
                    }
                }
            }
        }

        List<UpcomingDaySchedule> upcomingSchedule = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            String dayName = date.getDayOfWeek().toString().substring(0, 3);
            dayName = dayName.charAt(0) + dayName.substring(1).toLowerCase();
            upcomingSchedule.add(new UpcomingDaySchedule(date.toString(), dayName, dailyCounts[i]));
        }

        int streak = user.getCurrentStreak() != null ? user.getCurrentStreak() : 0;

        return new PracticeDueDetailsResponse(
                dueCards.size(),
                totalCards,
                streak,
                new ArrayList<>(breakdownMap.values()),
                upcomingSchedule,
                dueCards
        );
    }

    /**
     * SuperMemo-2 (SM-2) Spaced Repetition Algorithm Implementation.
     */
    @Transactional
    public ConceptReviewResponse recordReview(User user, ConceptReviewRequest request) {
        Long conceptId = request.getResolvedConceptId();
        if (conceptId == null) {
            throw new IllegalArgumentException("Concept or Card ID is required");
        }

        Concept concept = conceptRepository.findById(conceptId)
                .orElseThrow(() -> new IllegalArgumentException("Concept not found: " + conceptId));

        UserConceptReview review = reviewRepository.findByUserIdAndConceptId(user.getId(), concept.getId())
                .orElseGet(() -> new UserConceptReview(concept, user));

        int q = request.getResolvedQuality();
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
                concept.getId(),
                saved.getRepetitionNumber(),
                saved.getIntervalDays(),
                saved.getEasinessFactor(),
                saved.getNextReviewDate().toString()
        );
    }

    @Transactional(readOnly = true)
    public int getDueCountForUser(User user) {
        List<UserConceptReview> dueList = reviewRepository.findByUserIdAndNextReviewDateLessThanEqual(user.getId(), LocalDate.now());
        return dueList != null ? dueList.size() : 0;
    }
}
