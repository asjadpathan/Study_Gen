package com.asjad.studygen.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "resources")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "why_fits_you", columnDefinition = "TEXT")
    private String whyFitsYou;

    private String difficulty; // BEGINNER, INTERMEDIATE, ADVANCED

    @Column(name = "source_label")
    private String sourceLabel; // "AI suggested" or "Curated"

    private String confidence; // "96% Match", "Verified", etc.

    @Column(columnDefinition = "TEXT")
    private String url;

    @Column(name = "search_query")
    private String searchQuery;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
