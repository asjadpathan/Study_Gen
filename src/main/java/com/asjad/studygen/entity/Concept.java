package com.asjad.studygen.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "concepts")
@Getter
@Setter
@NoArgsConstructor
public class Concept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @Column(nullable = false)
    private String title;

    @Column(name = "content_body", nullable = false, columnDefinition = "TEXT")
    private String contentBody;

    @Column(name = "simplified_remediation_body", columnDefinition = "TEXT")
    private String simplifiedRemediationBody;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted = false;

    public Concept(Module module, String title, String contentBody) {
        this.module = module;
        this.title = title;
        this.contentBody = contentBody;
        this.isCompleted = false;
    }

    public Concept(Module module, String title, String contentBody, String simplifiedRemediationBody) {
        this.module = module;
        this.title = title;
        this.contentBody = contentBody;
        this.simplifiedRemediationBody = simplifiedRemediationBody;
        this.isCompleted = false;
    }
}
