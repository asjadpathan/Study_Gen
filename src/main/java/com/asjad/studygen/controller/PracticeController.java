package com.asjad.studygen.controller;

import com.asjad.studygen.dto.practice.ConceptReviewRequest;
import com.asjad.studygen.dto.practice.ConceptReviewResponse;
import com.asjad.studygen.dto.practice.PracticeDrillDTO;
import com.asjad.studygen.entity.User;
import com.asjad.studygen.service.PracticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    @GetMapping("/modules/{moduleId}/drills")
    public ResponseEntity<PracticeDrillDTO> getModuleDrills(
            @AuthenticationPrincipal User user,
            @PathVariable Long moduleId) {

        return ResponseEntity.ok(practiceService.getDrillsForModule(user, moduleId));
    }

    @PostMapping("/review")
    public ResponseEntity<ConceptReviewResponse> recordReview(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid ConceptReviewRequest request) {

        return ResponseEntity.ok(practiceService.recordReview(user, request));
    }
}
