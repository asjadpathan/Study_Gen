package com.asjad.studygen.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_chat_messages")
@Getter
@Setter
@NoArgsConstructor
public class StudyChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "module_id")
    private Long moduleId;

    @Column(nullable = false, length = 20)
    private String sender; // USER or ASSISTANT

    @Column(name = "message_text", nullable = false, columnDefinition = "TEXT")
    private String messageText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public StudyChatMessage(User user, Long moduleId, String sender, String messageText) {
        this.user = user;
        this.moduleId = moduleId;
        this.sender = sender;
        this.messageText = messageText;
        this.createdAt = LocalDateTime.now();
    }
}
