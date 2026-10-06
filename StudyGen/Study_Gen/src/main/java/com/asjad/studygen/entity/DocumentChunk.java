package com.asjad.studygen.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "document_chunks")
@Getter
@Setter
@NoArgsConstructor
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private UploadedDocument document;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "content_text", nullable = false, columnDefinition = "TEXT")
    private String contentText;

    public DocumentChunk(UploadedDocument document, int chunkIndex, String contentText) {
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.contentText = contentText;
    }
}
