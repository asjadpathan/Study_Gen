package com.asjad.studygen.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_assessment_attempts")
@Getter
@Setter
@NoArgsConstructor
public class UserAssessmentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private boolean passed;

    @Column(name = "user_answers_json", columnDefinition = "TEXT")
    private String userAnswersJson;

    @Column(name = "remediation_notes", columnDefinition = "TEXT")
    private String remediationNotes;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt = LocalDateTime.now();

    public UserAssessmentAttempt(Assessment assessment, User user, int score, boolean passed, String userAnswersJson) {
        this.assessment = assessment;
        this.user = user;
        this.score = score;
        this.passed = passed;
        this.userAnswersJson = userAnswersJson;
        this.attemptedAt = LocalDateTime.now();
    }
}
