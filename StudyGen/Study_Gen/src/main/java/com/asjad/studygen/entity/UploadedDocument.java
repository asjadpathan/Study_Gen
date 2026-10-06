package com.asjad.studygen.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "uploaded_documents")
@Getter
@Setter
@NoArgsConstructor
public class UploadedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String filename;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "status", nullable = false)
    private String status = "Ready";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roadmap_id")
    private Roadmap roadmap;

    @Column(name = "summary_overview", columnDefinition = "TEXT")
    private String summaryOverview;

    @Column(name = "summary_key_points_json", columnDefinition = "TEXT")
    private String summaryKeyPointsJson;

    @Column(name = "summary_audience")
    private String summaryAudience;

    @Column(name = "notes_markdown", columnDefinition = "TEXT")
    private String notesMarkdown;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentChunk> chunks = new ArrayList<>();

    public UploadedDocument(User user, String filename, String fileType, long fileSize) {
        this.user = user;
        this.filename = filename;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.status = "Ready";
        this.uploadedAt = LocalDateTime.now();
    }

    public void addChunk(DocumentChunk chunk) {
        chunks.add(chunk);
        chunk.setDocument(this);
    }
}
