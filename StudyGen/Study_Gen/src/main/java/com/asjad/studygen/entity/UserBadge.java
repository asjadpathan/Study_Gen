package com.asjad.studygen.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_badges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "badge_name", nullable = false, length = 100)
    private String badgeName;

    @Column(name = "badge_description", length = 255)
    private String badgeDescription;

    @Column(name = "earned_at", nullable = false, updatable = false)
    private LocalDateTime earnedAt;

    @PrePersist
    protected void onCreate() {
        if (this.earnedAt == null) {
            this.earnedAt = LocalDateTime.now();
        }
    }
}
