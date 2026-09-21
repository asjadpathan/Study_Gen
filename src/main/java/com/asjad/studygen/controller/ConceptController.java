package com.asjad.studygen.controller;

import com.asjad.studygen.dto.roadmap.ConceptResponse;
import com.asjad.studygen.entity.Concept;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.repository.ConceptRepository;
import com.asjad.studygen.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/concepts")
@RequiredArgsConstructor
public class ConceptController {

    private final ConceptRepository conceptRepository;
    private final UserActivityService userActivityService;

    @PostMapping("/{id}/toggle-complete")
    @Transactional
    public ResponseEntity<ConceptResponse> toggleComplete(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        Concept concept = conceptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Concept not found: " + id));

        // Check ownership through module -> roadmap -> user
        if (concept.getModule() != null && concept.getModule().getRoadmap() != null) {
            User owner = concept.getModule().getRoadmap().getUser();
            if (owner != null && !owner.getId().equals(user.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied to concept");
            }
        }

        boolean newStatus = !concept.isCompleted();
        concept.setCompleted(newStatus);
        Concept saved = conceptRepository.save(concept);

        if (newStatus) {
            userActivityService.logActivity(user, "CONCEPT_COMPLETED", 15);
        }

        return ResponseEntity.ok(new ConceptResponse(
                saved.getId(),
                saved.getModule() != null ? saved.getModule().getId() : null,
                saved.getTitle(),
                saved.getContentBody(),
                saved.getSimplifiedRemediationBody(),
                saved.isCompleted()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConceptResponse> getConcept(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        Concept concept = conceptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Concept not found: " + id));

        // Check ownership through module -> roadmap -> user
        if (concept.getModule() != null && concept.getModule().getRoadmap() != null) {
            User owner = concept.getModule().getRoadmap().getUser();
            if (owner != null && !owner.getId().equals(user.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied to concept");
            }
        }

        return ResponseEntity.ok(new ConceptResponse(
                concept.getId(),
                concept.getModule() != null ? concept.getModule().getId() : null,
                concept.getTitle(),
                concept.getContentBody(),
                concept.getSimplifiedRemediationBody(),
                concept.isCompleted()
        ));
    }
}
