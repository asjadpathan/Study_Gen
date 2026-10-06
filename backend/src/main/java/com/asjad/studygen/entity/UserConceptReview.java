package com.asjad.studygen.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_concept_reviews")
@Getter
@Setter
@NoArgsConstructor
public class UserConceptReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_id", nullable = false)
    private Concept concept;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "easiness_factor", nullable = false)
    private double easinessFactor = 2.5;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays = 1;

    @Column(name = "repetition_number", nullable = false)
    private int repetitionNumber = 0;

    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate = LocalDate.now();

    @Column(name = "last_reviewed_at", nullable = false)
    private LocalDateTime lastReviewedAt = LocalDateTime.now();

    public UserConceptReview(Concept concept, User user) {
        this.concept = concept;
        this.user = user;
        this.easinessFactor = 2.5;
        this.intervalDays = 1;
        this.repetitionNumber = 0;
        this.nextReviewDate = LocalDate.now();
        this.lastReviewedAt = LocalDateTime.now();
    }
}
