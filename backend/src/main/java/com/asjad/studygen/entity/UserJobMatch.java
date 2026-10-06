package com.asjad.studygen.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_job_matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserJobMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String jobTitle;

    private String company;

    private String seniority; // Junior, Mid-Level, Senior, Lead

    private int matchPercentage;

    @Column(columnDefinition = "TEXT")
    private String responsibilitiesJson; // List<String> up to 6 bullets

    @Column(columnDefinition = "TEXT")
    private String matchedSkillsJson; // List<String>

    @Column(columnDefinition = "TEXT")
    private String missingSkillsJson; // List<String>

    @Column(columnDefinition = "TEXT")
    private String skillsAnalysisJson; // List<SkillAnalysisItem>

    @Column(columnDefinition = "TEXT")
    private String suggestedModulesJson; // List<SuggestedModuleItem>

    @Column(columnDefinition = "TEXT")
    private String actionableAdvice;

    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    private String sourceUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
