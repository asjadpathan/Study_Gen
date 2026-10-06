package com.asjad.studygen.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assessments")
@Getter
@Setter
@NoArgsConstructor
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private Module module;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AssessmentType type;

    @Column(nullable = false)
    private String title;

    @Column(name = "passing_score", nullable = false)
    private int passingScore = 80;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssessmentQuestion> questions = new ArrayList<>();

    public void addQuestion(AssessmentQuestion question) {
        questions.add(question);
        question.setAssessment(this);
    }
}
