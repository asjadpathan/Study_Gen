package com.asjad.studygen.entity;

import jakarta.persistence.*;
import lombok.*;



@Entity
@Table(name = "modules")
@Getter
@Setter
@NoArgsConstructor
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roadmap_id", nullable = false)
    private Roadmap roadmap;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder;

    @Column(name = "is_locked", nullable = false)
    private boolean locked = true;

    @Column(name = "is_completed", nullable = false)
    private boolean completed = false;

    @Column(name = "mastery_score", nullable = false)
    private double masteryScore = 0.0;
}